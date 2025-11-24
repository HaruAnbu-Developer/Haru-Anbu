package com.haru_anbu.CallManager.call.service;

import java.net.URI;

import org.springframework.stereotype.Service;

import com.haru_anbu.CallManager.call.config.TwilioConfig;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Connect;
import com.twilio.twiml.voice.Say;
import com.twilio.twiml.voice.Stream;
import com.twilio.type.PhoneNumber;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioService {
    private final TwilioConfig twilioConfig;

    // 통화를 시작하는 메소드
    public String initiateCall(String toPhoneNumber , String userId) {
        try {
            log.info("Initiating call to {} for user {}", toPhoneNumber , userId);

            Call call = Call.creator(
                        new PhoneNumber(toPhoneNumber),
                        new PhoneNumber(twilioConfig.getPhoneNumber()),
                        URI.create(twilioConfig.getWebhookBaseUrl() + "/api/calls/twilio/voice?userId=" + userId)
                        )
                        .setStatusCallback(URI.create(twilioConfig.getStatusCallbackUrl()))
                        .setStatusCallbackEvent(java.util.Arrays.asList("initiated", "ringing", "answered", "completed"))
                        .setStatusCallbackMethod(com.twilio.http.HttpMethod.POST)
                        .setRecord(true)
                        .setRecordingStatusCallback(twilioConfig.getWebhookBaseUrl() + "/api/calls/twilio/recording")
                        .create();
            log.info("Call initiated with SID: {}", call.getSid());
            
            return call.getSid();

        } catch (Exception e) {
            log.error("Failed to initiate call : {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initiate call", e);
        }
    }

    // TwiML 생성 - 통화 연결 시 응답
    // XML을 반환
    public String generateConnectTwiML(String sessionId) {
        try {
            // WebSocket 스트림 URL
            String streamUrl = "wss://" + twilioConfig.getWebhookBaseUrl().replace("https://", "") 
                + "/ws/twilio/media-stream?sessionId=" + sessionId;
            
            Stream stream = new Stream.Builder()
                .url(streamUrl)
                .build();

            Connect connect = new Connect.Builder()
                .stream(stream)
                .build();

            Say greeting = new Say.Builder("안녕하세요. 안부 전화 드립니다.")
                .language(Say.Language.KO_KR)
                .build();

            VoiceResponse response = new VoiceResponse.Builder()
                .say(greeting)
                .connect(connect)
                .build();

            return response.toXml();
            
        } catch (Exception e) {
            log.error("Failed to generate TwiML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate TwiML", e);        }
    }
}
