package com.brandPitara.sfs.config;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class HikariStressProfileConfigTest {

    private static final String CONFIG_LOCATIONS = String.join(",",
            "classpath:/application-rate-limit.yml",
            "classpath:/application-hikari-stress.yml");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues("spring.config.location=" + CONFIG_LOCATIONS);

    @Test
    void stressProfileReplacesOnlyPublicProjectReadLimitsAndKeepsAuthenticationIdentity() {
        contextRunner.withPropertyValues("spring.profiles.active=hikari-stress")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RateLimitProperties properties = context.getBean(RateLimitProperties.class);
                    var projectRead = properties.getPolicies().get(RateLimitPolicy.PUBLIC_PROJECT_READ);
                    var homeRead = properties.getPolicies().get(RateLimitPolicy.PUBLIC_HOME_READ);

                    assertThat(projectRead.getLimits()).singleElement().satisfies(limit -> {
                        assertThat(limit.getKeyType()).isEqualTo(RateLimitKeyType.PRIMARY_IDENTITY);
                        assertThat(limit.getCapacity()).isEqualTo(60_000);
                        assertThat(limit.getRefillTokens()).isEqualTo(60_000);
                        assertThat(limit.getRefillPeriodSeconds()).isEqualTo(60);
                    });
                    assertThat(homeRead.getLimits()).singleElement()
                            .satisfies(limit -> assertThat(limit.getCapacity()).isEqualTo(120));
                    assertThat(properties.getAbuseCapacityMultiplier()).isEqualTo(10);

                    Environment environment = context.getEnvironment();
                    assertThat(environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class))
                            .isEqualTo(10);
                    assertThat(environment.getProperty("spring.datasource.hikari.minimum-idle", Integer.class))
                            .isEqualTo(2);
                    assertThat(environment.getProperty("spring.datasource.hikari.connection-timeout", Long.class))
                            .isEqualTo(5_000L);
                    assertThat(environment.getProperty("spring.datasource.hikari.leak-detection-threshold", Long.class))
                            .isEqualTo(5_000L);
                    assertThat(environment.getProperty("server.tomcat.mbeanregistry.enabled", Boolean.class)).isTrue();
                });
    }

    @Test
    void ordinaryLocalAndProdProfilesRetainNormalProjectLimitAndNoStressDiagnostics() {
        for (String profile : new String[]{"local", "prod"}) {
            contextRunner.withPropertyValues("spring.profiles.active=" + profile)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        RateLimitProperties properties = context.getBean(RateLimitProperties.class);
                        assertThat(properties.getPolicies().get(RateLimitPolicy.PUBLIC_PROJECT_READ).getLimits())
                                .singleElement()
                                .satisfies(limit -> {
                                    assertThat(limit.getKeyType()).isEqualTo(RateLimitKeyType.PRIMARY_IDENTITY);
                                    assertThat(limit.getCapacity()).isEqualTo(120);
                                });
                        assertThat(context.getEnvironment().getProperty(
                                "spring.datasource.hikari.leak-detection-threshold")).isNull();
                        assertThat(context.getEnvironment().getProperty(
                                "server.tomcat.mbeanregistry.enabled")).isNull();
                    });
        }
    }

    @Test
    void prodAndStressTogetherFailClosedToNormalLimits() {
        contextRunner.withPropertyValues("spring.profiles.active=prod,hikari-stress")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RateLimitProperties properties = context.getBean(RateLimitProperties.class);
                    assertThat(properties.getPolicies().get(RateLimitPolicy.PUBLIC_PROJECT_READ).getLimits())
                            .singleElement()
                            .satisfies(limit -> assertThat(limit.getCapacity()).isEqualTo(120));
                    assertThat(context.getEnvironment().getProperty(
                            "spring.datasource.hikari.leak-detection-threshold")).isNull();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RateLimitProperties.class)
    static class TestConfiguration {
    }
}
