package com.haru_anbu.CallManager.call.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * AI 서비스 통신 관련 DTO
 */
public class AIServiceDto {
    
    /**
     * AI 세션 시작 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StartSessionRequest {
        private String sessionId;
        private String userId;
        private String phoneNumber;
        private Map<String, String> metadata;
        private SessionConfig config;
    }
    
    /**
     * AI 세션 설정
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionConfig {
        private String language; // ko, en
        private String ttsVoice;
        private Float speechRate;
        private String conversationMode; // welfare_check, emergency, casual
        private Integer maxDurationMinutes;
    }
    
    /**
     * AI 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AIResponse {
        private String sessionId;
        private String responseText;
        private String emotion;
        private Float confidence;
        private List<String> suggestions;
        private Map<String, Object> metadata;
    }
    
    /**
     * 대화 요약
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationSummary {
        private String sessionId;
        private String summary;
        private Integer messageCount;
        private Integer durationSeconds;
        private String overallSentiment;
        private List<String> keyTopics;
        private Map<String, String> analytics;
    }
    
    /**
     * 텍스트 처리 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TextProcessRequest {
        private String sessionId;
        private String text;
        private String speaker; // user, assistant
        private Long timestamp;
        private Map<String, Object> context;
    }
    
    /**
     * 음성 인식 결과 (STT)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpeechRecognitionResult {
        private String sessionId;
        private String text;
        private Float confidence;
        private String language;
        private Long timestamp;
        private Boolean isFinal;
    }
    
    /**
     * 음성 합성 요청 (TTS)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TextToSpeechRequest {
        private String sessionId;
        private String text;
        private String voice;
        private Float speechRate;
        private String format; // mulaw, pcm
        private Integer sampleRate;
    }
}
