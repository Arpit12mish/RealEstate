package com.brandPitara.sfs.dashboard.user.entity;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for DashboardUserEntity's toString() allow-list - this is
 * the entity the "entity dumps included passwordHash" production issue was
 * traced to (via Hibernate's own EntityPrinter, not this toString(), but the
 * same allow-list principle closes the gap here too for any code path that
 * does call toString() directly).
 */
class DashboardUserEntityTest {

    @Test
    void toStringNeverIncludesPasswordHashEmailOrName() {
        DashboardUserEntity user = DashboardUserEntity.builder()
                .id(1L)
                .name("SFS Admin")
                .email("admin@example.com")
                .passwordHash("$2a$10$somebcrypthashvalue")
                .role(DashboardRole.ADMIN)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        String toString = user.toString();

        assertThat(toString).doesNotContain("$2a$10$somebcrypthashvalue");
        assertThat(toString).doesNotContain("admin@example.com");
        assertThat(toString).doesNotContain("SFS Admin");
    }

    @Test
    void toStringIncludesOnlyTheSafeOperationalFields() {
        DashboardUserEntity user = DashboardUserEntity.builder()
                .id(7L)
                .role(DashboardRole.REVIEWER)
                .active(true)
                .build();

        String toString = user.toString();

        assertThat(toString).contains("id=7");
        assertThat(toString).contains("role=REVIEWER");
        assertThat(toString).contains("active=true");
    }
}
