package com.brandPitara.sfs.mobileupdate.service.impl;

import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.mobileupdate.*;
import com.brandPitara.sfs.mobileupdate.cache.MobileAppUpdatePolicyCache;
import com.brandPitara.sfs.mobileupdate.config.MobileUpdateProperties;
import com.brandPitara.sfs.mobileupdate.dto.DashboardMobileAppUpdatePolicyRequest;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdateEmergencyRequest;
import com.brandPitara.sfs.mobileupdate.entity.MobileAppUpdatePolicyEntity;
import com.brandPitara.sfs.mobileupdate.exception.MobileUpdatePolicyException;
import com.brandPitara.sfs.mobileupdate.metrics.MobileUpdatePolicyMetrics;
import com.brandPitara.sfs.mobileupdate.repository.MobileAppUpdatePolicyRepository;
import com.brandPitara.sfs.mobileupdate.service.MobileAppUpdatePolicyAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileAppUpdatePolicyServiceImplTest {

    @Mock private MobileAppUpdatePolicyRepository repository;
    @Mock private MobileAppUpdatePolicyCache policyCache;
    @Mock private MobileUpdatePolicyMetrics metrics;
    @Mock private DashboardCurrentUserService currentUserService;
    @Mock private MobileAppUpdatePolicyAuditService auditService;
    private MobileUpdateProperties properties;
    private MobileAppUpdatePolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new MobileUpdateProperties();
        service = new MobileAppUpdatePolicyServiceImpl(
                repository, policyCache, properties, metrics, currentUserService, auditService);
    }

    @Test
    void canonicalBuildRulesAndBoundariesWorkForBothPlatforms() {
        stubPublicPolicy(activePolicy(MobilePlatform.ANDROID, 21, 18));
        assertThat(service.evaluate(MobilePlatform.ANDROID, 21).status()).isEqualTo(UpdateStatus.CURRENT);
        assertThat(service.evaluate(MobilePlatform.ANDROID, 20).status()).isEqualTo(UpdateStatus.OPTIONAL);
        assertThat(service.evaluate(MobilePlatform.ANDROID, 18).status()).isEqualTo(UpdateStatus.OPTIONAL);
        assertThat(service.evaluate(MobilePlatform.ANDROID, 17).status()).isEqualTo(UpdateStatus.REQUIRED);

        stubPublicPolicy(activePolicy(MobilePlatform.IOS, 12, 12));
        assertThat(service.evaluate(MobilePlatform.IOS, 11).status()).isEqualTo(UpdateStatus.REQUIRED);
    }

    @Test
    void draftSuspendedAndEmergencyPoliciesReturnCurrent() {
        MobileAppUpdatePolicyEntity policy = activePolicy(MobilePlatform.ANDROID, 21, 18);
        policy.setPolicyState(PolicyState.DRAFT);
        stubPublicPolicy(policy);
        assertThat(service.evaluate(MobilePlatform.ANDROID, 1).status()).isEqualTo(UpdateStatus.CURRENT);

        policy.setPolicyState(PolicyState.SUSPENDED);
        assertThat(service.evaluate(MobilePlatform.ANDROID, 1).status()).isEqualTo(UpdateStatus.CURRENT);

        policy.setPolicyState(PolicyState.ACTIVE);
        policy.setEmergencyDisabled(true);
        assertThat(service.evaluate(MobilePlatform.ANDROID, 1).status()).isEqualTo(UpdateStatus.CURRENT);
    }

    @Test
    void globalEmergencyKillSwitchDoesNotReadTheDatabase() {
        properties.setEmergencyKillSwitchEnabled(true);
        assertThat(service.evaluate(MobilePlatform.ANDROID, 1).status()).isEqualTo(UpdateStatus.CURRENT);
        verifyNoInteractions(policyCache);
    }

    @Test
    void missingPolicyProducesControlledServiceUnavailable() {
        when(policyCache.find(MobilePlatform.IOS)).thenReturn(null);
        assertThatThrownBy(() -> service.evaluate(MobilePlatform.IOS, 1))
                .isInstanceOf(MobileUpdatePolicyException.class)
                .extracting("code").isEqualTo("UPDATE_POLICY_UNAVAILABLE");
        verify(metrics).recordUnavailablePolicy(MobilePlatform.IOS, "missing");
    }

    @Test
    void buildAheadOfPolicyRemainsCurrentAndRecordsMetric() {
        stubPublicPolicy(activePolicy(MobilePlatform.ANDROID, 21, 18));
        assertThat(service.evaluate(MobilePlatform.ANDROID, 99).status()).isEqualTo(UpdateStatus.CURRENT);
        verify(metrics).recordBuildAheadOfPolicy(MobilePlatform.ANDROID);
    }

    @Test
    void androidAndIosPoliciesRemainIsolated() {
        MobileAppUpdatePolicyEntity android = activePolicy(MobilePlatform.ANDROID, 21, 18);
        when(policyCache.find(MobilePlatform.ANDROID)).thenReturn(android);
        assertThat(service.evaluate(MobilePlatform.ANDROID, 20).status()).isEqualTo(UpdateStatus.OPTIONAL);
        verify(policyCache).find(MobilePlatform.ANDROID);
        verify(policyCache, never()).find(MobilePlatform.IOS);
    }

    @Test
    void invalidPlatformStoreListingsAreRejected() {
        assertInvalid(request(MobilePlatform.ANDROID, 21, 18, PolicyState.DRAFT,
                StoreAvailability.UNVERIFIED, false, EnforcementMode.OFF, false, 0,
                "https://example.com/store/apps/details?id=com.squarefootstory.app"));
        assertInvalid(request(MobilePlatform.IOS, 21, 18, PolicyState.DRAFT,
                StoreAvailability.UNVERIFIED, false, EnforcementMode.OFF, false, 0,
                "https://apps.apple.com/app/id9999999999"), MobilePlatform.IOS);
    }

    @Test
    void partialOrUnconfirmedStoreRolloutCannotBeActivated() {
        assertInvalid(request(MobilePlatform.ANDROID, 21, 18, PolicyState.ACTIVE,
                StoreAvailability.PARTIAL, true, EnforcementMode.PROMPT_ONLY, false, 0, androidUrl()));
        assertInvalid(request(MobilePlatform.ANDROID, 21, 18, PolicyState.ACTIVE,
                StoreAvailability.FULLY_AVAILABLE, false, EnforcementMode.PROMPT_ONLY, false, 0, androidUrl()));
    }

    @Test
    void activePolicyCannotUseZeroBuildOrBootstrapVersion() {
        assertInvalid(request(MobilePlatform.ANDROID, 0, 0, PolicyState.ACTIVE,
                StoreAvailability.FULLY_AVAILABLE, true, EnforcementMode.PROMPT_ONLY, false, 0, androidUrl()));
        DashboardMobileAppUpdatePolicyRequest zeroVersion = new DashboardMobileAppUpdatePolicyRequest(
                "0.0.0", 21L, 18L, androidUrl(), "Update", "Please update", null, 24,
                PolicyState.ACTIVE, StoreAvailability.FULLY_AVAILABLE, true,
                EnforcementMode.PROMPT_ONLY, false, "Verified release", 0L);
        assertInvalid(zeroVersion);
    }

    @Test
    void minimumGreaterThanLatestAndBlankReasonAreRejected() {
        assertInvalid(request(MobilePlatform.ANDROID, 20, 21, PolicyState.DRAFT,
                StoreAvailability.UNVERIFIED, false, EnforcementMode.OFF, false, 0, androidUrl()));
        DashboardMobileAppUpdatePolicyRequest blankReason = new DashboardMobileAppUpdatePolicyRequest(
                "2.1.0", 21L, 18L, androidUrl(), "Update", "Please update", null, 24,
                PolicyState.DRAFT, StoreAvailability.UNVERIFIED, false,
                EnforcementMode.OFF, false, " ", 0L);
        assertInvalid(blankReason);
    }

    @Test
    void staleAdministratorWriteProducesOptimisticConflict() {
        MobileAppUpdatePolicyEntity entity = draftPolicy(MobilePlatform.ANDROID, 21, 18);
        entity.setVersion(4L);
        when(repository.findById(MobilePlatform.ANDROID)).thenReturn(Optional.of(entity));
        assertThatThrownBy(() -> service.update(MobilePlatform.ANDROID,
                request(MobilePlatform.ANDROID, 21, 18, PolicyState.DRAFT,
                        StoreAvailability.UNVERIFIED, false, EnforcementMode.OFF, false, 3, androidUrl())))
                .isInstanceOf(MobileUpdatePolicyException.class)
                .extracting("code").isEqualTo("UPDATE_POLICY_VERSION_CONFLICT");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void successfulActivationRecordsVerifierAuditAndEvictsCache() {
        MobileAppUpdatePolicyEntity entity = draftPolicy(MobilePlatform.ANDROID, 20, 18);
        when(repository.findById(MobilePlatform.ANDROID)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DashboardUserEntity actor = DashboardUserEntity.builder().id(7L).name("Admin").active(true).build();
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(actor);

        var result = service.update(MobilePlatform.ANDROID,
                request(MobilePlatform.ANDROID, 21, 18, PolicyState.ACTIVE,
                        StoreAvailability.FULLY_AVAILABLE, true,
                        EnforcementMode.PROMPT_ONLY, false, 0, androidUrl()));

        assertThat(result.availabilityVerifiedBy()).isEqualTo(7L);
        assertThat(result.availabilityVerifiedAt()).isNotNull();
        verify(policyCache).evict(MobilePlatform.ANDROID);
        verify(auditService).record(eq(PolicyAuditAction.ACTIVATED), any(), any(), eq(actor),
                eq("Verified release"));
    }

    @Test
    void loweringRequiredMinimumIsAuditedAsRollback() {
        MobileAppUpdatePolicyEntity entity = activePolicy(MobilePlatform.ANDROID, 21, 21);
        when(repository.findById(MobilePlatform.ANDROID)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DashboardUserEntity actor = DashboardUserEntity.builder().id(7L).name("Admin").active(true).build();
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(actor);

        service.update(MobilePlatform.ANDROID,
                request(MobilePlatform.ANDROID, 21, 18, PolicyState.ACTIVE,
                        StoreAvailability.FULLY_AVAILABLE, true,
                        EnforcementMode.PROMPT_ONLY, false, 0, androidUrl()));

        verify(auditService).record(eq(PolicyAuditAction.ROLLED_BACK), any(), any(), eq(actor), anyString());
        verify(policyCache).evict(MobilePlatform.ANDROID);
    }

    @Test
    void emergencyDisableImmediatelyRemovesRequiredEvenWhenStoreUrlIsBad() {
        MobileAppUpdatePolicyEntity entity = activePolicy(MobilePlatform.ANDROID, 21, 21);
        entity.setStoreUrl("https://wrong.example/app");
        when(repository.findById(MobilePlatform.ANDROID)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(policyCache.find(MobilePlatform.ANDROID)).thenReturn(entity);
        DashboardUserEntity actor = DashboardUserEntity.builder().id(7L).name("Admin").active(true).build();
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(actor);

        var result = service.setEmergencyDisabled(MobilePlatform.ANDROID,
                new MobileAppUpdateEmergencyRequest(true, "Bad store URL", 0L));

        assertThat(result.emergencyDisabled()).isTrue();
        assertThat(service.evaluate(MobilePlatform.ANDROID, 1).status()).isEqualTo(UpdateStatus.CURRENT);
        verify(policyCache).evict(MobilePlatform.ANDROID);
        verify(auditService).record(eq(PolicyAuditAction.DEACTIVATED), any(), any(), eq(actor),
                eq("Bad store URL"));
    }

    private void assertInvalid(DashboardMobileAppUpdatePolicyRequest request) {
        assertInvalid(request, MobilePlatform.ANDROID);
    }

    private void assertInvalid(DashboardMobileAppUpdatePolicyRequest request, MobilePlatform platform) {
        assertThatThrownBy(() -> service.update(platform, request))
                .isInstanceOf(MobileUpdatePolicyException.class)
                .extracting("code").isEqualTo("INVALID_UPDATE_POLICY_CONFIGURATION");
    }

    private void stubPublicPolicy(MobileAppUpdatePolicyEntity policy) {
        when(policyCache.find(policy.getPlatform())).thenReturn(policy);
    }

    private MobileAppUpdatePolicyEntity activePolicy(MobilePlatform platform, long latest, long minimum) {
        MobileAppUpdatePolicyEntity entity = draftPolicy(platform, latest, minimum);
        entity.setPolicyState(PolicyState.ACTIVE);
        entity.setStoreAvailability(StoreAvailability.FULLY_AVAILABLE);
        entity.setAvailabilityVerifiedAt(OffsetDateTime.now());
        entity.setAvailabilityVerifiedBy(7L);
        entity.setEnforcementMode(EnforcementMode.PROMPT_ONLY);
        return entity;
    }

    private MobileAppUpdatePolicyEntity draftPolicy(MobilePlatform platform, long latest, long minimum) {
        return MobileAppUpdatePolicyEntity.builder()
                .platform(platform).latestVersion("2.1.0").latestBuild(latest)
                .minimumSupportedBuild(minimum)
                .storeUrl(platform == MobilePlatform.ANDROID ? androidUrl() : iosUrl())
                .title("Update available").message("Please update").remindAfterHours(24)
                .policyState(PolicyState.DRAFT).storeAvailability(StoreAvailability.UNVERIFIED)
                .enforcementMode(EnforcementMode.OFF).emergencyDisabled(false).version(0L).build();
    }

    private DashboardMobileAppUpdatePolicyRequest request(
            MobilePlatform platform, long latest, long minimum, PolicyState state,
            StoreAvailability availability, boolean confirmed, EnforcementMode mode,
            boolean emergencyDisabled, long version, String url) {
        return new DashboardMobileAppUpdatePolicyRequest(
                "2.1.0", latest, minimum, url, "Update available", "Please update", null, 24,
                state, availability, confirmed, mode, emergencyDisabled, "Verified release", version);
    }

    private String androidUrl() {
        return "https://play.google.com/store/apps/details?id=com.squarefootstory.app";
    }

    private String iosUrl() {
        return "https://apps.apple.com/app/id6764284866";
    }
}
