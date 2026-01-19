package com.haru_anbu.CallManager.call.controller;

import com.haru_anbu.CallManager.call.service.RecordingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 녹음 파일 관리 API
 */
@RestController
@RequestMapping("/api/recordings")
@RequiredArgsConstructor
@Slf4j
public class RecordingController {
    
    private final RecordingService recordingService;
    
    /**
     * 녹음 파일 다운로드 URL 조회
     */
    @GetMapping("/sessions/{sessionId}/download-url")
    public ResponseEntity<Map<String, Object>> getRecordingDownloadUrl(
            @PathVariable String sessionId) {
        
        log.info("Requesting download URL for session: {}", sessionId);
        
        try {
            String downloadUrl = recordingService.getRecordingDownloadUrl(sessionId);
            
            if (downloadUrl == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("downloadUrl", downloadUrl);
            response.put("expiresIn", "60 minutes");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get download URL for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 녹음 파일 삭제
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteRecording(
            @PathVariable String sessionId) {
        
        log.info("Deleting recording for session: {}", sessionId);
        
        try {
            recordingService.deleteRecording(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recording deleted successfully");
            response.put("sessionId", sessionId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to delete recording for session: {}", sessionId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete recording: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
