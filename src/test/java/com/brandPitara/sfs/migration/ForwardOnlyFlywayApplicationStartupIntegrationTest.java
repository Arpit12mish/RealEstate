package com.brandPitara.sfs.migration;

import com.brandPitara.sfs.SfsApplication;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SfsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "spring.task.scheduling.enabled=false",
                "sfs.local-staging.fake-otp.enabled=true",
                "sfs.rate-limit.trusted-proxies[0]=127.0.0.1",
                "jwt.secret=release-5am-mobile-secret-release-5am-mobile-secret",
                "dashboard.jwt.secret=release-5am-dashboard-secret-release-5am-dashboard-secret",
                "dashboard.seed.enabled=false",
                "sfs.search.enabled=false",
                "google.maps.places.enabled=false",
                "sfs.instagram.meta.sync-enabled=false",
                "logging.level.org.springframework.web=INFO",
                "app.logging.path=target/test-logs"
        }
)
@ActiveProfiles({"test", "local-staging"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ForwardOnlyFlywayApplicationStartupIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_forward_startup")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired private DataSource dataSource;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private Flyway flyway;
    @Autowired private WebApplicationContext applicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    void configureMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void emptyPostgresMigratesValidatesHibernateAndStartsApplication() throws Exception {
        assertThat(entityManagerFactory.isOpen()).isTrue();
        flyway.validate();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT version, success
                     FROM flyway_schema_history
                     ORDER BY installed_rank DESC
                     LIMIT 1
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(Integer.parseInt(result.getString("version"))).isGreaterThanOrEqualTo(141);
            assertThat(result.getBoolean("success")).isTrue();
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT count(*)
                     FROM flyway_schema_history
                     WHERE version IN ('140', '141') AND success
                     """)) {
            assertThat(result.next()).isTrue();
            assertThat(result.getLong(1)).isEqualTo(2);
        }
    }

    @Test
    void freshBaselineSupportsCoreGuestAndPublicReadFlows() throws Exception {
        mockMvc.perform(post("/api/auth/guest/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "installationId": "release-5am2-fresh-smoke",
                                  "platform": "ANDROID",
                                  "appVersion": "5am2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestSessionId").isNumber())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.installationId").value("release-5am2-fresh-smoke"));

        mockMvc.perform(get("/api/public/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNumber())
                .andExpect(jsonPath("$.sections").isArray());
        mockMvc.perform(get("/api/public/home/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'architects')]").exists())
                .andExpect(jsonPath("$[?(@.slug == 'interior-designers')]").exists());
        mockMvc.perform(get("/api/public/cities/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/projects/feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mockMvc.perform(get("/api/public/search/suggest").param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections").isArray());
    }
}
