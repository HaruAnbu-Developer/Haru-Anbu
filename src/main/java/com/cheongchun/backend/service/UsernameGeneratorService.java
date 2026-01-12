package com.cheongchun.backend.service;

import com.cheongchun.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UsernameGeneratorService {

    private final UserRepository userRepository;

    public UsernameGeneratorService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateUniqueUsername(String baseName) {
        String baseUsername = sanitizeUsername(baseName);
        
        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        
        return username;
    }

    private String sanitizeUsername(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "user";
        }
        
        String baseUsername = name.replaceAll("[^a-zA-Z0-9]", "");
        if (baseUsername.length() < 4) {
            baseUsername = "user" + baseUsername;
        }
        
        return baseUsername;
    }
}