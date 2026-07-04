package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user WHERE rt.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    int deleteExpiredTokens(OffsetDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllByUserId(Long userId);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revoked = true
            WHERE rt.user.id = :userId
              AND rt.revoked = false
              AND (
                    (:deviceId IS NULL AND rt.deviceId IS NULL)
                    OR rt.deviceId = :deviceId
              )
            """)
    int revokeActiveByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteAllByUserId(Long userId);
}
