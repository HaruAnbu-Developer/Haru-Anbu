package com.cheongchun.backend.user.service;

import com.cheongchun.backend.global.error.BusinessException;
import com.cheongchun.backend.global.error.ErrorCode;
import com.cheongchun.backend.token.service.RefreshTokenService;
import com.cheongchun.backend.user.domain.User;
import com.cheongchun.backend.user.dto.request.AccountDeleteRequest;
import com.cheongchun.backend.user.dto.request.PasswordChangeRequest;
import com.cheongchun.backend.user.dto.request.ProfileUpdateRequest;
import com.cheongchun.backend.user.dto.request.UsernameChangeRequest;
import com.cheongchun.backend.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 프로필 관리 전담 서비스
 * SRP: 프로필 조회, 수정, 비밀번호 변경, 아이디 변경, 계정 삭제 담당
 */
@Service
@Transactional
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);
    private static final String DELETE_CONFIRMATION_TEXT = "계정을 삭제합니다";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UserProfileService(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 프로필 조회
     */
    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 프로필 수정 (name, profileImageUrl, phoneNumber, dateOfBirth)
     */
    public User updateProfile(User user, ProfileUpdateRequest request) {
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        logger.info("프로필 업데이트 완료: userId={}", user.getId());
        return userRepository.save(user);
    }

    /**
     * 비밀번호 변경 (LOCAL 사용자만 가능)
     */
    public void changePassword(User user, PasswordChangeRequest request) {
        // OAuth 사용자 체크
        if (user.getProviderType() != User.ProviderType.LOCAL) {
            throw new BusinessException(ErrorCode.OAUTH_USER_PASSWORD_CHANGE);
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 새 비밀번호 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 현재 비밀번호와 동일한지 체크
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        logger.info("비밀번호 변경 완료: userId={}", user.getId());
    }

    /**
     * 아이디(username) 변경
     */
    public User changeUsername(User user, UsernameChangeRequest request) {
        String newUsername = request.getNewUsername();

        // 현재 아이디와 동일한지 체크
        if (user.getUsername().equals(newUsername)) {
            throw new BusinessException(ErrorCode.SAME_AS_CURRENT_USERNAME);
        }

        // 중복 체크
        if (userRepository.existsByUsername(newUsername)) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        user.setUsername(newUsername);
        User updatedUser = userRepository.save(user);

        logger.info("아이디 변경 완료: userId={}, newUsername={}", user.getId(), newUsername);
        return updatedUser;
    }

    /**
     * 계정 삭제
     */
    public void deleteAccount(User user, AccountDeleteRequest request) {
        // 삭제 확인 문구 체크
        if (!DELETE_CONFIRMATION_TEXT.equals(request.getConfirmationText())) {
            throw new BusinessException(ErrorCode.INVALID_CONFIRMATION_TEXT);
        }

        // LOCAL 사용자는 비밀번호 확인 필요
        if (user.getProviderType() == User.ProviderType.LOCAL) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD,
                        "비밀번호를 입력해주세요");
            }
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
            }
        }

        // 모든 리프레시 토큰 무효화
        refreshTokenService.revokeAllTokensByUser(user);

        // 사용자 삭제 (CASCADE로 연관 데이터도 삭제됨)
        userRepository.delete(user);

        logger.info("계정 삭제 완료: userId={}, email={}", user.getId(), maskEmail(user.getEmail()));
    }

    /**
     * 보안: 이메일 마스킹 (로깅용)
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email.charAt(0) + "***";
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domain;
        } else {
            return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domain;
        }
    }
}
