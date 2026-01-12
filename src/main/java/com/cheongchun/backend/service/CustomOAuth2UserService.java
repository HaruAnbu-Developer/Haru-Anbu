package com.cheongchun.backend.service;

import com.cheongchun.backend.security.CustomOAuth2User;
import com.cheongchun.backend.security.OAuth2UserInfo;
import com.cheongchun.backend.security.OAuth2UserInfoFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2UserStrategy oAuth2UserStrategy;

    public CustomOAuth2UserService(OAuth2UserStrategy oAuth2UserStrategy) {
        this.oAuth2UserStrategy = oAuth2UserStrategy;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        
        try {
            return processOAuth2User(registrationId, oAuth2User);
        } catch (IllegalArgumentException ex) {
            log.warn("OAuth2 입력값 검증 실패: registrationId={}, error={}", registrationId, ex.getMessage());
            throw new OAuth2AuthenticationException(new OAuth2Error(
                "invalid_input", 
                ex.getMessage(), 
                null
            ));
        } catch (Exception ex) {
            log.error("OAuth2 사용자 정보 처리 중 예상치 못한 오류: registrationId={}, error={}", 
                    registrationId, ex.getMessage(), ex);
            throw new OAuth2AuthenticationException(new OAuth2Error(
                "processing_error",
                "OAuth2 사용자 정보 처리 중 오류가 발생했습니다",
                null
            ));
        }
    }


    private OAuth2User processOAuth2User(String registrationId, OAuth2User oAuth2User) {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        
        // 이메일 정보 검증
        validateEmailInfo(userInfo, registrationId);
        
        // 전략 서비스에 위임
        com.cheongchun.backend.entity.User user = oAuth2UserStrategy.processOAuth2User(registrationId, userInfo);
        
        return new CustomOAuth2User(
            oAuth2User, 
            user.getUsername(), 
            user.getId(), 
            user.getEmail(), 
            user.getName(), 
            user.getProviderType().name()
        );
    }

    private void validateEmailInfo(OAuth2UserInfo userInfo, String registrationId) {
        if (!StringUtils.hasText(userInfo.getEmail())) {
            String message = String.format("이메일 정보를 가져올 수 없습니다. %s 계정에서 이메일 제공 동의가 필요합니다.", 
                    registrationId.toUpperCase());
            
            log.warn("OAuth2 이메일 정보 부족: registrationId={}, providerId={}", 
                    registrationId, userInfo.getId());
                    
            throw new OAuth2AuthenticationException(new OAuth2Error(
                "email_not_found",
                message,
                null
            ));
        }
    }

}