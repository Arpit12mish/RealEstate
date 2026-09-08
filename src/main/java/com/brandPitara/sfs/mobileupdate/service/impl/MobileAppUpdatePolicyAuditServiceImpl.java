package com.brandPitara.sfs.mobileupdate.service.impl;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.PolicyAuditAction;
import com.brandPitara.sfs.mobileupdate.dto.DashboardMobileAppUpdatePolicyResponse;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdatePolicyAuditResponse;
import com.brandPitara.sfs.mobileupdate.entity.MobileAppUpdatePolicyAuditEntity;
import com.brandPitara.sfs.mobileupdate.repository.MobileAppUpdatePolicyAuditRepository;
import com.brandPitara.sfs.mobileupdate.service.MobileAppUpdatePolicyAuditService;
import com.brandPitara.sfs.observability.LoggingConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class MobileAppUpdatePolicyAuditServiceImpl implements MobileAppUpdatePolicyAuditService {

    private final MobileAppUpdatePolicyAuditRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void record(
            PolicyAuditAction action,
            DashboardMobileAppUpdatePolicyResponse previousPolicy,
            DashboardMobileAppUpdatePolicyResponse newPolicy,
            DashboardUserEntity actor,
            String changeReason) {
        repository.save(MobileAppUpdatePolicyAuditEntity.builder()
                .platform(newPolicy.platform())
                .action(action)
                .previousPolicy(objectMapper.valueToTree(previousPolicy))
                .newPolicy(objectMapper.valueToTree(newPolicy))
                .dashboardUserId(actor.getId())
                .dashboardUserName(actor.getName())
                .requestId(currentRequestId())
                .changeReason(changeReason.trim())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MobileAppUpdatePolicyAuditResponse> list(MobilePlatform platform, Pageable pageable) {
        return repository.findByPlatformOrderByCreatedAtDesc(platform, pageable).map(entity ->
                new MobileAppUpdatePolicyAuditResponse(
                        entity.getId(), entity.getPlatform(), entity.getAction(),
                        entity.getPreviousPolicy(), entity.getNewPolicy(),
                        entity.getDashboardUserId(), entity.getDashboardUserName(),
                        entity.getRequestId(), entity.getChangeReason(), entity.getCreatedAt()));
    }

    private String currentRequestId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            Object requestId = request.getAttribute(LoggingConstants.ATTR_REQUEST_ID);
            return requestId == null ? null : requestId.toString();
        }
        return null;
    }
}

