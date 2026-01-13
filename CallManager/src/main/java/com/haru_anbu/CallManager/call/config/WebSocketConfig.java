package com.haru_anbu.CallManager.call.config;

import com.haru_anbu.CallManager.call.handler.TwilioMediaStreamHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 설정
 * Twilio Media Stream과의 실시간 양방향 오디오 스트리밍
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final TwilioMediaStreamHandler twilioMediaStreamHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(twilioMediaStreamHandler, "/ws/twilio/media-stream")
            .setAllowedOrigins("*"); // Production에서는 Twilio IP만 허용 권장
    }
}
