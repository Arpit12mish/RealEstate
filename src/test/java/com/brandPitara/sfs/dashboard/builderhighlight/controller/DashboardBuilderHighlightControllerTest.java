package com.brandPitara.sfs.dashboard.builderhighlight.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardBuilderHighlightControllerTest {

    @Test
    void dashboardEndpointsKeepExpectedRoleRules() throws Exception {
        assertThat(preAuthorize("create")).contains("hasAnyRole('ADMIN', 'DATA_ENTRY')");
        assertThat(preAuthorize("update")).contains("hasAnyRole('ADMIN', 'DATA_ENTRY')");
        assertThat(preAuthorize("list")).contains("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')");
        assertThat(preAuthorize("get")).contains("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')");
        assertThat(preAuthorize("delete")).contains("hasRole('ADMIN')");
        assertThat(preAuthorize("setPublished")).contains("hasAnyRole('ADMIN', 'REVIEWER')");
    }

    private String preAuthorize(String methodName) {
        for (var method : DashboardBuilderHighlightController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                return annotation != null ? annotation.value() : null;
            }
        }
        throw new AssertionError("Method not found: " + methodName);
    }
}
