package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.RefreshTokenRepository;
import com.brandPitara.sfs.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

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
    public RefreshToken createRefreshToken(User user, String deviceId, String fcmToken) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(generateRandomToken())
                .deviceId(deviceId)
                .fcmToken(fcmToken)
                .expiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verifyAndGet(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (rt.isRevoked() || rt.isExpired()) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        return rt;
    }

    @Override
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
