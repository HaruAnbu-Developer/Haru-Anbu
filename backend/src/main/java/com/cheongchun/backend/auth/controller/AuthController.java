package com.cheongchun.backend.auth.controller;

import com.cheongchun.backend.auth.dto.request.LoginRequest;
import com.cheongchun.backend.auth.dto.request.SignUpRequest;
import com.cheongchun.backend.global.common.dto.ApiResponse;
import com.cheongchun.backend.user.dto.UserResponse;
import com.cheongchun.backend.token.domain.RefreshToken;
import com.cheongchun.backend.user.domain.User;
import com.cheongchun.backend.user.mapper.UserMapper;
import com.cheongchun.backend.auth.service.AuthService;
import com.cheongchun.backend.token.service.RefreshTokenService;
import com.cheongchun.backend.global.security.JwtUtil;
import com.cheongchun.backend.global.common.util.ControllerUtils;
import com.cheongchun.backend.auth.service.EmailVerificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    public AuthController(AuthService authService, JwtUtil jwtUtil, RefreshTokenService refreshTokenService, UserMapper userMapper ,EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody SignUpRequest signUpRequest,
                                          HttpServletRequest request, HttpServletResponse response) {
        try {
            User newUser = authService.createUser(signUpRequest);
            emailVerificationService.sendVerificationEmail(newUser);

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

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> testEndpoint() {
        ApiResponse<String> response = ApiResponse.success("cheongchun-backend", "인증 API가 정상적으로 작동하고 있습니다");
        return ResponseEntity.ok(response);
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        jakarta.servlet.http.Cookie jwtCookie = new jakarta.servlet.http.Cookie("accessToken", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(jwtCookie);

        jakarta.servlet.http.Cookie refreshCookie = new jakarta.servlet.http.Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshCookie);
    }

}
