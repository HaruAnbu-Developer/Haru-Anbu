package com.cheongchun.backend.repository;

import com.cheongchun.backend.entity.RefreshToken;
import com.cheongchun.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰으로 RefreshToken 조회
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자의 모든 유효한 리프레시 토큰 조회
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.used = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * 사용자의 모든 리프레시 토큰 조회
     */
    List<RefreshToken> findByUser(User user);

    /**
     * 만료된 토큰들 조회
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.expiresAt < :now")
    List<RefreshToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 사용자의 모든 토큰 무효화 (로그아웃 시 사용)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllByUser(@Param("user") User user);

    /**
     * 특정 토큰 무효화
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.token = :token")
    int revokeByToken(@Param("token") String token);

    /**
     * 만료된 토큰들 삭제 (정리 작업용)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :expiredBefore")
    int deleteExpiredTokens(@Param("expiredBefore") LocalDateTime expiredBefore);

    /**
     * 사용자의 오래된 토큰들 삭제 (최신 N개만 유지)
     */
    @Modifying
    @Query(value = "DELETE FROM refresh_tokens WHERE user_id = :userId AND id NOT IN " +
            "(SELECT id FROM (SELECT id FROM refresh_tokens WHERE user_id = :userId " +
            "ORDER BY created_at DESC LIMIT :limit) t)", nativeQuery = true)
    int deleteOldTokensByUser(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 토큰 존재 여부 확인
     */
    boolean existsByToken(String token);

    /**
     * 사용자의 유효한 토큰 개수 조회
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.used = false AND rt.expiresAt > :now")
    long countValidTokensByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}