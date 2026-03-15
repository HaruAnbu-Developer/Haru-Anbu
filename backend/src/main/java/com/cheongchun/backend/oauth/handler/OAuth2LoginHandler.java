package com.cheongchun.backend.oauth.handler;

import com.cheongchun.backend.token.domain.RefreshToken;
import com.cheongchun.backend.token.service.RefreshTokenService;

import com.cheongchun.backend.user.domain.User;
import com.cheongchun.backend.user.repository.UserRepository;

import com.cheongchun.backend.global.security.CustomOAuth2User;
import com.cheongchun.backend.global.common.util.ControllerUtils;
import com.cheongchun.backend.global.security.JwtUtil;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2LoginHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginHandler.class);

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    public OAuth2LoginHandler(JwtUtil jwtUtil, RefreshTokenService refreshTokenService, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        handleOAuth2Success(authentication.getPrincipal(), request, response);
    }

    public void handleOAuth2Success(Object principal, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (principal instanceof CustomOAuth2User) {
                CustomOAuth2User customUser = (CustomOAuth2User) principal;

                // 사용자 정보 조회
                Optional<User> userOpt = userRepository.findByEmail(customUser.getEmail());
                if (userOpt.isEmpty()) {
                    throw new RuntimeException("OAuth2 사용자를 찾을 수 없습니다: " + customUser.getEmail());
                }

                User user = userOpt.get();

                // JWT 토큰 생성
                String jwt = jwtUtil.generateTokenFromUsername(customUser.getUsername());

                // 리프레시 토큰 생성
                String userAgent = request.getHeader("User-Agent");
                String ipAddress = ControllerUtils.getClientIpAddress(request);
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);

                // JWT를 HttpOnly 쿠키로 설정 (7일)
                Cookie jwtCookie = new Cookie("accessToken", jwt);
                jwtCookie.setHttpOnly(true);
                jwtCookie.setSecure(false);
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge(7 * 24 * 60 * 60);
                response.addCookie(jwtCookie);

                // 리프레시 토큰을 HttpOnly 쿠키로 설정 (7일)
                Cookie refreshCookie = new Cookie("refreshToken", refreshToken.getToken());
                refreshCookie.setHttpOnly(true);
                refreshCookie.setSecure(false);
                refreshCookie.setPath("/");
                refreshCookie.setMaxAge(7 * 24 * 60 * 60);
                response.addCookie(refreshCookie);

                // 프론트엔드로 리다이렉트
                String redirectUrl = String.format(
                    "http://localhost:3000/oauth/callback?userId=%s&email=%s&name=%s&provider=%s",
                    user.getId(),
                    java.net.URLEncoder.encode(customUser.getEmail(), "UTF-8"),
                    java.net.URLEncoder.encode(customUser.getUserName(), "UTF-8"),
                    customUser.getProviderType().toLowerCase()
                );
                response.sendRedirect(redirectUrl);

            } else {
                String errorUrl = "http://localhost:3000/login?error=unsupported_principal_type&message=" +
                                java.net.URLEncoder.encode("지원하지 않는 사용자 타입입니다", "UTF-8");
                response.sendRedirect(errorUrl);
            }
        } catch (Exception e) {
            logger.error("OAuth2 로그인 처리 중 오류 발생", e);
            String errorUrl = String.format(
                "http://localhost:3000/login?error=oauth2_processing_error&message=%s",
                java.net.URLEncoder.encode("OAuth2 로그인 처리 중 오류가 발생했습니다: " + e.getMessage(), "UTF-8")
            );
            response.sendRedirect(errorUrl);
        }
    }


}
