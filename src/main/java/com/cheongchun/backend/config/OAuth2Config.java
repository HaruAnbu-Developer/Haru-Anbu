package com.cheongchun.backend.config;

import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.security.CustomOAuth2User;
import com.cheongchun.backend.security.OAuth2UserInfo;
import com.cheongchun.backend.service.CustomOAuth2UserService;
import com.cheongchun.backend.service.OAuth2UserStrategy;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class OAuth2Config {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2UserStrategy oAuth2UserStrategy;
    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;

    public OAuth2Config(CustomOAuth2UserService customOAuth2UserService,
                       OAuth2UserStrategy oAuth2UserStrategy,
                       AuthenticationSuccessHandler successHandler,
                       AuthenticationFailureHandler failureHandler) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2UserStrategy = oAuth2UserStrategy;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    public void configureOAuth2Login(OAuth2LoginConfigurer<HttpSecurity> oauth2) {
        oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService)
                .oidcUserService(this.createOidcUserService())
            )
            .successHandler(successHandler)
            .failureHandler(failureHandler);
    }

    private org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService createOidcUserService() {
        return new OidcUserService() {
            @Override
            public OidcUser loadUser(org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest userRequest) throws org.springframework.security.oauth2.core.OAuth2AuthenticationException {
                OidcUser oidcUser = super.loadUser(userRequest);
                
                String email = oidcUser.getEmail();
                String name = oidcUser.getFullName();
                String googleId = oidcUser.getSubject();
                
                // OAuth2UserInfo 생성 (Map 기반)
                java.util.Map<String, Object> attributes = new java.util.HashMap<>();
                attributes.put("sub", googleId);
                attributes.put("name", name);
                attributes.put("email", email);
                attributes.put("picture", oidcUser.getAttribute("picture"));
                
                OAuth2UserInfo googleUserInfo = new OAuth2UserInfo(attributes) {
                    @Override
                    public String getId() { return googleId; }
                    @Override
                    public String getName() { return name; }
                    @Override
                    public String getEmail() { return email; }
                    @Override
                    public String getImageUrl() { return (String) attributes.get("picture"); }
                };
                
                User user = oAuth2UserStrategy.processOAuth2User("google", googleUserInfo);
                
                return new CustomOAuth2User(
                    oidcUser,
                    user.getUsername(),
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getProviderType().name()
                );
            }
        };
    }
}