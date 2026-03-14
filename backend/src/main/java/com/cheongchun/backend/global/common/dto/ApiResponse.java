package com.cheongchun.backend.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "공통 API 응답")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 데이터")
    private T data;

    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다")
    private String message;

    @Schema(description = "에러 정보")
    private ErrorInfo error;

    @Schema(description = "응답 시각")
    private LocalDateTime timestamp;

    // Success response factory methods
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = message;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "요청이 성공적으로 처리되었습니다");
    }

    public static ApiResponse<Void> success(String message) {
        return success(null, message);
    }

    // Error response factory methods
    public static <T> ApiResponse<T> error(String code, String message, String details) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = new ErrorInfo(code, message, details);
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return error(code, message, null);
    }

    @Schema(description = "에러 상세 정보")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorInfo {

        @Schema(description = "에러 코드", example = "VALIDATION_ERROR")
        private String code;

        @Schema(description = "에러 메시지", example = "입력값이 올바르지 않습니다")
        private String message;

        @Schema(description = "에러 상세", example = "username: 사용자명은 4자 이상이어야 합니다")
        private String details;
    }
}
