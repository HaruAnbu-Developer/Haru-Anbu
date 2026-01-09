package com.haru_anbu.CallManager.call.repository;

import com.haru_anbu.CallManager.call.entity.CallSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, Long> {
    
    Optional<CallSession> findBySessionId(String sessionId);
    
    Optional<CallSession> findByTwilioCallSid(String twilioCallSid);
    
    List<CallSession> findByUserId(String userId);
    
    List<CallSession> findByUserIdAndStatus(String userId, CallSession.CallStatus status);
    
    @Query("SELECT c FROM CallSession c WHERE c.userId = :userId AND c.startedAt >= :startDate ORDER BY c.startedAt DESC")
    List<CallSession> findRecentCallsByUserId(String userId, LocalDateTime startDate);
    
    @Query("SELECT c FROM CallSession c WHERE c.status = :status AND c.startedAt < :before")
    List<CallSession> findStaleSessionsByStatus(CallSession.CallStatus status, LocalDateTime before);
    
    @Query("SELECT COUNT(c) FROM CallSession c WHERE c.userId = :userId AND c.status = 'IN_PROGRESS'")
    long countActiveCallsByUserId(String userId);
}
