package com.brandPitara.sfs.service;

import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user, String deviceId, String fcmToken);

    RefreshToken verifyAndGet(String token);

    void revokeToken(String token);

    void revokeAllByUser(Long userId);
}
