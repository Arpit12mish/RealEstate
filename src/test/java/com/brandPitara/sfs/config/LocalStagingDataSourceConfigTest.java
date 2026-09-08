package com.brandPitara.sfs.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStagingDataSourceConfigTest {

    @Test
    void bindsPoolBeforeConstructionAndKeepsTheProductionMaximum() {
        dataSourceContext("local-staging", "LocalStagingConfigTestPool")
                .run(context -> assertSafeDataSource(context, "LocalStagingConfigTestPool"));
    }

    @Test
    void prodProfileBindsAllHikariSettingsBeforeStartingItsSinglePool() {
        dataSourceContext("prod", "SfsHikariPool")
                .run(context -> assertSafeDataSource(context, "SfsHikariPool"));
    }

    @Test
    void unrelatedProfileDoesNotActivateTheCustomDataSourceConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(LocalStagingDataSourceConfig.class)
                .withPropertyValues("spring.profiles.active=test")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(DataSource.class);
                    assertThat(context).doesNotHaveBean(HikariConfig.class);
                });
    }

    private ApplicationContextRunner dataSourceContext(String profile, String poolName) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
                .withUserConfiguration(LocalStagingDataSourceConfig.class)
                .withPropertyValues(
                        "spring.profiles.active=" + profile,
                        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/datasource_config_test",
                        "spring.datasource.username=test",
                        "spring.datasource.password=test",
                        "spring.datasource.hikari.pool-name=" + poolName,
                        "spring.datasource.hikari.maximum-pool-size=10",
                        "spring.datasource.hikari.minimum-idle=2",
                        "spring.datasource.hikari.connection-timeout=5000",
                        "spring.datasource.hikari.initialization-fail-timeout=-1"
                );
    }

    private void assertSafeDataSource(
            org.springframework.boot.test.context.assertj.AssertableApplicationContext context,
            String poolName
    ) {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(DataSource.class);
        assertThat(context).hasSingleBean(HikariDataSource.class);
        assertThat(context.getBeanNamesForType(DataSource.class)).containsExactly("dataSource");

        HikariConfig config = context.getBean("localStagingHikariConfig", HikariConfig.class);
        assertThat(config).isNotInstanceOf(HikariDataSource.class);

        HikariDataSource dataSource = context.getBean(HikariDataSource.class);
        assertThat(dataSource.getPoolName()).isEqualTo(poolName);
        assertThat(dataSource.getMaximumPoolSize()).isEqualTo(10);
        assertThat(dataSource.getMinimumIdle()).isEqualTo(2);
        assertThat(dataSource.getConnectionTimeout()).isEqualTo(5000);
    }
}
