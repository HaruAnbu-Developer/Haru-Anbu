package com.cheongchun.backend.auth.service;

import com.cheongchun.backend.auth.dto.request.LoginRequest;
import com.cheongchun.backend.auth.dto.request.SignUpRequest;
import com.cheongchun.backend.auth.domain.UserAlreadyExistsException;
import com.cheongchun.backend.auth.domain.EmailAlreadyExistsException;

import com.cheongchun.backend.user.domain.User;
import com.cheongchun.backend.user.repository.UserRepository;

import com.cheongchun.backend.token.service.RefreshTokenService;

import com.cheongchun.backend.global.error.BusinessException;
import com.cheongchun.backend.global.error.ErrorCode;

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


    public User authenticateUserCookie(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        User authenticatedUser = (User) authentication.getPrincipal();

        if (authenticatedUser.getProviderType() == User.ProviderType.LOCAL &&
                !authenticatedUser.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "이메일 인증이 필요합니다. 메일함을 확인해주세요.");
        }

        return authenticatedUser;
    }



    @Transactional
    public void logout(User user, String refreshTokenStr) {
        if (refreshTokenStr != null && !refreshTokenStr.trim().isEmpty()) {
            refreshTokenService.revokeToken(refreshTokenStr);
        } else {
            refreshTokenService.revokeAllTokensByUser(user);
        }
    }

    @Transactional
    public void logoutFromAllDevices(User user) {
        refreshTokenService.revokeAllTokensByUser(user);
    }



    @Transactional(readOnly = true)
    public RefreshTokenService.RefreshTokenStats getUserTokenStats(User user) {
        return refreshTokenService.getTokenStats(user);
    }
}
