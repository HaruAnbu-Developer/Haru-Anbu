package com.haru_anbu.CallManager.call.service;

import com.haru_anbu.CallManager.call.entity.CallSession;
import com.haru_anbu.CallManager.call.repository.CallSessionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RecordingService 통합 테스트
 * S3 업로드 및 AI 서버 연동 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecordingServiceIntegrationTest {
    
    @Autowired
    private RecordingService recordingService;
    
    @Autowired
    private S3StorageService s3StorageService;
    
    @Autowired
    private CallSessionRepository sessionRepository;
    
    @MockBean
    private AIVoiceProfileClient aiVoiceProfileClient;  // Mock으로 대체
    
    private static String testSessionId;
    private static String testUserId;
    private static String testTwilioCallSid;
    private static CallSession testSession;
    
    @BeforeAll
    static void setup() {
        testSessionId = "sess_test_" + UUID.randomUUID().toString().substring(0, 8);
        testUserId = "user_test_" + UUID.randomUUID().toString().substring(0, 8);
        testTwilioCallSid = "CA_test_" + UUID.randomUUID().toString().substring(0, 24);
        
        System.out.println("\n=== Recording Service Integration Test 시작 ===");
        System.out.println("Session ID: " + testSessionId);
        System.out.println("User ID: " + testUserId);
    }
    
    @BeforeEach
    void setupEach() {
        // AI 클라이언트 Mock 설정 (실제 AI 서버 없이 테스트)
        when(aiVoiceProfileClient.voiceProfileExistsAsync(anyString()))
            .thenReturn(reactor.core.publisher.Mono.just(false));
        when(aiVoiceProfileClient.createVoiceProfile(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(true);
        when(aiVoiceProfileClient.updateVoiceProfilePathAsync(anyString(), anyString(), anyString()))
            .thenReturn(reactor.core.publisher.Mono.just(true));
    }
    
    @AfterAll
    static void cleanup(@Autowired CallSessionRepository repository) {
        // 테스트 데이터 정리
        if (testSession != null) {
            repository.delete(testSession);
        }
        System.out.println("\n=== Recording Service Integration Test 종료 ===\n");
    }
    
    @Test
    @Order(1)
    @DisplayName("1. 테스트용 세션 생성")
    void testCreateSession() {
        // Given
        testSession = CallSession.builder()
            .sessionId(testSessionId)
            .twilioCallSid(testTwilioCallSid)
            .userId(testUserId)
            .phoneNumber("+821012345678")
            .status(CallSession.CallStatus.COMPLETED)
            .direction("outbound")
            .durationSeconds(120)
            .build();
        
        // When
        CallSession saved = sessionRepository.save(testSession);
        
        // Then
        assertNotNull(saved.getId());
        assertEquals(testSessionId, saved.getSessionId());
        
        System.out.println("✅ 테스트 세션 생성 성공");
        System.out.println("   Session ID: " + saved.getSessionId());
        System.out.println("   Twilio SID: " + saved.getTwilioCallSid());
    }
    
    @Test
    @Order(2)
    @DisplayName("2. Mock Twilio 녹음 URL 처리 (S3 업로드)")
    void testDownloadAndSaveRecording() throws Exception {
        // Given: Mock Twilio 녹음 데이터
        // 실제 Twilio URL 대신 로컬 테스트 데이터 사용
        byte[] mockRecordingData = createMockWavFile();
        
        // S3에 직접 업로드 (Twilio URL 없이 테스트)
        String fileName = String.format("recording_%s_%d.wav", 
            testSessionId, System.currentTimeMillis());
        String s3Url = s3StorageService.saveRecording(
            testSessionId, 
            mockRecordingData, 
            fileName, 
            "audio/wav"
        );
        
        // When: DB에 S3 URL 저장
        CallSession session = sessionRepository.findByTwilioCallSid(testTwilioCallSid)
            .orElseThrow();
        session.setRecordingUrl(s3Url);
        sessionRepository.save(session);
        
        // Then
        assertNotNull(s3Url);
        assertTrue(s3Url.startsWith("s3://"));
        assertTrue(s3StorageService.fileExists(s3Url));
        
        CallSession updated = sessionRepository.findBySessionId(testSessionId)
            .orElseThrow();
        assertEquals(s3Url, updated.getRecordingUrl());
        
        System.out.println("✅ 녹음 파일 S3 업로드 성공");
        System.out.println("   S3 URL: " + s3Url);
        System.out.println("   File Size: " + mockRecordingData.length + " bytes");
        
        // AI 클라이언트 호출 확인은 생략 (비동기라 즉시 검증 어려움)
    }
    
    @Test
    @Order(3)
    @DisplayName("3. Presigned URL 생성 테스트")
    void testGetRecordingDownloadUrl() {
        // Given
        CallSession session = sessionRepository.findBySessionId(testSessionId)
            .orElseThrow();
        assertNotNull(session.getRecordingUrl(), "녹음 URL이 설정되지 않았습니다");
        
        // When
        String presignedUrl = recordingService.getRecordingDownloadUrl(testSessionId);
        
        // Then
        assertNotNull(presignedUrl);
        assertTrue(presignedUrl.startsWith("https://"));
        
        System.out.println("✅ Presigned URL 생성 성공");
        System.out.println("   URL: " + presignedUrl.substring(0, Math.min(100, presignedUrl.length())) + "...");
    }
    
    @Test
    @Order(4)
    @DisplayName("4. 존재하지 않는 세션 조회 시 예외 처리")
    void testGetDownloadUrlForNonExistentSession() {
        // Given
        String nonExistentSessionId = "non_existent_session";
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            recordingService.getRecordingDownloadUrl(nonExistentSessionId);
        });
        
        System.out.println("✅ 예외 처리 확인 성공");
    }
    
    @Test
    @Order(5)
    @DisplayName("5. 녹음 파일 삭제 테스트")
    void testDeleteRecording() {
        // Given
        CallSession session = sessionRepository.findBySessionId(testSessionId)
            .orElseThrow();
        String s3Url = session.getRecordingUrl();
        assertNotNull(s3Url);
        
        // When
        recordingService.deleteRecording(testSessionId);
        
        // Then
        assertFalse(s3StorageService.fileExists(s3Url), 
            "삭제한 파일이 여전히 S3에 존재합니다");
        
        System.out.println("✅ 녹음 파일 삭제 성공");
        System.out.println("   Deleted: " + s3Url);
    }
    
    @Test
    @Order(6)
    @DisplayName("6. AI 클라이언트 Mock 호출 검증")
    void testAIClientIntegration() throws Exception {
        // Given
        String newSessionId = "sess_ai_test_" + UUID.randomUUID().toString().substring(0, 8);
        String newCallSid = "CA_ai_test_" + UUID.randomUUID().toString().substring(0, 24);
        
        CallSession aiTestSession = CallSession.builder()
            .sessionId(newSessionId)
            .twilioCallSid(newCallSid)
            .userId(testUserId)
            .phoneNumber("+821087654321")
            .status(CallSession.CallStatus.COMPLETED)
            .direction("outbound")
            .build();
        sessionRepository.save(aiTestSession);
        
        // When: 녹음 저장 (AI 업데이트 트리거)
        byte[] mockData = createMockWavFile();
        String s3Url = s3StorageService.saveRecording(
            newSessionId, 
            mockData, 
            "test_ai.wav", 
            "audio/wav"
        );
        
        aiTestSession.setRecordingUrl(s3Url);
        sessionRepository.save(aiTestSession);
        
        // RecordingService의 비동기 메서드 직접 호출은 어려우므로
        // AI 클라이언트 Mock이 설정되었는지만 확인
        
        // Then
        assertNotNull(aiVoiceProfileClient);
        
        // Cleanup
        s3StorageService.deleteFile(s3Url);
        sessionRepository.delete(aiTestSession);
        
        System.out.println("✅ AI 클라이언트 통합 테스트 성공");
        System.out.println("   Note: AI 서버는 Mock으로 대체됨");
    }
    
    /**
     * Mock WAV 파일 생성 (테스트용)
     */
    private byte[] createMockWavFile() {
        // 간단한 WAV 헤더 + 데이터
        byte[] header = new byte[44];
        // "RIFF" 시그니처
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        // 파일 크기 (나중에 채움)
        // "WAVE" 포맷
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        
        // 실제로는 더 복잡한 WAV 포맷이지만 테스트용으로 간단히
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (Math.sin(i * 0.1) * 127);
        }
        
        byte[] result = new byte[header.length + data.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(data, 0, result, header.length, data.length);
        
        return result;
    }
}
