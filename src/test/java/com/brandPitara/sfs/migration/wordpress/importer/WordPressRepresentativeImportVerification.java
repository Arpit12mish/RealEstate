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
import java.security.MessageDigest;
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
 * Explicit opt-in verification of the ten-post representative WordPress import against the
 * <strong>real</strong> production export - this class deliberately does not end in
 * "Test"/"Tests"/"TestCase", so Maven Surefire's default discovery never selects it and a normal
 * {@code mvn test} run neither executes it nor fails when the source files are absent. Invoke it
 * explicitly once Docker and the two real files are available:
 *
 * <pre>
 *   mvn -q -o test -Dtest=WordPressRepresentativeImportVerification
 * </pre>
 *
 * Reads the real dump and {@code uploads.zip} from {@code /Users/mac/Downloads} by default
 * (override with {@code -Dsfs.migration.wordpress.dumpPath=...} /
 * {@code -Dsfs.migration.wordpress.uploadsZipPath=...}); both are opened strictly read-only and
 * are never modified. All persistence happens in a disposable Testcontainers PostgreSQL 16
 * container with no mounted volume, torn down with the JVM. Media goes through the real
 * {@link WordPressMediaImportService} lifecycle (validate -> store -> verify -> READY -> map),
 * backed by {@link InMemoryWordPressMigrationMediaStorage} - never real S3 - so every asset this
 * test marks READY has an actual stored, checksum-verified object. Only the ten sample post IDs
 * below are ever imported - never the full ~144-post corpus.
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
                "jwt.secret=wp-verify-test-mobile-secret-wp-verify-test-mobile-secret",
                "dashboard.jwt.secret=wp-verify-test-dashboard-secret-wp-verify-test-dashboard-secret",
                "dashboard.seed.enabled=false",
                "sfs.search.enabled=false",
                "google.maps.places.enabled=false",
                "sfs.instagram.meta.sync-enabled=false",
                "sfs.log.dir=target/test-logs",
                "app.cms.media.public-delivery.base-url=https://media.example.test",
                "app.cms.media.bucket=wp-representative-import-test-bucket"
        }
)
@Import(WordPressRepresentativeImportVerification.FakeStorageConfig.class)
@ActiveProfiles({"test", "local-staging"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WordPressRepresentativeImportVerification {

    private static final Set<Long> SAMPLE_POST_IDS = Set.of(
            10072L, 10054L, 9191L, 9778L, 8020L, 597L, 8601L, 8703L, 8886L, 640L
    );

    private static final Path DUMP_PATH = Path.of(System.getProperty(
            "sfs.migration.wordpress.dumpPath", "/Users/mac/Downloads/u427251222_Ue8rA.sql"
    ));
    private static final Path UPLOADS_ZIP_PATH = Path.of(System.getProperty(
            "sfs.migration.wordpress.uploadsZipPath", "/Users/mac/Downloads/uploads.zip"
    ));
    private static final String BUCKET = "wp-representative-import-test-bucket";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_wordpress_representative_import")
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
    private WordPressMigrationMediaStorage storage;

    private InMemoryWordPressMigrationMediaStorage fakeStorage;
    private DashboardUserEntity actor;

    @BeforeEach
    void createActor() {
        fakeStorage = (InMemoryWordPressMigrationMediaStorage) storage;
        actor = dashboardUserRepository.saveAndFlush(DashboardUserEntity.builder()
                .name("WordPress Migration Runner")
                .email("wp-migration-runner-" + UUID.randomUUID() + "@example.com")
                .passwordHash("encoded")
                .role(DashboardRole.CONTENT_STAFF)
                .active(true)
                .build());
    }

    @Test
    void importsTheTenSamplePostsEndToEndAgainstTheRealExport() throws Exception {
        Assumptions.assumeTrue(Files.exists(DUMP_PATH),
                "Real WordPress dump not found at " + DUMP_PATH + " - opt-in verification skipped.");
        Assumptions.assumeTrue(Files.exists(UPLOADS_ZIP_PATH),
                "uploads.zip not found at " + UPLOADS_ZIP_PATH + " - opt-in verification skipped.");

        System.out.println("Reading real WordPress dump: " + DUMP_PATH);
        WordPressDataset.Builder builder = WordPressDataset.builder();
        new WordPressDumpReader().read(DUMP_PATH, builder);
        WordPressDataset dataset = builder.build();
        System.out.println("Dataset loaded: " + dataset.postsById().size() + " total posts.");

        Map<Long, WordPressPostRow> samples = new LinkedHashMap<>();
        for (Long id : SAMPLE_POST_IDS) {
            WordPressPostRow row = dataset.postsById().get(id);
            assertThat(row)
                    .as("WordPress post %d must exist in the real dump - no silent substitution", id)
                    .isNotNull();
            samples.put(id, row);
        }

        Map<Long, GutenbergConversionResult> conversions = new LinkedHashMap<>();
        for (Map.Entry<Long, WordPressPostRow> entry : samples.entrySet()) {
            WordPressPostRow post = entry.getValue();
            conversions.put(entry.getKey(), new GutenbergDocumentConverter()
                    .convert(post.id(), post.postTitle(), post.postContent()));
        }

        Map<Long, String> attachmentPaths = new LinkedHashMap<>();
        for (Long postId : samples.keySet()) {
            for (Long attachmentId : conversions.get(postId).referencedAttachmentIds()) {
                addAttachmentPathIfResolvable(dataset, attachmentId, attachmentPaths);
            }
            String thumbnailIdRaw = dataset.metaValue(postId, "_thumbnail_id");
            if (thumbnailIdRaw != null && !thumbnailIdRaw.isBlank()) {
                try {
                    addAttachmentPathIfResolvable(dataset, Long.parseLong(thumbnailIdRaw.trim()), attachmentPaths);
                } catch (NumberFormatException notNumeric) {
                    System.out.println("Non-numeric _thumbnail_id for post " + postId + ": " + thumbnailIdRaw);
                }
            }
        }
        System.out.println("Attachment IDs to resolve from uploads.zip: " + attachmentPaths.keySet());

        Path tempMediaDir = Files.createTempDirectory("wp-representative-import-media-");
        try {
            Map<Long, Long> resolvedMediaAssetIds = importAllMediaAttachments(attachmentPaths, tempMediaDir);
            int uploadCallsFirstRun = fakeStorage.storeInvocationCount();
            int uniqueObjectsFirstRun = fakeStorage.objectCount();
            System.out.println("Media assets resolved: " + resolvedMediaAssetIds.size() + " of "
                    + attachmentPaths.size() + " referenced attachment IDs.");
            System.out.println("Upload calls on first run: " + uploadCallsFirstRun
                    + " ; unique objects stored: " + uniqueObjectsFirstRun);

            Map<Long, WordPressImportOutcome> outcomes = new LinkedHashMap<>();
            Map<Long, Exception> failures = new LinkedHashMap<>();
            for (Map.Entry<Long, WordPressPostRow> entry : samples.entrySet()) {
                long postId = entry.getKey();
                WordPressImportRequest request = new WordPressImportRequest(
                        entry.getValue(), dataset, conversions.get(postId), resolvedMediaAssetIds, actor
                );
                try {
                    outcomes.put(postId, importService.importPost(request));
                } catch (Exception failure) {
                    failures.put(postId, failure);
                }
            }

            printReport(samples, conversions, outcomes, failures);
            printResourceCounts(outcomes);

            assertThat(failures)
                    .as("unexpected import failures for sample posts: %s", describeFailures(failures))
                    .isEmpty();
            assertThat(outcomes).hasSize(SAMPLE_POST_IDS.size());

            WordPressImportOutcome blockedOutcome = outcomes.get(8886L);
            assertThat(blockedOutcome.migrationState()).isEqualTo(WordPressMigrationState.BLOCKED);
            assertThat(blockedOutcome.targetContentId()).isNull();

            verifyNoAttachmentIdLeaksIntoAnyDocument(outcomes);

            Map<Long, Exception> publicApiFailures = new LinkedHashMap<>();
            for (Map.Entry<Long, WordPressImportOutcome> entry : outcomes.entrySet()) {
                WordPressImportOutcome outcome = entry.getValue();
                if (outcome.migrationState() == WordPressMigrationState.PUBLISHED) {
                    ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
                    assertThat(saved.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
                    assertThat(saved.getCurrentPublishedRevision()).isNotNull();
                    System.out.println("Checking public detail for post " + entry.getKey() + " slug=" + saved.getSlug()
                            + " coverMediaAssetId=" + (saved.getCoverMediaAsset() == null ? null : saved.getCoverMediaAsset().getId()));
                    try {
                        var detail = publicContentService.getBySlug(saved.getSlug(), null).body();
                        assertThat(detail).as("published sample %d must be publicly fetchable", entry.getKey()).isNotNull();
                    } catch (Exception publicApiFailure) {
                        publicApiFailures.put(entry.getKey(), publicApiFailure);
                    }
                } else if (outcome.targetContentId() != null) {
                    ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
                    assertThat(saved.getStatus()).isNotEqualTo(ContentStatus.PUBLISHED);
                    String slug = saved.getSlug();
                    assertThatThrownBy(() -> publicContentService.getBySlug(slug, null))
                            .as("non-published sample %d must never be publicly fetchable", entry.getKey())
                            .isInstanceOf(PublicContentApiException.class);
                }
            }
            assertThat(publicApiFailures)
                    .as("published samples must be publicly fetchable: %s", describeFailures(publicApiFailures))
                    .isEmpty();

            for (Map.Entry<Long, WordPressPostRow> entry : samples.entrySet()) {
                long postId = entry.getKey();
                WordPressImportRequest rerunRequest = new WordPressImportRequest(
                        entry.getValue(), dataset, conversions.get(postId), resolvedMediaAssetIds, actor
                );
                WordPressImportOutcome rerun = importService.importPost(rerunRequest);
                WordPressImportOutcome original = outcomes.get(postId);
                assertThat(rerun.skippedIdempotent()).as("rerun of post %d must be idempotent", postId).isTrue();
                assertThat(rerun.targetContentId()).isEqualTo(original.targetContentId());
                assertThat(rerun.migrationState()).isEqualTo(original.migrationState());
            }
            System.out.println("Idempotent rerun verified for all " + samples.size() + " content posts: no duplicates, no status regression.");

            Map<Long, Long> rerunMediaIds = importAllMediaAttachments(attachmentPaths, tempMediaDir);
            int uploadCallsSecondRunTotal = fakeStorage.storeInvocationCount();
            int uploadCallsSecondRun = uploadCallsSecondRunTotal - uploadCallsFirstRun;
            assertThat(rerunMediaIds).isEqualTo(resolvedMediaAssetIds);
            assertThat(uploadCallsSecondRun)
                    .as("rerunning media import must perform zero duplicate uploads")
                    .isZero();
            System.out.println("Upload calls on second run: " + uploadCallsSecondRun + " (0 expected - all reused).");

            long checksumMismatches = verifyAllStoredChecksums(attachmentPaths, resolvedMediaAssetIds);
            assertThat(checksumMismatches).as("every stored object's bytes must hash to its recorded checksum").isZero();

            System.out.println("========== Media Verification Summary ==========");
            System.out.println("unique objects stored: " + fakeStorage.objectCount());
            System.out.println("cms_media_asset rows created: " + mediaAssetRepository.count());
            System.out.println("wordpress_migration_media_mapping rows created: " + mediaMappingRepository.count());
            System.out.println("deduplicated/reused attachment references: "
                    + (attachmentPaths.size() - fakeStorage.objectCount()) + " of " + attachmentPaths.size());
            System.out.println("bytes stored (sum of unique object sizes): " + totalStoredBytes());
            System.out.println("checksum verification: " + (checksumMismatches == 0 ? "ALL PASSED" : checksumMismatches + " MISMATCHES"));
            System.out.println("==================================================");
        } finally {
            deleteRecursively(tempMediaDir);
        }
    }

    private void verifyNoAttachmentIdLeaksIntoAnyDocument(Map<Long, WordPressImportOutcome> outcomes) {
        for (Map.Entry<Long, WordPressImportOutcome> entry : outcomes.entrySet()) {
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
                assertThat(mediaAssetRepository.findById(video.posterMediaAssetId())).isPresent();
            }
        } else if (block instanceof com.brandPitara.sfs.cms.content.document.ContentBlock.Gallery gallery) {
            for (var img : gallery.images()) {
                assertThat(mediaAssetRepository.findById(img.mediaAssetId())).isPresent();
            }
        } else if (block instanceof com.brandPitara.sfs.cms.content.document.ContentBlock.Layout layout) {
            for (var child : layout.children()) {
                if (child instanceof com.brandPitara.sfs.cms.content.document.ContentBlock.Image image) {
                    assertThat(mediaAssetRepository.findById(image.mediaAssetId())).isPresent();
                }
            }
        }
    }

    private long verifyAllStoredChecksums(Map<Long, String> attachmentPaths, Map<Long, Long> resolvedMediaAssetIds) throws Exception {
        long mismatches = 0;
        for (Long attachmentId : attachmentPaths.keySet()) {
            Long mediaAssetId = resolvedMediaAssetIds.get(attachmentId);
            if (mediaAssetId == null) {
                continue;
            }
            var mapping = mediaMappingRepository.findBySourceSystemAndWordPressAttachmentId(
                    WordPressMediaImportService.SOURCE_SYSTEM, attachmentId
            ).orElseThrow();
            byte[] stored = fakeStorage.storedBytes(BUCKET, mapping.getObjectKey());
            if (stored == null) {
                mismatches++;
                continue;
            }
            String actualSha256 = sha256Hex(stored);
            if (!actualSha256.equalsIgnoreCase(mapping.getSourceSha256())) {
                mismatches++;
            }
        }
        return mismatches;
    }

    private long totalStoredBytes() {
        return mediaMappingRepository.findAll().stream()
                .map(m -> fakeStorage.storedBytes(BUCKET, m.getObjectKey()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .mapToLong(b -> b.length)
                .sum();
    }

    private String sha256Hex(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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

    /**
     * Imports every referenced attachment through the real {@link WordPressMediaImportService}
     * lifecycle (validate -> store -> verify -> READY -> map) - never a direct-insert shortcut.
     * Safe to call twice: the second call must reuse every mapping without re-uploading.
     */
    private Map<Long, Long> importAllMediaAttachments(Map<Long, String> attachmentPaths, Path tempMediaDir) {
        Map<Long, Long> resolved = new LinkedHashMap<>();
        for (Map.Entry<Long, String> entry : attachmentPaths.entrySet()) {
            long attachmentId = entry.getKey();
            WordPressMediaImportRequest request = new WordPressMediaImportRequest(
                    attachmentId, UPLOADS_ZIP_PATH, entry.getValue(), tempMediaDir, actor
            );
            try {
                WordPressMediaImportOutcome outcome = mediaImportService.importAttachment(request);
                resolved.put(attachmentId, outcome.cmsMediaAssetId());
            } catch (Exception failure) {
                System.out.println("Media import failed for attachment " + attachmentId + ": " + failure.getMessage());
            }
        }
        return resolved;
    }

    private void printReport(
            Map<Long, WordPressPostRow> samples,
            Map<Long, GutenbergConversionResult> conversions,
            Map<Long, WordPressImportOutcome> outcomes,
            Map<Long, Exception> failures
    ) {
        System.out.println("========== Representative Import Report ==========");
        for (Long postId : samples.keySet()) {
            WordPressPostRow post = samples.get(postId);
            GutenbergConversionResult conversion = conversions.get(postId);
            WordPressImportOutcome outcome = outcomes.get(postId);
            Exception failure = failures.get(postId);
            System.out.printf(
                    "post=%d title='%s' status=%s eligible=%s manualReview=%s warnings=%d unsupported=%d "
                            + "outcome=%s targetId=%s error=%s%n",
                    postId, post.postTitle(), post.postStatus(),
                    conversion.eligibleForAutomaticPublication(), conversion.manualReviewRequired(),
                    conversion.warnings().size(), conversion.unsupportedBlocks().size(),
                    outcome == null ? "FAILED" : outcome.migrationState(),
                    outcome == null ? "-" : outcome.targetContentId(),
                    failure == null ? "-" : failure.getMessage()
            );
        }
        System.out.println("====================================================");
    }

    private void printResourceCounts(Map<Long, WordPressImportOutcome> outcomes) {
        Set<Long> categoryIds = new LinkedHashSet<>();
        Set<Long> authorIds = new LinkedHashSet<>();
        long published = 0, draft = 0, needsReview = 0, blocked = 0;
        for (WordPressImportOutcome outcome : outcomes.values()) {
            switch (outcome.migrationState()) {
                case PUBLISHED -> published++;
                case DRAFT -> draft++;
                case NEEDS_REVIEW -> needsReview++;
                case BLOCKED -> blocked++;
                default -> { }
            }
            if (outcome.targetContentId() != null) {
                ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
                if (saved.getCategory() != null) categoryIds.add(saved.getCategory().getId());
                if (saved.getPublicAuthor() != null) authorIds.add(saved.getPublicAuthor().getId());
            }
        }
        System.out.println("========== Resource Counts ==========");
        System.out.println("content_post rows created: " + (outcomes.size() - blocked)
                + " (of " + outcomes.size() + " sample posts; " + blocked + " BLOCKED -> no row)");
        System.out.println("content_post_revision rows created: " + published + " (one per PUBLISHED sample)");
        System.out.println("distinct CmsPublicAuthorEntity referenced: " + authorIds.size() + " " + authorIds);
        System.out.println("distinct CmsContentCategoryEntity referenced: " + categoryIds.size() + " " + categoryIds);
        System.out.println("cms_media_asset rows created (this run): " + mediaAssetRepository.count());
        System.out.println("total CmsPublicAuthorEntity rows in DB: " + authorRepository.count());
        System.out.println("total CmsContentCategoryEntity rows in DB: " + categoryRepository.count());
        System.out.println("total CmsContentTagEntity rows in DB: " + tagRepository.count());
        System.out.println("outcome breakdown: PUBLISHED=" + published + " DRAFT=" + draft
                + " NEEDS_REVIEW=" + needsReview + " BLOCKED=" + blocked);
        System.out.println("======================================");
    }

    private String describeFailures(Map<Long, Exception> failures) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Long, Exception> entry : failures.entrySet()) {
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
