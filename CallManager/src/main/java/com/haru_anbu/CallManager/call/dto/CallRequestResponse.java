package com.haru_anbu.CallManager.call.dto;

import com.haru_anbu.CallManager.call.entity.CallSession;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API 요청/응답 관련 DTO
 */
public class CallRequestResponse {
    
    /**
     * 통화 시작 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InitiateCallRequest {
        private String userId;
        private String phoneNumber;
        private String purpose; // welfare_check, emergency, reminder
        private Map<String, String> metadata;
        private CallConfig config;
    }
    
    /**
     * 통화 설정
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallConfig {
        private Boolean recordingEnabled;
        private Integer maxDurationMinutes;
        private String language;
        private String aiMode;
    }
    
    /**
     * 통화 시작 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InitiateCallResponse {
        private boolean success;
        private String message;
        private String sessionId;
        private String twilioCallSid;
        private CallSession.CallStatus status;
        private LocalDateTime createdAt;
    }
    
    /**
     * 통화 상태 업데이트 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateStatusRequest {
        private String sessionId;
        private CallSession.CallStatus status;
        private String reason;
    }
    
    /**
     * 통화 종료 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EndCallRequest {
        private String sessionId;
        private String reason;
        private Boolean saveRecording;
        private Boolean generateSummary;
    }
    
    /**
     * 통화 종료 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EndCallResponse {
        private boolean success;
        private String message;
        private String sessionId;
        private Integer durationSeconds;
        private String summary;
        private String recordingUrl;
    }
    
    /**
     * 세션 목록 조회 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionListResponse {
        private int totalCount;
        private int page;
        private int pageSize;
        private java.util.List<CallSessionDto.Response> sessions;
    }
    
    /**
     * 에러 응답
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorResponse {
        private String error;
        private String message;
        private String path;
        private LocalDateTime timestamp;
        private int status;
    }
    
    /**
     * 성공 응답 (간단)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SuccessResponse {
        private boolean success;
        private String message;
        private Map<String, Object> data;
    }
}
