package com.haru_anbu.CallManager.call.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.net.URL;

/**
 * Twilio 녹음 파일을 S3로 전송하고 AI 서버에 경로 업데이트
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordingService {
    
    private final S3StorageService s3StorageService;
    private final CallSessionService sessionService;
    private final AIVoiceProfileClient aiVoiceProfileClient;
    
    /**
     * Twilio 녹음 URL을 다운로드하여 S3에 저장 후 AI 서버에 경로 전달
     */
    public String downloadAndSaveRecording(String twilioCallSid, String twilioRecordingUrl) {
        try {
            log.info("Downloading recording from Twilio: {}", twilioRecordingUrl);
            
            // 1. 세션 정보 조회
            var callSession = sessionService.getSessionByTwilioCallSid(twilioCallSid);
            String sessionId = callSession.getSessionId();
            String userId = callSession.getUserId();
            String phoneNumber = callSession.getPhoneNumber();
            
            // 2. Twilio에서 녹음 다운로드
            URL url = new URL(twilioRecordingUrl);
            try (InputStream inputStream = url.openStream()) {
                
                String fileName = String.format("recording_%s_%d.wav", 
                    sessionId, System.currentTimeMillis());
                
                // 3. S3에 업로드
                byte[] recordingData = inputStream.readAllBytes();
                String s3Url = s3StorageService.saveRecording(
                    sessionId, 
                    recordingData, 
                    fileName, 
                    "audio/wav"
                );
                
                log.info("Recording saved to S3: {}", s3Url);
                
                // 4. CallManager DB에 S3 URL 저장
                sessionService.updateRecordingUrl(sessionId, s3Url);
                
                // 5. AI 서버에 음성 프로필 경로 업데이트 (비동기)
                updateAIVoiceProfileAsync(userId, sessionId, s3Url, phoneNumber);
                
                return s3Url;
            }
            
        } catch (Exception e) {
            log.error("Failed to download and save recording", e);
            throw new RuntimeException("Recording save failed", e);
        }
    }
    
    /**
     * AI 서버에 음성 프로필 경로 업데이트 (비동기)
     * WebClient의 Reactive 기능 활용
     */
    private void updateAIVoiceProfileAsync(String userId, String sessionId, String s3Path, 
                                          String phoneNumber) {
        // 1. 음성 프로필 존재 여부 확인
        // 2. 존재하면 업데이트, 없으면 생성
        // 3. 비동기로 처리하여 메인 플로우 블로킹 방지
        aiVoiceProfileClient.voiceProfileExistsAsync(userId)
            .flatMap(exists -> {
                if (exists) {
                    // 기존 프로필 업데이트
                    log.info("Updating existing voice profile for user: {}", userId);
                    return aiVoiceProfileClient.updateVoiceProfilePathAsync(
                        userId, sessionId, s3Path);
                } else {
                    // 신규 프로필 생성
                    log.info("Creating new voice profile for user: {}", userId);
                    // createVoiceProfile을 Mono로 래핑 (동기 메서드를 비동기로)
                    return Mono.fromCallable(() ->
                        aiVoiceProfileClient.createVoiceProfile(
                            userId, sessionId, s3Path, phoneNumber)
                    );
                }
            })
            .subscribe(
                success -> {
                    if (success) {
                        log.info("Successfully updated AI voice profile for user: {}", userId);
                    } else {
                        log.error("Failed to update AI voice profile for user: {}", userId);
                    }
                },
                error -> {
                    log.error("Error updating AI voice profile for user: {}", userId, error);
                    // TODO: 실패 시 재시도 큐에 추가 또는 알림 전송
                }
            );
    }
    
    /**
     * Presigned URL 생성 (다운로드용 임시 링크)
     */
    public String getRecordingDownloadUrl(String sessionId) {
        // 1. 세션 조회
        // sessionService.getSessionBySessionId 내부에서 예외를 던지지 않는다면 여기서 직접 던져야 합니다.
        var callSession = sessionService.getSessionBySessionId(sessionId);
        
        if (callSession == null) {
            throw new RuntimeException("존재하지 않는 세션 ID입니다: " + sessionId);
        }

        // 2. 녹음 URL 확인
        String s3Url = callSession.getRecordingUrl();
        
        if (s3Url == null || s3Url.isEmpty()) {
            log.warn("No recording URL for session: {}", sessionId);
            // 테스트 코드의 assertThrows를 만족시키기 위해 예외를 던집니다.
            throw new RuntimeException("해당 세션에 녹음 파일 URL이 없습니다.");
        }
        
        // 3. Presigned URL 생성
        // s3StorageService에서 발생하는 예외는 그대로 밖으로 던져지게 둡니다. (try-catch 제거)
        return s3StorageService.generatePresignedUrl(s3Url);
    }
        
    /**
     * 녹음 파일 삭제
     */
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
