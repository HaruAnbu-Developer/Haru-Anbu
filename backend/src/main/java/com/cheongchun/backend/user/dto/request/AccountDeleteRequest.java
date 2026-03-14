package com.cheongchun.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "계정 삭제 요청")
@Data
@NoArgsConstructor
public class AccountDeleteRequest {

    @Schema(description = "비밀번호 (LOCAL 사용자만 필요)", example = "password123")
    private String password;

    @Schema(description = "삭제 확인 문구 ('계정을 삭제합니다' 입력)", example = "계정을 삭제합니다")
    @NotBlank(message = "삭제 확인 문구를 입력해주세요")
    private String confirmationText;
}
