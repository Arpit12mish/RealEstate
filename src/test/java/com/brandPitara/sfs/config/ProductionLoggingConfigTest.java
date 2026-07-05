package com.brandPitara.sfs.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves application-prod.yml's logging.level block is production-safe, by
 * binding the tracked file through Spring Boot's own YAML property loader
 * (not string-matching, which could false-positive on a comment mentioning
 * "DEBUG"). Deliberately does not touch application.yml - that file is
 * gitignored and does not exist on a fresh checkout/CI, so it can never be a
 * dependable classpath resource for an automated test. Its logging.level
 * block was the actual production leak (see the comment left in that file);
 * the fix is that it no longer sets logging.level at all, which is exactly
 * why application-prod.yml must be the self-contained source of truth these
 * tests verify.
 */
class ProductionLoggingConfigTest {

    private PropertySource<?> loadProdConfig() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources =
                loader.load("application-prod", new ClassPathResource("application-prod.yml"));
        return sources.get(0);
    }

    /**
     * Reads a logging.level entry. Note the key format: YamlPropertySourceLoader
     * flattens {@code "[org.hibernate.SQL]": WARN} to the property name
     * {@code logging.level[org.hibernate.SQL]} - no dot between "level" and
     * the bracket - which is easy to get wrong (this helper centralizes it).
     */
    private String level(PropertySource<?> config, String logger) {
        Object value = config.getProperty("logging.level" + logger);
        return value == null ? null : value.toString();
    }

    @Test
    void rootLoggingLevelIsInfoNotDebug() throws IOException {
        // "root" is a plain (unbracketed) key: logging.level.root - unlike the
        // bracketed logger names the level() helper handles.
        assertThat(loadProdConfig().getProperty("logging.level.root")).isEqualTo("INFO");
    }

    @Test
    void hibernateSqlAndBindParameterLoggingAreSuppressed() throws IOException {
        PropertySource<?> config = loadProdConfig();

        assertThat(level(config, "[org.hibernate]")).isEqualTo("WARN");
        assertThat(level(config, "[org.hibernate.SQL]")).isEqualTo("WARN");
        // OFF, not just WARN: JdbcBindingLogging (the real Hibernate 7 category
        // backing bind-parameter value logging) is TRACE-gated in Hibernate's
        // own source, so only OFF reliably guarantees a bound value (which can
        // be a password hash or other PII) can never be printed.
        assertThat(level(config, "[org.hibernate.orm.jdbc.bind]")).isEqualTo("OFF");
        assertThat(level(config, "[org.hibernate.type.descriptor.jdbc.BasicBinder]")).isEqualTo("OFF");
    }

    @Test
    void entityPrinterIsSuppressed() throws IOException {
        // org.hibernate.internal.util.EntityPrinter is Hibernate's own internal
        // entity-state dumper, independent of any entity's custom toString() -
        // confirmed (via the hibernate-core:7.1.8.Final jar) as the real source
        // of the "entity dumps included passwordHash" production issue.
        assertThat(level(loadProdConfig(), "[org.hibernate.internal.util.EntityPrinter]")).isEqualTo("OFF");
    }

    @Test
    void observabilityLoggersRemainAtInfoNotReducedToWarnOrOff() throws IOException {
        PropertySource<?> config = loadProdConfig();

        assertThat(level(config, "[com.brandPitara.sfs.observability.api]")).isEqualTo("INFO");
        assertThat(level(config, "[com.brandPitara.sfs.observability.security]")).isEqualTo("INFO");
        assertThat(level(config, "[com.brandPitara.sfs.observability.audit]")).isEqualTo("INFO");
        assertThat(level(config, "[com.brandPitara.sfs.ratelimit]")).isEqualTo("INFO");
        assertThat(level(config, "[com.brandPitara.sfs]")).isEqualTo("INFO");
    }

    @Test
    void springFrameworkNoiseIsSuppressedToWarn() throws IOException {
        PropertySource<?> config = loadProdConfig();

        assertThat(level(config, "[org.springframework]")).isEqualTo("WARN");
        assertThat(level(config, "[org.springframework.web]")).isEqualTo("WARN");
        assertThat(level(config, "[org.springframework.security]")).isEqualTo("WARN");
        assertThat(level(config, "[org.springframework.boot.autoconfigure]")).isEqualTo("WARN");
        assertThat(level(config, "[org.springframework.beans.factory]")).isEqualTo("WARN");
    }

    @Test
    void jdkHttpClientDebugLeakageGuardsAreInPlace() throws IOException {
        // GooglePlacesClient/GoogleNearbyPlaceProvider send X-Goog-Api-Key via
        // raw java.net.http.HttpClient - these are exactly the loggers that
        // would print full request headers (including that key) at DEBUG/FINE.
        PropertySource<?> config = loadProdConfig();

        assertThat(level(config, "[sun.net.www.protocol.http]")).isEqualTo("WARN");
        assertThat(level(config, "[sun.net.www.protocol.https]")).isEqualTo("WARN");
        assertThat(level(config, "[jdk.internal.net.http]")).isEqualTo("WARN");
        assertThat(level(config, "[jdk.internal.httpclient]")).isEqualTo("WARN");
        assertThat(level(config, "[jdk.event.security]")).isEqualTo("WARN");
    }

    @Test
    void hikariPoolDebugStatsAreSuppressedButInfoStartupLogsRemain() throws IOException {
        assertThat(level(loadProdConfig(), "[com.zaxxer.hikari]")).isEqualTo("INFO");
    }

    // ── Regression guard for logback-spring.xml (text-based, matching this
    //    repo's existing convention for asserting file content, e.g.
    //    RateLimitConfigLoadingTest's profile-import checks) ──────────────────

    @Test
    void logbackSpringXmlAlsoGuardsTheSameLoggersInTheProductionProfileBlock() throws IOException {
        String logback = readClasspathResource("logback-spring.xml");

        // Just the "!local" (production) springProfile block, not "local".
        int prodBlockStart = logback.indexOf("springProfile name=\"!local\"");
        int prodBlockEnd = logback.indexOf("</springProfile>", prodBlockStart);
        String prodBlock = logback.substring(prodBlockStart, prodBlockEnd);

        assertThat(prodBlock).contains("org.hibernate.internal.util.EntityPrinter").contains("level=\"OFF\"");
        assertThat(prodBlock).contains("org.hibernate.orm.jdbc.bind");
        assertThat(prodBlock).contains("org.hibernate.type.descriptor.jdbc.BasicBinder");
        assertThat(prodBlock).contains("com.zaxxer.hikari");
        assertThat(prodBlock).contains("jdk.internal.httpclient");
        assertThat(prodBlock).contains("org.springframework.boot.autoconfigure");
    }

    private String readClasspathResource(String name) throws IOException {
        ClassPathResource resource = new ClassPathResource(name);
        return Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
    }
}
