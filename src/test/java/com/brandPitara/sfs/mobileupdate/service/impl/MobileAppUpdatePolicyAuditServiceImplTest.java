package com.brandPitara.sfs.mobileupdate.service.impl;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.mobileupdate.*;
import com.brandPitara.sfs.mobileupdate.dto.DashboardMobileAppUpdatePolicyResponse;
import com.brandPitara.sfs.mobileupdate.entity.MobileAppUpdatePolicyAuditEntity;
import com.brandPitara.sfs.mobileupdate.repository.MobileAppUpdatePolicyAuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MobileAppUpdatePolicyAuditServiceImplTest {

    @Test
    void recordsImmutableBeforeAfterActorAndReason() {
        MobileAppUpdatePolicyAuditRepository repository = mock(MobileAppUpdatePolicyAuditRepository.class);
        MobileAppUpdatePolicyAuditServiceImpl service =
                new MobileAppUpdatePolicyAuditServiceImpl(repository, new ObjectMapper().findAndRegisterModules());
        DashboardUserEntity actor = DashboardUserEntity.builder().id(9L).name("Release Admin").build();

        service.record(PolicyAuditAction.MINIMUM_RAISED, policy(18L), policy(21L), actor,
                "Security issue requires version 2.1.0");

        ArgumentCaptor<MobileAppUpdatePolicyAuditEntity> captor =
                ArgumentCaptor.forClass(MobileAppUpdatePolicyAuditEntity.class);
        verify(repository).save(captor.capture());
        MobileAppUpdatePolicyAuditEntity audit = captor.getValue();
        assertThat(audit.getPlatform()).isEqualTo(MobilePlatform.ANDROID);
        assertThat(audit.getAction()).isEqualTo(PolicyAuditAction.MINIMUM_RAISED);
        assertThat(audit.getPreviousPolicy().get("minimumSupportedBuild").asLong()).isEqualTo(18L);
        assertThat(audit.getNewPolicy().get("minimumSupportedBuild").asLong()).isEqualTo(21L);
        assertThat(audit.getDashboardUserId()).isEqualTo(9L);
        assertThat(audit.getDashboardUserName()).isEqualTo("Release Admin");
        assertThat(audit.getChangeReason()).isEqualTo("Security issue requires version 2.1.0");
    }

    private DashboardMobileAppUpdatePolicyResponse policy(Long minimum) {
        return new DashboardMobileAppUpdatePolicyResponse(
                MobilePlatform.ANDROID, "2.1.0", 21L, minimum,
                "https://play.google.com/store/apps/details?id=com.squarefootstory.app",
                "Update", "Please update", null, 24,
                PolicyState.ACTIVE, StoreAvailability.FULLY_AVAILABLE,
                java.time.OffsetDateTime.parse("2026-08-27T10:00:00Z"), 9L,
                EnforcementMode.PROMPT_ONLY, false, 1L, null, null);
    }
}
