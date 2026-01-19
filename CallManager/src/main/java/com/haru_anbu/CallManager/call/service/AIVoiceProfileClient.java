package com.haru_anbu.CallManager.call.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.time.Duration;

/**
 * AI 서버 API 클라이언트 (WebClient 버전)
 * voice_profiles 테이블 업데이트를 위한 API 호출
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIVoiceProfileClient {
    
    private final WebClient webClient;
    
    @Value("${ai-core.base-url:http://localhost:8001}")
    private String aiCoreBaseUrl;
    
    @Value("${ai-core.api-key:}")
    private String apiKey;
    
    @Value("${ai-core.timeout-seconds:30}")
    private int timeoutSeconds;
    
    /**
     * AI 서버에 음성 프로필 경로 업데이트 요청 (동기)
     * 
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     * @param s3Path S3 경로 (s3://bucket/key)
     * @return 성공 여부
     */
    public boolean updateVoiceProfilePath(String userId, String sessionId, String s3Path) {
        try {
            String url = aiCoreBaseUrl + "/api/voice-profiles/update-path";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user_id", userId);
            requestBody.put("session_id", sessionId);
            requestBody.put("raw_wav_path", s3Path);
            requestBody.put("updated_at", System.currentTimeMillis());
            
            // 동기 호출 (block)
            Map<String, Object> response = webClient.post()
                .uri(url)
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                log.info("Successfully updated voice profile path for user: {}, session: {}", 
                    userId, sessionId);
                return true;
            } else {
                log.warn("Failed to update voice profile path. Response: {}", response);
                return false;
            }
            
        } catch (WebClientResponseException e) {
            log.error("HTTP error updating voice profile path. Status: {}, Body: {}", 
                e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (Exception e) {
            log.error("Error updating voice profile path for user: {}, session: {}", 
                userId, sessionId, e);
            return false;
        }
    }
    
    /**
     * AI 서버에 음성 프로필 경로 업데이트 요청 (비동기)
     * 
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     * @param s3Path S3 경로
     * @return Mono<Boolean>
     */
    public Mono<Boolean> updateVoiceProfilePathAsync(String userId, String sessionId, String s3Path) {
        String url = aiCoreBaseUrl + "/api/voice-profiles/update-path";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_id", userId);
        requestBody.put("session_id", sessionId);
        requestBody.put("raw_wav_path", s3Path);
        requestBody.put("updated_at", System.currentTimeMillis());
        
        return webClient.post()
            .uri(url)
            .header("X-API-Key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .map(response -> Boolean.TRUE.equals(response.get("success")))
            .doOnSuccess(success -> {
                if (success) {
                    log.info("Successfully updated voice profile path (async) for user: {}", userId);
                }
            })
            .doOnError(error -> 
                log.error("Error updating voice profile path (async) for user: {}", userId, error)
            )
            .onErrorReturn(false);
    }
    
    /**
     * AI 서버에 음성 프로필 생성 요청 (동기)
     */
    public boolean createVoiceProfile(String userId, String sessionId, String s3Path, 
                                     String phoneNumber) {
        try {
            String url = aiCoreBaseUrl + "/api/voice-profiles/create";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user_id", userId);
            requestBody.put("session_id", sessionId);
            requestBody.put("raw_wav_path", s3Path);
            requestBody.put("phone_number", phoneNumber);
            requestBody.put("created_at", System.currentTimeMillis());
            
            Map<String, Object> response = webClient.post()
                .uri(url)
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                log.info("Successfully created voice profile for user: {}", userId);
                return true;
            } else {
                log.warn("Failed to create voice profile. Response: {}", response);
                return false;
            }
            
        } catch (WebClientResponseException e) {
            log.error("HTTP error creating voice profile. Status: {}, Body: {}", 
                e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (Exception e) {
            log.error("Error creating voice profile for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 음성 프로필 존재 여부 확인 (동기)
     */
    public boolean voiceProfileExists(String userId) {
        try {
            String url = aiCoreBaseUrl + "/api/voice-profiles/" + userId + "/exists";
            
            Map<String, Object> response = webClient.get()
                .uri(url)
                .header("X-API-Key", apiKey)
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();
            
            return response != null && Boolean.TRUE.equals(response.get("exists"));
            
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return false;
            }
            log.error("HTTP error checking voice profile existence. Status: {}", 
                e.getStatusCode(), e);
            return false;
        } catch (Exception e) {
            log.error("Error checking voice profile existence for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 음성 프로필 존재 여부 확인 (비동기)
     */
    public Mono<Boolean> voiceProfileExistsAsync(String userId) {
        String url = aiCoreBaseUrl + "/api/voice-profiles/" + userId + "/exists";
        
        return webClient.get()
            .uri(url)
            .header("X-API-Key", apiKey)
            .retrieve()
            .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .map(response -> Boolean.TRUE.equals(response.get("exists")))
            .onErrorReturn(WebClientResponseException.NotFound.class, false)
            .onErrorReturn(false);
    }
}
