package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.RefreshTokenRepository;
import com.brandPitara.sfs.service.RefreshTokenService;
import com.brandPitara.sfs.service.model.RefreshTokenRotationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.expiration.days:15}")
    private long refreshExpirationDays;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    private String generateRandomToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    @Override
    public String createRefreshToken(User user, String deviceId, String fcmToken) {
        String rawToken = generateRandomToken();

        saveRefreshToken(user, rawToken, deviceId, fcmToken);
        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verifyForLogoutOnly(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (rt.isRevoked() || rt.isExpired()) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        return rt;
    }

    @Override
    public RefreshTokenRotationResult rotateRefreshToken(String token) {
        RefreshToken oldToken = refreshTokenRepository.findByTokenForUpdate(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (oldToken.isRevoked()) {
            revokeTokenFamily(oldToken);
            throw new IllegalArgumentException("Refresh token reuse detected");
        }

        if (oldToken.isExpired()) {
            oldToken.setRevoked(true);
            refreshTokenRepository.save(oldToken);
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        String newRawToken = generateRandomToken();
        saveRefreshToken(oldToken.getUser(), newRawToken, oldToken.getDeviceId(), oldToken.getFcmToken());

        return new RefreshTokenRotationResult(
                oldToken.getUser(),
                newRawToken,
                oldToken.getDeviceId(),
                oldToken.getFcmToken()
        );
    }

    @Override
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(hashToken(token)).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    private void saveRefreshToken(User user, String rawToken, String deviceId, String fcmToken) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(hashToken(rawToken))
                .deviceId(deviceId)
                .fcmToken(fcmToken)
                .expiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    private void revokeTokenFamily(RefreshToken token) {
        Long userId = token.getUser().getId();
        if (token.getDeviceId() == null || token.getDeviceId().isBlank()) {
            refreshTokenRepository.revokeAllByUserId(userId);
            return;
        }
        refreshTokenRepository.revokeActiveByUserIdAndDeviceId(userId, token.getDeviceId());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }

    @Override
    public void revokeAllByUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
