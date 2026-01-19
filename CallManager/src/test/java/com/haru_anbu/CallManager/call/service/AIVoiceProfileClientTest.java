package com.haru_anbu.CallManager.call.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AIVoiceProfileClient 단위 테스트
 * MockWebServer를 사용하여 AI 서버 없이 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AIVoiceProfileClientTest {
    
    private static MockWebServer mockWebServer;
    private AIVoiceProfileClient aiClient;
    
    @BeforeAll
    static void setupClass() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        System.out.println("\n=== AI Client Unit Test 시작 ===");
        System.out.println("Mock Server: " + mockWebServer.url("/").toString());
    }
    
    @AfterAll
    static void tearDownClass() throws IOException {
        mockWebServer.shutdown();
        System.out.println("\n=== AI Client Unit Test 종료 ===\n");
    }
    
    @BeforeEach
    void setup() {
        // WebClient를 Mock 서버 URL로 설정
        WebClient webClient = WebClient.create();
        aiClient = new AIVoiceProfileClient(webClient);
        
        // Reflection으로 private 필드 설정 (테스트용)
        try {
            var baseUrlField = AIVoiceProfileClient.class.getDeclaredField("aiCoreBaseUrl");
            baseUrlField.setAccessible(true);
            baseUrlField.set(aiClient, mockWebServer.url("/").toString());
            
            var apiKeyField = AIVoiceProfileClient.class.getDeclaredField("apiKey");
            apiKeyField.setAccessible(true);
            apiKeyField.set(aiClient, "test_api_key");
            
            var timeoutField = AIVoiceProfileClient.class.getDeclaredField("timeoutSeconds");
            timeoutField.setAccessible(true);
            timeoutField.set(aiClient, 5);
        } catch (Exception e) {
            fail("Failed to setup test: " + e.getMessage());
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("1. 음성 프로필 업데이트 성공 테스트")
    void testUpdateVoiceProfilePath_Success() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": true, \"message\": \"Updated\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));
        
        // When
        boolean result = aiClient.updateVoiceProfilePath(
            "user123", 
            "sess_abc", 
            "s3://bucket/path/file.wav"
        );
        
        // Then
        assertTrue(result, "업데이트가 실패했습니다");
        
        // 요청 검증
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/api/voice-profiles/update-path"));
        assertEquals("test_api_key", request.getHeader("X-API-Key"));
        
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("user123"));
        assertTrue(body.contains("sess_abc"));
        assertTrue(body.contains("s3://bucket/path/file.wav"));
        
        System.out.println("✅ 음성 프로필 업데이트 성공");
    }
    
    @Test
    @Order(2)
    @DisplayName("2. 음성 프로필 업데이트 실패 테스트 (서버 에러)")
    void testUpdateVoiceProfilePath_ServerError() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": false, \"message\": \"Internal Server Error\"}")
            .setResponseCode(500));
        
        // When
        boolean result = aiClient.updateVoiceProfilePath(
            "user123", 
            "sess_abc", 
            "s3://bucket/path/file.wav"
        );
        
        // Then
        assertFalse(result, "서버 에러 시 false를 반환해야 합니다");
        
        System.out.println("✅ 서버 에러 처리 확인");
    }
    
    @Test
    @Order(3)
    @DisplayName("3. 음성 프로필 생성 성공 테스트")
    void testCreateVoiceProfile_Success() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": true, \"message\": \"Created\"}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));
        
        // When
        boolean result = aiClient.createVoiceProfile(
            "user456", 
            "sess_xyz", 
            "s3://bucket/path/new_file.wav",
            "+821012345678"
        );
        
        // Then
        assertTrue(result, "생성이 실패했습니다");
        
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertTrue(request.getPath().contains("/api/voice-profiles/create"));
        
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("user456"));
        assertTrue(body.contains("+821012345678"));
        
        System.out.println("✅ 음성 프로필 생성 성공");
    }
    
    @Test
    @Order(4)
    @DisplayName("4. 음성 프로필 존재 확인 - 존재함")
    void testVoiceProfileExists_True() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"exists\": true}")
            .setHeader("Content-Type", "application/json")
            .setResponseCode(200));
        
        // When
        boolean exists = aiClient.voiceProfileExists("user123");
        
        // Then
        assertTrue(exists, "프로필이 존재하지 않습니다");
        
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("GET", request.getMethod());
        assertTrue(request.getPath().contains("/api/voice-profiles/user123/exists"));
        
        System.out.println("✅ 프로필 존재 확인 성공");
    }
    
    @Test
    @Order(5)
    @DisplayName("5. 음성 프로필 존재 확인 - 존재하지 않음")
    void testVoiceProfileExists_False() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"exists\": false}")
            .setResponseCode(200));
        
        // When
        boolean exists = aiClient.voiceProfileExists("user999");
        
        // Then
        assertFalse(exists, "프로필이 존재합니다");
        
        System.out.println("✅ 프로필 미존재 확인 성공");
    }
    
    @Test
    @Order(6)
    @DisplayName("6. 음성 프로필 존재 확인 - 404 응답")
    void testVoiceProfileExists_NotFound() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404));
        
        // When
        boolean exists = aiClient.voiceProfileExists("user_not_found");
        
        // Then
        assertFalse(exists, "404 시 false를 반환해야 합니다");
        
        System.out.println("✅ 404 에러 처리 확인");
    }
    
    @Test
    @Order(7)
    @DisplayName("7. 비동기 업데이트 테스트")
    void testUpdateVoiceProfilePathAsync() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": true}")
            .setResponseCode(200));
        
        // When
        Boolean result = aiClient.updateVoiceProfilePathAsync(
            "user_async", 
            "sess_async", 
            "s3://bucket/async.wav"
        ).block();  // 테스트에서는 block으로 동기화
        
        // Then
        assertNotNull(result);
        assertTrue(result);
        
        System.out.println("✅ 비동기 업데이트 성공");
    }
    
    @Test
    @Order(8)
    @DisplayName("8. 비동기 존재 확인 테스트")
    void testVoiceProfileExistsAsync() {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"exists\": true}")
            .setResponseCode(200));
        
        // When
        Boolean exists = aiClient.voiceProfileExistsAsync("user_async")
            .block();
        
        // Then
        assertNotNull(exists);
        assertTrue(exists);
        
        System.out.println("✅ 비동기 존재 확인 성공");
    }
    
    @Test
    @Order(9)
    @DisplayName("9. 타임아웃 테스트")
    void testTimeout() {
        // Given: 응답 지연
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"success\": true}")
            .setBodyDelay(10, TimeUnit.SECONDS));  // 10초 지연
        
        // When & Then
        assertThrows(Exception.class, () -> {
            aiClient.updateVoiceProfilePath(
                "user_timeout", 
                "sess_timeout", 
                "s3://bucket/timeout.wav"
            );
        }, "타임아웃 예외가 발생해야 합니다");
        
        System.out.println("✅ 타임아웃 처리 확인");
    }
    
    @Test
    @Order(10)
    @DisplayName("10. 잘못된 JSON 응답 처리")
    void testInvalidJsonResponse() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
            .setBody("This is not JSON")
            .setResponseCode(200));
        
        // When
        boolean result = aiClient.updateVoiceProfilePath(
            "user_invalid", 
            "sess_invalid", 
            "s3://bucket/invalid.wav"
        );
        
        // Then
        assertFalse(result, "잘못된 응답 시 false를 반환해야 합니다");
        
        System.out.println("✅ 잘못된 JSON 응답 처리 확인");
    }
}
