package com.cheongchun.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "회원가입 요청")
@Data
@NoArgsConstructor
public class SignUpRequest {

    @Schema(description = "사용자명 (4~50자)", example = "user123")
    @NotBlank
    @Size(min = 4, max = 50)
    private String username;

    @Schema(description = "이메일", example = "user@example.com")
    @NotBlank
    @Size(max = 100)
    @Email
    private String email;

    @Schema(description = "비밀번호 (8자 이상)", example = "password123")
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @Schema(description = "이름", example = "홍길동")
    @NotBlank
    @Size(max = 100)
    private String name;
}
