package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.observability.LogEvents;
import com.brandPitara.sfs.observability.LoggingConstants;
import com.brandPitara.sfs.repository.RefreshTokenRepository;
import com.brandPitara.sfs.service.RefreshTokenService;
import com.brandPitara.sfs.service.model.RefreshTokenRotationResult;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger(LoggingConstants.LOGGER_SECURITY);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenFamilyRevoker refreshTokenFamilyRevoker;

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
                .orElseThrow(() -> {
                    logRefreshEvent(LogEvents.REFRESH_TOKEN_NOT_FOUND, null, null,
                            "Logout-all requested for an unrecognized refresh token");
                    return new IllegalArgumentException("Invalid refresh token");
                });

        if (rt.isRevoked()) {
            logRefreshEvent(LogEvents.REFRESH_TOKEN_REVOKED, rt.getUser().getId(), rt.getDeviceId(),
                    "Logout-all requested for an already-revoked refresh token");
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        if (rt.isExpired()) {
            logRefreshEvent(LogEvents.REFRESH_TOKEN_EXPIRED, rt.getUser().getId(), rt.getDeviceId(),
                    "Logout-all requested for an expired refresh token");
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        return rt;
    }

    @Override
    public RefreshTokenRotationResult rotateRefreshToken(String token) {
        RefreshToken oldToken = refreshTokenRepository.findByTokenForUpdate(hashToken(token))
                .orElseThrow(() -> {
                    logRefreshEvent(LogEvents.REFRESH_TOKEN_NOT_FOUND, null, null,
                            "Refresh requested with an unrecognized refresh token");
                    return new IllegalArgumentException("Invalid refresh token");
                });

        if (oldToken.isRevoked()) {
            // The client presented a token this service already rotated away
            // (or that was revoked by a prior logout/reuse event) — this is
            // exactly the signal a stolen or, as in the 2026-08-17 mobile
            // incident, a locally-stale-but-never-updated refresh token
            // produces. Revoking the whole family (not just this one token)
            // is deliberate: if this really is reuse, the descendant the
            // attacker doesn't know about must die too.
            logRefreshEvent(LogEvents.REFRESH_TOKEN_REUSE, oldToken.getUser().getId(), oldToken.getDeviceId(),
                    "Already-rotated refresh token replayed; revoking device session family");
            // Deliberately routed through a separate REQUIRES_NEW-transactional
            // bean, not a plain private-method call: this method is about to
            // throw, and this class's default @Transactional rollback rule
            // would otherwise silently undo the revocation along with it. See
            // RefreshTokenFamilyRevoker's javadoc.
            refreshTokenFamilyRevoker.revokeFamily(oldToken.getUser().getId(), oldToken.getDeviceId());
            throw new IllegalArgumentException("Refresh token reuse detected");
        }

        if (oldToken.isExpired()) {
            logRefreshEvent(LogEvents.REFRESH_TOKEN_EXPIRED, oldToken.getUser().getId(), oldToken.getDeviceId(),
                    "Refresh token past its expiry");
            oldToken.setRevoked(true);
            refreshTokenRepository.save(oldToken);
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        String newRawToken = generateRandomToken();
        saveRefreshToken(oldToken.getUser(), newRawToken, oldToken.getDeviceId(), oldToken.getFcmToken());

        logRefreshEvent(LogEvents.REFRESH_SUCCESS, oldToken.getUser().getId(), oldToken.getDeviceId(),
                "Refresh token rotated");

        return new RefreshTokenRotationResult(
                oldToken.getUser(),
                newRawToken,
                oldToken.getDeviceId(),
                oldToken.getFcmToken()
        );
    }

    @Override
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(hashToken(token)).ifPresentOrElse(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            logRefreshEvent(LogEvents.LOGOUT_SUCCESS, rt.getUser().getId(), rt.getDeviceId(),
                    "Refresh token revoked via logout");
        }, () -> logRefreshEvent(LogEvents.REFRESH_TOKEN_NOT_FOUND, null, null,
                "Logout requested for an unrecognized refresh token"));
    }

    /**
     * Structured, secret-free audit logging for the refresh-token lifecycle.
     * Never logs the raw or hashed token value — only the entity ids needed
     * to correlate an incident, matching the pattern already used by
     * {@code JwtRequestFilter}/{@code JwtAuthenticationEntryPoint}.
     */
    private void logRefreshEvent(String event, Long userId, String deviceId, String message) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event", event);
        if (userId != null) {
            fields.put("userId", userId);
        }
        if (deviceId != null) {
            fields.put("deviceId", deviceId);
        }
        String requestId = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        if (requestId != null) {
            fields.put("requestId", requestId);
        }
        fields.put("message", message);

        if (LogEvents.REFRESH_SUCCESS.equals(event) || LogEvents.LOGOUT_SUCCESS.equals(event)) {
            SECURITY_LOG.info("{}", StructuredArguments.entries(fields));
        } else {
            SECURITY_LOG.warn("{}", StructuredArguments.entries(fields));
        }
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
