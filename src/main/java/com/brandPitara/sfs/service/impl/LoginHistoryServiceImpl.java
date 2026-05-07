package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.entity.LoginHistory;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.LoginHistoryRepository;
import com.brandPitara.sfs.service.LoginHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    @Override
    public void recordLogin(User user, String loginType, boolean success,
                            String deviceId, String fcmToken, HttpServletRequest request) {

        LoginHistory history = new LoginHistory();
        history.setUser(user);
        history.setLoginType(loginType);
        history.setSuccess(success);
        history.setDeviceId(deviceId);
        history.setFcmToken(fcmToken);
        history.setIpAddress(extractClientIp(request));
        history.setUserAgent(request.getHeader("User-Agent"));
        history.setLoginTime(OffsetDateTime.now());

        loginHistoryRepository.save(history);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
