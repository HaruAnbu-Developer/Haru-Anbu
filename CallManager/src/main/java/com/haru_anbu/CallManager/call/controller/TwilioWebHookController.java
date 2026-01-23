package com.haru_anbu.CallManager.call.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.haru_anbu.CallManager.call.service.CallManagerService;
import com.haru_anbu.CallManager.call.service.RecordingService;
import com.haru_anbu.CallManager.call.service.TwilioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("api/call/twilio")
@RequiredArgsConstructor
@Slf4j
public class TwilioWebHookController {
    
    private final TwilioService twilioService;
    private final CallManagerService callManagerService;
    private final RecordingService recordingService; // 추가

    // Voice Webhook 핸들러 - 통화 연결 시 TwiML 반환
    // Twilio가 통화 연결 시 이 엔드포인트를 호출
    @PostMapping(value = "/voice", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleVoiceWebhook(
            @RequestParam String CallSid,
            @RequestParam(required = false) String userId) {
            log.info("Received Voice Webhook: CallSid={}, userId={}", CallSid, userId);

        try {
            // userId를 기반으로 세션 ID 조회, 생성
            // 실제로는 CallSid로 DB에서 세션을 찾기
            String sessionId = "sess_" + CallSid.substring(CallSid.length() - 16);

            // TwiML 생성
            String twiml = twilioService.generateConnectTwiML(sessionId);

            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_XML)
                .body(twiml);
        } catch (Exception e) {
            log.error("Error handling Voice Webhook: {}", e.getMessage());
            return ResponseEntity.status(500).body("<Response><Say>There was an error processing your call.</Say></Response>");
        }

    }

    // Status Callback 핸들러 - 통화 상태 변경 알림
    @PostMapping("/status")
    public ResponseEntity<Void> handleStatusCallback(
            @RequestParam String CallSid,
            @RequestParam String CallStatus,
            @RequestParam(required = false) String Duration) {
        log.info("Status Callback: CallSid={}, CallStatus={}, Duration={}", CallSid, CallStatus, Duration);

        try {
            callManagerService.updateCallStatus(CallSid, CallStatus);
        } catch (Exception e) {
            log.error("Error handling Status Callback: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }


    /**
     * Recording Callback - Twilio 녹음을 S3로 저장
     */
    @PostMapping("/recording")
    public ResponseEntity<Void> handleRecordingCallback(
            @RequestParam String CallSid,
            @RequestParam String RecordingUrl,
            @RequestParam String RecordingSid,
            @RequestParam(required = false) String RecordingDuration) {
        log.info("Recording Callback: CallSid={}, RecordingUrl={}, RecordingSid={}, Duration={}", 
            CallSid, RecordingUrl, RecordingSid, RecordingDuration);

        try {
            // Twilio 녹음 URL에 .wav 확장자 추가 (필요시)
            String downloadUrl = RecordingUrl;
            if (!downloadUrl.endsWith(".wav")) {
                downloadUrl = downloadUrl + ".wav";
            }
            
            // Twilio에서 다운로드 후 S3에 저장
            String s3Url = recordingService.downloadAndSaveRecording(CallSid, downloadUrl);
            
            log.info("Recording saved to S3: {}", s3Url);
            
        } catch (Exception e) {
            log.error("Error handling Recording Callback: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}