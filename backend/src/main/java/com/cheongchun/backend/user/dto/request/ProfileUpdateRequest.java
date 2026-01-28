package com.cheongchun.backend.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ProfileUpdateRequest {

    @Size(max = 100, message = "이름은 100자 이하여야 합니다")
    private String name;

    private String profileImageUrl;

    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)")
    private String phoneNumber;

    private LocalDate dateOfBirth;
}
