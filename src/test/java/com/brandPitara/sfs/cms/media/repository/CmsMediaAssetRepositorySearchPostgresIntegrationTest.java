package com.brandPitara.sfs.cms.media.repository;

import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Reproduces the exact production defect (GET /api/dashboard/cms/media -> 500 PostgreSQL "function
 * lower(bytea) does not exist") with a real Postgres instance. The `original_filename` column and
 * entity mapping were both always correct VARCHAR/String (verified directly against the live local
 * DB's information_schema during investigation) — the actual defect was JPQL `lower(concat('%',
 * :search, '%'))`: when :search binds as SQL NULL, PgJDBC cannot statically infer its type inside the
 * concat() call and falls back to the bytea OID, so lower() on the concat result has no overload. The
 * fix removes concat() from JPQL entirely (the pattern is now pre-built in Java, exactly like the
 * already-working author/category/tag search), so there is no longer an ambiguous null parameter type
 * for Postgres to guess at. The no-search case is the one that reproduces the original failure.
 */
@SpringBootTest(
        classes = CmsMediaAssetRepositorySearchPostgresIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "sfs.log.dir=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CmsMediaAssetRepositorySearchPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_cms_media_search_test")
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
    private CmsMediaAssetRepository media;

    @Autowired
    private DashboardUserRepository users;

    private Long clubhouseId;
    private Long interviewId;
    private Long floorPlanId;

    @BeforeEach
    void seedMedia() {
        media.deleteAll();
        users.deleteAll();
        DashboardUserEntity creator = users.saveAndFlush(DashboardUserEntity.builder()
                .name("Test Uploader").email("uploader@sfs.local").passwordHash("hash")
                .role(DashboardRole.ADMIN).build());

        clubhouseId = media.saveAndFlush(asset("clubhouse.jpg", CmsMediaType.IMAGE, CmsMediaStatus.READY, creator)).getId();
        interviewId = media.saveAndFlush(asset("Developer-Interview.mp4", CmsMediaType.VIDEO, CmsMediaStatus.PENDING_UPLOAD, creator)).getId();
        floorPlanId = media.saveAndFlush(asset("floor-plan.webp", CmsMediaType.IMAGE, CmsMediaStatus.READY, creator)).getId();
    }

    @Test
    void noSearchReproducesAndFixesTheProductionFailure() {
        // Before the fix this threw InvalidDataAccessResourceUsageException: function lower(bytea)
        // does not exist, because :search bound NULL inside a JPQL concat() call.
        assertThatCode(() -> media.findPage(null, null, null, null, defaultPage()))
                .doesNotThrowAnyException();

        var page = media.findPage(null, null, null, null, defaultPage());
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void searchLowercaseMatchesPartialFilename() {
        var page = media.findPage(null, null, null, pattern("club"), defaultPage());
        assertThat(page.getContent()).extracting(CmsMediaAssetEntity::getId).containsExactly(clubhouseId);
    }

    @Test
    void searchIsCaseInsensitive() {
        var page = media.findPage(null, null, null, pattern("INTERVIEW"), defaultPage());
        assertThat(page.getContent()).extracting(CmsMediaAssetEntity::getId).containsExactly(interviewId);
    }

    @Test
    void searchMatchingNothingReturnsEmptyPageNotAnError() {
        var page = media.findPage(null, null, null, pattern("nonexistent"), defaultPage());
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void mediaTypeAndStatusFiltersNarrowResults() {
        var readyImages = media.findPage(CmsMediaType.IMAGE, CmsMediaStatus.READY, null, null, defaultPage());
        assertThat(readyImages.getContent()).extracting(CmsMediaAssetEntity::getId)
                .containsExactlyInAnyOrder(clubhouseId, floorPlanId);

        var pending = media.findPage(null, CmsMediaStatus.PENDING_UPLOAD, null, null, defaultPage());
        assertThat(pending.getContent()).extracting(CmsMediaAssetEntity::getId).containsExactly(interviewId);
    }

    @Test
    void paginationIsBounded() {
        var firstPage = media.findPage(null, null, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"))));
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    private PageRequest defaultPage() {
        return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
    }

    /** Mirrors CmsMediaPersistenceService.pattern() so the test binds exactly what production binds. */
    private String pattern(String value) {
        return "%" + value.toLowerCase(java.util.Locale.ROOT) + "%";
    }

    private CmsMediaAssetEntity asset(String filename, CmsMediaType type, CmsMediaStatus status, DashboardUserEntity creator) {
        var builder = CmsMediaAssetEntity.builder()
                .mediaType(type).status(status)
                .storageBucket("test-bucket").storageKey("cms/test/" + filename)
                .originalFilename(filename).contentType(type == CmsMediaType.IMAGE ? "image/jpeg" : "video/mp4")
                .declaredSizeBytes(1024L).createdBy(creator);
        if (status == CmsMediaStatus.READY) {
            builder.sizeBytes(1024L).readyAt(OffsetDateTime.now());
            if (type == CmsMediaType.IMAGE) builder.width(100).height(100);
        }
        return builder.build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {CmsMediaAssetEntity.class, DashboardUserEntity.class})
    @EnableJpaRepositories(basePackageClasses = {CmsMediaAssetRepository.class, DashboardUserRepository.class})
    static class TestApplication {
    }
}
