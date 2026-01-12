package com.cheongchun.backend.service;

import com.cheongchun.backend.entity.User;
import com.cheongchun.backend.exception.BusinessException;
import com.cheongchun.backend.exception.ErrorCode;
import com.cheongchun.backend.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.cheongchun.backend.repository.UserRepository;

@Service
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final JavaMailSender mailSender;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final String fromAddress;
    private final String baseUrl;

    public EmailVerificationService(JavaMailSender mailSender,
                                   JwtUtil jwtUtil,
                                   UserRepository userRepository,
                                   @Value("${app.email.from-address}") String fromAddress,
                                   @Value("${app.email.base-url}") String baseUrl) {
        this.mailSender = mailSender;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
    }


    public void sendVerificationEmail(User user){
        try{
            // 여기서 이메일 인증용 JWT token 생성
            String emailtoken = jwtUtil.generateEmailVerificationToken(user.getId());

            //Email sender
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail()); // 고객 이메일 얻기
            message.setSubject("청춘장터 이메일 인증");
            String verificationUrl = baseUrl + "/auth/verify-email?token=" + emailtoken;
            String emailBody = String.format(
                "안녕하세요!\n\n" +
                "청춘장터 회원가입을 완료하시려면 아래 링크를 클릭해주세요.\n\n" +
                "%s\n\n" +
                "이 링크는 24시간 후 만료됩니다.\n\n" +
                "청춘장터 팀",
                user.getName(),
                verificationUrl
            );

            message.setText(emailBody);
            mailSender.send(message);
            logger.info("인증 이메일 발송 완료: {}",user.getEmail());
        }
        catch (Exception e){
            logger.error("Email 발송 실패: {}",user.getEmail());
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, "이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 이메일 인증 처리
     */
    public User verifyEmail(String token) {
        try {
            // JWT 토큰 검증 및 사용자 ID 추출
            Long userId = jwtUtil.validateEmailVerificationToken(token);

            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // 이미 인증된 경우 처리
            if (user.isEmailVerified()) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
            }

            // 이메일 인증 완료
            user.setEmailVerified(true);
            User savedUser = userRepository.save(user);

            logger.info("이메일 인증 완료: {} (사용자 ID: {})", user.getEmail(), user.getId());
            return savedUser;

        } catch (Exception e) {
            logger.error("이메일 인증 실패: {}", e.getMessage());
            throw e; // 원래 예외를 그대로 던져서 컨트롤러에서 적절한 메시지 처리
        }
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "해당 이메일로 가입된 계정이 없습니다"));

        if (user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        sendVerificationEmail(user);
        logger.info("인증 이메일 재발송: {}", email);
    }
}

