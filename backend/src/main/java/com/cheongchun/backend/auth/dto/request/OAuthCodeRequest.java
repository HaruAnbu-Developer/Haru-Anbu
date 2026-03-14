package com.cheongchun.backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "OAuth 인가 코드 요청")
@Data
@NoArgsConstructor
public class OAuthCodeRequest {

    @Schema(description = "OAuth 인가 코드", example = "abc123def456")
    @NotBlank
    private String code;

    @Schema(description = "리다이렉트 URI", example = "http://localhost:3000/oauth/callback")
    @NotBlank
    private String redirectUri;
}
