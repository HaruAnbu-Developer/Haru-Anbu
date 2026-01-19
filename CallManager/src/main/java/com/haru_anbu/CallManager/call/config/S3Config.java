package com.haru_anbu.CallManager.call.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import jakarta.annotation.PreDestroy;

@Configuration
@Slf4j
@Getter
public class S3Config {
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    @Value("${aws.s3.region:ap-southeast-2}")
    private String region;
    
    @Value("${aws.s3.access-key}")
    private String accessKey;
    
    @Value("${aws.s3.secret-key}")
    private String secretKey;
    
    @Value("${aws.s3.recordings-prefix:uploads/}")
    private String recordingsPrefix;
    
    @Value("${aws.s3.audio-chunks-prefix:audio-chunks/}")
    private String audioChunksPrefix;
    
    @Value("${aws.s3.presigned-url-duration-minutes:60}")
    private int presignedUrlDurationMinutes;
    
    private S3Client s3Client;
    private S3Presigner s3Presigner;
    
    @Bean
    public S3Client s3Client() {
        if (s3Client == null) {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
            
            s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
            
            log.info("S3 Client initialized for region: {}, bucket: {}", region, bucketName);
        }
        return s3Client;
    }
    
    @Bean
    public S3Presigner s3Presigner() {
        if (s3Presigner == null) {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
            
            s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
            
            log.info("S3 Presigner initialized");
        }
        return s3Presigner;
    }
    
    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            s3Client.close();
            log.info("S3 Client closed");
        }
        if (s3Presigner != null) {
            s3Presigner.close();
            log.info("S3 Presigner closed");
        }
    }
    
    public String getRecordingKey(String sessionId, String fileName) {
        return recordingsPrefix + sessionId + "/" + fileName;
    }
    
    public String getAudioChunkKey(String sessionId, long timestamp, String direction) {
        return audioChunksPrefix + sessionId + "/" + direction + "/" + timestamp + ".pcm";
    }
}
