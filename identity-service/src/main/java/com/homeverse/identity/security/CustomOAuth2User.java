package com.homeverse.identity.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User extends DefaultOAuth2User {
    private final Long userId; // Đây là "long mạch" của chúng ta

    public CustomOAuth2User(OAuth2User oauth2User, Long userId) {
        super(oauth2User.getAuthorities(), oauth2User.getAttributes(), "email");
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}