package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.exception.BusinessException;
import com.cheongchun.backend.exception.ErrorCode;
import com.cheongchun.backend.repository.UserRepository;
import com.cheongchun.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService 단위 테스트")
class EmailVerificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User testUser;
    private final String FROM_ADDRESS = "noreply@cheongchun.com";
    private final String BASE_URL = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        // @Value 주입 시뮬레이션
        ReflectionTestUtils.setField(emailVerificationService, "fromAddress", FROM_ADDRESS);
        ReflectionTestUtils.setField(emailVerificationService, "baseUrl", BASE_URL);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("테스트유저");
        testUser.setEmailVerified(false);
    }

    @Test
    @DisplayName("이메일 인증 메일 발송 성공")
    void sendVerificationEmail_Success() {
        // Given
        String emailToken = "mock-email-token";
        when(jwtUtil.generateEmailVerificationToken(testUser.getId())).thenReturn(emailToken);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailVerificationService.sendVerificationEmail(testUser);

        // Then
        verify(jwtUtil).generateEmailVerificationToken(1L);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("이메일 발송 실패 - BusinessException 발생")
    void sendVerificationEmail_Failure() {
        // Given
        when(jwtUtil.generateEmailVerificationToken(testUser.getId())).thenReturn("token");
        doThrow(new RuntimeException("SMTP 연결 실패")).when(mailSender).send(any(SimpleMailMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(testUser))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_SEND_FAILED);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("이메일 인증 성공 - 토큰 검증 및 사용자 활성화")
    void verifyEmail_Success() {
        // Given
        String validToken = "valid-token";
        when(jwtUtil.validateEmailVerificationToken(validToken)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User verifiedUser = emailVerificationService.verifyEmail(validToken);

        // Then
        assertThat(verifiedUser).isNotNull();
        assertThat(verifiedUser.isEmailVerified()).isTrue();

        verify(jwtUtil).validateEmailVerificationToken(validToken);
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("이메일 인증 실패 - 사용자를 찾을 수 없음")
    void verifyEmail_UserNotFound() {
        // Given
        String validToken = "valid-token";
        when(jwtUtil.validateEmailVerificationToken(validToken)).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(validToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("이메일 인증 실패 - 이미 인증된 계정")
    void verifyEmail_AlreadyVerified() {
        // Given
        testUser.setEmailVerified(true); // 이미 인증됨

        String validToken = "valid-token";
        when(jwtUtil.validateEmailVerificationToken(validToken)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(validToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_VERIFIED);

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("이메일 인증 실패 - 유효하지 않은 토큰")
    void verifyEmail_InvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        when(jwtUtil.validateEmailVerificationToken(invalidToken))
                .thenThrow(new RuntimeException("Invalid JWT signature"));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(invalidToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid JWT signature");

        verify(jwtUtil).validateEmailVerificationToken(invalidToken);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("인증 이메일 재발송 성공")
    void resendVerificationEmail_Success() {
        // Given
        String emailToken = "new-token";
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateEmailVerificationToken(testUser.getId())).thenReturn(emailToken);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailVerificationService.resendVerificationEmail(testUser.getEmail());

        // Then
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(jwtUtil).generateEmailVerificationToken(1L);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("인증 이메일 재발송 실패 - 사용자를 찾을 수 없음")
    void resendVerificationEmail_UserNotFound() {
        // Given
        String unknownEmail = "unknown@example.com";
        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.resendVerificationEmail(unknownEmail))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userRepository).findByEmail(unknownEmail);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("인증 이메일 재발송 실패 - 이미 인증된 계정")
    void resendVerificationEmail_AlreadyVerified() {
        // Given
        testUser.setEmailVerified(true); // 이미 인증됨
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.resendVerificationEmail(testUser.getEmail()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_VERIFIED);

        verify(userRepository).findByEmail(testUser.getEmail());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
