package com.haru_anbu.CallManager.call.config;

import com.twilio.Twilio;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
@Getter
public class TwilioConfig {
    
    @Value("${twilio.account-sid}")
    private String accountSid;
    
    @Value("${twilio.auth-token}")
    private String authToken;
    
    @Value("${twilio.phone-number}")
    private String phoneNumber;
    
    @Value("${twilio.webhook-base-url}")
    private String webhookBaseUrl;
    
    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }
    
    /**
     * Twilio Media Stream WebSocket URL
     * Twilio와 실시간 오디오 스트리밍하는 웹소켓
     * wss://your-domain.com/ws/twilio/media-stream
     * 이하 메소드는 twilio 전용(twilio가 호출하는) API로 jwt 인증이 필요하지 않음
     */
    public String getMediaStreamWebhookUrl() {
        return webhookBaseUrl.replace("https://", "wss://")
                             .replace("http://", "ws://") 
                + "/ws/twilio/media-stream";
    }
    
    /**
     * Twilio Voice Webhook URL
     * Twilio가 전화 연결 시 twiML을 요청
     * https://your-domain.com/api/webhooks/twilio/voice
     */
    public String getVoiceWebhookUrl() {
        return webhookBaseUrl + "/api/webhooks/twilio/voice";
    }
    
    /**
     * Twilio Status Callback URL
     * Twilio가 통화 상태 변경 시 알림을 보냄
     * https://your-domain.com/api/webhooks/twilio/status
     */
    public String getStatusCallbackUrl() {
        return webhookBaseUrl + "/api/webhooks/twilio/status";
    }
    
    /**
     * Twilio Recording Callback URL
     * Twilio가 녹음 완료 시 알림을 보냄
     * https://your-domain.com/api/webhooks/twilio/recording
     */
    public String getRecordingCallbackUrl() {
        return webhookBaseUrl + "/api/webhooks/twilio/recording";
    }
}