package com.cheongchun.backend.token.service;

import com.cheongchun.backend.token.domain.RefreshToken;
import com.cheongchun.backend.user.domain.User;
import com.cheongchun.backend.token.repository.RefreshTokenRepository;
import com.cheongchun.backend.global.config.properties.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.secureRandom = new SecureRandom();
    }

    /**
     * 새로운 리프레시 토큰 생성
     */
    public RefreshToken createRefreshToken(User user, String userAgent, String ipAddress) {
        // 사용자의 기존 토큰 개수 확인 및 정리
        cleanupOldTokensForUser(user);

        // 새 토큰 생성
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpiration() / 1000);

        RefreshToken refreshToken = new RefreshToken(token, user, expiresAt);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setIpAddress(ipAddress);

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * 리프레시 토큰으로 조회
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * 리프레시 토큰 검증
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (!token.isValid()) {
            String errorMessage;
            if (token.isExpired()) {
                errorMessage = "리프레시 토큰이 만료되었습니다";
            } else if (token.getUsed()) {
                errorMessage = "이미 사용된 리프레시 토큰입니다";
            } else if (token.getRevoked()) {
                errorMessage = "무효화된 리프레시 토큰입니다";
            } else {
                errorMessage = "유효하지 않은 리프레시 토큰입니다";
            }

            refreshTokenRepository.delete(token);
            throw new RuntimeException(errorMessage);
        }

        return token;
    }

    /**
     * 리프레시 토큰 사용 처리 (one-time use)
     */
    public void markTokenAsUsed(RefreshToken refreshToken) {
        refreshToken.markAsUsed();
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * 사용자의 모든 리프레시 토큰 무효화 (로그아웃)
     */
    public void revokeAllTokensByUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    /**
     * 특정 토큰 무효화
     */
    public void revokeToken(String token) {
        refreshTokenRepository.revokeByToken(token);
    }

    /**
     * 사용자의 오래된 토큰들 정리 (최신 N개만 유지)
     */
    private void cleanupOldTokensForUser(User user) {
        long validTokenCount = refreshTokenRepository.countValidTokensByUser(user, LocalDateTime.now());

        if (validTokenCount >= jwtProperties.getMaxRefreshTokensPerUser()) {
            // 현재 유효한 토큰이 최대 개수를 초과하면 오래된 것들 삭제
            refreshTokenRepository.deleteOldTokensByUser(user.getId(), jwtProperties.getMaxRefreshTokensPerUser() - 1);
        }
    }

    /**
     * 만료된 토큰들 정리 (스케줄링)
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void cleanupExpiredTokens() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(oneDayAgo);

        if (deletedCount > 0) {
            logger.info("정리된 만료 토큰 개수: {}", deletedCount);
        }
    }

    /**
     * 보안성 높은 토큰 생성
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[64]; // 512비트
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 사용자의 활성 세션 조회 (관리용)
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> getActiveSessionsByUser(User user) {
        return refreshTokenRepository.findValidTokensByUser(user, LocalDateTime.now());
    }

    /**
     * 토큰 통계 정보
     */
    @Transactional(readOnly = true)
    public RefreshTokenStats getTokenStats(User user) {
        List<RefreshToken> validTokens = refreshTokenRepository.findValidTokensByUser(user, LocalDateTime.now());
        List<RefreshToken> allTokens = refreshTokenRepository.findByUser(user);

        return new RefreshTokenStats(
                validTokens.size(),
                allTokens.size(),
                jwtProperties.getMaxRefreshTokensPerUser()
        );
    }

    /**
     * 토큰 통계 정보를 담는 내부 클래스
     */
    public static class RefreshTokenStats {
        private final int activeTokens;
        private final int totalTokens;
        private final int maxAllowed;

        public RefreshTokenStats(int activeTokens, int totalTokens, int maxAllowed) {
            this.activeTokens = activeTokens;
            this.totalTokens = totalTokens;
            this.maxAllowed = maxAllowed;
        }

        public int getActiveTokens() { return activeTokens; }
        public int getTotalTokens() { return totalTokens; }
        public int getMaxAllowed() { return maxAllowed; }
        public boolean isNearLimit() { return activeTokens >= maxAllowed - 1; }
    }
}
