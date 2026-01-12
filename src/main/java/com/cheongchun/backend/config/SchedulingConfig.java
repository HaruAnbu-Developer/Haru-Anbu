package com.cheongchun.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // 이 설정으로 @Scheduled 어노테이션이 활성화됩니다
    // RefreshTokenService의 정리 작업이 자동으로 실행됩니다
}