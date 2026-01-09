package com.haru_anbu.CallManager.call.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "call_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String sessionId;
    
    @Column(nullable = false, length = 100)
    private String twilioCallSid;
    
    @Column(length = 100)
    private String aiCoreSessionId;
    
    @Column(nullable = false, length = 50)
    private String userId;
    
    @Column(nullable = false, length = 20)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;
    
    @Column(length = 20)
    private String direction; // inbound, outbound
    
    @Column
    private LocalDateTime startedAt;
    
    @Column
    private LocalDateTime endedAt;
    
    @Column
    private Integer durationSeconds;
    
    @Column(length = 500)
    private String recordingUrl;
    
    @Column(columnDefinition = "TEXT")
    private String conversationSummary;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @Column(length = 100)
    private String endReason;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
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
        BUSY,
        NO_ANSWER,
        CANCELED
    }
    
    // 통화 시간 계산
    public void calculateDuration() {
        if (startedAt != null && endedAt != null) {
            this.durationSeconds = (int) java.time.Duration
                .between(startedAt, endedAt)
                .getSeconds();
        }
    }
}
