package com.cheongchun.backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "아이디 변경 요청")
@Data
@NoArgsConstructor
public class UsernameChangeRequest {

    @Schema(description = "새 아이디 (4~50자)", example = "newUser123")
    @NotBlank(message = "새 아이디를 입력해주세요")
    @Size(min = 4, max = 50, message = "아이디는 4자 이상 50자 이하여야 합니다")
    private String newUsername;
}
