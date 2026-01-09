package com.haru_anbu.CallManager.call.dto;

import lombok.*;

/**
 * 오디오 처리 관련 DTO
 */
public class AudioDto {
    
    /**
     * 오디오 청크 DTO (Twilio -> AI)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AudioChunk {
        private String sessionId;
        private byte[] audioData;
        private Integer sampleRate;
        private String format; // mulaw, pcm
        private Long timestamp;
        private String direction; // inbound, outbound
        private String track; // inbound_track, outbound_track
    }
    
    /**
     * 오디오 스트림 설정
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StreamConfig {
        private String sessionId;
        private String encoding;
        private Integer sampleRate;
        private Integer channels;
        private Integer bufferSize;
    }
    
    /**
     * 오디오 처리 결과
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessResult {
        private String sessionId;
        private boolean success;
        private String message;
        private byte[] processedAudio;
        private String transcription;
        private Long processingTimeMs;
    }
}
