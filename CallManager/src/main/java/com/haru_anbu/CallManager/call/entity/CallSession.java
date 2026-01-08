package com.haru_anbu.CallManager.call.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String sessionId;  // Twilio Call SID
    
    @Column(nullable = false)
    private String userId;  // 대상자 ID
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status;  // INITIATED, RINGING, IN_PROGRESS, COMPLETED, FAILED
    
    @Column
    private String twilioCallSid;
    
    @Column
    private String aiCoreSessionId;
    
    @Column
    private LocalDateTime startedAt;
    
    @Column
    private LocalDateTime endedAt;
    
    @Column
    private Integer durationSeconds;
    
    @Column(columnDefinition = "TEXT")
    private String conversationSummary;
    
    @Column
    private String recordingUrl;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum CallStatus {
        INITIATED,
        RINGING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        NO_ANSWER,
        BUSY
    }
}