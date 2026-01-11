package com.cheongchun.backend.oauth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        logger.error("OAuth2 인증 실패", exception);

        String errorMessage = exception.getMessage() != null ? exception.getMessage() : "OAuth2 인증에 실패했습니다";
        String errorUrl = String.format(
            "https://cheongchun-backend-40635111975.asia-northeast3.run.app/auth/oauth-error?code=oauth2_authentication_failed&message=%s",
            URLEncoder.encode(errorMessage, "UTF-8")
        );

        response.sendRedirect(errorUrl);
    }
}
