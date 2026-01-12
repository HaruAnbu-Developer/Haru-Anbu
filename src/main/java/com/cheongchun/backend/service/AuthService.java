package com.cheongchun.backend.service;

import com.cheongchun.backend.dto.LoginRequest;
import com.cheongchun.backend.dto.SignUpRequest;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.exception.UserAlreadyExistsException;
import com.cheongchun.backend.exception.EmailAlreadyExistsException;
import com.cheongchun.backend.exception.BusinessException;
import com.cheongchun.backend.exception.ErrorCode;
import com.cheongchun.backend.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    //회원가입할 떄 사용
    public User createUser(SignUpRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new UserAlreadyExistsException("사용자명이 이미 사용 중입니다: " + signUpRequest.getUsername());
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new EmailAlreadyExistsException("이메일이 이미 사용 중입니다: " + signUpRequest.getEmail());
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setName(signUpRequest.getName());
        user.setProviderType(User.ProviderType.LOCAL);
        user.setRole(User.Role.USER);
        user.setEmailVerified(false);
        
        return userRepository.save(user);
    }


    // 쿠키 기반 사용자 인증 (토큰 없이)
    public User authenticateUserCookie(LoginRequest loginRequest) {
        // 인증 수행
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 인증된 사용자 객체 반환
        User authenticatedUser = (User) authentication.getPrincipal();

        // ✅ 로컬 계정만 이메일 인증 확인 (소셜 로그인은 통과)
        if (authenticatedUser.getProviderType() == User.ProviderType.LOCAL &&
                !authenticatedUser.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "이메일 인증이 필요합니다. 메일함을 확인해주세요.");
        }

        return authenticatedUser;
    }



    @Transactional
    public void logout(User user, String refreshTokenStr) {
        // 특정 리프레시 토큰만 무효화 (다른 디바이스 로그인 유지)
        if (refreshTokenStr != null && !refreshTokenStr.trim().isEmpty()) {
            refreshTokenService.revokeToken(refreshTokenStr);
        } else {
            // 리프레시 토큰이 없으면 해당 사용자의 모든 토큰 무효화
            refreshTokenService.revokeAllTokensByUser(user);
        }
    }

    @Transactional
    public void logoutFromAllDevices(User user) {
        // 모든 디바이스에서 로그아웃
        refreshTokenService.revokeAllTokensByUser(user);
    }



    /**
     * 사용자 활성 세션 조회
     */
    @Transactional(readOnly = true)
    public RefreshTokenService.RefreshTokenStats getUserTokenStats(User user) {
        return refreshTokenService.getTokenStats(user);
    }
}