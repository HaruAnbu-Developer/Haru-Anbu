package com.cheongchun.backend.service;

import com.cheongchun.backend.dto.LoginRequest;
import com.cheongchun.backend.dto.SignUpRequest;
import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.exception.BusinessException;
import com.cheongchun.backend.exception.EmailAlreadyExistsException;
import com.cheongchun.backend.exception.ErrorCode;
import com.cheongchun.backend.exception.UserAlreadyExistsException;
import com.cheongchun.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private SignUpRequest validSignUpRequest;
    private LoginRequest validLoginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 회원가입 요청 데이터
        validSignUpRequest = new SignUpRequest();
        validSignUpRequest.setUsername("testuser");
        validSignUpRequest.setEmail("test@example.com");
        validSignUpRequest.setPassword("password123");
        validSignUpRequest.setName("테스트유저");

        // 로그인 요청 데이터
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password123");

        // 테스트 사용자
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setName("테스트유저");
        testUser.setProviderType(User.ProviderType.LOCAL);
        testUser.setEmailVerified(true);
    }

    @Test
    @DisplayName("정상 회원가입 - 사용자 생성 성공")
    void createUser_Success() {
        // Given
        when(userRepository.existsByUsername(validSignUpRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(validSignUpRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validSignUpRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User createdUser = authService.createUser(validSignUpRequest);

        // Then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo("testuser");
        assertThat(createdUser.getEmail()).isEqualTo("test@example.com");
        assertThat(createdUser.getProviderType()).isEqualTo(User.ProviderType.LOCAL);

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 사용자명")
    void createUser_DuplicateUsername() {
        // Given
        when(userRepository.existsByUsername(validSignUpRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.createUser(validSignUpRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("사용자명이 이미 사용 중입니다");

        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일")
    void createUser_DuplicateEmail() {
        // Given
        when(userRepository.existsByUsername(validSignUpRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(validSignUpRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.createUser(validSignUpRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("이메일이 이미 사용 중입니다");

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공 - 이메일 인증 완료된 LOCAL 사용자")
    void authenticateUserCookie_Success() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // When
        User authenticatedUser = authService.authenticateUserCookie(validLoginRequest);

        // Then
        assertThat(authenticatedUser).isNotNull();
        assertThat(authenticatedUser.getUsername()).isEqualTo("testuser");
        assertThat(authenticatedUser.isEmailVerified()).isTrue();

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 미인증 LOCAL 사용자")
    void authenticateUserCookie_EmailNotVerified() {
        // Given
        testUser.setEmailVerified(false); // 이메일 미인증 상태

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // When & Then
        assertThatThrownBy(() -> authService.authenticateUserCookie(validLoginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이메일 인증이 필요합니다")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("로그인 성공 - 소셜 로그인 사용자는 이메일 인증 체크 안함")
    void authenticateUserCookie_SocialLogin_SkipEmailVerification() {
        // Given
        testUser.setProviderType(User.ProviderType.GOOGLE);
        testUser.setEmailVerified(false); // 이메일 미인증이어도 OK

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // When
        User authenticatedUser = authService.authenticateUserCookie(validLoginRequest);

        // Then
        assertThat(authenticatedUser).isNotNull();
        assertThat(authenticatedUser.getProviderType()).isEqualTo(User.ProviderType.GOOGLE);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("로그아웃 성공 - 특정 리프레시 토큰 무효화")
    void logout_WithToken_Success() {
        // Given
        String refreshToken = "valid-refresh-token";
        doNothing().when(refreshTokenService).revokeToken(refreshToken);

        // When
        authService.logout(testUser, refreshToken);

        // Then
        verify(refreshTokenService).revokeToken(refreshToken);
        verify(refreshTokenService, never()).revokeAllTokensByUser(any(User.class));
    }

    @Test
    @DisplayName("로그아웃 성공 - 토큰 없으면 모든 토큰 무효화")
    void logout_WithoutToken_RevokeAll() {
        // Given
        doNothing().when(refreshTokenService).revokeAllTokensByUser(testUser);

        // When
        authService.logout(testUser, null);

        // Then
        verify(refreshTokenService).revokeAllTokensByUser(testUser);
        verify(refreshTokenService, never()).revokeToken(anyString());
    }

    @Test
    @DisplayName("모든 디바이스에서 로그아웃 성공")
    void logoutFromAllDevices_Success() {
        // Given
        doNothing().when(refreshTokenService).revokeAllTokensByUser(testUser);

        // When
        authService.logoutFromAllDevices(testUser);

        // Then
        verify(refreshTokenService).revokeAllTokensByUser(testUser);
    }
}
