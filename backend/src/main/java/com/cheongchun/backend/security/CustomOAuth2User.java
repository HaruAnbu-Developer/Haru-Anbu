package com.cheongchun.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User, OidcUser {
    
    private OAuth2User oauth2User;
    private String username;
    private Long userId;
    private String email;
    private String name;
    private String providerType;

    public CustomOAuth2User(OAuth2User oauth2User, String username, Long userId, String email, String name, String providerType) {
        this.oauth2User = oauth2User;
        this.username = username;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.providerType = providerType;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return this.username;
    }

    public String getUsername() {
        return this.username;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getEmail() {
        return this.email;
    }

    public String getUserName() {
        return this.name;
    }

    public String getProviderType() {
        return this.providerType;
    }

    @Override
    public OidcIdToken getIdToken() {
        if (oauth2User instanceof OidcUser) {
            return ((OidcUser) oauth2User).getIdToken();
        }
        return null;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        if (oauth2User instanceof OidcUser) {
            return ((OidcUser) oauth2User).getUserInfo();
        }
        return null;
    }

    @Override
    public Map<String, Object> getClaims() {
        if (oauth2User instanceof OidcUser) {
            return ((OidcUser) oauth2User).getClaims();
        }
        return getAttributes();
    }
}