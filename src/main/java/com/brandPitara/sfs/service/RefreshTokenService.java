package com.brandPitara.sfs.service;

import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;

public interface RefreshTokenService {

    /**
     * Creates a refresh token for the user, stores its SHA-256 hash in the DB,
     * and returns the raw opaque token to be sent to the client.
     * The raw token is never persisted.
     */
    String createRefreshToken(User user, String deviceId, String fcmToken);

    RefreshToken verifyAndGet(String token);

    void revokeToken(String token);

    void revokeAllByUser(Long userId);
}
