package com.haru_anbu.CallManager.call.dto;

import com.haru_anbu.CallManager.call.entity.CallSession;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

public class CallSessionDto {
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String userId;
        private String phoneNumber;
        private String direction;
        private Map<String, String> metadata;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String sessionId;
        private String twilioCallSid;
        private String aiCoreSessionId;
        private String userId;
        private String phoneNumber;
        private CallSession.CallStatus status;
        private String direction;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private Integer durationSeconds;
        private String recordingUrl;
        private String conversationSummary;
        private String endReason;
        
        public static Response from(CallSession session) {
            return Response.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .twilioCallSid(session.getTwilioCallSid())
                .aiCoreSessionId(session.getAiCoreSessionId())
                .userId(session.getUserId())
                .phoneNumber(session.getPhoneNumber())
                .status(session.getStatus())
                .direction(session.getDirection())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .durationSeconds(session.getDurationSeconds())
                .recordingUrl(session.getRecordingUrl())
                .conversationSummary(session.getConversationSummary())
                .endReason(session.getEndReason())
                .build();
        }
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusUpdate {
        private String sessionId;
        private CallSession.CallStatus status;
        private String message;
        private LocalDateTime timestamp;
    }
}
