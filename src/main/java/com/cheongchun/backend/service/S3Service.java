package com.cheongchun.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URL;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(byte[] fileData, String fileName) {
        try {
            log.info("Uploading file to S3: {}", fileName);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("audio/wav")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));

            return getFileUrl(fileName);
        } catch (Exception e) {
            log.error("S3 Upload Failed", e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public String getFileUrl(String fileName) {
        URL url = s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build());
        return url.toString();
    }
}
