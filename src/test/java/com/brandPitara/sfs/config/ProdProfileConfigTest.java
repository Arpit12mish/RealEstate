package com.brandPitara.sfs.config;

import com.brandPitara.sfs.dashboard.auth.service.DashboardJwtService;
import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.brandPitara.sfs.util.JwtTokenUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the production-profile config changes made to make
 * `SPRING_PROFILES_ACTIVE=prod` safe to enable actually work, without a real
 * database, Docker, or any external network call:
 *
 *  1. application-prod.yml's required keys can be satisfied entirely by env
 *     vars (no baked-in secret defaults) - verified per feature area below.
 *  2. Elasticsearch stays functionally disabled (sfs.search.enabled=false)
 *     while its config beans still construct successfully - the inert
 *     localhost placeholder never causes a real network call at
 *     construction time (RestClient/ElasticsearchClient just parse the URL).
 *  3. Meta/Instagram config binds correctly, including when sync is enabled.
 *  4. Dashboard seed defaults to disabled without needing to touch the
 *     seeder bean itself (which requires a real DashboardUserRepository).
 *  5. JwtTokenUtil/DashboardJwtService (which now fail fast via
 *     @PostConstruct on a blank secret) construct successfully given
 *     non-blank fake env values.
 *
 * Hibernate SQL/bind/EntityPrinter suppression in prod is already covered by
 * ProductionLoggingConfigTest - not duplicated here.
 */
class ProdProfileConfigTest {

    // ── 1 & 4: pure property-binding checks via application-prod.yml itself,
    //    the same technique ProductionLoggingConfigTest already established -
    //    no Spring context needed at all. ───────────────────────────────────

    @Nested
    class YamlDefaults {

        private org.springframework.core.env.PropertySource<?> loadProdConfig() throws IOException {
            var loader = new org.springframework.boot.env.YamlPropertySourceLoader();
            return loader.load("application-prod", new ClassPathResource("application-prod.yml")).get(0);
        }

        private String readClasspathResource(String name) throws IOException {
            return Files.readString(new ClassPathResource(name).getFile().toPath(), StandardCharsets.UTF_8);
        }

        @Test
        void dashboardSeedDefaultsToDisabledWhenEnvVarsAreNotSet() throws IOException {
            var config = loadProdConfig();

            assertThat(config.getProperty("dashboard.seed.enabled")).isEqualTo("${DASHBOARD_SEED_ENABLED:false}");
            assertThat(config.getProperty("dashboard.seed.update-passwords"))
                    .isEqualTo("${DASHBOARD_SEED_UPDATE_PASSWORDS:false}");
            // The literal default embedded in the placeholder is what resolves
            // when the env var is absent - assert it says "false", not "true".
            assertThat(config.getProperty("dashboard.seed.enabled").toString()).contains(":false}");
            assertThat(config.getProperty("dashboard.seed.update-passwords").toString()).contains(":false}");
        }

        @Test
        void elasticsearchHasNoRealSecretDefaultAndSearchStaysDisabledByDefault() throws IOException {
            var config = loadProdConfig();

            // Inert but syntactically valid - never a real Elastic Cloud endpoint.
            assertThat(config.getProperty("elasticsearch.url").toString()).contains("localhost:9200");
            // No API key baked in.
            assertThat(config.getProperty("elasticsearch.api-key").toString()).isEqualTo("${ELASTICSEARCH_API_KEY:}");
            assertThat(config.getProperty("sfs.search.enabled").toString()).contains(":false}");
        }

        @Test
        void datasourceAndTwilioAndJwtSecretsHaveNoBakedInDefaults() throws IOException {
            var config = loadProdConfig();

            // These must be pure ${VAR} with no ":" default - a missing env var
            // should fail startup loudly (datasource/JWT/Twilio), not silently
            // fall back to some baked-in value.
            assertThat(config.getProperty("spring.datasource.password").toString()).isEqualTo("${SPRING_DATASOURCE_PASSWORD}");
            assertThat(config.getProperty("jwt.secret").toString()).isEqualTo("${JWT_SECRET}");
            assertThat(config.getProperty("dashboard.jwt.secret").toString()).isEqualTo("${DASHBOARD_JWT_SECRET}");
            assertThat(config.getProperty("twilio.authToken").toString()).isEqualTo("${TWILIO_AUTH_TOKEN}");
        }

        @Test
        void metaInstagramConfigHasNoBakedInSecretsAndDefaultsSyncToDisabled() throws IOException {
            var config = loadProdConfig();

            // Plain nested keys (no literal dots in "access-token" etc.), unlike
            // the bracket-escaped "[org.hibernate.SQL]"-style logger keys - no
            // brackets needed here, just normal dot-joined segments.
            assertThat(config.getProperty("sfs.instagram.meta.access-token").toString()).isEqualTo("${META_ACCESS_TOKEN:}");
            assertThat(config.getProperty("sfs.instagram.meta.app-secret").toString()).isEqualTo("${META_APP_SECRET:}");
            assertThat(config.getProperty("sfs.instagram.meta.sync-enabled").toString()).contains(":false}");
        }

        @Test
        void awsCredentialsAreBlankByDefaultPreferringEc2IamRole() throws IOException {
            var config = loadProdConfig();

            assertThat(config.getProperty("aws.credentials.access-key").toString()).isEqualTo("${AWS_ACCESS_KEY_ID:}");
            assertThat(config.getProperty("aws.credentials.secret-key").toString()).isEqualTo("${AWS_SECRET_ACCESS_KEY:}");
        }

        @Test
        void prodProfileDeclaresTheRateLimitConfigImport() throws IOException {
            assertThat(readClasspathResource("application-prod.yml")).contains("application-rate-limit.yml");
        }
    }

