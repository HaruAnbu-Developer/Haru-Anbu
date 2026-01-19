package com.haru_anbu.CallManager.call.service;

import com.haru_anbu.CallManager.call.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config s3Config;
    
    /**
     * 음성 청크를 S3에 비동기 저장
     * @param sessionId 세션 ID
     * @param audioData PCM 오디오 데이터
     * @param timestamp 타임스탬프
     * @param direction 방향 (inbound/outbound)
     * @return S3 키
     */
    public CompletableFuture<String> saveAudioChunkAsync(
            String sessionId, byte[] audioData, long timestamp, String direction) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = s3Config.getAudioChunkKey(sessionId, timestamp, direction);
                
                Map<String, String> metadata = new HashMap<>();
                metadata.put("session-id", sessionId);
                metadata.put("timestamp", String.valueOf(timestamp));
                metadata.put("direction", direction);
                metadata.put("size", String.valueOf(audioData.length));
                
                PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .contentType("audio/pcm")
                    .contentLength((long) audioData.length)
                    .metadata(metadata)
                    .build();
                
                s3Client.putObject(putRequest, RequestBody.fromBytes(audioData));
                
                log.debug("Audio chunk saved to S3: {}", key);
                return key;
                
            } catch (Exception e) {
                log.error("Failed to save audio chunk to S3", e);
                throw new RuntimeException("S3 upload failed", e);
            }
        });
    }
    
    /**
     * 통화 녹음 파일을 S3에 저장
     * @param sessionId 세션 ID
     * @param audioData 오디오 데이터
     * @param fileName 파일명
     * @param contentType 콘텐츠 타입 (audio/wav, audio/mp3 등)
     * @return S3 URL
     */
    public String saveRecording(String sessionId, byte[] audioData, 
                                String fileName, String contentType) {
        try {
            String key = s3Config.getRecordingKey(sessionId, fileName);
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("session-id", sessionId);
            metadata.put("file-name", fileName);
            metadata.put("uploaded-at", String.valueOf(System.currentTimeMillis()));
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .contentType(contentType)
                .contentLength((long) audioData.length)
                .metadata(metadata)
                .build();
            
            s3Client.putObject(putRequest, RequestBody.fromBytes(audioData));
            
            log.info("Recording saved to S3: {}", key);
            
            // S3 URL 반환
            return String.format("s3://%s/%s", s3Config.getBucketName(), key);
            
        } catch (Exception e) {
            log.error("Failed to save recording to S3", e);
            throw new RuntimeException("S3 recording upload failed", e);
        }
    }
    
    /**
     * InputStream으로부터 녹음 저장
     */
    public String saveRecording(String sessionId, InputStream inputStream, 
                                String fileName, String contentType, long contentLength) {
        try {
            String key = s3Config.getRecordingKey(sessionId, fileName);
            
            Map<String, String> metadata = new HashMap<>();
            metadata.put("session-id", sessionId);
            metadata.put("file-name", fileName);
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .metadata(metadata)
                .build();
            
            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
            
            log.info("Recording saved to S3 from InputStream: {}", key);
            return String.format("s3://%s/%s", s3Config.getBucketName(), key);
            
        } catch (Exception e) {
            log.error("Failed to save recording from InputStream", e);
            throw new RuntimeException("S3 recording upload failed", e);
        }
    }
    
    /**
     * Presigned URL 생성 (임시 다운로드 링크)
     */
    public String generatePresignedUrl(String s3Url) {
        try {
            // s3://bucket/key 형식에서 key 추출
            String key = s3Url.replace("s3://" + s3Config.getBucketName() + "/", "");
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(s3Config.getPresignedUrlDurationMinutes()))
                .getObjectRequest(getObjectRequest)
                .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            URL url = presignedRequest.url();
            
            log.debug("Generated presigned URL for: {}", key);
            return url.toString();
            
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            return null;
        }
    }
    
    /**
     * S3에서 파일 다운로드
     */
    public byte[] downloadFile(String s3Url) {
        try {
            String key = s3Url.replace("s3://" + s3Config.getBucketName() + "/", "");
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .build();
            
            return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
            
        } catch (Exception e) {
            log.error("Failed to download file from S3", e);
            throw new RuntimeException("S3 download failed", e);
        }
    }
    
    /**
     * S3에서 파일 삭제
     */
    public void deleteFile(String s3Url) {
        try {
            String key = s3Url.replace("s3://" + s3Config.getBucketName() + "/", "");
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .build();
            
            s3Client.deleteObject(deleteRequest);
            log.info("Deleted file from S3: {}", key);
            
        } catch (Exception e) {
            log.error("Failed to delete file from S3", e);
            throw new RuntimeException("S3 deletion failed", e);
        }
    }
    
    /**
     * 세션의 모든 오디오 청크 삭제
     */
    public void deleteSessionAudioChunks(String sessionId) {
        try {
            String prefix = s3Config.getAudioChunksPrefix() + sessionId + "/";
            
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(s3Config.getBucketName())
                .prefix(prefix)
                .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            for (S3Object s3Object : listResponse.contents()) {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(s3Object.key())
                    .build();
                
                s3Client.deleteObject(deleteRequest);
            }
            
            log.info("Deleted all audio chunks for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Failed to delete session audio chunks", e);
        }
    }
    
    /**
     * 파일 존재 여부 확인
     */
    public boolean fileExists(String s3Url) {
        try {
            String key = s3Url.replace("s3://" + s3Config.getBucketName() + "/", "");
            
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .build();
            
            s3Client.headObject(headRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking file existence", e);
            return false;
        }
    }
}
