package com.cheongchun.backend.auth.controller;

import com.cheongchun.backend.auth.dto.request.LoginRequest;
import com.cheongchun.backend.auth.dto.request.OAuthCodeRequest;
import com.cheongchun.backend.auth.dto.request.SignUpRequest;
import com.cheongchun.backend.auth.service.AuthService;
import com.cheongchun.backend.auth.service.EmailVerificationService;
import com.cheongchun.backend.oauth.service.KakaoOAuthService;

import com.cheongchun.backend.user.dto.UserResponse;
import com.cheongchun.backend.user.domain.User;
import com.cheongchun.backend.user.mapper.UserMapper;

import com.cheongchun.backend.token.domain.RefreshToken;
import com.cheongchun.backend.token.service.RefreshTokenService;

import com.cheongchun.backend.global.common.dto.ApiResponse;
import com.cheongchun.backend.global.common.util.ControllerUtils;
import com.cheongchun.backend.global.security.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "회원가입, 로그인, 이메일 인증, 로그아웃 API")
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final EmailVerificationService emailVerificationService;
    private final KakaoOAuthService kakaoOAuthService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, RefreshTokenService refreshTokenService,
                          UserMapper userMapper, EmailVerificationService emailVerificationService,
                          KakaoOAuthService kakaoOAuthService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.emailVerificationService = emailVerificationService;
        this.kakaoOAuthService = kakaoOAuthService;
    }

    @Operation(summary = "회원가입", description = "사용자명, 이메일, 비밀번호, 이름으로 새 계정을 생성합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody SignUpRequest signUpRequest,
                                          HttpServletRequest request, HttpServletResponse response) {
        try {
            User newUser = authService.registerUser(signUpRequest);

            UserResponse userResponse = userMapper.toUserResponse(newUser);
            ApiResponse<UserResponse> apiResponse = ApiResponse.success(userResponse, "회원가입이 완료되었습니다");

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("회원가입 중 오류 발생", e);
            ApiResponse<UserResponse> errorResponse = ApiResponse.error(
                "SIGNUP_FAILED",
                e.getMessage(),
                "회원가입 중 오류가 발생했습니다"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "이메일 인증", description = "회원가입 시 발송된 이메일의 인증 토큰을 검증합니다.")
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            User user = emailVerificationService.verifyEmail(token);

            UserResponse userResponse = userMapper.toUserResponse(user);
            ApiResponse<UserResponse> apiResponse = ApiResponse.success(userResponse, "인증을 위해 이메일을 확인해주세요.");

            return ResponseEntity.ok(apiResponse);

        } catch (RuntimeException e) {
            logger.warn("이메일 인증 실패: {}", e.getMessage());
            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "EMAIL_VERIFICATION_FAILED",
                    e.getMessage(),
                    "인증 링크를 다시 확인해주세요"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("email 인증중 오류 발생", e);
            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "EMAIL_VERIFICATION_ERROR",
                    "이메일 인증 중 오류가 발생했습니다",
                    e.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @Operation(summary = "인증 이메일 재발송", description = "이메일 인증 메일을 다시 발송합니다.")
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@RequestParam String email) {
        try {
            emailVerificationService.resendVerificationEmail(email);

            ApiResponse<Void> response = ApiResponse.success(null,
                    "인증 이메일이 재발송되었습니다. 메일함을 확인해주세요.");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.warn("이메일 재발송 실패: {}", e.getMessage());
            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "RESEND_FAILED",
                    e.getMessage(),
                    null
            );
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("이메일 재발송 중 오류 발생", e);
            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "RESEND_ERROR",
                    "이메일 재발송 중 오류가 발생했습니다",
                    e.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @Operation(summary = "로그인", description = "사용자명 또는 이메일과 비밀번호로 로그인합니다. 인증 쿠키가 설정됩니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request, HttpServletResponse response) {
        try {
            User authenticatedUser = authService.authenticateUserCookie(loginRequest);

            String jwt = jwtUtil.generateTokenFromUsername(authenticatedUser.getEmail());

            String userAgent = request.getHeader("User-Agent");
            String ipAddress = ControllerUtils.getClientIpAddress(request);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(authenticatedUser, userAgent, ipAddress);

            setAuthCookies(response, jwt, refreshToken.getToken());

            UserResponse userResponse = userMapper.toUserResponse(authenticatedUser);
            ApiResponse<UserResponse> apiResponse = ApiResponse.success(userResponse, "로그인이 완료되었습니다");

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("로그인 중 오류 발생", e);
            ApiResponse<UserResponse> errorResponse = ApiResponse.error(
                "LOGIN_FAILED",
                "로그인에 실패했습니다",
                e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "현재 사용자 정보 조회", description = "인증된 현재 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {

                UserResponse userResponse = userMapper.toUserResponse(user);
                ApiResponse<UserResponse> apiResponse = ApiResponse.success(userResponse, "사용자 정보 조회 성공");

                return ResponseEntity.ok(apiResponse);
            } else {
                ApiResponse<UserResponse> errorResponse = ApiResponse.error(
                    "USER_INFO_ERROR",
                    "사용자 정보 조회 실패",
                    "인증되지 않은 사용자입니다"
                );
                return ResponseEntity.status(401).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("사용자 정보 조회 중 오류 발생", e);
            ApiResponse<UserResponse> errorResponse = ApiResponse.error(
                "USER_INFO_ERROR",
                "사용자 정보 조회 중 오류가 발생했습니다",
                e.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @Operation(summary = "Kakao 로그인", description = "Kakao OAuth 인가 코드로 로그인합니다.")
    @PostMapping("/oauth/kakao")
    public ResponseEntity<ApiResponse<UserResponse>> kakaoLogin(@Valid @RequestBody OAuthCodeRequest oAuthCodeRequest,
                                                                 HttpServletRequest request, HttpServletResponse response) {
        try {
            User user = kakaoOAuthService.loginWithKakaoCode(oAuthCodeRequest.getCode(), oAuthCodeRequest.getRedirectUri());

            String jwt = jwtUtil.generateTokenFromUsername(user.getEmail());

            String userAgent = request.getHeader("User-Agent");
            String ipAddress = ControllerUtils.getClientIpAddress(request);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, userAgent, ipAddress);

            setAuthCookies(response, jwt, refreshToken.getToken());

            UserResponse userResponse = userMapper.toUserResponse(user);
            ApiResponse<UserResponse> apiResponse = ApiResponse.success(userResponse, "Kakao 로그인이 완료되었습니다");

            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("Kakao 로그인 중 오류 발생", e);
            ApiResponse<UserResponse> errorResponse = ApiResponse.error(
                "KAKAO_LOGIN_FAILED",
                e.getMessage(),
                "Kakao 로그인 중 오류가 발생했습니다"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 종료하고 인증 쿠키를 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                // 쿠키에서 리프레시 토큰 읽기
                String refreshTokenStr = null;
                if (request.getCookies() != null) {
                    for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                        if ("refreshToken".equals(cookie.getName())) {
                            refreshTokenStr = cookie.getValue();
                            break;
                        }
                    }
                }
                authService.logout(user, refreshTokenStr);
            }

            // 쿠키 삭제
            clearAuthCookies(response);

            ApiResponse<Void> apiResponse = ApiResponse.success(null, "로그아웃이 완료되었습니다");
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            logger.error("로그아웃 중 오류 발생", e);
            // 오류가 발생해도 쿠키는 삭제
            clearAuthCookies(response);
            ApiResponse<Void> apiResponse = ApiResponse.success(null, "로그아웃이 완료되었습니다");
            return ResponseEntity.ok(apiResponse);
        }
    }

    @Operation(summary = "API 테스트", description = "인증 API가 정상 작동하는지 확인하는 테스트 엔드포인트입니다.")
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> testEndpoint() {
        ApiResponse<String> response = ApiResponse.success("cheongchun-backend", "인증 API가 정상적으로 작동하고 있습니다");
        return ResponseEntity.ok(response);
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("accessToken", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(jwtCookie);

        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("accessToken", "");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }

}
