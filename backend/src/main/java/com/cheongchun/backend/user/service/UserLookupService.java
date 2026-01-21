package com.cheongchun.backend.user.service;

import com.cheongchun.backend.user.domain.SocialAccount;
import com.cheongchun.backend.user.domain.User;
import com.cheongchun.backend.user.repository.SocialAccountRepository;
import com.cheongchun.backend.user.repository.UserRepository;
import com.cheongchun.backend.oauth.domain.OAuth2UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 사용자 조회 전담 서비스
 * SRP: 사용자 존재 여부 확인과 조회만 담당
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserLookupService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    public UserLookupService(UserRepository userRepository,
                           SocialAccountRepository socialAccountRepository) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    /**
     * 소셜 계정으로 사용자 조회
     */
    public Optional<User> findBySocialAccount(SocialAccount.Provider provider, String providerId) {
        return socialAccountRepository.findByProviderAndProviderId(provider, providerId)
                .map(SocialAccount::getUser);
    }

    /**
     * 이메일로 사용자 조회
     */
    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.debug("이메일이 null이거나 비어있음");
            return Optional.empty();
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> result = userRepository.findByEmail(normalizedEmail);

        log.info("이메일로 사용자 조회: email={}, found={}", maskEmail(normalizedEmail), result.isPresent());

        return result;
    }

    /**
     * OAuth2 사용자 정보로 기존 사용자 조회
     *  이메일로 조회
     */
    public Optional<User> findExistingUser(SocialAccount.Provider provider, OAuth2UserInfo userInfo) {

        // 2. 이메일로 조회
        Optional<User> emailUser = findByEmail(userInfo.getEmail());
        if (emailUser.isPresent()) {
            log.info("이메일로 기존 사용자 발견: userId={}", emailUser.get().getId());
        } else {
            log.info("기존 사용자를 찾지 못함 - 새 사용자로 처리");
        }

        return emailUser;
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
