package com.brandPitara.sfs.dashboard.auth.security;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardUserDetailsSnapshotTest {

    @Test
    void principalContainsOnlyImmutableAuthenticationSnapshot() {
        DashboardUserDetails details = new DashboardUserDetails(new DashboardAuthenticationUserSnapshot(
                4L,
                "admin@example.com",
                "Admin",
                DashboardRole.ADMIN,
                true
        ));

        assertThat(details.getPassword()).isEmpty();
        assertThat(details.getId()).isEqualTo(4L);
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(details.getUser().getPasswordHash()).isNull();
    }
}
