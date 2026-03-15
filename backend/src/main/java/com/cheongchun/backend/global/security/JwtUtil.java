package com.cheongchun.backend.global.security;

import com.cheongchun.backend.global.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        // JWT 시크릿 키를 바이트 배열로 변환 후 HMAC-SHA 키 생성
        byte[] keyBytes = jwtProperties.getSecret().getBytes();

        // 키가 256비트(32바이트) 이상인지 확인하고, 부족하면 패딩 추가
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        logger.info("JWT 유틸리티 초기화 완료");
    }

    // Username or Email인증인지 확인
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 Claims는 반환 (필요한 경우)
            return e.getClaims();
        } catch (Exception e) {
            throw new RuntimeException("토큰에서 정보를 추출할 수 없습니다", e);
        }
    }

    /**
     * JWT 토큰 생성 (사용자명 기반)
     */
    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(username)
                .issuer("authentication")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWT 토큰에서 사용자명 추출
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (Exception e) {
            logger.error("JWT 토큰에서 사용자명 추출 실패: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * JWT 토큰 유효성 검사
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            logger.debug("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // 이메일 인증용 토큰 생성
    public String generateEmailVerificationToken(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuer("email-verification")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(secretKey)
                .compact();
    }

    // 이메일 인증 토큰 단기 일회용
    public Long validateEmailVerificationToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)           // setSigningKey → verifyWith
                    .build()
                    .parseSignedClaims(token)        // parseClaimsJws → parseSignedClaims
                    .getPayload();                   // getBody → getPayload

            // 발급자 확인
            if (!"email-verification".equals(claims.getIssuer())) {
                throw new RuntimeException("잘못된 토큰 타입입니다");
            }

            return Long.parseLong(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("인증 링크가 만료되었습니다");
        } catch (Exception e) {
            throw new RuntimeException("유효하지 않은 인증 링크입니다");
        }
    }

}
