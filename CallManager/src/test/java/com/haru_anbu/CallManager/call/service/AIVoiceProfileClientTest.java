package com.haru_anbu.CallManager.call.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AIVoiceProfileClientTest {

    private MockWebServer mockWebServer; // static 제거
    private AIVoiceProfileClient aiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() throws IOException {
        // 매 테스트마다 서버를 새로 생성하고 시작
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        aiClient = new AIVoiceProfileClient(
                WebClient.create(),
                mockWebServer.url("/").toString(),
                "test_api_key",
                2
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        // 매 테스트 종료 후 서버 종료 (큐가 자동으로 비워짐)
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("1. 음성 프로필 업데이트: 성공 시 요청 바디 및 헤더 정밀 검증")
    void testUpdateVoiceProfilePath_Success() throws Exception {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"success\": true}")
                .setHeader("Content-Type", "application/json"));

        // When
        boolean result = aiClient.updateVoiceProfilePath("user123", "sess_abc", "s3://path");

        // Then
        assertTrue(result);
        RecordedRequest request = mockWebServer.takeRequest();
        
        // JSON 바디 정밀 검증 (단순 contains 방지)
        JsonNode body = objectMapper.readTree(request.getBody().readUtf8());
        assertEquals("user123", body.get("user_id").asText());
        assertEquals("s3://path", body.get("raw_wav_path").asText());
        assertEquals("test_api_key", request.getHeader("X-API-Key"));
    }

    @Test
    @DisplayName("2. 비동기 존재 확인: StepVerifier를 사용한 논블로킹 검증")
    void testVoiceProfileExistsAsync_Success() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"exists\": true}")
                .setHeader("Content-Type", "application/json"));

        // When & Then (block() 대신 StepVerifier 사용)
        StepVerifier.create(aiClient.voiceProfileExistsAsync("user_async"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("3. 타임아웃 발생 시: 예외를 먹고 false를 반환하는지 확인")
    void testTimeout_ReturnsFalse() {
        // Given: 타임아웃(2초)보다 긴 지연 설정
        mockWebServer.enqueue(new MockResponse()
                .setBodyDelay(5, TimeUnit.SECONDS)
                .setBody("{\"success\": true}"));

        // When
        boolean result = aiClient.updateVoiceProfilePath("user_timeout", "sess", "path");

        // Then: 현재 코드 로직상 Exception을 catch하고 false를 반환해야 함
        assertFalse(result, "타임아웃 발생 시 false를 반환해야 합니다.");
    }

    @Test
    @DisplayName("4. 404 에러 발생 시: 존재하지 않음(false)으로 정상 처리")
    void testVoiceProfileExists_Handle404() {
        // Given
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        // When
        boolean exists = aiClient.voiceProfileExists("unknown_user");

        // Then
        assertFalse(exists);
    }
}
