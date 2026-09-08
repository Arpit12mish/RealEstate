package com.brandPitara.sfs.cdn;

import com.brandPitara.sfs.cdn.config.CdnInvalidationConfiguration;
import com.brandPitara.sfs.cdn.config.CdnProperties;
import com.brandPitara.sfs.cdn.gateway.CloudFrontCdnInvalidationGateway;
import com.brandPitara.sfs.cdn.service.DisabledProjectPublicCacheEvictionService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CdnConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class, CdnInvalidationConfiguration.class,
                    DisabledProjectPublicCacheEvictionService.class)
            .withBean(com.brandPitara.sfs.cdn.service.ProjectPublicCacheEvictionMetrics.class)
            .withBean(io.micrometer.core.instrument.MeterRegistry.class,
                    io.micrometer.core.instrument.simple.SimpleMeterRegistry::new);

    @Test
    void disabledByDefaultNeedsNoDistributionOrCloudFrontBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DisabledProjectPublicCacheEvictionService.class);
            assertThat(context).doesNotHaveBean(CloudFrontCdnInvalidationGateway.class);
            assertThat(context).doesNotHaveBean(software.amazon.awssdk.services.cloudfront.CloudFrontClient.class);
            assertThat(context.getBean(CdnProperties.class).isEnabled()).isFalse();
        });
    }

    @Test
    void enabledConfigurationRequiresDistributionId() {
        CdnProperties properties = new CdnProperties();
        properties.setEnabled(true);
        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(properties))
                .isNotEmpty();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CdnProperties.class)
    static class PropertiesConfiguration {
    }
}
