package com.haru_anbu.CallManager.call.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

/**
 * 오디오 포맷 변환 유틸리티
 * Twilio (mulaw 8kHz) ↔ AI Service (PCM 16kHz) -> 확인 필요
 */
@Component
@Slf4j
public class AudioConverter {
    
    // μ-law 압축 테이블
    private static final short[] MULAW_TO_PCM_TABLE = new short[256];
    
    static {
        // μ-law to PCM 변환 테이블 초기화
        for (int i = 0; i < 256; i++) {
            MULAW_TO_PCM_TABLE[i] = mulawToPcm((byte) i);
        }
    }
    
    /**
     * Twilio Base64 mulaw → PCM 16-bit
     * 8kHz mulaw → 16kHz PCM (업샘플링 포함)
     */
    public byte[] twilioToAI(String base64Mulaw) {
        try {
            // 1. Base64 디코딩
            byte[] mulawData = Base64.getDecoder().decode(base64Mulaw);
            
            // 2. mulaw → PCM 16-bit
            short[] pcm8k = mulawToPcm(mulawData);
            
            // 3. 8kHz → 16kHz 업샘플링 (선형 보간)
            short[] pcm16k = upsample8to16(pcm8k);
            
            // 4. short[] → byte[] (리틀 엔디안)
            return shortArrayToByteArray(pcm16k);
            
        } catch (Exception e) {
            log.error("Failed to convert Twilio audio to AI format", e);
            return new byte[0];
        }
    }
    
    /**
     * AI PCM 16-bit → Twilio Base64 mulaw
     * 16kHz PCM → 8kHz mulaw (다운샘플링 포함)
     */
    public String aiToTwilio(byte[] pcmData) {
        try {
            // 1. byte[] → short[] (리틀 엔디안)
            short[] pcm16k = byteArrayToShortArray(pcmData);
            
            // 2. 16kHz → 8kHz 다운샘플링
            short[] pcm8k = downsample16to8(pcm16k);
            
            // 3. PCM → mulaw
            byte[] mulawData = pcmToMulaw(pcm8k);
            
            // 4. Base64 인코딩
            return Base64.getEncoder().encodeToString(mulawData);
            
        } catch (Exception e) {
            log.error("Failed to convert AI audio to Twilio format", e);
            return "";
        }
    }
    
    // ============== 내부 변환 메서드 ==============
    
    /**
     * mulaw 배열 → PCM short 배열
     */
    private short[] mulawToPcm(byte[] mulawData) {
        short[] pcmData = new short[mulawData.length];
        for (int i = 0; i < mulawData.length; i++) {
            pcmData[i] = MULAW_TO_PCM_TABLE[mulawData[i] & 0xFF];
        }
        return pcmData;
    }
    
    /**
     * PCM short 배열 → mulaw 배열
     */
    private byte[] pcmToMulaw(short[] pcmData) {
        byte[] mulawData = new byte[pcmData.length];
        for (int i = 0; i < pcmData.length; i++) {
            mulawData[i] = pcmToMulaw(pcmData[i]);
        }
        return mulawData;
    }
    
    /**
     * 단일 mulaw byte → PCM short
     */
    private static short mulawToPcm(byte mulaw) {
        mulaw = (byte) ~mulaw;
        int sign = (mulaw & 0x80);
        int exponent = (mulaw >> 4) & 0x07;
        int mantissa = mulaw & 0x0F;
        
        int sample = ((mantissa << 3) + 132) << exponent;
        sample -= 132;
        
        return (short) (sign != 0 ? -sample : sample);
    }
    
    /**
     * 단일 PCM short → mulaw byte
     */
    private byte pcmToMulaw(short pcm) {
        int sign = (pcm < 0) ? 0x80 : 0;
        int magnitude = Math.abs(pcm);
        
        magnitude += 132;
        if (magnitude > 32767) magnitude = 32767;
        
        int exponent = 7;
        for (int shift = 14; shift >= 8; shift--) {
            if ((magnitude & (1 << shift)) != 0) {
                exponent = shift - 8;
                break;
            }
        }
        
        int mantissa = (magnitude >> (exponent + 3)) & 0x0F;
        int mulaw = sign | (exponent << 4) | mantissa;
        
        return (byte) ~mulaw;
    }
    
    /**
     * 8kHz → 16kHz 업샘플링 (선형 보간)
     */
    private short[] upsample8to16(short[] input) {
        short[] output = new short[input.length * 2];
        
        for (int i = 0; i < input.length - 1; i++) {
            output[i * 2] = input[i];
            // 선형 보간으로 중간값 생성
            output[i * 2 + 1] = (short) ((input[i] + input[i + 1]) / 2);
        }
        
        // 마지막 샘플 처리
        if (input.length > 0) {
            output[output.length - 2] = input[input.length - 1];
            output[output.length - 1] = input[input.length - 1];
        }
        
        return output;
    }
    
    /**
     * 16kHz → 8kHz 다운샘플링 (간단한 데시메이션)
     */
    private short[] downsample16to8(short[] input) {
        short[] output = new short[input.length / 2];
        
        for (int i = 0; i < output.length; i++) {
            // 2개 샘플 중 하나 선택 (또는 평균)
            output[i] = input[i * 2];
        }
        
        return output;
    }
    
    /**
     * short[] → byte[] (리틀 엔디안)
     */
    private byte[] shortArrayToByteArray(short[] shorts) {
        ByteBuffer buffer = ByteBuffer.allocate(shorts.length * 2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : shorts) {
            buffer.putShort(s);
        }
        return buffer.array();
    }
    
    /**
     * byte[] → short[] (리틀 엔디안)
     */
    private short[] byteArrayToShortArray(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }
    
    /**
     * 오디오 품질 검증
     */
    public boolean isValidAudio(byte[] audioData, int minSize) {
        return audioData != null && audioData.length >= minSize;
    }
    
    /**
     * 무음 감지 (RMS 기반)
     */
    public boolean isSilence(short[] pcmData, double threshold) {
        if (pcmData == null || pcmData.length == 0) return true;
        
        long sum = 0;
        for (short sample : pcmData) {
            sum += (long) sample * sample;
        }
        
        double rms = Math.sqrt((double) sum / pcmData.length);
        return rms < threshold;
    }
}
