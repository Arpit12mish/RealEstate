package com.brandPitara.sfs.mobileupdate.dto;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.PolicyAuditAction;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

public record MobileAppUpdatePolicyAuditResponse(
        Long id,
        MobilePlatform platform,
        PolicyAuditAction action,
        JsonNode previousPolicy,
        JsonNode newPolicy,
        Long dashboardUserId,
        String dashboardUserName,
        String requestId,
        String changeReason,
        OffsetDateTime createdAt
) {
}

