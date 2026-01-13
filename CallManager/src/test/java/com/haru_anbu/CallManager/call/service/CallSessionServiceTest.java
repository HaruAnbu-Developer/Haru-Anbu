package com.haru_anbu.CallManager.call.service;

import com.haru_anbu.CallManager.call.entity.CallSession;
import com.haru_anbu.CallManager.call.repository.CallSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CallSessionService 통합 테스트
 * 실제 DB와 연동하여 세션 관리 기능 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional // 각 테스트 후 롤백
class CallSessionServiceTest {
    
    @Autowired
    private CallSessionService sessionService;
    
    @Autowired
    private CallSessionRepository sessionRepository;
    
    @Test
    @DisplayName("세션 생성 테스트")
    void testCreateSession() {
        // Given
        String userId = "user123";
        String phoneNumber = "+821012345678";
        String twilioCallSid = "CAxxxxxxxxxxxxxxxxxxxx";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("purpose", "welfare_check");
        
        // When
        CallSession session = sessionService.createSession(
            userId, phoneNumber, twilioCallSid, metadata);
        
        // Then
        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals(userId, session.getUserId());
        assertEquals(phoneNumber, session.getPhoneNumber());
        assertEquals(twilioCallSid, session.getTwilioCallSid());
        assertEquals(CallSession.CallStatus.INITIATED, session.getStatus());
        
        System.out.println("✅ 세션 생성 성공: " + session.getSessionId());
    }
    
    @Test
    @DisplayName("세션 상태 업데이트 테스트")
    void testUpdateSessionStatus() {
        // Given: 세션 생성
        CallSession session = createTestSession("user456", "+821098765432");
        String sessionId = session.getSessionId();
        
        // When: 상태를 IN_PROGRESS로 변경
        CallSession updated = sessionService.updateSessionStatus(
            sessionId, CallSession.CallStatus.IN_PROGRESS);
        
        // Then
        assertEquals(CallSession.CallStatus.IN_PROGRESS, updated.getStatus());
        assertNotNull(updated.getStartedAt());
        
        System.out.println("✅ 세션 상태 업데이트 성공: " + updated.getStatus());
    }
    
    @Test
    @DisplayName("세션 종료 테스트")
    void testEndSession() {
        // Given: 진행 중인 세션
        CallSession session = createTestSession("user789", "+821011112222");
        String sessionId = session.getSessionId();
        sessionService.updateSessionStatus(sessionId, CallSession.CallStatus.IN_PROGRESS);
        
        // When: 세션 종료
        String reason = "completed";
        String summary = "대화가 정상적으로 완료되었습니다.";
        CallSession ended = sessionService.endSession(sessionId, reason, summary);
        
        // Then
        assertEquals(CallSession.CallStatus.COMPLETED, ended.getStatus());
        assertNotNull(ended.getEndedAt());
        assertNotNull(ended.getDurationSeconds());
        assertEquals(reason, ended.getEndReason());
        assertEquals(summary, ended.getConversationSummary());
        
        System.out.println("✅ 세션 종료 성공");
        System.out.println("   통화 시간: " + ended.getDurationSeconds() + "초");
    }
    
    @Test
    @DisplayName("사용자의 세션 목록 조회 테스트")
    void testGetUserSessions() {
        // Given: 같은 사용자의 여러 세션 생성
        String userId = "user_multi";
        createTestSession(userId, "+821011111111");
        createTestSession(userId, "+821022222222");
        createTestSession(userId, "+821033333333");
        
        // When
        var sessions = sessionService.getUserSessions(userId);
        
        // Then
        assertNotNull(sessions);
        assertTrue(sessions.size() >= 3);
        
        System.out.println("✅ 사용자 세션 조회 성공: " + sessions.size() + "개");
    }
    
    @Test
    @DisplayName("활성 세션 수 확인 테스트")
    void testActiveSessionCount() {
        // Given: 활성 세션 생성
        String userId = "user_active";
        CallSession session1 = createTestSession(userId, "+821044444444");
        CallSession session2 = createTestSession(userId, "+821055555555");
        
        sessionService.updateSessionStatus(
            session1.getSessionId(), CallSession.CallStatus.IN_PROGRESS);
        sessionService.updateSessionStatus(
            session2.getSessionId(), CallSession.CallStatus.IN_PROGRESS);
        
        // When
        long activeCount = sessionService.getActiveSessionCount(userId);
        
        // Then
        assertTrue(activeCount >= 2);
        
        System.out.println("✅ 활성 세션 수 확인: " + activeCount + "개");
    }
    
    @Test
    @DisplayName("녹음 URL 업데이트 테스트")
    void testUpdateRecordingUrl() {
        // Given
        CallSession session = createTestSession("user_rec", "+821066666666");
        String sessionId = session.getSessionId();
        String recordingUrl = "https://api.twilio.com/recordings/RExxxxxxxxxxxx";
        
        // When
        sessionService.updateRecordingUrl(sessionId, recordingUrl);
        
        // Then
        CallSession updated = sessionService.getSessionBySessionId(sessionId);
        assertEquals(recordingUrl, updated.getRecordingUrl());
        
        System.out.println("✅ 녹음 URL 업데이트 성공");
    }
    
    @Test
    @DisplayName("대화 요약 업데이트 테스트")
    void testUpdateConversationSummary() {
        // Given
        CallSession session = createTestSession("user_sum", "+821077777777");
        String sessionId = session.getSessionId();
        String summary = "사용자는 건강하며 특별한 문제가 없음을 확인했습니다.";
        
        // When
        sessionService.updateConversationSummary(sessionId, summary);
        
        // Then
        CallSession updated = sessionService.getSessionBySessionId(sessionId);
        assertEquals(summary, updated.getConversationSummary());
        
        System.out.println("✅ 대화 요약 업데이트 성공");
    }
    
    @Test
    @DisplayName("존재하지 않는 세션 조회 시 예외 발생")
    void testGetNonExistentSession() {
        // Given
        String nonExistentId = "sess_nonexistent123456";
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            sessionService.getSessionBySessionId(nonExistentId);
        });
        
        System.out.println("✅ 존재하지 않는 세션 예외 처리 성공");
    }
    
    // Helper method
    private CallSession createTestSession(String userId, String phoneNumber) {
        String twilioCallSid = "CA" + System.currentTimeMillis();
        return sessionService.createSession(
            userId, phoneNumber, twilioCallSid, new HashMap<>());
    }
}
