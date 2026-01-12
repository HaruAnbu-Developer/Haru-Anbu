package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.SocialAccount;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.repository.SocialAccountRepository;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.security.OAuth2UserInfo;
import org.springframework.dao.DataIntegrityViolationException;
// import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
// import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UsernameGeneratorService usernameGeneratorService;

    public UserRegistrationService(UserRepository userRepository,
                                 SocialAccountRepository socialAccountRepository,
                                 UsernameGeneratorService usernameGeneratorService) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.usernameGeneratorService = usernameGeneratorService;
    }



    public User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setProfileImageUrl(oAuth2UserInfo.getImageUrl());
        return userRepository.save(existingUser);
    }

}