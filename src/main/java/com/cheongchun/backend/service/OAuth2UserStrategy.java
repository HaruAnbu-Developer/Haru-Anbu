package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.SocialAccount;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.security.OAuth2UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * OAuth2 사용자 처리 전략 서비스
 * SRP: OAuth2 로그인 플로우 조정만 담당
 * 보안: 입력값 검증, 로깅, 예외 처리 강화
 */
@Slf4j
@Service
@Transactional
public class OAuth2UserStrategy {

    private final UserLookupService userLookupService;
    private final UserCreationService userCreationService;
    private final UserRegistrationService userRegistrationService;

    public OAuth2UserStrategy(UserLookupService userLookupService,
                            UserCreationService userCreationService,
                            UserRegistrationService userRegistrationService) {
        this.userLookupService = userLookupService;
        this.userCreationService = userCreationService;
        this.userRegistrationService = userRegistrationService;
    }

    /**
     * OAuth2 사용자 처리 메인 로직
     * 보안: 입력값 검증과 상세 로깅
     */
    public User processOAuth2User(String registrationId, OAuth2UserInfo userInfo) {
        // 입력값 검증
        validateInputs(registrationId, userInfo);
        
        SocialAccount.Provider provider = SocialAccount.Provider.valueOf(registrationId.toUpperCase());
        
        log.info("OAuth2 사용자 처리 시작: provider={}, email={}", provider, maskEmail(userInfo.getEmail()));

        try {
            // 1. 기존 사용자 조회 (이메일)
            Optional<User> existingUser = userLookupService.findExistingUser(provider, userInfo);
            
            if (existingUser.isPresent()) {
                return handleExistingUser(existingUser.get(), provider, userInfo);
            } else {
                return handleNewUser(registrationId, userInfo);
            }
            
        } catch (Exception e) {
            log.error("OAuth2 사용자 처리 중 오류 발생: provider={}, email={}, error={}", 
                    provider, maskEmail(userInfo.getEmail()), e.getMessage(), e);
            throw e;
        }
    }

    private User handleExistingUser(User user, SocialAccount.Provider provider, OAuth2UserInfo userInfo) {
        log.info("기존 사용자 처리: userId={}, provider={}", user.getId(), provider);
        
        // 사용자 정보 업데이트
        User updatedUser = userRegistrationService.updateExistingUser(user, userInfo);
        
        // 소셜 계정 연결 (필요시)
        userCreationService.linkSocialAccount(updatedUser, provider, userInfo);
        
        return updatedUser;
    }

    private User handleNewUser(String registrationId, OAuth2UserInfo userInfo) {
        log.info("새 사용자 생성: provider={}, email={}", 
                registrationId, maskEmail(userInfo.getEmail()));
        
        return userCreationService.createNewUserSafely(registrationId, userInfo);
    }

    private void validateInputs(String registrationId, OAuth2UserInfo userInfo) {
        if (registrationId == null || registrationId.trim().isEmpty()) {
            throw new IllegalArgumentException("등록 ID는 필수입니다");
        }
        
        if (userInfo == null) {
            throw new IllegalArgumentException("사용자 정보는 필수입니다");
        }
        
        if (userInfo.getEmail() == null || userInfo.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("이메일 정보는 필수입니다");
        }

        // 이메일 형식 기본 검증
        if (!userInfo.getEmail().contains("@")) {
            throw new IllegalArgumentException("올바르지 않은 이메일 형식입니다");
        }
    }

    // 보안: 이메일 마스킹 (로깅용)
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