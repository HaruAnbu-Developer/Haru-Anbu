package com.cheongchun.backend.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuth2Properties {

    private String successUrl = "https://cheongchun-backend-40635111975.asia-northeast3.run.app/auth/oauth-success";
    private String errorUrl = "https://cheongchun-backend-40635111975.asia-northeast3.run.app/auth/oauth-error";
    private String baseUrl = "https://cheongchun-backend-40635111975.asia-northeast3.run.app";

    // Getters and Setters
    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getErrorUrl() {
        return errorUrl;
    }

    public void setErrorUrl(String errorUrl) {
        this.errorUrl = errorUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
