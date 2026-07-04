package com.brandPitara.sfs.service;

import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.service.model.RefreshTokenRotationResult;

public interface RefreshTokenService {

    /**
     * Creates a refresh token for the user, stores its SHA-256 hash in the DB,
     * and returns the raw opaque token to be sent to the client.
     * The raw token is never persisted.
     */
    String createRefreshToken(User user, String deviceId, String fcmToken);

    /**
     * Read-only lookup used exclusively to resolve which user a logout-all request is
     * for. Deliberately does NOT rotate, lock, or perform reuse-family revocation —
     * callers must never treat a call to this method as consuming/validating the token
     * for continued use. Any flow that needs to trust/consume a refresh token must go
     * through {@link #rotateRefreshToken(String)} instead.
     */
    RefreshToken verifyForLogoutOnly(String token);

    RefreshTokenRotationResult rotateRefreshToken(String token);

    void revokeToken(String token);

    void revokeAllByUser(Long userId);
}
