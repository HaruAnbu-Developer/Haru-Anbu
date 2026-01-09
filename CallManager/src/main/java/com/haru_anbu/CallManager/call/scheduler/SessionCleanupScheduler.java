package com.haru_anbu.CallManager.call.scheduler;

import com.haru_anbu.CallManager.call.service.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "call.session-cleanup.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupScheduler {
    
    private final CallSessionService sessionService;
    
    @Value("${call.session-cleanup.timeout-minutes:35}")
    private int timeoutMinutes;
    
    /**
     * 오래된 활성 세션 정리
     * 기본: 10분마다 실행
     */
    @Scheduled(cron = "${call.session-cleanup.schedule-cron:0 */10 * * * *}")
    public void cleanupStaleSessions() {
        log.info("Running session cleanup task (timeout: {} minutes)", timeoutMinutes);
        
        try {
            sessionService.cleanupStaleSessions(timeoutMinutes);
            log.info("Session cleanup completed");
        } catch (Exception e) {
            log.error("Error during session cleanup", e);
        }
    }
}
