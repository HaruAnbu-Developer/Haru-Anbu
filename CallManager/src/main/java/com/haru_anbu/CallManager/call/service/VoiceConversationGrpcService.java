package com.haru_anbu.CallManager.call.service;

import com.google.protobuf.ByteString;
import com.haru_anbu.CallManager.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * AI 음성 대화 gRPC 서비스
 * Context Listener 및 Inactivity Watchdog을 통한 메모리 누수 방지 버전
 */
@Service
@Slf4j
public class VoiceConversationGrpcService {

    private final VoiceConversationGrpc.VoiceConversationStub asyncStub;
    private final ManagedChannel channel;

    // 활성 세션 관리 맵
    private final Map<String, StreamObserver<VoiceRequest>> activeStreams = new ConcurrentHashMap<>();
    private final Map<String, Consumer<byte[]>> audioCallbacks = new ConcurrentHashMap<>();
    private final Map<String, Consumer<String>> transcriptCallbacks = new ConcurrentHashMap<>();
    private final Map<String, Consumer<String>> aiResponseCallbacks = new ConcurrentHashMap<>();
    
    // 타임아웃 관리를 위한 활동 기록 맵
    private final Map<String, Long> lastActivityTimes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watchdogExecutor;

    // 설정: 5분 동안 활동 없으면 타임아웃
    private static final long SESSION_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);

    public VoiceConversationGrpcService(ManagedChannel aiServiceChannel) {
        this.channel = aiServiceChannel;
        this.asyncStub = VoiceConversationGrpc.newStub(channel);

        // 1분마다 무활동 세션을 체크하는 데몬 스레드 시작
        this.watchdogExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VoiceStream-Watchdog");
            t.setDaemon(true);
            return t;
        });

        startInactivityCheck();
        log.info("Voice Conversation gRPC Service initialized with Inactivity Watchdog");
    }

    /**
     * 음성 대화 스트림 시작
     */
    public void startVoiceStream(String sessionId, String userId, String phoneNumber,
                                 Consumer<byte[]> audioOutputCallback,
                                 Consumer<String> transcriptCallback,
                                 Consumer<String> aiResponseCallback) {

        // 중복 스트림 확인
        if (activeStreams.containsKey(sessionId)) {
            log.warn("Stream already exists for session: {}", sessionId);
            return;
        }

        // 1. gRPC Context Listener 등록 (물리적 연결 단절/취소 감지)
        io.grpc.Context.current().addListener(context -> {
            if (context.isCancelled()) {
                log.info("gRPC Context cancelled for session: {}. Cleaning up.", sessionId);
                cleanup(sessionId);
            }
        }, Runnable::run);

        log.info("Starting voice stream for session: {}", sessionId);

        // 콜백 및 초기 활동 시간 저장
        audioCallbacks.put(sessionId, audioOutputCallback);
        transcriptCallbacks.put(sessionId, transcriptCallback);
        aiResponseCallbacks.put(sessionId, aiResponseCallback);
        updateActivity(sessionId);

        // 응답 Observer 생성
        StreamObserver<VoiceResponse> responseObserver = new StreamObserver<VoiceResponse>() {
            @Override
            public void onNext(VoiceResponse response) {
                updateActivity(sessionId); // 응답 올 때마다 시간 갱신
                handleVoiceResponse(sessionId, response);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Voice stream error for session: {}", sessionId, t);
                cleanup(sessionId);
            }

            @Override
            public void onCompleted() {
                log.info("Voice stream completed for session: {}", sessionId);
                cleanup(sessionId);
            }
        };

        // 요청 Observer 생성 및 저장
        StreamObserver<VoiceRequest> requestObserver = asyncStub.streamConversation(responseObserver);
        activeStreams.put(sessionId, requestObserver);

        // 초기 설정 전송
        try {
            SessionConfig config = SessionConfig.newBuilder()
                    .setUserId(userId)
                    .setSessionId(sessionId)
                    .setSampleRate(16000)
                    .setLanguageCode("ko-KR")
                    .setPhoneNumber(phoneNumber != null ? phoneNumber : "")
                    .build();

            requestObserver.onNext(VoiceRequest.newBuilder().setConfig(config).build());
        } catch (Exception e) {
            log.error("Failed to send session config for session: {}", sessionId, e);
            requestObserver.onError(e);
            cleanup(sessionId);
        }
    }

    /**
     * 오디오 데이터 전송 및 활동 시간 갱신
     */
    public void sendAudioData(String sessionId, byte[] audioData) {
        StreamObserver<VoiceRequest> stream = activeStreams.get(sessionId);
        if (stream == null) return;

        updateActivity(sessionId); // 데이터 보낼 때마다 시간 갱신
        try {
            stream.onNext(VoiceRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(audioData))
                    .build());
        } catch (Exception e) {
            log.error("Failed to send audio data for session: {}", sessionId, e);
        }
    }

    /**
     * 무활동 세션 감시 로직
     */
    private void startInactivityCheck() {
        watchdogExecutor.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                lastActivityTimes.forEach((sessionId, lastTime) -> {
                    if (now - lastTime > SESSION_TIMEOUT_MS) {
                        log.warn("Session {} timed out (Inactivity for {}s)", 
                                 sessionId, (now - lastTime) / 1000);
                        endVoiceStream(sessionId); 
                    }
                });
            } catch (Exception e) {
                log.error("Error during watchdog execution", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private void updateActivity(String sessionId) {
        lastActivityTimes.put(sessionId, System.currentTimeMillis());
    }

    public void endVoiceStream(String sessionId) {
        StreamObserver<VoiceRequest> stream = activeStreams.get(sessionId);
        if (stream != null) {
            try {
                stream.onCompleted();
            } catch (Exception e) {
                log.warn("Error completing stream for session: {}", sessionId, e);
            }
        }
        cleanup(sessionId);
    }

    private void handleVoiceResponse(String sessionId, VoiceResponse response) {
        try {
            switch (response.getPayloadCase()) {
                case AUDIO_OUTPUT:
                    Consumer<byte[]> audioCb = audioCallbacks.get(sessionId);
                    if (audioCb != null) audioCb.accept(response.getAudioOutput().toByteArray());
                    break;
                case TRANSCRIPT:
                    Consumer<String> transCb = transcriptCallbacks.get(sessionId);
                    if (transCb != null) transCb.accept(response.getTranscript());
                    break;
                case AI_RESPONSE:
                    Consumer<String> aiCb = aiResponseCallbacks.get(sessionId);
                    if (aiCb != null) aiCb.accept(response.getAiResponse());
                    break;
                case IS_FINAL:
                    if (response.getIsFinal()) log.debug("Conversation turn final for: {}", sessionId);
                    break;
                case PAYLOAD_NOT_SET:
                    log.warn("Received empty response for session: {}", sessionId);
                    break;
            }
        } catch (Exception e) {
            log.error("Error in response handling for session: {}", sessionId, e);
        }
    }

    /**
     * 모든 리소스를 안전하게 제거하는 단일 지점
     */
    private synchronized void cleanup(String sessionId) {
        // 중복 정리 방지를 위해 activeStreams에서 제거되었을 때만 진행
        if (activeStreams.remove(sessionId) != null) {
            audioCallbacks.remove(sessionId);
            transcriptCallbacks.remove(sessionId);
            aiResponseCallbacks.remove(sessionId);
            lastActivityTimes.remove(sessionId);
            log.info("Cleaned up all resources for session: {}", sessionId);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Voice Conversation Service");
        watchdogExecutor.shutdownNow();
        activeStreams.keySet().forEach(this::endVoiceStream);
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            channel.shutdownNow();
        }
    }
}
