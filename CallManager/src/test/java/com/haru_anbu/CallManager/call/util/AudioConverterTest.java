package com.haru_anbu.CallManager.call.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Base64;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AudioConverter 단위 테스트
 * 실제 AI 서버 없이 오디오 변환 로직만 테스트
 */
class AudioConverterTest {
    
    private AudioConverter audioConverter;
    private Random random;
    
    @BeforeEach
    void setUp() {
        audioConverter = new AudioConverter();
        random = new Random(42); // 재현 가능한 테스트를 위해 시드 고정
    }
    
    @Test
    @DisplayName("mulaw → PCM 변환 테스트")
    void testTwilioToAI() {
        // Given: Twilio 형식의 mulaw 데이터 생성 (160 bytes = 20ms @ 8kHz)
        byte[] mulawData = generateMulawData(160);
        String base64Mulaw = Base64.getEncoder().encodeToString(mulawData);
        
        // When: Twilio → AI 변환
        byte[] pcmData = audioConverter.twilioToAI(base64Mulaw);
        
        // Then: PCM 데이터가 생성되고 크기가 2배 이상이어야 함
        assertNotNull(pcmData);
        assertTrue(pcmData.length > 0);
        
        // 160 mulaw → 320 PCM (8kHz) → 640 PCM (16kHz)
        // PCM은 16-bit이므로 640 samples * 2 bytes = 1280 bytes
        assertEquals(640, pcmData.length); // 160 * 2 (업샘플링) * 2 (16-bit)
        
        System.out.println("✅ mulaw → PCM 변환 성공");
        System.out.println("   Input:  " + mulawData.length + " bytes (mulaw)");
        System.out.println("   Output: " + pcmData.length + " bytes (PCM 16kHz)");
    }
    
    @Test
    @DisplayName("PCM → mulaw 변환 테스트")
    void testAIToTwilio() {
        // Given: AI 형식의 PCM 데이터 생성 (640 bytes = 20ms @ 16kHz)
        byte[] pcmData = generatePcmData(640);
        
        // When: AI → Twilio 변환
        String base64Mulaw = audioConverter.aiToTwilio(pcmData);
        
        // Then: Base64 mulaw 문자열이 생성되어야 함
        assertNotNull(base64Mulaw);
        assertFalse(base64Mulaw.isEmpty());
        
        // Base64 디코딩 가능 확인
        byte[] decoded = Base64.getDecoder().decode(base64Mulaw);
        assertTrue(decoded.length > 0);
        
        // 640 PCM (16kHz) → 320 PCM (8kHz) → 160 mulaw
        assertEquals(160, decoded.length);
        
        System.out.println("✅ PCM → mulaw 변환 성공");
        System.out.println("   Input:  " + pcmData.length + " bytes (PCM 16kHz)");
        System.out.println("   Output: " + decoded.length + " bytes (mulaw)");
    }
    
    @Test
    @DisplayName("왕복 변환 테스트 (mulaw → PCM → mulaw)")
    void testRoundTrip() {
        // Given: 원본 mulaw 데이터
        byte[] originalMulaw = generateMulawData(160);
        String base64Original = Base64.getEncoder().encodeToString(originalMulaw);
        
        // When: mulaw → PCM → mulaw
        byte[] pcmData = audioConverter.twilioToAI(base64Original);
        String base64Converted = audioConverter.aiToTwilio(pcmData);
        byte[] convertedMulaw = Base64.getDecoder().decode(base64Converted);
        
        // Then: 크기는 같아야 함 (내용은 손실 압축으로 인해 다를 수 있음)
        assertEquals(originalMulaw.length, convertedMulaw.length);
        
        System.out.println("✅ 왕복 변환 테스트 성공");
        System.out.println("   원본:   " + originalMulaw.length + " bytes");
        System.out.println("   중간:   " + pcmData.length + " bytes (PCM)");
        System.out.println("   결과:   " + convertedMulaw.length + " bytes");
    }
    
    @Test
    @DisplayName("오디오 유효성 검증 테스트")
    void testIsValidAudio() {
        // Given
        byte[] validAudio = new byte[64];
        byte[] tooSmallAudio = new byte[16];
        
        // When & Then
        assertTrue(audioConverter.isValidAudio(validAudio, 32));
        assertFalse(audioConverter.isValidAudio(tooSmallAudio, 32));
        assertFalse(audioConverter.isValidAudio(null, 32));
        
        System.out.println("✅ 오디오 유효성 검증 성공");
    }
    
    @Test
    @DisplayName("무음 감지 테스트")
    void testSilenceDetection() {
        // Given: 무음 데이터 (모두 0)
        short[] silenceData = new short[320];
        
        // Given: 노이즈 데이터
        short[] noiseData = new short[320];
        for (int i = 0; i < noiseData.length; i++) {
            noiseData[i] = (short) (random.nextInt(1000) - 500);
        }
        
        // When & Then
        assertTrue(audioConverter.isSilence(silenceData, 100.0));
        assertFalse(audioConverter.isSilence(noiseData, 100.0));
        
        System.out.println("✅ 무음 감지 테스트 성공");
    }
    
    @Test
    @DisplayName("빈 데이터 처리 테스트")
    void testEmptyData() {
        // Given
        String emptyBase64 = "";
        byte[] emptyPcm = new byte[0];
        
        // When
        byte[] resultPcm = audioConverter.twilioToAI(emptyBase64);
        String resultMulaw = audioConverter.aiToTwilio(emptyPcm);
        
        // Then: 빈 데이터가 반환되어야 함 (오류 발생 X)
        assertEquals(0, resultPcm.length);
        assertTrue(resultMulaw.isEmpty());
        
        System.out.println("✅ 빈 데이터 처리 테스트 성공");
    }
    
    @Test
    @DisplayName("대용량 데이터 처리 테스트 (1초 오디오)")
    void testLargeAudio() {
        // Given: 1초 분량의 오디오 (8kHz mulaw = 8000 bytes)
        byte[] largeMulaw = generateMulawData(8000);
        String base64Large = Base64.getEncoder().encodeToString(largeMulaw);
        
        // When
        long startTime = System.currentTimeMillis();
        byte[] pcmData = audioConverter.twilioToAI(base64Large);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertNotNull(pcmData);
        assertTrue(pcmData.length > 0);
        
        System.out.println("✅ 대용량 데이터 처리 성공");
        System.out.println("   Input:  " + largeMulaw.length + " bytes (1초 mulaw)");
        System.out.println("   Output: " + pcmData.length + " bytes (1초 PCM)");
        System.out.println("   처리 시간: " + (endTime - startTime) + "ms");
    }
    
    // Helper methods
    
    private byte[] generateMulawData(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            // 간단한 사인파 생성 후 mulaw 인코딩 시뮬레이션
            data[i] = (byte) (random.nextInt(256) - 128);
        }
        return data;
    }
    
    private byte[] generatePcmData(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length / 2; i++) {
            // 16-bit PCM 시뮬레이션
            short sample = (short) (random.nextInt(10000) - 5000);
            data[i * 2] = (byte) (sample & 0xFF);
            data[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return data;
    }
}
