package com.brandPitara.sfs.dashboard.auth.service;

import com.brandPitara.sfs.dashboard.auth.entity.DashboardRefreshTokenEntity;
import com.brandPitara.sfs.dashboard.auth.repository.DashboardRefreshTokenRepository;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
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
public class DashboardRefreshTokenService {

    private final DashboardRefreshTokenRepository refreshTokenRepository;

    @Value("${dashboard.refresh.expiration-days}")
    private long refreshExpirationDays;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String createRefreshToken(DashboardUserEntity user) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        DashboardRefreshTokenEntity token = DashboardRefreshTokenEntity.builder()
                .dashboardUser(user)
                .tokenHash(tokenHash)
                .revoked(false)
                .expiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays))
                .build();

        refreshTokenRepository.save(token);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public DashboardRefreshTokenEntity validateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        DashboardRefreshTokenEntity token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!token.isUsable()) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        if (!Boolean.TRUE.equals(token.getDashboardUser().getActive())) {
            throw new BadCredentialsException("Dashboard user is disabled");
        }

        return token;
    }

    @Transactional
    public String rotateRefreshToken(DashboardRefreshTokenEntity oldToken) {
        String newRawToken = generateSecureToken();
        String newHash = hashToken(newRawToken);

        oldToken.revoke(newHash);
        refreshTokenRepository.save(oldToken);

        DashboardRefreshTokenEntity newToken = DashboardRefreshTokenEntity.builder()
                .dashboardUser(oldToken.getDashboardUser())
                .tokenHash(newHash)
                .revoked(false)
                .expiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays))
                .build();

        refreshTokenRepository.save(newToken);

        return newRawToken;
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }
}