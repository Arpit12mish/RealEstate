package com.brandPitara.sfs.mobileupdate.service.impl;

import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.mobileupdate.*;
import com.brandPitara.sfs.mobileupdate.cache.MobileAppUpdatePolicyCache;
import com.brandPitara.sfs.mobileupdate.config.MobileUpdateProperties;
import com.brandPitara.sfs.mobileupdate.dto.DashboardMobileAppUpdatePolicyRequest;
import com.brandPitara.sfs.mobileupdate.dto.DashboardMobileAppUpdatePolicyResponse;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdatePolicyResponse;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdateEmergencyRequest;
import com.brandPitara.sfs.mobileupdate.entity.MobileAppUpdatePolicyEntity;
import com.brandPitara.sfs.mobileupdate.exception.MobileUpdatePolicyException;
import com.brandPitara.sfs.mobileupdate.metrics.MobileUpdatePolicyMetrics;
import com.brandPitara.sfs.mobileupdate.repository.MobileAppUpdatePolicyRepository;
import com.brandPitara.sfs.mobileupdate.service.MobileAppUpdatePolicyAuditService;
import com.brandPitara.sfs.mobileupdate.service.MobileAppUpdatePolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobileAppUpdatePolicyServiceImpl implements MobileAppUpdatePolicyService {

    private static final String ANDROID_PACKAGE_ID = "com.squarefootstory.app";
    private static final String IOS_APP_ID = "6764284866";

    private final MobileAppUpdatePolicyRepository repository;
    private final MobileAppUpdatePolicyCache policyCache;
    private final MobileUpdateProperties properties;
    private final MobileUpdatePolicyMetrics metrics;
    private final DashboardCurrentUserService currentUserService;
    private final MobileAppUpdatePolicyAuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public MobileAppUpdatePolicyResponse evaluate(MobilePlatform platform, long currentBuild) {
        if (currentBuild < 0) throw invalidRequest("currentBuild must be zero or greater");
        if (properties.isEmergencyKillSwitchEnabled()) return current(platform);

        MobileAppUpdatePolicyEntity policy = policyCache.find(platform);
        if (policy == null) {
            log.error("Mobile update policy row is missing: platform={}", platform);
            metrics.recordUnavailablePolicy(platform, "missing");
            throw unavailable("UPDATE_POLICY_UNAVAILABLE", "Update policy is temporarily unavailable");
        }
        if (Boolean.TRUE.equals(policy.getEmergencyDisabled())
                || policy.getPolicyState() != PolicyState.ACTIVE) {
            return current(platform);
        }

        validateActivatedPolicy(policy);
        if (currentBuild > policy.getLatestBuild()) {
            metrics.recordBuildAheadOfPolicy(platform);
            return current(platform);
        }
        if (policy.getEnforcementMode() == EnforcementMode.OFF
                || policy.getEnforcementMode() == EnforcementMode.OBSERVE) {
            return current(platform);
        }
        return toPublicResponse(policy, currentBuild);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DashboardMobileAppUpdatePolicyResponse> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(policy -> policy.getPlatform().name()))
                .map(this::toDashboardResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardMobileAppUpdatePolicyResponse get(MobilePlatform platform) {
        return toDashboardResponse(findForDashboard(platform));
    }

    @Override
    @Transactional
    public DashboardMobileAppUpdatePolicyResponse update(
            MobilePlatform platform, DashboardMobileAppUpdatePolicyRequest request) {
        validateConfiguration(platform, request);
        MobileAppUpdatePolicyEntity entity = findForDashboard(platform);
        if (!entity.getVersion().equals(request.expectedVersion())) {
            throw new MobileUpdatePolicyException(HttpStatus.CONFLICT,
                    "UPDATE_POLICY_VERSION_CONFLICT",
                    "The update policy changed; reload it before saving");
        }

        DashboardUserEntity actor = currentUserService.getCurrentUserOrThrow();
        DashboardMobileAppUpdatePolicyResponse previous = toDashboardResponse(entity);

        entity.setLatestVersion(request.latestVersion().trim());
        entity.setLatestBuild(request.latestBuild());
        entity.setMinimumSupportedBuild(request.minimumSupportedBuild());
        entity.setStoreUrl(request.storeUrl().trim());
        entity.setTitle(request.title().trim());
        entity.setMessage(request.message().trim());
        entity.setReleaseNotes(clean(request.releaseNotes()));
        entity.setRemindAfterHours(request.remindAfterHours());
        entity.setPolicyState(request.policyState());
        entity.setStoreAvailability(request.storeAvailability());
        entity.setEnforcementMode(request.enforcementMode());
        entity.setEmergencyDisabled(request.emergencyDisabled());

        if (request.storeAvailability() == StoreAvailability.FULLY_AVAILABLE
                && Boolean.TRUE.equals(request.availabilityConfirmed())) {
            entity.setAvailabilityVerifiedAt(OffsetDateTime.now());
            entity.setAvailabilityVerifiedBy(actor.getId());
        } else if (request.storeAvailability() != StoreAvailability.FULLY_AVAILABLE) {
            entity.setAvailabilityVerifiedAt(null);
            entity.setAvailabilityVerifiedBy(null);
        }

        MobileAppUpdatePolicyEntity saved = repository.saveAndFlush(entity);
        DashboardMobileAppUpdatePolicyResponse updated = toDashboardResponse(saved);
        auditService.record(auditAction(previous, updated), previous, updated, actor, request.changeReason());
        policyCache.evict(platform);
        return updated;
    }

    @Override
    @Transactional
    public DashboardMobileAppUpdatePolicyResponse setEmergencyDisabled(
            MobilePlatform platform, MobileAppUpdateEmergencyRequest request) {
        if (!StringUtils.hasText(request.changeReason())) {
            throw invalidConfiguration("changeReason is required for emergency changes");
        }
        MobileAppUpdatePolicyEntity entity = findForDashboard(platform);
        if (!entity.getVersion().equals(request.expectedVersion())) {
            throw new MobileUpdatePolicyException(HttpStatus.CONFLICT,
                    "UPDATE_POLICY_VERSION_CONFLICT",
                    "The update policy changed; reload it before saving");
        }
        if (!Boolean.TRUE.equals(request.disabled()) && entity.getPolicyState() == PolicyState.ACTIVE) {
            validateActivatedPolicy(entity);
        }

        DashboardUserEntity actor = currentUserService.getCurrentUserOrThrow();
        DashboardMobileAppUpdatePolicyResponse previous = toDashboardResponse(entity);
        entity.setEmergencyDisabled(request.disabled());
        DashboardMobileAppUpdatePolicyResponse updated =
                toDashboardResponse(repository.saveAndFlush(entity));
        PolicyAuditAction action = Boolean.TRUE.equals(request.disabled())
                ? PolicyAuditAction.DEACTIVATED : PolicyAuditAction.UPDATED;
        auditService.record(action, previous, updated, actor, request.changeReason());
        policyCache.evict(platform);
        return updated;
    }

    private MobileAppUpdatePolicyResponse toPublicResponse(
            MobileAppUpdatePolicyEntity policy, long currentBuild) {
        UpdateStatus status = currentBuild < policy.getMinimumSupportedBuild()
                ? UpdateStatus.REQUIRED
                : currentBuild < policy.getLatestBuild()
                ? UpdateStatus.OPTIONAL
                : UpdateStatus.CURRENT;
        return new MobileAppUpdatePolicyResponse(
                policy.getPlatform(), status, policy.getLatestVersion(),
                policy.getLatestBuild(), policy.getMinimumSupportedBuild(),
                policy.getTitle(), policy.getMessage(), policy.getReleaseNotes(),
                policy.getRemindAfterHours(), policy.getStoreUrl());
    }

    private MobileAppUpdatePolicyResponse current(MobilePlatform platform) {
        return new MobileAppUpdatePolicyResponse(
                platform, UpdateStatus.CURRENT, null, null, null,
                null, null, null, null, null);
    }

    private void validateConfiguration(
            MobilePlatform platform, DashboardMobileAppUpdatePolicyRequest request) {
        if (!StringUtils.hasText(request.changeReason())) {
            throw invalidConfiguration("changeReason is required for every policy change");
        }
        if (request.minimumSupportedBuild() > request.latestBuild()) {
            throw invalidConfiguration("minimumSupportedBuild cannot exceed latestBuild");
        }
        validateStoreUrl(platform, request.storeUrl());
        if (request.policyState() == PolicyState.ACTIVE) {
            if (request.latestBuild() == 0 || "0.0.0".equals(request.latestVersion().trim())) {
                throw invalidConfiguration("An active policy must identify a real non-zero release");
            }
            if (request.storeAvailability() != StoreAvailability.FULLY_AVAILABLE
                    || !Boolean.TRUE.equals(request.availabilityConfirmed())) {
                throw invalidConfiguration(
                        "An active policy requires explicit FULLY_AVAILABLE store confirmation");
            }
            if (request.enforcementMode() == EnforcementMode.OFF) {
                throw invalidConfiguration(
                        "An active policy cannot use OFF enforcement mode");
            }
        }
    }

    private void validateActivatedPolicy(MobileAppUpdatePolicyEntity policy) {
        boolean valid = policy.getLatestBuild() != null
                && policy.getLatestBuild() > 0
                && !"0.0.0".equals(policy.getLatestVersion())
                && policy.getStoreAvailability() == StoreAvailability.FULLY_AVAILABLE
                && policy.getAvailabilityVerifiedAt() != null
                && policy.getAvailabilityVerifiedBy() != null;
        if (!valid) {
            log.error("Active mobile update policy is invalid: platform={}", policy.getPlatform());
            metrics.recordUnavailablePolicy(policy.getPlatform(), "invalid_configuration");
            throw unavailable("UPDATE_POLICY_INVALID", "Update policy is temporarily unavailable");
        }
        try {
            validateStoreUrl(policy.getPlatform(), policy.getStoreUrl());
        } catch (MobileUpdatePolicyException ex) {
            log.error("Active mobile update policy has an invalid store URL: platform={}", policy.getPlatform());
            metrics.recordUnavailablePolicy(policy.getPlatform(), "invalid_store_url");
            throw unavailable("UPDATE_POLICY_INVALID", "Update policy is temporarily unavailable");
        }
    }

    private void validateStoreUrl(MobilePlatform platform, String rawUrl) {
        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (RuntimeException ex) {
            throw invalidConfiguration("storeUrl must be the verified platform store listing");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getUserInfo() != null || uri.getPort() != -1 || uri.getFragment() != null) {
            throw invalidConfiguration("storeUrl must be the verified platform store listing");
        }

        if (platform == MobilePlatform.ANDROID) {
            boolean packageMatches = Arrays.stream(
                            StringUtils.hasText(uri.getRawQuery()) ? uri.getRawQuery().split("&") : new String[0])
                    .map(value -> value.split("=", 2))
                    .anyMatch(pair -> pair.length == 2
                            && "id".equals(URLDecoder.decode(pair[0], StandardCharsets.UTF_8))
                            && ANDROID_PACKAGE_ID.equals(URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
            if (!"play.google.com".equalsIgnoreCase(uri.getHost())
                    || !"/store/apps/details".equals(uri.getPath()) || !packageMatches) {
                throw invalidConfiguration("Android storeUrl must target " + ANDROID_PACKAGE_ID + " on Google Play");
            }
            return;
        }

        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!"apps.apple.com".equalsIgnoreCase(uri.getHost())
                || !path.matches(".*/id" + IOS_APP_ID + "/?")) {
            throw invalidConfiguration("iOS storeUrl must target App Store ID " + IOS_APP_ID);
        }
    }

    private PolicyAuditAction auditAction(
            DashboardMobileAppUpdatePolicyResponse previous,
            DashboardMobileAppUpdatePolicyResponse updated) {
        if (previous.policyState() != PolicyState.ACTIVE && updated.policyState() == PolicyState.ACTIVE) {
            return PolicyAuditAction.ACTIVATED;
        }
        if ((previous.policyState() == PolicyState.ACTIVE && updated.policyState() != PolicyState.ACTIVE)
                || (!Boolean.TRUE.equals(previous.emergencyDisabled())
                && Boolean.TRUE.equals(updated.emergencyDisabled()))) {
            return PolicyAuditAction.DEACTIVATED;
        }
        if (updated.minimumSupportedBuild() > previous.minimumSupportedBuild()) {
            return PolicyAuditAction.MINIMUM_RAISED;
        }
        if (updated.minimumSupportedBuild() < previous.minimumSupportedBuild()) {
            return PolicyAuditAction.ROLLED_BACK;
        }
        return PolicyAuditAction.UPDATED;
    }

    private MobileAppUpdatePolicyEntity findForDashboard(MobilePlatform platform) {
        return repository.findById(platform).orElseThrow(() ->
                new MobileUpdatePolicyException(HttpStatus.NOT_FOUND,
                        "UPDATE_POLICY_NOT_FOUND", "Update policy not found for " + platform));
    }

    private DashboardMobileAppUpdatePolicyResponse toDashboardResponse(
            MobileAppUpdatePolicyEntity entity) {
        return new DashboardMobileAppUpdatePolicyResponse(
                entity.getPlatform(), entity.getLatestVersion(), entity.getLatestBuild(),
                entity.getMinimumSupportedBuild(), entity.getStoreUrl(), entity.getTitle(),
                entity.getMessage(), entity.getReleaseNotes(), entity.getRemindAfterHours(),
                entity.getPolicyState(), entity.getStoreAvailability(),
                entity.getAvailabilityVerifiedAt(), entity.getAvailabilityVerifiedBy(),
                entity.getEnforcementMode(), entity.getEmergencyDisabled(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private MobileUpdatePolicyException invalidRequest(String message) {
        return new MobileUpdatePolicyException(HttpStatus.BAD_REQUEST,
                "INVALID_UPDATE_POLICY_REQUEST", message);
    }

    private MobileUpdatePolicyException invalidConfiguration(String message) {
        return new MobileUpdatePolicyException(HttpStatus.BAD_REQUEST,
                "INVALID_UPDATE_POLICY_CONFIGURATION", message);
    }

    private MobileUpdatePolicyException unavailable(String code, String message) {
        return new MobileUpdatePolicyException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
}
