package com.haru_anbu.CallManager.call.controller;

import com.haru_anbu.CallManager.call.dto.CallRequestResponse;
import com.haru_anbu.CallManager.call.dto.CallSessionDto;
import com.haru_anbu.CallManager.call.entity.CallSession;
import com.haru_anbu.CallManager.call.service.CallManagerService;
import com.haru_anbu.CallManager.call.service.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
@Slf4j
public class CallController {
    
    private final CallManagerService callManagerService;
    private final CallSessionService sessionService;
    
    /**
     * 통화 시작
     */
    @PostMapping("/initiate")
    public ResponseEntity<CallRequestResponse.InitiateCallResponse> initiateCall(
            @RequestBody CallRequestResponse.InitiateCallRequest request) {
        
        log.info("Initiating call request: user={}, phone={}", 
            request.getUserId(), request.getPhoneNumber());
        
        try {
            // metadata 생성
            Map<String, String> metadata = new HashMap<>();
            if (request.getMetadata() != null) {
                metadata.putAll(request.getMetadata());
            }
            if (request.getPurpose() != null) {
                metadata.put("purpose", request.getPurpose());
            }
            
            CallSession session = callManagerService.initiateCall(
                request.getUserId(),
                request.getPhoneNumber(),
                metadata
            );
            
            return ResponseEntity.ok(
                CallRequestResponse.InitiateCallResponse.builder()
                    .success(true)
                    .message("Call initiated successfully")
                    .sessionId(session.getSessionId())
                    .twilioCallSid(session.getTwilioCallSid())
                    .status(session.getStatus())
                    .createdAt(session.getCreatedAt())
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Failed to initiate call", e);
            return ResponseEntity.internalServerError().body(
                CallRequestResponse.InitiateCallResponse.builder()
                    .success(false)
                    .message("Failed to initiate call: " + e.getMessage())
                    .build()
            );
        }
    }
    
    /**
     * 세션 상태 조회
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<CallSessionDto.Response> getSession(
            @PathVariable String sessionId) {
        
        try {
            CallSession session = sessionService.getSessionBySessionId(sessionId);
            return ResponseEntity.ok(CallSessionDto.Response.from(session));
        } catch (Exception e) {
            log.error("Session not found: {}", sessionId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 사용자의 모든 세션 조회
     */
    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<CallRequestResponse.SessionListResponse> getUserSessions(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "30") int days,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize) {
        
        List<CallSessionDto.Response> sessions = 
            sessionService.getRecentUserSessions(userId, days);
        
        return ResponseEntity.ok(
            CallRequestResponse.SessionListResponse.builder()
                .totalCount(sessions.size())
                .page(page)
                .pageSize(pageSize)
                .sessions(sessions)
                .build()
        );
    }
    
    /**
     * 통화 종료
     */
    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<CallRequestResponse.EndCallResponse> endCall(
            @PathVariable String sessionId,
            @RequestBody(required = false) CallRequestResponse.EndCallRequest request) {
        
        String reason = request != null && request.getReason() != null 
            ? request.getReason() 
            : "user_initiated";
        
        log.info("Ending call: {} - {}", sessionId, reason);
        
        try {
            callManagerService.endCall(sessionId, reason);
            CallSession session = sessionService.getSessionBySessionId(sessionId);
            
            return ResponseEntity.ok(
                CallRequestResponse.EndCallResponse.builder()
                    .success(true)
                    .message("Call ended successfully")
                    .sessionId(sessionId)
                    .durationSeconds(session.getDurationSeconds())
                    .summary(session.getConversationSummary())
                    .recordingUrl(session.getRecordingUrl())
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Failed to end call", e);
            return ResponseEntity.internalServerError().body(
                CallRequestResponse.EndCallResponse.builder()
                    .success(false)
                    .message("Failed to end call: " + e.getMessage())
                    .sessionId(sessionId)
                    .build()
            );
        }
    }
    
    /**
     * 텍스트 입력 처리 (테스트용)
     */
    @PostMapping("/sessions/{sessionId}/input")
    public ResponseEntity<CallRequestResponse.SuccessResponse> processInput(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> input) {
        
        try {
            String text = input.get("text");
            String response = callManagerService.processUserInput(sessionId, text);
            
            Map<String, Object> data = new HashMap<>();
            data.put("response", response);
            data.put("sessionId", sessionId);
            
            return ResponseEntity.ok(
                CallRequestResponse.SuccessResponse.builder()
                    .success(true)
                    .message("Input processed successfully")
                    .data(data)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Failed to process input", e);
            return ResponseEntity.internalServerError().body(
                CallRequestResponse.SuccessResponse.builder()
                    .success(false)
                    .message("Failed to process input: " + e.getMessage())
                    .build()
            );
        }
    }
}
