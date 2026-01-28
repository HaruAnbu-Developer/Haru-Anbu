package com.cheongchun.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String username;
    private String role;
    private String provider;
    private Boolean emailVerified;
    private String profileImageUrl;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private LocalDateTime createdAt;
}
