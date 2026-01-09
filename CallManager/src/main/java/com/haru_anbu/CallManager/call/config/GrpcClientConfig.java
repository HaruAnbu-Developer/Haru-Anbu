package com.haru_anbu.CallManager.call.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class GrpcClientConfig {
    
    @Value("${grpc.ai-service.host:localhost}")
    private String aiServiceHost;
    
    @Value("${grpc.ai-service.port:50051}")
    private int aiServicePort;
    
    @Value("${grpc.ai-service.max-inbound-message-size:10485760}") // 10MB
    private int maxInboundMessageSize;
    
    @Value("${grpc.ai-service.keepalive-time:30}")
    private long keepaliveTime;
    
    @Value("${grpc.ai-service.keepalive-timeout:10}")
    private long keepaliveTimeout;
    
    @Bean
    public ManagedChannel aiServiceChannel() {
        log.info("Creating gRPC channel to AI Service at {}:{}", aiServiceHost, aiServicePort);
        
        return ManagedChannelBuilder
            .forAddress(aiServiceHost, aiServicePort)
            .usePlaintext() // Production에서는 TLS 사용 권장
            .maxInboundMessageSize(maxInboundMessageSize)
            .keepAliveTime(keepaliveTime, TimeUnit.SECONDS)
            .keepAliveTimeout(keepaliveTimeout, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .build();
    }
}
