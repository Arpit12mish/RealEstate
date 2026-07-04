package com.brandPitara.sfs.service.model;

import com.brandPitara.sfs.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefreshTokenRotationResult {
    private final User user;
    private final String refreshToken;
    private final String deviceId;
    private final String fcmToken;
}
