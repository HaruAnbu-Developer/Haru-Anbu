package com.cheongchun.backend.user.controller;

import com.cheongchun.backend.global.common.dto.ApiResponse;
import com.cheongchun.backend.user.domain.User;
import com.cheongchun.backend.user.dto.UserResponse;
import com.cheongchun.backend.user.dto.request.AccountDeleteRequest;
import com.cheongchun.backend.user.dto.request.PasswordChangeRequest;
import com.cheongchun.backend.user.dto.request.ProfileUpdateRequest;
import com.cheongchun.backend.user.dto.request.UsernameChangeRequest;
import com.cheongchun.backend.user.mapper.UserMapper;
import com.cheongchun.backend.user.service.UserProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 프로필 조회/수정, 비밀번호 변경, 아이디 변경, 계정 삭제 API")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserProfileService userProfileService;
    private final UserMapper userMapper;

    public UserController(UserProfileService userProfileService, UserMapper userMapper) {
        this.userProfileService = userProfileService;
        this.userMapper = userMapper;
    }

    @Operation(summary = "프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        User currentUser = getCurrentUser();
        User user = userProfileService.getProfile(currentUser.getId());
        UserResponse response = userMapper.toUserResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response, "프로필 조회 성공"));
    }

    @Operation(summary = "프로필 수정", description = "이름, 프로필 이미지 URL, 전화번호, 생년월일을 수정합니다.")
    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request) {
        User currentUser = getCurrentUser();
        User updatedUser = userProfileService.updateProfile(currentUser, request);
        UserResponse response = userMapper.toUserResponse(updatedUser);
        return ResponseEntity.ok(ApiResponse.success(response, "프로필이 수정되었습니다"));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다. (LOCAL 사용자만 가능)")
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request) {
        User currentUser = getCurrentUser();
        userProfileService.changePassword(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다"));
    }

    @Operation(summary = "아이디 변경", description = "사용자 아이디(username)를 변경합니다. 중복된 아이디는 사용할 수 없습니다.")
    @PatchMapping("/me/username")
    public ResponseEntity<ApiResponse<UserResponse>> changeUsername(
            @Valid @RequestBody UsernameChangeRequest request) {
        User currentUser = getCurrentUser();
        User updatedUser = userProfileService.changeUsername(currentUser, request);
        UserResponse response = userMapper.toUserResponse(updatedUser);
        return ResponseEntity.ok(ApiResponse.success(response, "아이디가 변경되었습니다"));
    }

    @Operation(summary = "계정 삭제", description = "사용자 계정을 영구 삭제합니다. 확인 문구 '계정을 삭제합니다' 입력 필요, LOCAL 사용자는 비밀번호 확인 필요")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Valid @RequestBody AccountDeleteRequest request) {
        User currentUser = getCurrentUser();
        userProfileService.deleteAccount(currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("계정이 삭제되었습니다"));
    }

    @Operation(summary = "인증 제공자 정보 조회", description = "현재 사용자의 인증 제공자(LOCAL, GOOGLE, NAVER, KAKAO) 정보와 비밀번호 변경 가능 여부를 조회합니다.")
    @GetMapping("/me/provider")
    public ResponseEntity<ApiResponse<ProviderInfoResponse>> getProviderInfo() {
        User currentUser = getCurrentUser();
        ProviderInfoResponse response = new ProviderInfoResponse(
                currentUser.getProviderType().name(),
                currentUser.getProviderType() == User.ProviderType.LOCAL
        );
        return ResponseEntity.ok(ApiResponse.success(response, "인증 제공자 정보 조회 성공"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다");
        }
        return (User) authentication.getPrincipal();
    }

    public record ProviderInfoResponse(String provider, boolean canChangePassword) {}
}
