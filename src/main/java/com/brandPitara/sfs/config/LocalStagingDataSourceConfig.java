package com.brandPitara.sfs.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Constructs the local-staging pool only after all Hikari settings have been
 * bound. This avoids late mutation of a sealed Hikari 7 pool while retaining
 * Spring Boot's canonical datasource property names.
 */
@Configuration(proxyBeanMethods = false)
@Profile("local-staging")
public class LocalStagingDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    HikariConfig localStagingHikariConfig(DataSourceProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.determineUrl());
        config.setUsername(properties.determineUsername());
        config.setPassword(properties.determinePassword());
        if (properties.getDriverClassName() != null) {
            config.setDriverClassName(properties.determineDriverClassName());
        }
        return config;
    }

    @Bean
    DataSource dataSource(HikariConfig localStagingHikariConfig) {
        return new HikariDataSource(localStagingHikariConfig);
    }
}
