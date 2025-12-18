package com.brandPitara.sfs.service;

import com.brandPitara.sfs.entity.User;

import jakarta.servlet.http.HttpServletRequest;

public interface LoginHistoryService {
    void recordLogin(User user, String loginType, boolean success,
                     String deviceId, String fcmToken, HttpServletRequest request);
}
