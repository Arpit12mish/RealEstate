package com.brandPitara.sfs.migration.wordpress.importer;

import com.brandPitara.sfs.SfsApplication;
import com.brandPitara.sfs.cms.author.repository.CmsPublicAuthorRepository;
import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.taxonomy.repository.CmsContentCategoryRepository;
import com.brandPitara.sfs.cms.taxonomy.repository.CmsContentTagRepository;
import com.brandPitara.sfs.cms.workflow.repository.ContentPostRevisionRepository;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.migration.wordpress.audit.WordPressDataset;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressDumpReader;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergConversionResult;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergDocumentConverter;
import com.brandPitara.sfs.migration.wordpress.media.InMemoryWordPressMigrationMediaStorage;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaImportOutcome;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaImportRequest;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaImportService;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaMappingRepository;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMigrationMediaStorage;
import com.brandPitara.sfs.publiccontent.exception.PublicContentApiException;
import com.brandPitara.sfs.publiccontent.service.PublicContentService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Explicit opt-in, disposable end-to-end verification of the <strong>full</strong> WordPress
 * corpus (every {@code post_type='post'} row with status {@code publish} or {@code draft} - the
 * same filter {@code WordPressAuditRunner} uses) against the real production export. This is the
 * scale-up of {@link WordPressRepresentativeImportVerification} (10 posts) to the entire corpus,
 * run once before any real production write so genuinely new edge cases across all ~144 posts
 * surface here first, against disposable infrastructure, not against production.
 * <p>
 * Deliberately does not end in "Test"/"Tests"/"TestCase", so a normal {@code mvn test} run never
 * selects it and never fails when the source files are absent. Invoke explicitly:
 * <pre>
 *   mvn -q -o test -Dtest=WordPressFullCorpusImportVerification
 * </pre>
 * Still Testcontainers Postgres (disposable, torn down with the JVM) + the in-memory fake storage
 * - never real production PostgreSQL, S3, or CloudFront. Only the same source files already used
 * elsewhere in this migration are read, strictly read-only.
 */
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
                "jwt.secret=wp-full-corpus-test-mobile-secret-wp-full-corpus-secret",
                "dashboard.jwt.secret=wp-full-corpus-test-dashboard-secret-wp-full-corpus-secret",
                "dashboard.seed.enabled=false",
                "sfs.search.enabled=false",
                "google.maps.places.enabled=false",
                "sfs.instagram.meta.sync-enabled=false",
                "sfs.log.dir=target/test-logs",
                "app.cms.media.public-delivery.base-url=https://media.example.test",
                "app.cms.media.bucket=wp-full-corpus-test-bucket"
        }
)
@Import(WordPressFullCorpusImportVerification.FakeStorageConfig.class)
@ActiveProfiles({"test", "local-staging"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WordPressFullCorpusImportVerification {

    private static final String BUCKET = "wp-full-corpus-test-bucket";

    private static final Path DUMP_PATH = Path.of(System.getProperty(
            "sfs.migration.wordpress.dumpPath", "/Users/mac/Downloads/u427251222_Ue8rA.sql"
    ));
    private static final Path UPLOADS_ZIP_PATH = Path.of(System.getProperty(
            "sfs.migration.wordpress.uploadsZipPath", "/Users/mac/Downloads/uploads.zip"
    ));

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_wordpress_full_corpus")
            .withUsername("sfs_test")
            .withPassword("sfs_test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @TestConfiguration
    static class FakeStorageConfig {
        @Bean
        @Primary
        WordPressMigrationMediaStorage wordPressMigrationMediaStorage() {
            return new InMemoryWordPressMigrationMediaStorage();
        }
    }

    @Autowired
    private WordPressContentImportService importService;
    @Autowired
    private WordPressMediaImportService mediaImportService;
    @Autowired
    private WordPressMigrationMappingRepository mappingRepository;
    @Autowired
    private WordPressMediaMappingRepository mediaMappingRepository;
    @Autowired
    private ContentPostRepository contentPostRepository;
    @Autowired
    private ContentPostRevisionRepository revisionRepository;
    @Autowired
    private DashboardUserRepository dashboardUserRepository;
    @Autowired
    private CmsMediaAssetRepository mediaAssetRepository;
    @Autowired
    private CmsPublicAuthorRepository authorRepository;
    @Autowired
    private CmsContentCategoryRepository categoryRepository;
    @Autowired
    private CmsContentTagRepository tagRepository;
    @Autowired
    private PublicContentService publicContentService;
    @Autowired
    private com.brandPitara.sfs.cms.content.document.CmsMediaReferenceService mediaReferenceService;
    @Autowired
    private WordPressMigrationMediaStorage storage;

    private InMemoryWordPressMigrationMediaStorage fakeStorage;
    private DashboardUserEntity actor;

    @BeforeEach
    void setUp() {
        fakeStorage = (InMemoryWordPressMigrationMediaStorage) storage;
        actor = dashboardUserRepository.saveAndFlush(DashboardUserEntity.builder()
                .name("Full Corpus Migration Runner")
                .email("full-corpus-runner-" + UUID.randomUUID() + "@example.com")
                .passwordHash("encoded")
                .role(DashboardRole.CONTENT_STAFF)
                .active(true)
                .build());
    }

    @Test
    void importsTheFullCorpusEndToEndAgainstTheRealExport() throws Exception {
        Assumptions.assumeTrue(Files.exists(DUMP_PATH),
                "Real WordPress dump not found at " + DUMP_PATH + " - opt-in verification skipped.");
        Assumptions.assumeTrue(Files.exists(UPLOADS_ZIP_PATH),
                "uploads.zip not found at " + UPLOADS_ZIP_PATH + " - opt-in verification skipped.");

        long startedAt = System.currentTimeMillis();
        System.out.println("Reading real WordPress dump: " + DUMP_PATH);
        WordPressDataset.Builder builder = WordPressDataset.builder();
        new WordPressDumpReader().read(DUMP_PATH, builder);
        WordPressDataset dataset = builder.build();
        System.out.println("Dataset loaded: " + dataset.postsById().size() + " total rows.");

        // Same corpus filter as WordPressAuditRunner: post_type='post', status in (publish, draft).
        Map<Long, WordPressPostRow> posts = new LinkedHashMap<>();
        for (WordPressPostRow row : dataset.postsById().values()) {
            if ("post".equals(row.postType()) && ("publish".equals(row.postStatus()) || "draft".equals(row.postStatus()))) {
                posts.put(row.id(), row);
            }
        }
        System.out.println("Full corpus size: " + posts.size() + " posts (post_type=post, status in publish/draft).");

        Map<Long, GutenbergConversionResult> conversions = new LinkedHashMap<>();
        Map<Long, Exception> conversionFailures = new LinkedHashMap<>();
        for (var entry : posts.entrySet()) {
            WordPressPostRow post = entry.getValue();
            try {
                conversions.put(entry.getKey(), new GutenbergDocumentConverter()
                        .convert(post.id(), post.postTitle(), post.postContent()));
            } catch (Exception failure) {
                conversionFailures.put(entry.getKey(), failure);
            }
        }
        assertThat(conversionFailures)
                .as("no post should throw during Gutenberg conversion: %s", describeFailures(conversionFailures))
                .isEmpty();

        Map<Long, String> attachmentPaths = new LinkedHashMap<>();
        for (Long postId : posts.keySet()) {
            for (Long attachmentId : conversions.get(postId).referencedAttachmentIds()) {
                addAttachmentPathIfResolvable(dataset, attachmentId, attachmentPaths);
            }
            String thumbnailIdRaw = dataset.metaValue(postId, "_thumbnail_id");
            if (thumbnailIdRaw != null && !thumbnailIdRaw.isBlank()) {
                try {
                    addAttachmentPathIfResolvable(dataset, Long.parseLong(thumbnailIdRaw.trim()), attachmentPaths);
                } catch (NumberFormatException ignored) {
                    // handled per-post below via the resolvedMediaAssetIds gap, not fatal here
                }
            }
        }
        System.out.println("Distinct referenced attachment IDs across full corpus: " + attachmentPaths.size());

        Path tempMediaDir = Files.createTempDirectory("wp-full-corpus-media-");
        try {
            Map<Long, Long> resolvedMediaAssetIds = new LinkedHashMap<>();
            Map<Long, Exception> mediaFailures = new LinkedHashMap<>();
            for (var entry : attachmentPaths.entrySet()) {
                WordPressMediaImportRequest request = new WordPressMediaImportRequest(
                        entry.getKey(), UPLOADS_ZIP_PATH, entry.getValue(), tempMediaDir, actor
                );
                try {
                    WordPressMediaImportOutcome outcome = mediaImportService.importAttachment(request);
                    resolvedMediaAssetIds.put(entry.getKey(), outcome.cmsMediaAssetId());
                } catch (Exception failure) {
                    mediaFailures.put(entry.getKey(), failure);
                }
            }
            int uploadCallsFirstRun = fakeStorage.storeInvocationCount();
            int uniqueObjectsFirstRun = fakeStorage.objectCount();
            System.out.println("Media resolved: " + resolvedMediaAssetIds.size() + " of " + attachmentPaths.size()
                    + " ; failures: " + mediaFailures.size()
                    + " ; upload calls: " + uploadCallsFirstRun + " ; unique objects: " + uniqueObjectsFirstRun);
            if (!mediaFailures.isEmpty()) {
                System.out.println("Media failures (never fabricated - these posts' unresolved references will "
                        + "surface as content-import failures below): " + describeFailures(mediaFailures));
            }

            Map<Long, WordPressImportOutcome> outcomes = new LinkedHashMap<>();
            Map<Long, Exception> contentFailures = new LinkedHashMap<>();
            for (var entry : posts.entrySet()) {
                long postId = entry.getKey();
                WordPressImportRequest request = new WordPressImportRequest(
                        entry.getValue(), dataset, conversions.get(postId), resolvedMediaAssetIds, actor
                );
                try {
                    outcomes.put(postId, importService.importPost(request));
                } catch (Exception failure) {
                    contentFailures.put(postId, failure);
                }
            }

            printClassificationBreakdown(posts, outcomes, contentFailures);

            // Every content-import failure must trace back to a media resolution gap (never an
            // unexplained bug) - print full detail so any genuinely new failure is investigated.
            if (!contentFailures.isEmpty()) {
                System.out.println("========== Content Import Failures (investigate before production) ==========");
                for (var entry : contentFailures.entrySet()) {
                    boolean explainedByMediaGap = false;
                    for (Long attachmentId : conversions.get(entry.getKey()).referencedAttachmentIds()) {
                        if (mediaFailures.containsKey(attachmentId) || !attachmentPaths.containsKey(attachmentId)) {
                            explainedByMediaGap = true;
                            break;
                        }
                    }
                    System.out.println("post=" + entry.getKey() + " explainedByMediaGap=" + explainedByMediaGap
                            + " error=" + entry.getValue().getMessage());
                }
                System.out.println("================================================================");
            }
            assertThat(contentFailures)
                    .as("unexpected content import failures (not explained by a media gap): %s",
                            describeFailures(contentFailures))
                    .isEmpty();
            assertThat(outcomes).hasSize(posts.size());

            verifyNoAttachmentIdLeaksIntoAnyDocument(outcomes);

            int published = 0, draft = 0, needsReview = 0, blocked = 0;
            List<Long> blockedIds = new ArrayList<>();
            for (var entry : outcomes.entrySet()) {
                switch (entry.getValue().migrationState()) {
                    case PUBLISHED -> published++;
                    case DRAFT -> draft++;
                    case NEEDS_REVIEW -> needsReview++;
                    case BLOCKED -> { blocked++; blockedIds.add(entry.getKey()); }
                    default -> { }
                }
            }
            System.out.println("Blocked post IDs: " + blockedIds);

            Map<Long, Exception> publicApiFailures = new LinkedHashMap<>();
            int publicChecks = 0;
            for (var entry : outcomes.entrySet()) {
                WordPressImportOutcome outcome = entry.getValue();
                if (outcome.migrationState() == WordPressMigrationState.PUBLISHED) {
                    ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
                    assertThat(saved.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
                    assertThat(saved.getCurrentPublishedRevision()).isNotNull();
                    try {
                        var detail = publicContentService.getBySlug(saved.getSlug(), null).body();
                        assertThat(detail).as("published post %d must be publicly fetchable", entry.getKey()).isNotNull();
                        publicChecks++;
                    } catch (Exception publicApiFailure) {
                        publicApiFailures.put(entry.getKey(), publicApiFailure);
                        try {
                            mediaReferenceService.validateAndResolve(saved.getContentDocument());
                            System.out.println("post " + entry.getKey() + ": document-only validateAndResolve succeeded "
                                    + "(failure must be cover/author-profile media) coverMediaAssetId="
                                    + (saved.getCoverMediaAsset() == null ? null : saved.getCoverMediaAsset().getId()));
                        } catch (RuntimeException realCause) {
                            System.out.println("post " + entry.getKey() + ": real cause = " + realCause);
                        }
                    }
                } else if (outcome.targetContentId() != null) {
                    ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
                    assertThat(saved.getStatus()).isNotEqualTo(ContentStatus.PUBLISHED);
                    String slug = saved.getSlug();
                    assertThatThrownBy(() -> publicContentService.getBySlug(slug, null))
                            .as("non-published post %d must never be publicly fetchable", entry.getKey())
                            .isInstanceOf(PublicContentApiException.class);
                    publicChecks++;
                }
            }
            assertThat(publicApiFailures)
                    .as("published posts must be publicly fetchable: %s", describeFailures(publicApiFailures))
                    .isEmpty();
            System.out.println("Public API contract checked for " + publicChecks + " of " + outcomes.size() + " posts.");

            // Idempotent rerun - content.
            for (var entry : posts.entrySet()) {
                long postId = entry.getKey();
                WordPressImportRequest rerunRequest = new WordPressImportRequest(
                        entry.getValue(), dataset, conversions.get(postId), resolvedMediaAssetIds, actor
                );
                WordPressImportOutcome original = outcomes.get(postId);
                if (original == null) {
                    continue;
                }
                WordPressImportOutcome rerun = importService.importPost(rerunRequest);
                assertThat(rerun.skippedIdempotent()).as("rerun of post %d must be idempotent", postId).isTrue();
                assertThat(rerun.targetContentId()).isEqualTo(original.targetContentId());
                assertThat(rerun.migrationState()).isEqualTo(original.migrationState());
            }
            System.out.println("Idempotent content rerun verified for all " + outcomes.size() + " posts.");

            // Idempotent rerun - media.
            int reimported = 0;
            for (var entry : attachmentPaths.entrySet()) {
                if (!resolvedMediaAssetIds.containsKey(entry.getKey())) {
                    continue;
                }
                WordPressMediaImportRequest request = new WordPressMediaImportRequest(
                        entry.getKey(), UPLOADS_ZIP_PATH, entry.getValue(), tempMediaDir, actor
                );
                WordPressMediaImportOutcome rerun = mediaImportService.importAttachment(request);
                assertThat(rerun.cmsMediaAssetId()).isEqualTo(resolvedMediaAssetIds.get(entry.getKey()));
                reimported++;
            }
            int uploadCallsSecondRun = fakeStorage.storeInvocationCount() - uploadCallsFirstRun;
            assertThat(uploadCallsSecondRun).as("rerunning media import must perform zero duplicate uploads").isZero();
            System.out.println("Idempotent media rerun verified for " + reimported + " attachments - "
                    + uploadCallsSecondRun + " additional upload calls (0 expected).");

            System.out.println("========== Full Corpus Resource Counts ==========");
            System.out.println("posts processed: " + posts.size());
            System.out.println("content_post rows created: " + (outcomes.size() - blocked));
            System.out.println("content_post_revision rows created: " + published);
            System.out.println("outcome breakdown: PUBLISHED=" + published + " DRAFT=" + draft
                    + " NEEDS_REVIEW=" + needsReview + " BLOCKED=" + blocked);
            System.out.println("total CmsPublicAuthorEntity rows: " + authorRepository.count());
            System.out.println("total CmsContentCategoryEntity rows: " + categoryRepository.count());
            System.out.println("total CmsContentTagEntity rows: " + tagRepository.count());
            System.out.println("cms_media_asset rows created: " + mediaAssetRepository.count());
            System.out.println("wordpress_migration_mapping rows: " + mappingRepository.count());
            System.out.println("wordpress_migration_media_mapping rows: " + mediaMappingRepository.count());
            System.out.println("unique objects stored: " + fakeStorage.objectCount());
            System.out.println("deduplicated/reused attachment references: "
                    + (attachmentPaths.size() - fakeStorage.objectCount()) + " of " + attachmentPaths.size());
            System.out.println("elapsed: " + (System.currentTimeMillis() - startedAt) + " ms");
            System.out.println("===================================================");
        } finally {
            deleteRecursively(tempMediaDir);
        }
    }

    private void verifyNoAttachmentIdLeaksIntoAnyDocument(Map<Long, WordPressImportOutcome> outcomes) {
        for (var entry : outcomes.entrySet()) {
            if (entry.getValue().targetContentId() == null) {
                continue;
            }
            ContentPostEntity saved = contentPostRepository.findById(entry.getValue().targetContentId()).orElseThrow();
            for (var block : saved.getContentDocument().blocks()) {
                verifyBlockHasNoLeakedAttachmentId(block, entry.getKey());
            }
        }
    }

    private void verifyBlockHasNoLeakedAttachmentId(com.brandPitara.sfs.cms.content.document.ContentBlock block, long postId) {
        if (block instanceof com.brandPitara.sfs.cms.content.document.ContentBlock.Image image) {
            assertThat(mediaAssetRepository.findById(image.mediaAssetId()))
                    .as("post %d image block must reference a real CmsMediaAssetEntity, never a WP attachment id", postId)
                    .isPresent();
        } else if (block instanceof com.brandPitara.sfs.cms.content.document.ContentBlock.Video video) {
            assertThat(mediaAssetRepository.findById(video.mediaAssetId()))
                    .as("post %d video block must reference a real CmsMediaAssetEntity, never a WP attachment id", postId)
                    .isPresent();
            if (video.posterMediaAssetId() != null) {
                assertThat(mediaAssetRepository.findById(video.posterMediaAssetId()))
                        .as("post %d video poster must reference a real CmsMediaAssetEntity, never a WP attachment id", postId)
                        .isPresent();
            }
        } else if (block instanceof com.brandPitara.sfs.cms.content.document.ContentBlock.Gallery gallery) {
            for (var img : gallery.images()) {
                assertThat(mediaAssetRepository.findById(img.mediaAssetId()))
                        .as("post %d gallery image must reference a real CmsMediaAssetEntity, never a WP attachment id", postId)
                        .isPresent();
            }
        } else if (block instanceof com.brandPitara.sfs.cms.content.document.ContentBlock.Layout layout) {
            for (var child : layout.children()) {
                if (child instanceof com.brandPitara.sfs.cms.content.document.ContentBlock.Image image) {
                    assertThat(mediaAssetRepository.findById(image.mediaAssetId()))
                            .as("post %d layout image must reference a real CmsMediaAssetEntity, never a WP attachment id", postId)
                            .isPresent();
                }
            }
        }
    }

    private void addAttachmentPathIfResolvable(WordPressDataset dataset, Long attachmentId, Map<Long, String> out) {
        if (out.containsKey(attachmentId)) {
            return;
        }
        WordPressPostRow attachment = dataset.postsById().get(attachmentId);
        if (attachment == null || !"attachment".equals(attachment.postType())) {
            return;
        }
        String relativePath = dataset.metaValue(attachmentId, "_wp_attached_file");
        if (relativePath != null && !relativePath.isBlank()) {
            out.put(attachmentId, relativePath.trim());
        }
    }

    private void printClassificationBreakdown(
            Map<Long, WordPressPostRow> posts, Map<Long, WordPressImportOutcome> outcomes, Map<Long, Exception> failures
    ) {
        Set<Long> reviewOrBlocked = new LinkedHashSet<>();
        System.out.println("========== Full Corpus Import Report ==========");
        for (Long postId : posts.keySet()) {
            WordPressImportOutcome outcome = outcomes.get(postId);
            Exception failure = failures.get(postId);
            String state = outcome == null ? "FAILED" : outcome.migrationState().toString();
            if ("NEEDS_REVIEW".equals(state) || "BLOCKED".equals(state) || "FAILED".equals(state)) {
                reviewOrBlocked.add(postId);
            }
            System.out.println("post=" + postId + " outcome=" + state
                    + " targetId=" + (outcome == null ? "-" : outcome.targetContentId())
                    + " error=" + (failure == null ? "-" : failure.getMessage()));
        }
        System.out.println("Posts needing attention (NEEDS_REVIEW/BLOCKED/FAILED): " + reviewOrBlocked.size()
                + " -> " + reviewOrBlocked);
        System.out.println("=================================================");
    }

    private String describeFailures(Map<Long, Exception> failures) {
        StringBuilder sb = new StringBuilder();
        for (var entry : failures.entrySet()) {
            sb.append(entry.getKey()).append(" -> ").append(entry.getValue().getMessage()).append("; ");
        }
        return sb.toString();
    }

    private void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var walk = Files.walk(directory)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}
