package com.haru_anbu.CallManager.call.service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers; // 스레드 제어를 위해 필수

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordingService {
    
    private final S3StorageService s3StorageService;
    private final CallSessionService sessionService;
    private final AIVoiceProfileClient aiVoiceProfileClient;
    
    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;
    
    /**
     * Twilio 녹음 다운로드 및 S3 저장 (동기/블로킹 구간)
     */
    public String downloadAndSaveRecording(String twilioCallSid, String twilioRecordingUrl) {
        try {
            log.info("Starting recording download process for CallSid: {}", twilioCallSid);
            
            // 1. 세션 정보 조회 (DB 블로킹)
            var callSession = sessionService.getSessionByTwilioCallSid(twilioCallSid);
            String sessionId = callSession.getSessionId();
            String userId = callSession.getUserId();
            String phoneNumber = callSession.getPhoneNumber();
            
            // 2. 인증 헤더 준비
            String auth = accountSid + ":" + authToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + encodedAuth;

            // 3. Twilio 다운로드 및 S3 업로드
            URL url = new URL(twilioRecordingUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", authHeaderValue);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Twilio download failed: HTTP " + connection.getResponseCode());
            }

            try (InputStream inputStream = connection.getInputStream()) {
                String fileName = String.format("recording_%s_%d.wav", sessionId, System.currentTimeMillis());
                byte[] recordingData = inputStream.readAllBytes();
                
                String s3Url = s3StorageService.saveRecording(sessionId, recordingData, fileName, "audio/wav");
                log.info("Recording successfully uploaded to S3: {}", s3Url);
                
                // 4. DB 업데이트
                sessionService.updateRecordingUrl(sessionId, s3Url);
                
                // 5. AI 서버 연동 (여기서부터 비동기/논블로킹 전환)
                updateAIVoiceProfileAsync(userId, sessionId, s3Url, phoneNumber);
                
                return s3Url;
            }
        } catch (Exception e) {
            log.error("Failed to process recording for CallSid: {}", twilioCallSid, e);
            throw new RuntimeException("Recording processing failed", e);
        }
    }
    
    /**
     * AI 서버 음성 프로필 연동 (비동기 체이닝)
     */
    private void updateAIVoiceProfileAsync(String userId, String sessionId, String s3Path, String phoneNumber) {
        // 1. 존재 여부 확인 (비동기)
        aiVoiceProfileClient.voiceProfileExistsAsync(userId)
            .flatMap(exists -> {
                if (exists) {
                    // 2-A. 존재하면 경로 업데이트 (Mono<Boolean> 반환됨)
                    log.info("Voice profile exists. Updating path for user: {}", userId);
                    return aiVoiceProfileClient.updateVoiceProfilePathAsync(userId, sessionId, s3Path);
                } else {
                    // 2-B. 존재하지 않으면 생성 요청
                    log.info("Voice profile not found. Requesting creation for user: {}", userId);
                    // 클라이언트의 createVoiceProfile은 void이며 내부에서 .subscribe() 하므로 
                    // Mono.fromRunnable로 감싸서 체인에 연결만 해줌
                    return Mono.fromRunnable(() -> 
                        aiVoiceProfileClient.createVoiceProfile(userId, sessionId, s3Path, phoneNumber)
                    ).thenReturn(true); // Void를 Boolean으로 변환하여 흐름 유지
                }
            })
            // 중요: 모든 비동기 작업 후의 후속 로직을 NIO 스레드가 아닌 Elastic 스레드에서 수행하도록 보장
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                result -> log.info("AI Voice Profile sync flow completed for user: {}", userId),
                error -> log.error("Critical error in AI Voice Profile sync for user: {}", userId, error)
            );
    }

    public String getRecordingDownloadUrl(String sessionId) {
        var callSession = sessionService.getSessionBySessionId(sessionId);
        if (callSession == null) throw new RuntimeException("존재하지 않는 세션 ID: " + sessionId);

        String s3Url = callSession.getRecordingUrl();
        if (s3Url == null || s3Url.isEmpty()) throw new RuntimeException("녹음 파일 URL이 없습니다.");
        
        return s3StorageService.generatePresignedUrl(s3Url);
    }
        
    public void deleteRecording(String sessionId) {
        try {
            var callSession = sessionService.getSessionBySessionId(sessionId);
            String s3Url = callSession.getRecordingUrl();
            if (s3Url != null && !s3Url.isEmpty()) {
                s3StorageService.deleteFile(s3Url);
                log.info("Deleted recording for session: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Failed to delete recording", e);
        }
    }
}
