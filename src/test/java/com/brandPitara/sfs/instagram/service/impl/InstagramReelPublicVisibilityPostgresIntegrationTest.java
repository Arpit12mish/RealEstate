package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.config.AppInstagramProperties;
import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.brandPitara.sfs.instagram.dto.PublicInstagramReelItemResponse;
import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.instagram.repository.InstagramReelRepository;
import com.brandPitara.sfs.instagram.service.InstagramReelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the public visibility invariant for /api/public/home INSTAGRAM_REELS: a reel deactivated
 * from the dashboard (active=false) must never appear in InstagramReelService#publicHomeItems, which is
 * what InstagramReelsSectionLoader calls for the home feed. Exercises the real repository query
 * (findByActiveTrueAndDeletedFalseOrderByPublishedAtDescIdDesc) against real Postgres, not a mock -
 * the existing InstagramReelServiceImplTest mocks the repository entirely and so can never catch a
 * broken/missing active predicate in the JPQL itself.
 */
@SpringBootTest(
        classes = InstagramReelPublicVisibilityPostgresIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "sfs.log.dir=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InstagramReelPublicVisibilityPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_instagram_reel_test")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private InstagramReelRepository repository;

    private InstagramReelServiceImpl service;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        AppInstagramProperties appInstagramProperties = new AppInstagramProperties();
        InstagramMetaProperties metaProperties = new InstagramMetaProperties();
        service = new InstagramReelServiceImpl(
                repository,
                new InstagramReelMapper(appInstagramProperties),
                metaProperties
        );
    }

    @Test
    void activeManualReelAppearsInPublicHome() {
        Long id = repository.save(reel("active-manual", true, false, 1)).getId();

        List<PublicInstagramReelItemResponse> items = service.publicHomeItems(10);

        assertThat(items).extracting(PublicInstagramReelItemResponse::getId).containsExactly(id);
    }

    @Test
    void inactiveReelNeverAppearsInPublicHome() {
        repository.save(reel("inactive-manual", false, false, 1));

        List<PublicInstagramReelItemResponse> items = service.publicHomeItems(10);

        assertThat(items).isEmpty();
    }

    @Test
    void mixedActiveAndInactiveReelsExcludeOnlyTheInactiveOne() {
        Long a = repository.save(reel("reel-a", true, false, 3)).getId();
        repository.save(reel("reel-b", false, false, 2));
        Long c = repository.save(reel("reel-c", true, false, 1)).getId();

        List<PublicInstagramReelItemResponse> items = service.publicHomeItems(10);

        // ORDER BY publishedAt DESC, id DESC - reel-a was published most recently (order index 3).
        assertThat(items).extracting(PublicInstagramReelItemResponse::getId).containsExactly(a, c);
    }

    @Test
    void dashboardDeactivationRemovesReelFromPublicHomeImmediately() {
        Long id = repository.save(reel("deactivate-me", true, false, 1)).getId();
        assertThat(service.publicHomeItems(10))
                .extracting(PublicInstagramReelItemResponse::getId)
                .containsExactly(id);

        service.setActive(id, false);

        assertThat(service.publicHomeItems(10)).isEmpty();
    }

    @Test
    void reactivatingAReelMakesItVisibleAgain() {
        Long id = repository.save(reel("toggle-me", false, false, 1)).getId();
        assertThat(service.publicHomeItems(10)).isEmpty();

        service.setActive(id, true);

        assertThat(service.publicHomeItems(10))
                .extracting(PublicInstagramReelItemResponse::getId)
                .containsExactly(id);
    }

    @Test
    void inactiveManuallyCreatedReelWithNullPublishedAtIsStillExcluded() {
        // Mirrors the production shape reported for manually-managed dashboard reels: no publishedAt
        // (never synced from Meta), zero engagement, deactivated from the dashboard.
        InstagramReelEntity unpublishedInactive = InstagramReelEntity.builder()
                .instagramMediaId(null)
                .title("Manual reel, never published, deactivated")
                .instagramUrl("https://www.instagram.com/reel/manual-null-published/")
                .thumbnailUrl("https://cdn.example.com/manual.webp")
                .cachedThumbnailUrl("https://cdn.example.com/manual.webp")
                .publishedAt(null)
                .active(false)
                .syncedFromMeta(false)
                .deleted(false)
                .build();
        repository.save(unpublishedInactive);

        assertThat(service.publicHomeItems(10)).isEmpty();
    }

    @Test
    void metaSyncedReelsShareTheSameActiveFlagSemanticsAsManualReels() {
        Long activeSynced = repository.save(reel("synced-active", true, true, 2)).getId();
        repository.save(reel("synced-inactive", false, true, 1));

        List<PublicInstagramReelItemResponse> items = service.publicHomeItems(10);

        assertThat(items).extracting(PublicInstagramReelItemResponse::getId).containsExactly(activeSynced);
    }

    /** publishOrder controls publishedAt spacing so ORDER BY publishedAt DESC is deterministic. */
    private InstagramReelEntity reel(String slug, boolean active, boolean syncedFromMeta, int publishOrder) {
        return InstagramReelEntity.builder()
                .instagramMediaId("media-" + slug)
                .title("Reel " + slug)
                .instagramUrl("https://www.instagram.com/reel/" + slug + "/")
                .thumbnailUrl("https://cdn.example.com/" + slug + ".webp")
                .cachedThumbnailUrl("https://cdn.example.com/" + slug + ".webp")
                .publishedAt(OffsetDateTime.now().minusMinutes(60L - publishOrder))
                .active(active)
                .syncedFromMeta(syncedFromMeta)
                .deleted(false)
                .build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = InstagramReelEntity.class)
    @EnableJpaRepositories(basePackageClasses = InstagramReelRepository.class)
    static class TestApplication {
    }
}
