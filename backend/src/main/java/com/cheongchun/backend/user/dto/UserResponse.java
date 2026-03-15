package com.cheongchun.backend.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "사용자 정보 응답")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "사용자명", example = "user123")
    private String username;

    @Schema(description = "권한", example = "USER")
    private String role;

    @Schema(description = "인증 제공자", example = "LOCAL")
    private String provider;

    @Schema(description = "이메일 인증 여부", example = "true")
    private Boolean emailVerified;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "생년월일", example = "2000-01-01")
    private LocalDate dateOfBirth;

    @Schema(description = "가입일시")
    private LocalDateTime createdAt;
}
