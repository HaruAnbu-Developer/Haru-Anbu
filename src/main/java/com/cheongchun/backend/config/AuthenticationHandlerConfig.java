package com.cheongchun.backend.config;

import com.cheongchun.backend.service.OAuth2LoginHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AuthenticationHandlerConfig {

    private final OAuth2LoginHandler oauth2LoginHandler;
    private final ObjectMapper objectMapper;

    public AuthenticationHandlerConfig(OAuth2LoginHandler oauth2LoginHandler) {
        this.oauth2LoginHandler = oauth2LoginHandler;
        this.objectMapper = new ObjectMapper();
    }

    @Bean
    public AuthenticationSuccessHandler oauth2SuccessHandler() {
        return (request, response, authentication) -> {
            oauth2LoginHandler.handleOAuth2Success(authentication.getPrincipal(), request, response);
        };
    }

    @Bean
    public AuthenticationFailureHandler oauth2FailureHandler() {
        return (request, response, exception) -> {
            String errorCode = "unknown_error";
            String errorDescription = "OAuth2 로그인 중 오류가 발생했습니다";
            
            if (exception instanceof OAuth2AuthenticationException) {
                OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
                if (oauth2Exception.getError() != null) {
                    errorCode = oauth2Exception.getError().getErrorCode();
                    errorDescription = oauth2Exception.getError().getDescription();
                }
            }
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("success", false);
            Map<String, String> errorInfo = new HashMap<>();
            errorInfo.put("code", errorCode != null ? errorCode : "unknown_error");
            errorInfo.put("message", "소셜 로그인에 실패했습니다");
            errorInfo.put("details", errorDescription != null ? errorDescription : "알 수 없는 오류가 발생했습니다");
            
            errorData.put("error", errorInfo);
            
            response.setStatus(400);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(errorData));
        };
    }
}