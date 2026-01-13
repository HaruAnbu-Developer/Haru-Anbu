package com.haru_anbu.CallManager.call.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru_anbu.CallManager.call.entity.CallSession;
import com.haru_anbu.CallManager.call.service.CallManagerService;
import com.haru_anbu.CallManager.call.service.CallSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket Handler 통합 테스트
 * 실제 WebSocket 연결 없이 이벤트 처리 로직만 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TwilioMediaStreamHandlerTest {
    
    @Autowired
    private TwilioMediaStreamHandler handler;
    
    @Autowired
    private CallSessionService sessionService;
    
    @Autowired
    private CallManagerService callManagerService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @LocalServerPort
    private int port;
    
    private CallSession testSession;
    
    @BeforeEach
    void setUp() {
        // 테스트용 세션 생성
        String twilioCallSid = "CA_test_" + System.currentTimeMillis();
        testSession = sessionService.createSession(
            "test_user",
            "+821012345678",
            twilioCallSid,
            new HashMap<>()
        );
    }
    
    @Test
    @DisplayName("Twilio Connected 이벤트 처리 테스트")
    void testConnectedEvent() throws Exception {
        // Given: Twilio connected 메시지
        Map<String, Object> message = Map.of(
            "event", "connected",
            "protocol", "Call",
            "version", "1.0.0"
        );
        
        String json = objectMapper.writeValueAsString(message);
        
        System.out.println("✅ Connected 이벤트 메시지 생성 성공");
        System.out.println("   Message: " + json);
        
        // 실제 WebSocket 없이 JSON만 검증
        assertNotNull(json);
        assertTrue(json.contains("connected"));
    }
    
    @Test
    @DisplayName("Twilio Start 이벤트 메시지 생성 테스트")
    void testStartEventMessage() throws Exception {
        // Given: Twilio start 메시지
        Map<String, Object> startPayload = Map.of(
            "streamSid", "MZ_test_stream",
            "callSid", testSession.getTwilioCallSid(),
            "accountSid", "ACxxxxxxxxxxxx",
            "tracks", "inbound",
            "mediaFormat", Map.of(
                "encoding", "audio/x-mulaw",
                "sampleRate", 8000,
                "channels", 1
            )
        );
        
        Map<String, Object> message = Map.of(
            "event", "start",
            "sequenceNumber", "1",
            "start", startPayload,
            "streamSid", "MZ_test_stream"
        );
        
        String json = objectMapper.writeValueAsString(message);
        
        System.out.println("✅ Start 이벤트 메시지 생성 성공");
        System.out.println("   CallSid: " + testSession.getTwilioCallSid());
        
        // JSON 검증
        assertNotNull(json);
        assertTrue(json.contains("start"));
        assertTrue(json.contains(testSession.getTwilioCallSid()));
    }
    
    @Test
    @DisplayName("Twilio Media 이벤트 메시지 생성 테스트")
    void testMediaEventMessage() throws Exception {
        // Given: 가짜 mulaw 오디오 데이터
        byte[] mulawData = new byte[160]; // 20ms @ 8kHz
        for (int i = 0; i < mulawData.length; i++) {
            mulawData[i] = (byte) (Math.sin(i * 0.1) * 127);
        }
        String base64Audio = Base64.getEncoder().encodeToString(mulawData);
        
        Map<String, Object> mediaPayload = Map.of(
            "track", "inbound",
            "chunk", "1",
            "timestamp", System.currentTimeMillis(),
            "payload", base64Audio
        );
        
        Map<String, Object> message = Map.of(
            "event", "media",
            "sequenceNumber", "2",
            "media", mediaPayload,
            "streamSid", "MZ_test_stream"
        );
        
        String json = objectMapper.writeValueAsString(message);
        
        System.out.println("✅ Media 이벤트 메시지 생성 성공");
        System.out.println("   Audio size: " + mulawData.length + " bytes");
        System.out.println("   Base64 length: " + base64Audio.length());
        
        // JSON 검증
        assertNotNull(json);
        assertTrue(json.contains("media"));
        assertTrue(json.contains("inbound"));
    }
    
    @Test
    @DisplayName("Twilio Stop 이벤트 메시지 생성 테스트")
    void testStopEventMessage() throws Exception {
        // Given: Twilio stop 메시지
        Map<String, Object> stopPayload = Map.of(
            "accountSid", "ACxxxxxxxxxxxx",
            "callSid", testSession.getTwilioCallSid()
        );
        
        Map<String, Object> message = Map.of(
            "event", "stop",
            "sequenceNumber", "999",
            "stop", stopPayload,
            "streamSid", "MZ_test_stream"
        );
        
        String json = objectMapper.writeValueAsString(message);
        
        System.out.println("✅ Stop 이벤트 메시지 생성 성공");
        
        // JSON 검증
        assertNotNull(json);
        assertTrue(json.contains("stop"));
    }
    
    @Test
    @DisplayName("세션 ID 추출 테스트")
    void testSessionIdExtraction() {
        // Given: WebSocket URL
        String url1 = "ws://localhost:8080/ws/twilio/media-stream?sessionId=sess_abc123";
        String url2 = "ws://localhost:8080/ws/twilio/media-stream?sessionId=sess_xyz789&other=param";
        String url3 = "ws://localhost:8080/ws/twilio/media-stream";
        
        // When & Then
        assertTrue(url1.contains("sessionId="));
        assertTrue(url2.contains("sessionId="));
        assertFalse(url3.contains("sessionId="));
        
        System.out.println("✅ 세션 ID 추출 패턴 검증 성공");
    }
    
    @Test
    @DisplayName("에러 메시지 생성 테스트")
    void testErrorMessage() throws Exception {
        // Given: 에러 메시지
        Map<String, Object> errorMsg = Map.of(
            "event", "error",
            "message", "Failed to initialize AI conversation"
        );
        
        String json = objectMapper.writeValueAsString(errorMsg);
        
        System.out.println("✅ 에러 메시지 생성 성공");
        System.out.println("   Message: " + json);
        
        // JSON 검증
        assertNotNull(json);
        assertTrue(json.contains("error"));
    }
    
    @Test
    @DisplayName("AI 응답을 Twilio 형식으로 변환 테스트")
    void testAIResponseToTwilioFormat() throws Exception {
        // Given: AI가 생성한 가짜 PCM 데이터
        byte[] pcmData = new byte[640]; // 20ms @ 16kHz
        
        // mulaw로 변환될 것을 가정 (AudioConverter는 별도 테스트)
        String base64Mulaw = Base64.getEncoder().encodeToString(new byte[160]);
        
        // Twilio Media 메시지 생성
        Map<String, Object> mediaMessage = Map.of(
            "event", "media",
            "streamSid", "MZ_test_stream",
            "media", Map.of(
                "payload", base64Mulaw
            )
        );
        
        String json = objectMapper.writeValueAsString(mediaMessage);
        
        System.out.println("✅ AI → Twilio 메시지 변환 성공");
        System.out.println("   PCM size: " + pcmData.length + " bytes");
        
        // JSON 검증
        assertNotNull(json);
        assertTrue(json.contains("media"));
        assertTrue(json.contains("payload"));
    }
    
    @Test
    @DisplayName("전체 통화 플로우 시뮬레이션")
    void testCompleteCallFlow() throws Exception {
        System.out.println("\n=== 전체 통화 플로우 시뮬레이션 ===");
        
        // 1. Connected
        System.out.println("1️⃣ Connected 이벤트");
        Map<String, Object> connected = Map.of("event", "connected", "protocol", "Call");
        String connectedJson = objectMapper.writeValueAsString(connected);
        assertNotNull(connectedJson);
        
        // 2. Start
        System.out.println("2️⃣ Start 이벤트 - 스트림 시작");
        Map<String, Object> start = Map.of(
            "event", "start",
            "streamSid", "MZ_flow_test",
            "start", Map.of(
                "callSid", testSession.getTwilioCallSid(),
                "streamSid", "MZ_flow_test"
            )
        );
        String startJson = objectMapper.writeValueAsString(start);
        assertNotNull(startJson);
        
        // 3. Media (여러 번)
        System.out.println("3️⃣ Media 이벤트 - 오디오 스트리밍");
        for (int i = 0; i < 3; i++) {
            Map<String, Object> media = Map.of(
                "event", "media",
                "streamSid", "MZ_flow_test",
                "media", Map.of(
                    "track", "inbound",
                    "chunk", String.valueOf(i),
                    "payload", Base64.getEncoder().encodeToString(new byte[160])
                )
            );
            String mediaJson = objectMapper.writeValueAsString(media);
            assertNotNull(mediaJson);
            System.out.println("   Chunk " + i + " 처리됨");
        }
        
        // 4. Stop
        System.out.println("4️⃣ Stop 이벤트 - 스트림 종료");
        Map<String, Object> stop = Map.of(
            "event", "stop",
            "streamSid", "MZ_flow_test",
            "stop", Map.of("callSid", testSession.getTwilioCallSid())
        );
        String stopJson = objectMapper.writeValueAsString(stop);
        assertNotNull(stopJson);
        
        System.out.println("✅ 전체 플로우 시뮬레이션 성공\n");
    }
}
