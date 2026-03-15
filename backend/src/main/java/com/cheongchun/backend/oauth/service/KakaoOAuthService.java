package com.cheongchun.backend.oauth.service;

import com.cheongchun.backend.user.domain.User;
import com.cheongchun.backend.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
public class KakaoOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(KakaoOAuthService.class);

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;


    public KakaoOAuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public User loginWithKakaoCode(String code, String redirectUri) {
        // 1. 인가 코드로 토큰 교환
        Map<String, Object> tokenResponse = exchangeCodeForToken(code, redirectUri);

        String accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null) {
            throw new RuntimeException("Kakao 토큰 교환에 실패했습니다.");
        }

        // 2. 액세스 토큰으로 사용자 정보 조회
        Map<String, Object> userInfo = fetchKakaoUserInfo(accessToken);

        // 3. 사용자 정보 추출
        String kakaoId = String.valueOf(userInfo.get("id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) userInfo.get("properties");

        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : null;
        String profileImage = profile != null ? (String) profile.get("profile_image") : null;

        // 4. 사용자 조회 또는 생성 (kakaoId 기반 우선 조회)
        Optional<User> existingUser = userRepository.findByProviderTypeAndProviderId(User.ProviderType.KAKAO, kakaoId);

        if (existingUser.isEmpty() && email != null && !email.isBlank()) {
            existingUser = userRepository.findByEmail(email);
        }

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (nickname != null) {
                user.setName(nickname);
            }
            if (profileImage != null) {
                user.setProfileImageUrl(profileImage);
            }
            if (user.getProviderType() == User.ProviderType.LOCAL) {
                user.setProviderType(User.ProviderType.KAKAO);
                user.setProviderId(kakaoId);
            }
            return userRepository.save(user);
        } else {
            User newUser = new User();
            newUser.setEmail(email != null && !email.isBlank() ? email : "kakao_" + kakaoId + "@kakao.user");
            newUser.setName(nickname != null ? nickname : "사용자");
            newUser.setUsername("kakao_" + kakaoId);
            newUser.setProviderType(User.ProviderType.KAKAO);
            newUser.setProviderId(kakaoId);
            newUser.setEmailVerified(true);
            newUser.setRole(User.Role.USER);
            if (profileImage != null) {
                newUser.setProfileImageUrl(profileImage);
            }
            return userRepository.save(newUser);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeCodeForToken(String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        logger.info("Kakao 토큰 교환 요청 - client_id: {}, redirect_uri: {}, code: {}...",
                kakaoClientId, redirectUri, code.substring(0, Math.min(10, code.length())));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    KAKAO_TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Kakao 토큰 교환 실패", e);
            throw new RuntimeException("Kakao 인증에 실패했습니다: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Kakao 사용자 정보 조회 실패", e);
            throw new RuntimeException("Kakao 사용자 정보를 가져올 수 없습니다: " + e.getMessage());
        }
    }
}
