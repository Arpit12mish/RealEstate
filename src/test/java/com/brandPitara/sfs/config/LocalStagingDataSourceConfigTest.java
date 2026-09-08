package com.brandPitara.sfs.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStagingDataSourceConfigTest {

    @Test
    void bindsPoolBeforeConstructionAndKeepsTheProductionMaximum() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
                .withUserConfiguration(LocalStagingDataSourceConfig.class)
                .withPropertyValues(
                        "spring.profiles.active=local-staging",
                        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/local_staging_config_test",
                        "spring.datasource.username=test",
                        "spring.datasource.password=test",
                        "spring.datasource.hikari.pool-name=LocalStagingConfigTestPool",
                        "spring.datasource.hikari.maximum-pool-size=10",
                        "spring.datasource.hikari.minimum-idle=2",
                        "spring.datasource.hikari.connection-timeout=5000",
                        "spring.datasource.hikari.initialization-fail-timeout=-1"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DataSource.class);
                    HikariDataSource dataSource = context.getBean(HikariDataSource.class);
                    assertThat(dataSource.getPoolName()).isEqualTo("LocalStagingConfigTestPool");
                    assertThat(dataSource.getMaximumPoolSize()).isEqualTo(10);
                    assertThat(dataSource.getMinimumIdle()).isEqualTo(2);
                    assertThat(dataSource.getConnectionTimeout()).isEqualTo(5000);
                });
    }
}
