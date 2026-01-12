package com.haru_anbu.CallManager.call.service;

import com.haru_anbu.CallManager.call.entity.CallSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Twilio와 AI gRPC 서비스를 통합하여 통화를 관리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallManagerService {
    
    private final TwilioService twilioService;
    private final CallSessionService sessionService;
    private final VoiceConversationGrpcService voiceGrpcService;
    
    /**
     * 통화 시작 (Twilio + AI 세션 생성)
     */
    @Transactional
    public CallSession initiateCall(String userId, String phoneNumber, Map<String, String> metadata) {
        log.info("Initiating call for user {} to {}", userId, phoneNumber);
        
        try {
            // 1. Twilio 통화 시작
            String twilioCallSid = twilioService.initiateCall(phoneNumber, userId);
            
            // 2. DB에 세션 생성
            CallSession session = sessionService.createSession(userId, phoneNumber, twilioCallSid, metadata);
            
            log.info("Call initiated successfully: session={}, twilioSid={}", 
                session.getSessionId(), twilioCallSid);
            
            return session;
            
        } catch (Exception e) {
            log.error("Failed to initiate call", e);
            throw new RuntimeException("Failed to initiate call", e);
        }
    }
    
    /**
     * 통화 연결됨 - AI 음성 스트림 시작
     * WebSocket Handler에서 호출됨
     */
    @Transactional
    public void onCallConnected(String sessionId, String userId, String phoneNumber) {
        log.info("Call connected: {}", sessionId);
        
        try {
            // 세션 상태 업데이트
            sessionService.updateSessionStatus(sessionId, CallSession.CallStatus.IN_PROGRESS);
            
            log.info("Session {} status updated to IN_PROGRESS", sessionId);
            
        } catch (Exception e) {
            log.error("Error on call connected", e);
            sessionService.updateSessionStatus(sessionId, CallSession.CallStatus.FAILED);
        }
    }
    
    /**
     * 통화 종료
     */
    @Transactional
    public void endCall(String sessionId, String reason) {
        log.info("Ending call: {} - {}", sessionId, reason);
        
        try {
            // AI 음성 스트림 종료
            voiceGrpcService.endVoiceStream(sessionId);
            
            // DB 업데이트
            sessionService.endSession(sessionId, reason, "대화 요약 생성 중...");
            
            log.info("Call ended successfully: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Error ending call: {}", sessionId, e);
            // 오류가 있어도 세션은 종료 처리
            sessionService.updateSessionStatus(sessionId, CallSession.CallStatus.FAILED);
        }
    }
    
    /**
     * 통화 상태 업데이트 (Twilio webhook에서 호출)
     */
    @Transactional
    public void updateCallStatus(String twilioCallSid, String status) {
        log.info("Updating call status: {} -> {}", twilioCallSid, status);
        
        try {
            CallSession session = sessionService.getSessionByTwilioCallSid(twilioCallSid);
            CallSession.CallStatus newStatus = mapTwilioStatus(status);
            
            sessionService.updateSessionStatus(session.getSessionId(), newStatus);
            
            // 통화 종료 상태인 경우 AI 세션도 종료
            if (isEndStatus(newStatus)) {
                voiceGrpcService.endVoiceStream(session.getSessionId());
            }
            
        } catch (Exception e) {
            log.error("Failed to update call status", e);
        }
    }
    
    /**
     * 녹음 URL 업데이트
     */
    @Transactional
    public void updateRecording(String twilioCallSid, String recordingUrl) {
        log.info("Updating recording URL for call: {}", twilioCallSid);
        
        try {
            CallSession session = sessionService.getSessionByTwilioCallSid(twilioCallSid);
            sessionService.updateRecordingUrl(session.getSessionId(), recordingUrl);
        } catch (Exception e) {
            log.error("Failed to update recording URL", e);
        }
    }
    
    /**
     * 텍스트 처리 (테스트용)
     */
    public String processUserInput(String sessionId, String text) {
        log.info("Processing user input for session: {}", sessionId);
        // 이 기능은 실제 음성 스트림에서는 사용하지 않음
        return "Text processing not available in voice stream mode";
    }
    
    // Helper methods
    
    private CallSession.CallStatus mapTwilioStatus(String twilioStatus) {
        return switch (twilioStatus.toLowerCase()) {
            case "initiated" -> CallSession.CallStatus.INITIATED;
            case "ringing" -> CallSession.CallStatus.RINGING;
            case "in-progress" -> CallSession.CallStatus.IN_PROGRESS;
            case "completed" -> CallSession.CallStatus.COMPLETED;
            case "failed" -> CallSession.CallStatus.FAILED;
            case "busy" -> CallSession.CallStatus.BUSY;
            case "no-answer" -> CallSession.CallStatus.NO_ANSWER;
            case "canceled" -> CallSession.CallStatus.CANCELED;
            default -> {
                log.warn("Unknown Twilio status: {}", twilioStatus);
                yield CallSession.CallStatus.FAILED;
            }
        };
    }
    
    private boolean isEndStatus(CallSession.CallStatus status) {
        return status == CallSession.CallStatus.COMPLETED ||
               status == CallSession.CallStatus.FAILED ||
               status == CallSession.CallStatus.BUSY ||
               status == CallSession.CallStatus.NO_ANSWER ||
               status == CallSession.CallStatus.CANCELED;
    }
}
