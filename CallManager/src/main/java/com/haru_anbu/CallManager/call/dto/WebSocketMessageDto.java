package com.haru_anbu.CallManager.call.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * WebSocket 메시지 관련 DTO
 */
public class WebSocketMessageDto {
    
    /**
     * WebSocket 메시지 타입
     */
    public enum MessageType {
        CONNECTED,
        START,
        MEDIA,
        STOP,
        TEXT,
        ERROR,
        PING,
        PONG
    }
    
    /**
     * 기본 WebSocket 메시지
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        private MessageType type;
        private String sessionId;
        private Object payload;
        private LocalDateTime timestamp;
    }
    
    /**
     * 연결 시작 메시지
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConnectedMessage {
        private String sessionId;
        private String message;
        private LocalDateTime timestamp;
    }
    
    /**
     * 오디오 스트림 시작
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StreamStartMessage {
        private String sessionId;
        private String streamSid;
        private String callSid;
        private AudioConfig audioConfig;
    }
    
    /**
     * 오디오 설정
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AudioConfig {
        private String encoding;
        private Integer sampleRate;
        private Integer channels;
    }
    
    /**
     * 오디오 데이터 메시지
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MediaMessage {
        private String sessionId;
        private String track; // inbound, outbound
        private String payload; // base64 audio
        private Long timestamp;
        private Integer sequenceNumber;
    }
    
    /**
     * 텍스트 메시지 (STT 결과 또는 TTS 요청)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TextMessage {
        private String sessionId;
        private String text;
        private String speaker; // user, assistant
        private Float confidence;
        private Boolean isFinal;
        private LocalDateTime timestamp;
    }
    
    /**
     * 스트림 종료 메시지
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StreamStopMessage {
        private String sessionId;
        private String streamSid;
        private String reason;
        private LocalDateTime timestamp;
    }
    
    /**
     * 에러 메시지
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorMessage {
        private String sessionId;
        private String error;
        private String message;
        private String code;
        private LocalDateTime timestamp;
    }
}
