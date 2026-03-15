package com.cheongchun.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "비밀번호 변경 요청")
@Data
@NoArgsConstructor
public class PasswordChangeRequest {

    @Schema(description = "현재 비밀번호", example = "oldPassword123")
    @NotBlank(message = "현재 비밀번호를 입력해주세요")
    private String currentPassword;

    @Schema(description = "새 비밀번호 (8자 이상)", example = "newPassword456")
    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    private String newPassword;

    @Schema(description = "새 비밀번호 확인", example = "newPassword456")
    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String confirmPassword;
}
