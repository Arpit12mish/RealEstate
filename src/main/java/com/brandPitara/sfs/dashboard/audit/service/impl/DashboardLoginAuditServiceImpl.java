package com.brandPitara.sfs.dashboard.audit.service.impl;

import com.brandPitara.sfs.dashboard.audit.entity.DashboardLoginAuditEntity;
import com.brandPitara.sfs.dashboard.audit.repository.DashboardLoginAuditRepository;
import com.brandPitara.sfs.dashboard.audit.service.DashboardLoginAuditService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DashboardLoginAuditServiceImpl implements DashboardLoginAuditService {

    private final DashboardLoginAuditRepository loginAuditRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(DashboardUserEntity user, HttpServletRequest request) {
        loginAuditRepository.save(
                DashboardLoginAuditEntity.success(
                        user,
                        resolveIp(request),
                        resolveUserAgent(request)
                )
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String email, String reason, HttpServletRequest request) {
        loginAuditRepository.save(
                DashboardLoginAuditEntity.failure(
                        email,
                        reason,
                        resolveIp(request),
                        resolveUserAgent(request)
                )
        );
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        return request.getHeader("User-Agent");
    }
}