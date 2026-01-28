package com.cheongchun.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UsernameChangeRequest {

    @NotBlank(message = "새 아이디를 입력해주세요")
    @Size(min = 4, max = 50, message = "아이디는 4자 이상 50자 이하여야 합니다")
    private String newUsername;
}
