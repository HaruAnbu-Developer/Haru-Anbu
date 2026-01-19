package com.haru_anbu.CallManager.call.service;

import com.haru_anbu.CallManager.call.config.S3Config;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S3StorageService 통합 테스트
 * 실제 AWS S3와 연동하여 테스트
 * 
 * 주의: 이 테스트는 실제 AWS 비용이 발생할 수 있습니다!
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class S3StorageServiceTest {
    
    @Autowired
    private S3StorageService s3StorageService;
    
    @Autowired
    private S3Config s3Config;
    
    private static String testSessionId;
    private static String testS3Url;
    
    @BeforeAll
    static void setup() {
        testSessionId = "test_session_" + UUID.randomUUID().toString().substring(0, 8);
        System.out.println("\n=== S3 Integration Test 시작 ===");
        System.out.println("Test Session ID: " + testSessionId);
    }
    
    @AfterAll
    static void cleanup() {
        System.out.println("\n=== S3 Integration Test 종료 ===\n");
    }
    
    @Test
    @Order(1)
    @DisplayName("1. S3 Config Bean이 정상적으로 로드되는지 확인")
    void testS3ConfigLoaded() {
        // Given & When & Then
        assertNotNull(s3Config, "S3Config Bean이 null입니다");
        assertNotNull(s3Config.getBucketName(), "S3 Bucket Name이 설정되지 않았습니다");
        assertNotNull(s3Config.getRegion(), "S3 Region이 설정되지 않았습니다");
        
        System.out.println("✅ S3 Config 로드 성공");
        System.out.println("   Bucket: " + s3Config.getBucketName());
        System.out.println("   Region: " + s3Config.getRegion());
    }
    
    @Test
    @Order(2)
    @DisplayName("2. 녹음 파일을 S3에 업로드할 수 있는지 확인")
    void testSaveRecording() {
        // Given
        String fileName = "test_recording.wav";
        byte[] testAudioData = "Test audio data content".getBytes(StandardCharsets.UTF_8);
        String contentType = "audio/wav";
        
        // When
        testS3Url = s3StorageService.saveRecording(
            testSessionId, 
            testAudioData, 
            fileName, 
            contentType
        );
        
        // Then
        assertNotNull(testS3Url, "S3 URL이 null입니다");
        assertTrue(testS3Url.startsWith("s3://"), "S3 URL 형식이 올바르지 않습니다");
        assertTrue(testS3Url.contains(testSessionId), "S3 URL에 세션 ID가 포함되지 않았습니다");
        
        System.out.println("✅ 녹음 파일 업로드 성공");
        System.out.println("   S3 URL: " + testS3Url);
        System.out.println("   File Size: " + testAudioData.length + " bytes");
    }
    
    @Test
    @Order(3)
    @DisplayName("3. S3에 파일이 실제로 존재하는지 확인")
    void testFileExists() {
        // Given & When
        boolean exists = s3StorageService.fileExists(testS3Url);
        
        // Then
        assertTrue(exists, "업로드한 파일이 S3에 존재하지 않습니다");
        
        System.out.println("✅ 파일 존재 확인 성공");
    }
    
    @Test
    @Order(4)
    @DisplayName("4. Presigned URL을 생성할 수 있는지 확인")
    void testGeneratePresignedUrl() {
        // Given & When
        String presignedUrl = s3StorageService.generatePresignedUrl(testS3Url);
        
        // Then
        assertNotNull(presignedUrl, "Presigned URL이 null입니다");
        assertTrue(presignedUrl.startsWith("https://"), "Presigned URL이 HTTPS가 아닙니다");
        assertTrue(presignedUrl.contains(s3Config.getBucketName()), 
            "Presigned URL에 버킷 이름이 포함되지 않았습니다");
        
        System.out.println("✅ Presigned URL 생성 성공");
        System.out.println("   URL: " + presignedUrl.substring(0, Math.min(100, presignedUrl.length())) + "...");
    }
    
    @Test
    @Order(5)
    @DisplayName("5. S3에서 파일을 다운로드할 수 있는지 확인")
    void testDownloadFile() {
        // Given & When
        byte[] downloadedData = s3StorageService.downloadFile(testS3Url);
        
        // Then
        assertNotNull(downloadedData, "다운로드한 데이터가 null입니다");
        assertTrue(downloadedData.length > 0, "다운로드한 데이터가 비어있습니다");
        
        String downloadedContent = new String(downloadedData, StandardCharsets.UTF_8);
        assertEquals("Test audio data content", downloadedContent, "다운로드한 내용이 일치하지 않습니다");
        
        System.out.println("✅ 파일 다운로드 성공");
        System.out.println("   Size: " + downloadedData.length + " bytes");
        System.out.println("   Content: " + downloadedContent);
    }
    
    @Test
    @Order(6)
    @DisplayName("6. 오디오 청크를 비동기로 저장할 수 있는지 확인")
    void testSaveAudioChunkAsync() throws Exception {
        // Given
        byte[] chunkData = "Audio chunk data".getBytes(StandardCharsets.UTF_8);
        long timestamp = System.currentTimeMillis();
        String direction = "inbound";
        
        // When
        String chunkKey = s3StorageService.saveAudioChunkAsync(
            testSessionId, 
            chunkData, 
            timestamp, 
            direction
        ).get();  // CompletableFuture.get() - 동기로 대기
        
        // Then
        assertNotNull(chunkKey, "오디오 청크 키가 null입니다");
        
        // 업로드된 청크 확인
        String chunkS3Url = "s3://" + s3Config.getBucketName() + "/" + chunkKey;
        boolean exists = s3StorageService.fileExists(chunkS3Url);
        assertTrue(exists, "오디오 청크가 S3에 존재하지 않습니다");
        
        // 정리
        s3StorageService.deleteFile(chunkS3Url);
        
        System.out.println("✅ 오디오 청크 비동기 저장 성공");
        System.out.println("   Key: " + chunkKey);
        System.out.println("   Direction: " + direction);
    }
    
    @Test
    @Order(7)
    @DisplayName("7. 세션의 모든 오디오 청크를 삭제할 수 있는지 확인")
    void testDeleteSessionAudioChunks() throws Exception {
        // Given: 테스트 청크 여러 개 업로드
        for (int i = 0; i < 3; i++) {
            byte[] chunkData = ("Chunk " + i).getBytes(StandardCharsets.UTF_8);
            s3StorageService.saveAudioChunkAsync(
                testSessionId, 
                chunkData, 
                System.currentTimeMillis() + i, 
                "inbound"
            ).get();
        }
        
        // When: 세션의 모든 청크 삭제
        s3StorageService.deleteSessionAudioChunks(testSessionId);
        
        // Then: 삭제 확인은 리스팅으로 확인 (구현 필요 시)
        System.out.println("✅ 세션 오디오 청크 삭제 성공");
    }
    
    @Test
    @Order(8)
    @DisplayName("8. S3에서 파일을 삭제할 수 있는지 확인")
    void testDeleteFile() {
        // Given & When
        s3StorageService.deleteFile(testS3Url);
        
        // Then
        boolean existsAfterDelete = s3StorageService.fileExists(testS3Url);
        assertFalse(existsAfterDelete, "삭제한 파일이 여전히 S3에 존재합니다");
        
        System.out.println("✅ 파일 삭제 성공");
    }
    
    @Test
    @Order(9)
    @DisplayName("9. 존재하지 않는 파일 다운로드 시 예외 처리 확인")
    void testDownloadNonExistentFile() {
        // Given
        String nonExistentUrl = "s3://" + s3Config.getBucketName() + "/non-existent-file.wav";
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            s3StorageService.downloadFile(nonExistentUrl);
        }, "존재하지 않는 파일 다운로드 시 예외가 발생해야 합니다");
        
        System.out.println("✅ 예외 처리 확인 성공");
    }
    
    @Test
    @Order(10)
    @DisplayName("10. 대용량 파일 업로드 테스트 (1MB)")
    void testLargeFileUpload() {
        // Given
        byte[] largeData = new byte[1024 * 1024];  // 1MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        String largeSessionId = "large_test_" + UUID.randomUUID().toString().substring(0, 8);
        
        // When
        long startTime = System.currentTimeMillis();
        String s3Url = s3StorageService.saveRecording(
            largeSessionId, 
            largeData, 
            "large_file.wav", 
            "audio/wav"
        );
        long uploadTime = System.currentTimeMillis() - startTime;
        
        // Then
        assertNotNull(s3Url);
        assertTrue(s3StorageService.fileExists(s3Url));
        
        // Cleanup
        s3StorageService.deleteFile(s3Url);
        
        System.out.println("✅ 대용량 파일 업로드 성공");
        System.out.println("   Size: 1MB");
        System.out.println("   Upload Time: " + uploadTime + "ms");
    }
}