    // ── 3: InstagramMetaProperties binds correctly, including sync-enabled ──

    @Nested
    @SpringBootTest(
            classes = MetaConfigBinding.TestApplication.class,
            properties = {
                    "spring.config.import=classpath:application-prod.yml",
                    "META_INSTAGRAM_SYNC_ENABLED=true",
                    "META_ACCESS_TOKEN=fake-test-access-token-not-real",
                    "META_APP_ID=fake-test-app-id",
                    "META_APP_SECRET=fake-test-app-secret-not-real",
                    "META_FACEBOOK_PAGE_ID=1234567890",
                    "META_INSTAGRAM_BUSINESS_ACCOUNT_ID=9876543210"
            }
    )
    class MetaConfigBinding {

        @Autowired
        private InstagramMetaProperties metaProperties;

        @Test
        void metaConfigBindsCorrectlyWhenSyncIsEnabled() {
            assertThat(metaProperties.isSyncEnabled()).isTrue();
            assertThat(metaProperties.getAccessToken()).isEqualTo("fake-test-access-token-not-real");
            assertThat(metaProperties.getInstagramBusinessAccountId()).isEqualTo("9876543210");
            assertThat(metaProperties.hasRequiredSyncConfig()).isTrue();
        }

        @SpringBootConfiguration
        @EnableConfigurationProperties(InstagramMetaProperties.class)
        static class TestApplication {
        }
    }

    // ── 2: Elasticsearch config beans construct with the safe inert default,
    //    no real cluster contacted (RestClient/ElasticsearchClient only parse
    //    the URL and build a client object at construction time). ───────────

    @Nested
    @SpringBootTest(
            classes = ElasticsearchDisabled.TestApplication.class,
            properties = {
                    "spring.config.import=classpath:application-prod.yml"
            }
    )
    @ActiveProfiles("prod")
    class ElasticsearchDisabled {

        @Autowired
        private RestClient restClient;

        @Autowired
        private ElasticsearchClient elasticsearchClient;

        @Test
        void elasticsearchBeansConstructWithoutContactingARealCluster() {
            // Construction succeeding at all (no exception during context
            // startup) is the actual assertion - HttpHost.create() only
            // parses the inert localhost:9200 default, it never connects.
            assertThat(restClient).isNotNull();
            assertThat(elasticsearchClient).isNotNull();
        }

        @SpringBootConfiguration
        @org.springframework.context.annotation.Import(com.brandPitara.sfs.config.ElasticsearchConfig.class)
        static class TestApplication {
        }
    }

    // ── 5: JwtTokenUtil / DashboardJwtService construct successfully given
    //    non-blank fake secrets (proves the new @PostConstruct fail-fast
    //    checks pass on valid config, not just that they throw on blank). ───

    @Nested
    @SpringBootTest(
            classes = JwtConfigBinding.TestApplication.class,
            properties = {
                    "spring.config.import=classpath:application-prod.yml",
                    "JWT_SECRET=fake-test-jwt-secret-not-real-0123456789",
                    "JWT_EXPIRATION_MS=900000",
                    "DASHBOARD_JWT_SECRET=fake-test-dashboard-secret-not-real-0123456789",
                    "DASHBOARD_JWT_ACCESS_EXPIRATION_MS=900000"
            }
    )
    class JwtConfigBinding {

        @Autowired
        private JwtTokenUtil jwtTokenUtil;

        @Autowired
        private DashboardJwtService dashboardJwtService;

        @Test
        void jwtAndDashboardJwtBeansConstructSuccessfullyWithFakeEnvValues() {
            assertThat(jwtTokenUtil).isNotNull();
            assertThat(dashboardJwtService).isNotNull();
            assertThat(dashboardJwtService.getAccessExpirationMs()).isEqualTo(900000L);
        }

        @Test
        void jwtTokenUtilActuallyGeneratesAndParsesATokenWithTheFakeSecret() {
            var userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("+919876500000")
                    .password("encoded")
                    .roles("CUSTOMER")
                    .build();

            String token = jwtTokenUtil.generateToken(userDetails, 1L, "+919876500000", "CUSTOMER");

            assertThat(jwtTokenUtil.getUsernameFromToken(token)).isEqualTo("+919876500000");
        }

        @SpringBootConfiguration
        static class TestApplication {
            @Bean
            JwtTokenUtil jwtTokenUtil() {
                return new JwtTokenUtil();
            }

            @Bean
            DashboardJwtService dashboardJwtService() {
                return new DashboardJwtService();
            }
        }
    }

    // ── Regression guard: a blank secret must still fail fast, not silently
    //    pass through to first-token-use. ────────────────────────────────────

    @Nested
    class FailFastOnBlankSecret {

        @Test
        void jwtTokenUtilThrowsAtConstructionWhenSecretIsBlank() {
            JwtTokenUtil util = new JwtTokenUtil();
            org.springframework.test.util.ReflectionTestUtils.setField(util, "secret", "");

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> invokePostConstruct(util, "validateConfig"))
                    .hasCauseInstanceOf(IllegalStateException.class);
        }

        @Test
        void dashboardJwtServiceThrowsAtConstructionWhenSecretIsBlank() {
            DashboardJwtService service = new DashboardJwtService();
            org.springframework.test.util.ReflectionTestUtils.setField(service, "dashboardJwtSecret", null);

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> invokePostConstruct(service, "validateConfig"))
                    .hasCauseInstanceOf(IllegalStateException.class);
        }

        private void invokePostConstruct(Object target, String methodName) throws Exception {
            var method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        }
    }
}
