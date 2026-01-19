package com.haru_anbu.CallManager.call.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        // Netty HttpClient 설정
        HttpClient httpClient = HttpClient.create()
            // 연결 타임아웃: 10초
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            // 응답 타임아웃: 30초
            .responseTimeout(Duration.ofSeconds(30))
            // 연결 풀 설정
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS))
            );
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            // 요청 로깅 필터 (디버깅용)
            .filter(logRequest())
            // 응답 로깅 필터 (디버깅용)
            .filter(logResponse())
            .build();
    }
    
    /**
     * 요청 로깅 필터
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> 
                values.forEach(value -> log.debug("  {}: {}", name, value))
            );
            return Mono.just(clientRequest);
        });
    }
    
    /**
     * 응답 로깅 필터
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Response status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
