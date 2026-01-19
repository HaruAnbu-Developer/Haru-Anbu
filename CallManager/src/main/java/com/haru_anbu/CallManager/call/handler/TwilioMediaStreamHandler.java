package com.haru_anbu.CallManager.call.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru_anbu.CallManager.call.service.CallManagerService;
import com.haru_anbu.CallManager.call.service.CallSessionService;
import com.haru_anbu.CallManager.call.service.S3StorageService;
import com.haru_anbu.CallManager.call.service.VoiceConversationGrpcService;
import com.haru_anbu.CallManager.call.util.AudioConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Twilio Media Stream WebSocket Handler
 * 
 * 흐름:
 * 1. Twilio가 WebSocket 연결 → "connected" 이벤트
 * 2. "start" 이벤트 → AI gRPC 스트림 시작
 * 3. "media" 이벤트 (연속) → mulaw → PCM → AI 전송
 * 4. AI 응답 (별도 스레드) → PCM → mulaw → Twilio 전송
 * 5. "stop" 이벤트 → AI 스트림 종료
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TwilioMediaStreamHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final AudioConverter audioConverter;
    private final VoiceConversationGrpcService voiceGrpcService;
    private final CallSessionService sessionService;
    private final CallManagerService callManagerService;
    private final S3StorageService s3StorageService; // 추가
    
    @Value("${call.s3-storage.enabled:true}")
    private boolean s3StorageEnabled;
    
    @Value("${call.s3-storage.save-chunks:false}")
    private boolean saveAudioChunks; // 실시간 청크 저장 여부
    
    // WebSocket 세션 관리 (streamSid → WebSocketSession)
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    // streamSid → sessionId 매핑
    private final Map<String, String> streamToSession = new ConcurrentHashMap<>();
    
    // sessionId → streamSid 매핑 (AI → Twilio 전송용)
    private final Map<String, String> sessionToStream = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connected: {}", session.getId());
        
        // Query parameter에서 sessionId 추출 (선택적)
        String uri = session.getUri().toString();
        if (uri.contains("sessionId=")) {
            String sessionId = extractSessionId(uri);
            log.info("Pre-assigned sessionId: {}", sessionId);
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode json = objectMapper.readTree(payload);
        
        String event = json.get("event").asText();
        
        switch (event) {
            case "connected":
                handleConnected(session, json);
                break;
            case "start":
                handleStart(session, json);
                break;
            case "media":
                handleMedia(session, json);
                break;
            case "stop":
                handleStop(session, json);
                break;
            default:
                log.warn("Unknown event type: {}", event);
        }
    }
    
    /**
     * 1. Connected 이벤트
     */
    private void handleConnected(WebSocketSession session, JsonNode json) {
        log.info("Twilio connected: protocol={}", json.get("protocol").asText());
    }
    
    /**
     * 2. Start 이벤트 - 스트림 시작
     */
    // handleStart 메서드 수정
    private void handleStart(WebSocketSession session, JsonNode json) throws IOException {
        JsonNode start = json.get("start");
        String streamSid = start.get("streamSid").asText();
        String callSid = start.get("callSid").asText();
        
        log.info("Stream started: streamSid={}, callSid={}", streamSid, callSid);
        
        activeSessions.put(streamSid, session);
        
        try {
            var callSession = sessionService.getSessionByTwilioCallSid(callSid);
            String sessionId = callSession.getSessionId();
            String userId = callSession.getUserId();
            String phoneNumber = callSession.getPhoneNumber();
            
            streamToSession.put(streamSid, sessionId);
            sessionToStream.put(sessionId, streamSid);
            
            log.info("Mapped streamSid {} to sessionId {}", streamSid, sessionId);
            
            // 🔥 개선: 텍스트 콜백 추가
            voiceGrpcService.startVoiceStream(
                sessionId, 
                userId, 
                phoneNumber,
                // AI 음성 → Twilio
                audioData -> sendAudioToTwilio(sessionId, audioData),
                // STT 결과 → DB 저장
                transcript -> saveTranscript(sessionId, "user", transcript),
                // AI 응답 → DB 저장
                aiResponse -> saveTranscript(sessionId, "assistant", aiResponse)
            );
            
            callManagerService.onCallConnected(sessionId, userId, phoneNumber);
            
        } catch (Exception e) {
            log.error("Failed to start AI stream for callSid: {}", callSid, e);
            sendError(session, "Failed to initialize AI conversation");
        }
    }

    /**
     * 🔥 추가: 대화 내용 DB 저장
     */
    private void saveTranscript(String sessionId, String role, String text) {
        try {
            // TODO: 대화 내용을 DB에 저장하는 로직 구현
            log.info("Saving transcript for session {}: [{}] {}", sessionId, role, text);
            // 예: conversationRepository.save(new Conversation(sessionId, role, text));
        } catch (Exception e) {
            log.error("Failed to save transcript", e);
        }
    }
    
    /**
     * 3. Media 이벤트 - 오디오 데이터 수신
     */
    private void handleMedia(WebSocketSession session, JsonNode json) {
        JsonNode media = json.get("media");
        String payload = media.get("payload").asText();
        String track = media.get("track").asText();
        
        // inbound만 처리 (사용자 음성)
        if (!"inbound".equals(track)) {
            return;
        }
        
        String streamSid = json.get("streamSid").asText();
        String sessionId = streamToSession.get(streamSid);
        
        if (sessionId == null) {
            log.warn("No sessionId for streamSid: {}", streamSid);
            return;
        }
        
        try {
            // mulaw Base64 → PCM 16kHz
            byte[] pcmData = audioConverter.twilioToAI(payload);
            
            if (audioConverter.isValidAudio(pcmData, 32)) {
                // 1. AI로 전송
                voiceGrpcService.sendAudioData(sessionId, pcmData);
                
                // 2. S3에 저장 (옵션)
                if (s3StorageEnabled && saveAudioChunks) {
                    long timestamp = System.currentTimeMillis();
                    s3StorageService.saveAudioChunkAsync(
                        sessionId, pcmData, timestamp, "inbound"
                    ).exceptionally(ex -> {
                        log.warn("Failed to save audio chunk to S3: {}", ex.getMessage());
                        return null;
                    });
                }
                
                log.debug("Sent {} bytes to AI for session {}", pcmData.length, sessionId);
            }
            
        } catch (Exception e) {
            log.error("Failed to process media for session: {}", sessionId, e);
        }
    }
    
    /**
     * 4. Stop 이벤트 - 스트림 종료
     */
    private void handleStop(WebSocketSession session, JsonNode json) {
        String streamSid = json.get("streamSid").asText();
        String sessionId = streamToSession.get(streamSid);
        
        log.info("Stream stopped: streamSid={}, sessionId={}", streamSid, sessionId);
        
        if (sessionId != null) {
            // AI 스트림 종료
            voiceGrpcService.endVoiceStream(sessionId);
            
            // 통화 종료 처리
            callManagerService.endCall(sessionId, "stream_ended");
            
            // 매핑 제거
            streamToSession.remove(streamSid);
            sessionToStream.remove(sessionId);
        }
        
        activeSessions.remove(streamSid);
    }
    
    /**
     * AI 음성을 Twilio로 전송
     */
    private void sendAudioToTwilio(String sessionId, byte[] pcmData) {
        String streamSid = sessionToStream.get(sessionId);
        if (streamSid == null) {
            log.warn("No streamSid for sessionId: {}", sessionId);
            return;
        }
        
        WebSocketSession wsSession = activeSessions.get(streamSid);
        if (wsSession == null || !wsSession.isOpen()) {
            log.warn("WebSocket not available for streamSid: {}", streamSid);
            return;
        }
        
        try {
            // S3에 outbound 오디오 저장 (옵션)
            if (s3StorageEnabled && saveAudioChunks) {
                long timestamp = System.currentTimeMillis();
                s3StorageService.saveAudioChunkAsync(
                    sessionId, pcmData, timestamp, "outbound"
                ).exceptionally(ex -> {
                    log.warn("Failed to save outbound audio to S3: {}", ex.getMessage());
                    return null;
                });
            }
            
            // PCM → mulaw Base64
            String base64Mulaw = audioConverter.aiToTwilio(pcmData);
            
            // Twilio Media 메시지 생성
            Map<String, Object> mediaMessage = Map.of(
                "event", "media",
                "streamSid", streamSid,
                "media", Map.of(
                    "payload", base64Mulaw
                )
            );
            
            String json = objectMapper.writeValueAsString(mediaMessage);
            wsSession.sendMessage(new TextMessage(json));
            
            log.debug("Sent {} bytes to Twilio for session {}", pcmData.length, sessionId);
            
        } catch (Exception e) {
            log.error("Failed to send audio to Twilio", e);
        }
    }
    
    /**
     * 에러 메시지 전송
     */
    private void sendError(WebSocketSession session, String error) {
        try {
            Map<String, Object> errorMsg = Map.of(
                "event", "error",
                "message", error
            );
            String json = objectMapper.writeValueAsString(errorMsg);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Failed to send error message", e);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error: {}", session.getId(), exception);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket closed: {} - {}", session.getId(), status);
        
        // 정리 작업
        activeSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
    }
    
    /**
     * URI에서 sessionId 추출
     */
    private String extractSessionId(String uri) {
        int idx = uri.indexOf("sessionId=");
        if (idx == -1) return null;
        
        String sub = uri.substring(idx + 10);
        int end = sub.indexOf("&");
        return end == -1 ? sub : sub.substring(0, end);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
