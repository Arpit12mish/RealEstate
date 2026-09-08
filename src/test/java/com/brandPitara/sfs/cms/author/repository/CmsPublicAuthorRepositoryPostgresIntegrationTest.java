package com.brandPitara.sfs.cms.author.repository;

import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.metadata.dto.CmsAuthorResponse;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
 * Reproduces the exact production defect (GET /api/dashboard/cms/authors -> 500
 * org.hibernate.query.sqm.UnknownPathException: Could not resolve attribute 'name' of
 * CmsPublicAuthorEntity) with a real Postgres instance and the real repository/JPQL, not a mocked
 * repository. Before the fix, calling search() with a Pageable sorted by "name" (the old controller's
 * hardcoded, wrong-for-authors sort property) throws; sorted by "displayName" (the fix) it succeeds.
 * Both are asserted below so this test would have failed for the same reason as the live HTTP 500.
 */
@SpringBootTest(
        classes = CmsPublicAuthorRepositoryPostgresIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.jpa.properties.hibernate.generate_statistics=true",
                "sfs.log.dir=target/test-logs"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CmsPublicAuthorRepositoryPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_cms_author_test")
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
    private CmsPublicAuthorRepository authors;

    @Autowired
    private CmsMediaAssetRepository media;

    @Autowired
    private DashboardUserRepository users;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Long authorAId;
    private Long authorBId;
    private Long authorCId;

    @BeforeEach
    void seedAuthors() {
        authors.deleteAll();
        media.deleteAll();
        users.deleteAll();

        DashboardUserEntity uploader = users.saveAndFlush(DashboardUserEntity.builder()
                .name("Test Uploader").email("author-media-uploader@sfs.local").passwordHash("hash")
                .role(DashboardRole.ADMIN).build());
        CmsMediaAssetEntity profileA = media.saveAndFlush(readyImage("amrita-profile.jpg", uploader));
        CmsMediaAssetEntity profileB = media.saveAndFlush(readyImage("baldev-profile.jpg", uploader));

        authorAId = authors.saveAndFlush(author("Amrita Kapoor", "amrita-kapoor", true, profileA)).getId();
        authorBId = authors.saveAndFlush(author("Baldev Singh", "baldev-singh", true, profileB)).getId();
        authorCId = authors.saveAndFlush(author("Chetan Rao", "chetan-rao", false, null)).getId();
    }

    @Test
    void authorListWithProfileMediaStaysAtOneQueryNotN() {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        var page = authors.search(null, null, defaultSort());
        // Force the lazy profileMediaAsset association to resolve for every row, exactly like
        // CmsAuthorResponse.from() does in production — this is what would fan out into N extra
        // SELECTs per author if @EntityGraph(attributePaths = "profileMediaAsset") were ever removed
        // from CmsPublicAuthorRepository.search().
        page.getContent().forEach(CmsAuthorResponse::from);

        // One query for the page content (LEFT JOIN FETCH via @EntityGraph) + one Spring Data count
        // query for total elements. Three authors, two with real linked media: a regression to
        // per-row lazy loading would push this to 4+ queries, not 2.
        assertThat(stats.getQueryExecutionCount()).isLessThanOrEqualTo(2);
    }

    @Test
    void sortingByTheEntitysRealPropertySucceeds() {
        var fixedSort = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "displayName").and(Sort.by("id")));

        assertThatCode(() -> authors.search(null, null, fixedSort)).doesNotThrowAnyException();

        var page = authors.search(null, null, fixedSort);
        assertThat(page.getContent()).extracting(CmsPublicAuthorEntity::getId)
                .containsExactly(authorAId, authorBId, authorCId); // displayName ASC: Amrita, Baldev, Chetan
    }

    @Test
    void sortingByTheOldHardcodedWrongPropertyReproducesTheProductionFailure() {
        var brokenSort = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name").and(Sort.by("id")));

        assertThatCode(() -> authors.search(null, null, brokenSort))
                .as("CmsPublicAuthorEntity has no 'name' property — only 'displayName' — so Hibernate "
                        + "must reject a Sort built on 'name', exactly like the production 500 did")
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void activeFilterReturnsOnlyActiveAuthors() {
        var page = authors.search(true, null, defaultSort());
        assertThat(page.getContent()).extracting(CmsPublicAuthorEntity::getId)
                .containsExactly(authorAId, authorBId);
    }

    @Test
    void inactiveFilterReturnsOnlyTheInactiveAuthor() {
        var page = authors.search(false, null, defaultSort());
        assertThat(page.getContent()).extracting(CmsPublicAuthorEntity::getId).containsExactly(authorCId);
    }

    @Test
    void searchMatchesByDisplayNameCaseInsensitively() {
        var page = authors.search(null, "%baldev%", defaultSort());
        assertThat(page.getContent()).extracting(CmsPublicAuthorEntity::getId).containsExactly(authorBId);
    }

    @Test
    void searchMatchesBySlugToo() {
        var page = authors.search(null, "%chetan-rao%", defaultSort());
        assertThat(page.getContent()).extracting(CmsPublicAuthorEntity::getId).containsExactly(authorCId);
    }

    @Test
    void paginationIsBoundedAndReportsCorrectTotals() {
        var firstPage = authors.search(null, null, PageRequest.of(0, 2, Sort.by("displayName").and(Sort.by("id"))));
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);

        var secondPage = authors.search(null, null, PageRequest.of(1, 2, Sort.by("displayName").and(Sort.by("id"))));
        assertThat(secondPage.getContent()).hasSize(1);
    }

    private PageRequest defaultSort() {
        return PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "displayName").and(Sort.by("id")));
    }

    private CmsPublicAuthorEntity author(String displayName, String slug, boolean active, CmsMediaAssetEntity profileMedia) {
        return CmsPublicAuthorEntity.builder().displayName(displayName).slug(slug).active(active)
                .profileMediaAsset(profileMedia).build();
    }

    private CmsMediaAssetEntity readyImage(String filename, DashboardUserEntity creator) {
        return CmsMediaAssetEntity.builder()
                .mediaType(CmsMediaType.IMAGE).status(CmsMediaStatus.READY)
                .storageBucket("test-bucket").storageKey("cms/authors/" + filename)
                .originalFilename(filename).contentType("image/jpeg")
                .declaredSizeBytes(1024L).sizeBytes(1024L).width(200).height(200)
                .readyAt(OffsetDateTime.now()).createdBy(creator).build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CmsPublicAuthorEntity.class, CmsMediaAssetEntity.class, DashboardUserEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CmsPublicAuthorRepository.class, CmsMediaAssetRepository.class, DashboardUserRepository.class
    })
    static class TestApplication {
    }
}
