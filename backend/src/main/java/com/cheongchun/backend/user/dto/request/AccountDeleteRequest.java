package com.cheongchun.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AccountDeleteRequest {

    // LOCAL 사용자는 비밀번호 확인 필요
    private String password;

    @NotBlank(message = "삭제 확인 문구를 입력해주세요")
    private String confirmationText;  // "계정을 삭제합니다"
}
