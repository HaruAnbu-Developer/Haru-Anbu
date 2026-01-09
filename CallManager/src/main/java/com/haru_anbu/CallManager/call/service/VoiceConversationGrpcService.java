package com.haru_anbu.CallManager.call.service;

import com.google.protobuf.ByteString;
import com.haru_anbu.CallManager.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * AI 음성 대화 gRPC 서비스
 * Python AI 서버와 실시간 양방향 음성 스트리밍
 */
@Service
@Slf4j
public class VoiceConversationGrpcService {
    
    private final VoiceConversationGrpc.VoiceConversationStub asyncStub;
    private final ManagedChannel channel;
    
    // 활성 음성 스트림 관리 (sessionId -> StreamObserver)
    private final Map<String, StreamObserver<VoiceRequest>> activeStreams = new ConcurrentHashMap<>();
    
    // 스트림 응답 핸들러 관리
    private final Map<String, VoiceStreamHandler> streamHandlers = new ConcurrentHashMap<>();
    
    public VoiceConversationGrpcService(ManagedChannel aiServiceChannel) {
        this.channel = aiServiceChannel;
        this.asyncStub = VoiceConversationGrpc.newStub(channel);
        log.info("Voice Conversation gRPC Service initialized");
    }
    
    /**
     * 음성 대화 스트림 시작
     */
    public void startVoiceStream(String sessionId, String userId, String phoneNumber, 
                                  VoiceStreamHandler handler) {
        
        log.info("Starting voice stream for session: {}", sessionId);
        
        if (activeStreams.containsKey(sessionId)) {
            log.warn("Stream already exists for session: {}", sessionId);
            return;
        }
        
        // 응답 핸들러 저장
        streamHandlers.put(sessionId, handler);
        
        // 응답 Observer 생성
        StreamObserver<VoiceResponse> responseObserver = new StreamObserver<VoiceResponse>() {
            @Override
            public void onNext(VoiceResponse response) {
                handleVoiceResponse(sessionId, response, handler);
            }
            
            @Override
            public void onError(Throwable t) {
                log.error("Voice stream error for session: {}", sessionId, t);
                handler.onError(t);
                cleanup(sessionId);
            }
            
            @Override
            public void onCompleted() {
                log.info("Voice stream completed for session: {}", sessionId);
                handler.onCompleted();
                cleanup(sessionId);
            }
        };
        
        // 요청 Observer 생성 (양방향 스트림)
        StreamObserver<VoiceRequest> requestObserver = asyncStub.streamConversation(responseObserver);
        activeStreams.put(sessionId, requestObserver);
        
        // 세션 설정 전송 (첫 메시지)
        try {
            SessionConfig config = SessionConfig.newBuilder()
                .setUserId(userId)
                .setSessionId(sessionId)
                .setSampleRate(16000)  // 16kHz 권장
                .setLanguageCode("ko-KR")
                .setPhoneNumber(phoneNumber != null ? phoneNumber : "")
                .build();
            
            VoiceRequest initRequest = VoiceRequest.newBuilder()
                .setConfig(config)
                .build();
            
            requestObserver.onNext(initRequest);
            log.info("Session config sent for session: {}", sessionId);
            
            handler.onSessionStarted(sessionId);
            
        } catch (Exception e) {
            log.error("Failed to send session config", e);
            requestObserver.onError(e);
            cleanup(sessionId);
        }
    }
    
    /**
     * 오디오 데이터 전송 (Twilio -> AI)
     */
    public void sendAudioData(String sessionId, byte[] audioData) {
        StreamObserver<VoiceRequest> stream = activeStreams.get(sessionId);
        
        if (stream == null) {
            log.warn("No active stream for session: {}", sessionId);
            return;
        }
        
        try {
            VoiceRequest audioRequest = VoiceRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(audioData))
                .build();
            
            stream.onNext(audioRequest);
            log.debug("Sent {} bytes of audio for session: {}", audioData.length, sessionId);
            
        } catch (Exception e) {
            log.error("Failed to send audio data for session: {}", sessionId, e);
        }
    }
    
    /**
     * 음성 스트림 종료
     */
    public void endVoiceStream(String sessionId) {
        log.info("Ending voice stream for session: {}", sessionId);
        
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
    
    /**
     * AI 응답 처리
     */
    private void handleVoiceResponse(String sessionId, VoiceResponse response, VoiceStreamHandler handler) {
        try {
            switch (response.getPayloadCase()) {
                case AUDIO_OUTPUT:
                    // AI가 생성한 음성 데이터
                    byte[] audioOutput = response.getAudioOutput().toByteArray();
                    log.debug("Received {} bytes of audio output for session: {}", audioOutput.length, sessionId);
                    handler.onAudioOutput(audioOutput);
                    break;
                    
                case TRANSCRIPT:
                    // 사용자 음성의 텍스트 변환 결과 (STT)
                    String transcript = response.getTranscript();
                    log.info("User transcript for session {}: {}", sessionId, transcript);
                    handler.onTranscript(transcript);
                    break;
                    
                case AI_RESPONSE:
                    // AI의 텍스트 응답
                    String aiResponse = response.getAiResponse();
                    log.info("AI response for session {}: {}", sessionId, aiResponse);
                    handler.onAIResponse(aiResponse);
                    break;
                    
                case IS_FINAL:
                    // 대화 턴 종료 신호
                    boolean isFinal = response.getIsFinal();
                    log.debug("Is final flag for session {}: {}", sessionId, isFinal);
                    handler.onTurnComplete(isFinal);
                    break;
                    
                case PAYLOAD_NOT_SET:
                    log.warn("Received empty response for session: {}", sessionId);
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling voice response for session: {}", sessionId, e);
        }
    }
    
    /**
     * 세션 정리
     */
    private void cleanup(String sessionId) {
        activeStreams.remove(sessionId);
        streamHandlers.remove(sessionId);
        log.debug("Cleaned up session: {}", sessionId);
    }
    
    /**
     * 활성 스트림 확인
     */
    public boolean isStreamActive(String sessionId) {
        return activeStreams.containsKey(sessionId);
    }
    
    /**
     * 활성 스트림 수
     */
    public int getActiveStreamCount() {
        return activeStreams.size();
    }
    
    /**
     * 음성 스트림 이벤트 핸들러 인터페이스
     */
    public interface VoiceStreamHandler {
        void onSessionStarted(String sessionId);
        void onAudioOutput(byte[] audioData);
        void onTranscript(String text);
        void onAIResponse(String text);
        void onTurnComplete(boolean isFinal);
        void onError(Throwable t);
        void onCompleted();
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Voice Conversation gRPC service");
        
        // 모든 활성 스트림 종료
        activeStreams.forEach((sessionId, stream) -> {
            try {
                stream.onCompleted();
            } catch (Exception e) {
                log.warn("Error closing stream for session: {}", sessionId, e);
            }
        });
        
        activeStreams.clear();
        streamHandlers.clear();
        
        // 채널 종료
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error shutting down gRPC channel", e);
            channel.shutdownNow();
        }
    }
}
