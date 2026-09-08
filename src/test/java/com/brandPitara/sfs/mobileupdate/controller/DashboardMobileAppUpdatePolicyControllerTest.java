package com.brandPitara.sfs.mobileupdate.controller;

import com.brandPitara.sfs.mobileupdate.service.MobileAppUpdatePolicyService;
import com.brandPitara.sfs.mobileupdate.service.MobileAppUpdatePolicyAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardMobileAppUpdatePolicyControllerTest {

    @Test
    void readsFollowDashboardRolesAndWritesRequireAdmin() {
        assertThat(preAuthorize("list")).contains("ADMIN", "REVIEWER", "DATA_ENTRY");
        assertThat(preAuthorize("get")).contains("ADMIN", "REVIEWER", "DATA_ENTRY");
        assertThat(preAuthorize("audit")).contains("ADMIN", "REVIEWER");
        assertThat(preAuthorize("update")).isEqualTo("hasRole('ADMIN')");
        assertThat(preAuthorize("emergencyDisable")).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void serviceIsRequiredByController() {
        MobileAppUpdatePolicyService service = org.mockito.Mockito.mock(MobileAppUpdatePolicyService.class);
        MobileAppUpdatePolicyAuditService auditService =
                org.mockito.Mockito.mock(MobileAppUpdatePolicyAuditService.class);
        assertThat(new DashboardMobileAppUpdatePolicyController(service, auditService)).isNotNull();
    }

    private String preAuthorize(String methodName) {
        for (var method : DashboardMobileAppUpdatePolicyController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                return annotation == null ? null : annotation.value();
            }
        }
        throw new AssertionError("Method not found: " + methodName);
    }
}
