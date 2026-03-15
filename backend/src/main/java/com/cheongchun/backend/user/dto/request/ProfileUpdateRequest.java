package com.cheongchun.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Schema(description = "프로필 수정 요청")
@Data
@NoArgsConstructor
public class ProfileUpdateRequest {

    @Schema(description = "이름", example = "홍길동")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다")
    private String name;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "전화번호", example = "010-1234-5678")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)")
    private String phoneNumber;

    @Schema(description = "생년월일", example = "2000-01-01")
    private LocalDate dateOfBirth;
}
