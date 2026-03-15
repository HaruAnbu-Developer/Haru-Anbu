package com.cheongchun.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "로그인 요청")
@Data
@NoArgsConstructor
public class LoginRequest {

    @Schema(description = "사용자명 또는 이메일", example = "user123")
    @NotBlank
    private String username;

    @Schema(description = "비밀번호", example = "password123")
    @NotBlank
    private String password;
}
