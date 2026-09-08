package com.brandPitara.sfs.analytics.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Production-hardening pass (Part A): proves the sfs.analytics.ingestion.* contract
 * binds correctly with safe defaults, that invalid values fail fast rather than
 * silently starting with nonsense, and that production no longer depends on the
 * gitignored base application.yml for this contract - see application-prod.yml.
 */
class AnalyticsIngestionPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void defaultsBindWithNoPropertiesSet() {
        contextRunner.run(context -> {
            AnalyticsIngestionProperties properties = context.getBean(AnalyticsIngestionProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getQueueCapacity()).isEqualTo(10_000);
            assertThat(properties.getDrainBatchSize()).isEqualTo(300);
            assertThat(properties.getDrainIntervalMs()).isEqualTo(2_000);
            assertThat(properties.getMaxPropertiesJsonLength()).isEqualTo(2_000);
            assertThat(properties.getRawRetentionDays()).isEqualTo(120);
            assertThat(properties.getPartitionLookaheadMonths()).isEqualTo(3);
        });
    }

    @Test
    void explicitPropertiesOverrideDefaults() {
        contextRunner
                .withPropertyValues(
                        "sfs.analytics.ingestion.enabled=false",
                        "sfs.analytics.ingestion.queue-capacity=5000",
                        "sfs.analytics.ingestion.drain-batch-size=100",
                        "sfs.analytics.ingestion.drain-interval-ms=1000",
                        "sfs.analytics.ingestion.max-properties-json-length=1000",
                        "sfs.analytics.ingestion.raw-retention-days=90",
                        "sfs.analytics.ingestion.partition-lookahead-months=2"
                )
                .run(context -> {
                    AnalyticsIngestionProperties properties = context.getBean(AnalyticsIngestionProperties.class);
                    assertThat(properties.isEnabled()).isFalse();
                    assertThat(properties.getQueueCapacity()).isEqualTo(5_000);
                    assertThat(properties.getDrainBatchSize()).isEqualTo(100);
                    assertThat(properties.getDrainIntervalMs()).isEqualTo(1_000);
                    assertThat(properties.getMaxPropertiesJsonLength()).isEqualTo(1_000);
                    assertThat(properties.getRawRetentionDays()).isEqualTo(90);
                    assertThat(properties.getPartitionLookaheadMonths()).isEqualTo(2);
                });
    }

    @Test
    void queueCapacityBelowMinimumFailsValidation() {
        AnalyticsIngestionProperties properties = new AnalyticsIngestionProperties();
        properties.setQueueCapacity(50); // below @Min(100)

        Set<ConstraintViolation<AnalyticsIngestionProperties>> violations = validator.validate(properties);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void drainBatchSizeExceedingQueueCapacityFailsValidation() {
        AnalyticsIngestionProperties properties = new AnalyticsIngestionProperties();
        properties.setQueueCapacity(200);
        properties.setDrainBatchSize(500); // larger than the queue itself

        Set<ConstraintViolation<AnalyticsIngestionProperties>> violations = validator.validate(properties);

        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("drain-batch-size must not exceed queue-capacity"));
    }

    @Test
    void validConfigurationHasNoViolations() {
        AnalyticsIngestionProperties properties = new AnalyticsIngestionProperties();
        assertThat(validator.validate(properties)).isEmpty();
    }

    /**
     * Reads the tracked application-prod.yml directly off the classpath (not through
     * Spring's YAML loader) to prove the analytics contract is declared there - i.e.
     * that production no longer depends solely on the gitignored base application.yml
     * for this contract. A regression here (someone removing the block from
     * application-prod.yml) fails this test even though the properties class itself
     * would still bind fine via Java defaults.
     */
    @Test
    void productionProfileDeclaresTheAnalyticsContract() throws IOException {
        String prodYaml = readClasspathResource("application-prod.yml");

        assertThat(prodYaml)
                .contains("sfs:")
                .contains("analytics:")
                .contains("SFS_ANALYTICS_ENABLED")
                .contains("SFS_ANALYTICS_QUEUE_CAPACITY")
                .contains("SFS_ANALYTICS_DRAIN_BATCH_SIZE")
                .contains("SFS_ANALYTICS_DRAIN_INTERVAL_MS")
                .contains("SFS_ANALYTICS_AGGREGATION_SCHEDULE")
                .contains("SFS_ANALYTICS_PARTITION_MAINTENANCE_SCHEDULE");
    }

    @Test
    void localStagingProfileDeclaresTheAnalyticsContract() throws IOException {
        String yaml = readClasspathResource("application-local-staging.yml");

        assertThat(yaml)
                .contains("analytics:")
                .contains("SFS_ANALYTICS_ENABLED")
                .contains("SFS_ANALYTICS_QUEUE_CAPACITY");
    }

    private String readClasspathResource(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            assertThat(in).as("classpath resource " + name).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AnalyticsIngestionProperties.class)
    static class PropertiesConfiguration {
    }
}
