package com.brandPitara.sfs.mobileupdate;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class MobileAppUpdatePolicyMigrationSafetyTest {

    @Test
    void migrationSeedsBothPlatformsInactiveWithSafeConstraints() throws Exception {
        var resource = new ClassPathResource(
                "db/migration/V157__create_mobile_app_update_policy.sql");
        String sql = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("'ANDROID'")
                .contains("'IOS'")
                .contains("NULL, 24, false")
                .contains("minimum_supported_build <= latest_build")
                .contains("latest_build BETWEEN 0 AND 9007199254740991")
                .contains("minimum_supported_build BETWEEN 0 AND 9007199254740991")
                .contains("NOT active OR")
                .contains("ON CONFLICT (platform) DO NOTHING");
        assertThat(sql).doesNotContain("NULL, 24, true");
    }

    @Test
    void hardeningMigrationDefaultsToDraftOffAndCreatesAuditHistory() throws Exception {
        var resource = new ClassPathResource(
                "db/migration/V158__harden_mobile_app_update_policy.sql");
        String sql = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("DEFAULT 'DRAFT'")
                .contains("DEFAULT 'UNVERIFIED'")
                .contains("DEFAULT 'OFF'")
                .contains("emergency_disabled boolean NOT NULL DEFAULT false")
                .contains("store_availability = 'FULLY_AVAILABLE'")
                .contains("mobile_app_update_policy_audit")
                .contains("previous_policy jsonb NOT NULL")
                .contains("new_policy jsonb NOT NULL");
    }
}
