package com.brandPitara.sfs.dashboard.audit.service.impl;

import com.brandPitara.sfs.dashboard.audit.dto.DashboardActionAuditEntryDto;
import com.brandPitara.sfs.dashboard.audit.entity.DashboardActionAuditEntity;
import com.brandPitara.sfs.dashboard.audit.repository.DashboardActionAuditRepository;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;

import java.time.OffsetDateTime;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardActionAuditServiceImpl implements DashboardActionAuditService {

    private final DashboardActionAuditRepository auditRepository;
    private final DashboardCurrentUserService currentUserService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void record(DashboardAuditAction action, ReviewEntityType entityType, Long entityId, Long projectId) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            tx.executeWithoutResult(status -> {
                DashboardUserEntity user = tryGetCurrentUser();

                auditRepository.save(DashboardActionAuditEntity.builder()
                        .dashboardUserId(user != null ? user.getId() : null)
                        .dashboardUserName(user != null ? user.getName() : null)
                        .dashboardUserRole(user != null ? user.getRole() : null)
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId)
                        .projectId(projectId)
                        .summary(buildSummary(action, entityType, entityId, user))
                        .ipAddress(resolveIp())
                        .userAgent(resolveUserAgent())
                        .build());
            });
        } catch (Exception e) {
            log.warn("Failed to record dashboard action audit: action={}, entityType={}, entityId={}",
                    action, entityType, entityId, e);
        }
    }

    private DashboardUserEntity tryGetCurrentUser() {
        try {
            return currentUserService.getCurrentUserOrThrow();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildSummary(DashboardAuditAction action, ReviewEntityType entityType, Long entityId, DashboardUserEntity user) {
        String actor = user != null ? user.getName() + " [" + user.getRole() + "]" : "unknown";
        return actor + " · " + action.name() + " · " + entityType.name() + " #" + entityId;
    }

    private String resolveIp() {
        try {
            HttpServletRequest req = currentRequest();
            if (req == null) return null;
            String xff = req.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xff)) return xff.split(",")[0].trim();
            String xri = req.getHeader("X-Real-IP");
            if (StringUtils.hasText(xri)) return xri.trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUserAgent() {
        try {
            HttpServletRequest req = currentRequest();
            return req != null ? req.getHeader("User-Agent") : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardActionAuditEntryDto> listByProject(Long projectId, Pageable pageable) {
        return auditRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable)
                .map(DashboardActionAuditEntryDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardActionAuditEntryDto> listByUser(Long dashboardUserId, Pageable pageable) {
        return auditRepository.findByDashboardUserIdOrderByCreatedAtDesc(dashboardUserId, pageable)
                .map(DashboardActionAuditEntryDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardActionAuditEntryDto> listAll(
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            DashboardAuditAction action,
            ReviewEntityType entityType,
            DashboardRole userRole,
            Pageable pageable
    ) {
        return auditRepository.findAllFiltered(fromDate, toDate, action, entityType, userRole, pageable)
                .map(DashboardActionAuditEntryDto::from);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
