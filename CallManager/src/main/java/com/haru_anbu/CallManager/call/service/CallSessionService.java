package com.haru_anbu.CallManager.call.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru_anbu.CallManager.call.dto.CallSessionDto;
import com.haru_anbu.CallManager.call.entity.CallSession;
import com.haru_anbu.CallManager.call.repository.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallSessionService {
    
    private final CallSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 새로운 통화 세션 생성
     */
    @Transactional
    public CallSession createSession(String userId, String phoneNumber, String twilioCallSid, Map<String, String> metadata) {
        String sessionId = generateSessionId();
        
        CallSession session = CallSession.builder()
            .sessionId(sessionId)
            .twilioCallSid(twilioCallSid)
            .userId(userId)
            .phoneNumber(phoneNumber)
            .status(CallSession.CallStatus.INITIATED)
            .direction("outbound")
            .metadata(serializeMetadata(metadata))
            .build();
        
        CallSession saved = sessionRepository.save(session);
        log.info("Created new call session: {}", sessionId);
        
        return saved;
    }
    
    /**
     * 세션 상태 업데이트
     */
    @Transactional
    public CallSession updateSessionStatus(String sessionId, CallSession.CallStatus status) {
        CallSession session = getSessionBySessionId(sessionId);
        session.setStatus(status);
        
        // IN_PROGRESS 상태로 변경 시 실제 시작 시간 기록
        if (status == CallSession.CallStatus.IN_PROGRESS && session.getStartedAt() == null) {
            session.setStartedAt(LocalDateTime.now());
        }
        
        // 종료 상태로 변경 시 종료 시간 및 통화 시간 계산
        if (isEndStatus(status) && session.getEndedAt() == null) {
            session.setEndedAt(LocalDateTime.now());
            session.calculateDuration();
        }
        
        CallSession updated = sessionRepository.save(session);
        log.info("Updated session {} status to {}", sessionId, status);
        
        return updated;
    }
    
    /**
     * 세션 종료
     */
    @Transactional
    public CallSession endSession(String sessionId, String reason, String summary) {
        CallSession session = getSessionBySessionId(sessionId);
        
        if (session.getEndedAt() == null) {
            session.setEndedAt(LocalDateTime.now());
            session.calculateDuration();
        }
        
        session.setStatus(CallSession.CallStatus.COMPLETED);
        session.setEndReason(reason);
        session.setConversationSummary(summary);
        
        CallSession updated = sessionRepository.save(session);
        log.info("Ended session {}: {}", sessionId, reason);
        
        return updated;
    }
    
    /**
     * 녹음 URL 저장
     */
    @Transactional
    public void updateRecordingUrl(String sessionId, String recordingUrl) {
        CallSession session = getSessionBySessionId(sessionId);
        session.setRecordingUrl(recordingUrl);
        sessionRepository.save(session);
        log.info("Updated recording URL for session {}", sessionId);
    }
    
    /**
     * 대화 내용(transcript) 저장
     */
    @Transactional
    public void updateConversationSummary(String sessionId, String summary) {
        CallSession session = getSessionBySessionId(sessionId);
        session.setConversationSummary(summary);
        sessionRepository.save(session);
        log.info("Updated conversation summary for session {}", sessionId);
    }
    
    /**
     * 세션 조회 (세션 ID로)
     */
    public CallSession getSessionBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }
    
    /**
     * 세션 조회 (Twilio Call SID로)
     */
    public CallSession getSessionByTwilioCallSid(String twilioCallSid) {
        return sessionRepository.findByTwilioCallSid(twilioCallSid)
            .orElseThrow(() -> new RuntimeException("Session not found for call SID: " + twilioCallSid));
    }
    
    /**
     * 사용자의 모든 세션 조회
     */
    public List<CallSessionDto.Response> getUserSessions(String userId) {
        return sessionRepository.findByUserId(userId).stream()
            .map(CallSessionDto.Response::from)
            .collect(Collectors.toList());
    }
    
    /**
     * 사용자의 최근 세션 조회
     */
    public List<CallSessionDto.Response> getRecentUserSessions(String userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return sessionRepository.findRecentCallsByUserId(userId, startDate).stream()
            .map(CallSessionDto.Response::from)
            .collect(Collectors.toList());
    }
    
    /**
     * 활성 세션 수 확인
     */
    public long getActiveSessionCount(String userId) {
        return sessionRepository.countActiveCallsByUserId(userId);
    }
    
    /**
     * 오래된 활성 세션 정리 (타임아웃)
     */
    @Transactional
    public void cleanupStaleSessions(int timeoutMinutes) {
        LocalDateTime before = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<CallSession> staleSessions = sessionRepository
            .findStaleSessionsByStatus(CallSession.CallStatus.IN_PROGRESS, before);
        
        for (CallSession session : staleSessions) {
            session.setStatus(CallSession.CallStatus.FAILED);
            session.setEndedAt(LocalDateTime.now());
            session.setEndReason("Session timeout");
            session.calculateDuration();
            sessionRepository.save(session);
            log.warn("Cleaned up stale session: {}", session.getSessionId());
        }
    }
    
    // Helper methods
    
    private String generateSessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private boolean isEndStatus(CallSession.CallStatus status) {
        return status == CallSession.CallStatus.COMPLETED ||
               status == CallSession.CallStatus.FAILED ||
               status == CallSession.CallStatus.BUSY ||
               status == CallSession.CallStatus.NO_ANSWER ||
               status == CallSession.CallStatus.CANCELED;
    }
    
    private String serializeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata", e);
            return null;
        }
    }
}
