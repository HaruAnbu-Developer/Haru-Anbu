package com.cheongchun.backend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    
    private String secret = "dev-secret-key-please-change-in-production";
    private long expiration = 3600000; // 1시간 (밀리초)
    private long refreshExpiration = 604800000; // 7일 (밀리초)
    private int maxRefreshTokensPerUser = 5;

    // Getters and Setters
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setRefreshExpiration(long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }

    public int getMaxRefreshTokensPerUser() {
        return maxRefreshTokensPerUser;
    }

    public void setMaxRefreshTokensPerUser(int maxRefreshTokensPerUser) {
        this.maxRefreshTokensPerUser = maxRefreshTokensPerUser;
    }
}