package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.SocialAccount;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.SocialAccountRepository;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.security.OAuth2UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 생성 전담 서비스
 * SRP: 사용자 생성과 소셜 계정 연결만 담당
 */
@Slf4j
@Service
@Transactional
public class UserCreationService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UsernameGeneratorService usernameGeneratorService;
    private final UserLookupService userLookupService;

    public UserCreationService(UserRepository userRepository,
                             SocialAccountRepository socialAccountRepository,
                             UsernameGeneratorService usernameGeneratorService,
                             UserLookupService userLookupService) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.usernameGeneratorService = usernameGeneratorService;
        this.userLookupService = userLookupService;
    }

    /**
     * OAuth2 정보로 새 사용자 생성 (중복 방지)
     */
    public User createNewUserSafely(String registrationId, OAuth2UserInfo userInfo) {
        SocialAccount.Provider provider = SocialAccount.Provider.valueOf(registrationId.toUpperCase());
        User.ProviderType providerType = User.ProviderType.valueOf(registrationId.toUpperCase());

        try {
            User user = buildUser(userInfo, providerType);
            User savedUser = userRepository.save(user);
            createSocialAccount(savedUser, provider, userInfo);
            
            log.info("새 사용자 생성 완료: email={}, provider={}", userInfo.getEmail(), provider);
            return savedUser;
            
        } catch (DataIntegrityViolationException e) {
            log.warn("사용자 생성 중 중복 키 에러 발생, 기존 사용자 조회: email={}", userInfo.getEmail());
            
            // 중복 키 에러 시 기존 사용자 반환
            return userLookupService.findByEmail(userInfo.getEmail())
                    .orElseThrow(() -> new IllegalStateException("사용자 생성 실패 후 조회에도 실패했습니다: " + userInfo.getEmail()));
        }
    }

    /**
     * 기존 사용자에 소셜 계정 연결
     */
    public void linkSocialAccount(User user, SocialAccount.Provider provider, OAuth2UserInfo userInfo) {
        // 이미 연결된 소셜 계정인지 확인
        boolean alreadyLinked = socialAccountRepository.findByProviderAndProviderId(provider, userInfo.getId())
                .isPresent();

        if (!alreadyLinked) {
            createSocialAccount(user, provider, userInfo);
            log.info("소셜 계정 연결 완료: userId={}, provider={}, email={}", 
                    user.getId(), provider, userInfo.getEmail());
        } else {
            log.debug("이미 연결된 소셜 계정: userId={}, provider={}", user.getId(), provider);
        }
    }

    private User buildUser(OAuth2UserInfo userInfo, User.ProviderType providerType) {
        User user = new User();
        user.setProviderType(providerType);
        user.setProviderId(userInfo.getId());
        user.setName(userInfo.getName());
        user.setEmail(userInfo.getEmail() != null ? userInfo.getEmail().toLowerCase() : null);
        user.setProfileImageUrl(userInfo.getImageUrl());
        user.setEmailVerified(true);
        user.setUsername(usernameGeneratorService.generateUniqueUsername(userInfo.getName()));
        return user;
    }

    private void createSocialAccount(User user, SocialAccount.Provider provider, OAuth2UserInfo userInfo) {
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.setUser(user);
        socialAccount.setProvider(provider);
        socialAccount.setProviderId(userInfo.getId());
        socialAccount.setProviderEmail(userInfo.getEmail());
        socialAccount.setProviderName(userInfo.getName());
        socialAccount.setProfileImageUrl(userInfo.getImageUrl());
        
        socialAccountRepository.save(socialAccount);
    }
}