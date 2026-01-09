package com.haru_anbu.CallManager.call.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Twilio Webhook 관련 DTO 클래스
public class TwilioWebhookDto {

    // 1. Twilio Status Callback DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class StatusCallback {
        private String callSid;
        private String accountSid;
        private String from;
        private String to;
        private String callStatus;
        private String apiVersion;
        private String direction;
        private String forwardedFrom;
        private String callerName;
        private String parentCallSid;
        private String callDuration;
        private String duration;
        private LocalDateTime timestamp;
        private String sequenceNumber;
    }

    // 2. Twilio Recording Callback DTO
     @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordingCallback {
        private String accountSid;
        private String callSid;
        private String recordingSid;
        private String recordingUrl;
        private String recordingStatus;
        private String recordingDuration;
        private String recordingChannels;
        private String recordingSource;
        private String errorCode;
    }
    
    // Twilio Media Stream Message DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaStreamMessage {
        private String event;
        private String streamSid;
        private String accountSid;
        private String callSid;
        private MediaPayload media;
        private StartPayload start;
        private StopPayload stop;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaPayload {
        private String track;
        private String chunk;
        private Long timestamp;
        private String payload; // base64 encoded audio
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartPayload {
        private String streamSid;
        private String accountSid;
        private String callSid;
        private String tracks;
        private MediaFormat mediaFormat;
        private CustomParameters customParameters;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StopPayload {
        private String streamSid;
        private String accountSid;
        private String callSid;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaFormat {
        private String encoding;
        private Integer sampleRate;
        private Integer channels;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomParameters {
        private String sessionId;
        private String userId;
    }
}
