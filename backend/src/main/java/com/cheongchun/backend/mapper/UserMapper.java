package com.cheongchun.backend.mapper;

import com.cheongchun.backend.dto.response.UserResponse;
import com.cheongchun.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole() != null ? user.getRole().toString() : null);
        response.setProvider(user.getProviderType() != null ? user.getProviderType().toString() : null);
        response.setEmailVerified(user.isEmailVerified());
        response.setProfileImageUrl(user.getProfileImageUrl());
        
        return response;
    }

    public UserResponse toBasicUserResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        
        return response;
    }
}