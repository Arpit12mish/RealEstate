package com.brandPitara.sfs.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayRepairStructureTest {

    @Test
    void baselineAndForwardRepairExistWithoutSchemaRepairCallback() throws Exception {
        assertThat(new ClassPathResource("db/migration/B134__sfs_schema_baseline.sql").exists()).isTrue();
        assertThat(new ClassPathResource("db/migration/V140__repair_historical_schema_drift.sql").exists()).isTrue();
        assertThat(new ClassPathResource("db/migration/V141__restore_canonical_baseline_reference_data.sql").exists()).isTrue();
        assertThat(new ClassPathResource("db/local-staging/beforeEachMigrate.sql").exists()).isFalse();
    }

    @Test
    void committedForwardRepairHasNoLateLowerVersionCollision() throws Exception {
        Path migrations = new ClassPathResource("db/migration").getFile().toPath();
        try (var files = Files.list(migrations)) {
            assertThat(files.map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("V13[5-9]__.*\\.sql"))
                    .toList())
                    .as("unreleased migrations must be sequenced after immutable V140")
                    .isEmpty();
        }
    }

    @Test
    void localStagingLoadsOnlyNormalMigrationLocation() throws Exception {
        Path config = new ClassPathResource("application-local-staging.yml").getFile().toPath();
        String yaml = Files.readString(config, StandardCharsets.UTF_8);

        assertThat(yaml).contains("locations: classpath:db/migration");
        assertThat(yaml).doesNotContain("classpath:db/local-staging");
        assertThat(yaml).contains("ddl-auto: validate");
        assertThat(yaml).contains("open-in-view: false");
    }

    @Test
    void localStagingSmokeChecksActualDockerPortBindings() throws Exception {
        Path smoke = Path.of("infra/local-staging/smoke.sh");
        String script = Files.readString(smoke, StandardCharsets.UTF_8);

        assertThat(script).contains(".HostConfig.PortBindings");
        assertThat(script).doesNotContain("compose port backend");
        assertThat(script).doesNotContain("compose port postgres");
    }
}
