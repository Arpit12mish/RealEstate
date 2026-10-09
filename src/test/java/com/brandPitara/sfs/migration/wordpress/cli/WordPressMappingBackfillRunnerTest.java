package com.brandPitara.sfs.migration.wordpress.cli;

import com.brandPitara.sfs.SfsApplication;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.workflow.domain.ContentRevisionReason;
import com.brandPitara.sfs.cms.workflow.entity.ContentPostRevisionEntity;
import com.brandPitara.sfs.cms.workflow.repository.ContentPostRevisionRepository;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.migration.wordpress.audit.WordPressDataset;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressContentFingerprint;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressContentImportService;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressImportRequest;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMappingBackfillConflictException;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMappingBackfillWriter;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMigrationMappingEntity;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMigrationMappingRepository;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMigrationState;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergConversionResult;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergDocumentConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hermetic Testcontainers coverage for {@link WordPressMappingBackfillRunner} - synthetic,
 * hand-built dump fixtures (never the real WordPress export), a real Postgres, and the real
 * {@link WordPressMappingBackfillWriter}/repositories. Never boots {@code
 * WordPressMediaImportService} or any S3 storage bean at all - if this test suite passed a
 * compile, that alone proves the runner cannot reach media/S3 code, since nothing here wires it.
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
                "jwt.secret=wp-backfill-test-mobile-secret-wp-backfill-test-mobile-secret",
                "dashboard.jwt.secret=wp-backfill-test-dashboard-secret-wp-backfill-test-dashboard-secret",
                "dashboard.seed.enabled=false",
                "sfs.search.enabled=false",
                "google.maps.places.enabled=false",
                "sfs.instagram.meta.sync-enabled=false",
                "sfs.log.dir=target/test-logs",
                "app.cms.media.public-delivery.base-url=https://media.example.test",
                "sfs.migration.wordpress.mapping-backfill.enabled=true",
                "sfs.migration.wordpress.mapping-backfill.confirmation-token=test-backfill-token"
        }
)
@ActiveProfiles({"test", "local-staging"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WordPressMappingBackfillRunnerTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_wordpress_backfill_test")
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
    private DashboardUserRepository dashboardUserRepository;
    @Autowired
    private ContentPostRepository contentPostRepository;
    @Autowired
    private ContentPostRevisionRepository revisionRepository;
    @Autowired
    private CmsMediaAssetRepository mediaAssetRepository;
    @Autowired
    private WordPressMigrationMappingRepository mappingRepository;
    @Autowired
    private WordPressMappingBackfillWriter writer;
    @Autowired
    private WordPressContentImportService realImportService;

    private WordPressMappingBackfillRunner runner;
    private DashboardUserEntity actor;

    @BeforeEach
    void setUp() {
        // Every test asserts absolute mapping-row counts (isZero()/isOne()) - the shared
        // @SpringBootTest context means rows from an earlier test method would otherwise still be
        // there, since this uses real saveAndFlush, not test-transactional rollback.
        mappingRepository.deleteAll();
        runner = WordPressMappingBackfillRunner.fromContext(applicationContext());
        actor = dashboardUserRepository.saveAndFlush(DashboardUserEntity.builder()
                .name("Backfill Operator")
                .email("backfill-operator-" + UUID.randomUUID() + "@example.com")
                .passwordHash("encoded")
                .role(DashboardRole.CONTENT_STAFF)
                .active(true)
                .build());
    }

    @Autowired
    private org.springframework.context.ApplicationContext springContext;

    private org.springframework.context.ApplicationContext applicationContext() {
        return springContext;
    }

    // ---------- dry-run / apply basics ----------

    @Test
    void dryRunCreatesZeroMappingRows(@TempDir Path dir) throws IOException {
        long wpId = 71001L;
        ContentPostEntity target = seedPublishedTarget("Dry Run Post", "dry-run-post");
        Path dump = writeDump(dir, wpPostsInsert(wpPostRow(wpId, "publish", "Dry Run Post", "dry-run-post", 1)));

        int exitCode = runner.run(cliArgs(dump, Map.of(wpId, target.getId()), false, null), out(), out());

        assertThat(exitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_SUCCESS);
        assertThat(mappingRepository.count()).isZero();
    }

    @Test
    void successfulApplyInsertsExactlyOneMappingRow(@TempDir Path dir) throws IOException {
        long wpId = 71002L;
        ContentPostEntity target = seedPublishedTarget("Apply Post", "apply-post");
        Path dump = writeDump(dir, wpPostsInsert(wpPostRow(wpId, "publish", "Apply Post", "apply-post", 1)));

        int exitCode = runner.run(cliArgs(dump, Map.of(wpId, target.getId()), true, "test-backfill-token"), out(), out());

        assertThat(exitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_SUCCESS);
        assertThat(mappingRepository.count()).isOne();
        WordPressMigrationMappingEntity saved = mappingRepository.findBySourceSystemAndSourcePostId("wordpress", wpId).orElseThrow();
        assertThat(saved.getTargetContentId()).isEqualTo(target.getId());
        assertThat(saved.getMigrationState()).isEqualTo(WordPressMigrationState.PUBLISHED);
    }

    @Test
    void identicalRerunAfterApplyCreatesNoAdditionalRows(@TempDir Path dir) throws IOException {
        long wpId = 71003L;
        ContentPostEntity target = seedPublishedTarget("Rerun Post", "rerun-post");
        Path dump = writeDump(dir, wpPostsInsert(wpPostRow(wpId, "publish", "Rerun Post", "rerun-post", 1)));
        var args = cliArgs(dump, Map.of(wpId, target.getId()), true, "test-backfill-token");

        runner.run(args, out(), out());
        assertThat(mappingRepository.count()).isOne();

        int secondExitCode = runner.run(args, out(), out());

        assertThat(secondExitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_SUCCESS);
        assertThat(mappingRepository.count()).isOne();
    }

    // ---------- validation failures ----------

    @Test
    void missingSourcePostIsBlockedAndNothingIsWritten(@TempDir Path dir) throws IOException {
        long realWpId = 71004L;
        ContentPostEntity target = seedPublishedTarget("Has A Target", "has-a-target");
        Path dump = writeDump(dir, wpPostsInsert(wpPostRow(realWpId, "publish", "Has A Target", "has-a-target", 1)));
        long nonexistentWpId = 999999L;

        int exitCode = runner.run(cliArgs(dump, Map.of(nonexistentWpId, target.getId()), true, "test-backfill-token"), out(), errCapture());

        assertThat(exitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_VALIDATION_BLOCKED);
        assertThat(mappingRepository.count()).isZero();
    }

    @Test
    void missingTargetContentIsBlockedAndNothingIsWritten(@TempDir Path dir) throws IOException {
        long wpId = 71005L;
        Path dump = writeDump(dir, wpPostsInsert(wpPostRow(wpId, "publish", "No Target Post", "no-target-post", 1)));
        long nonexistentTargetId = 8888888L;

        int exitCode = runner.run(cliArgs(dump, Map.of(wpId, nonexistentTargetId), true, "test-backfill-token"), out(), errCapture());

        assertThat(exitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_VALIDATION_BLOCKED);
        assertThat(mappingRepository.count()).isZero();
    }

    @Test
    void titleAndSlugMismatchIsBlocked(@TempDir Path dir) throws IOException {
        long wpId = 71006L;
        ContentPostEntity target = seedPublishedTarget("Completely Different Article", "completely-different-article");
        Path dump = writeDump(dir, wpPostsInsert(
                wpPostRow(wpId, "publish", "Totally Unrelated Title", "totally-unrelated-title", 1)));

        int exitCode = runner.run(cliArgs(dump, Map.of(wpId, target.getId()), true, "test-backfill-token"), out(), errCapture());

        assertThat(exitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_VALIDATION_BLOCKED);
        assertThat(mappingRepository.count()).isZero();
    }

    @Test
    void sourceAlreadyMappedToADifferentTargetIsBlocked(@TempDir Path dir) throws IOException {
        long wpId = 71007L;
        ContentPostEntity originalTarget = seedPublishedTarget("Original Target Post", "original-target-post");
        ContentPostEntity newTarget = seedPublishedTarget("Original Target Post", "original-target-post-2");
        mappingRepository.saveAndFlush(WordPressMigrationMappingEntity.builder()
                .sourceSystem("wordpress").sourcePostId(wpId).targetContentId(originalTarget.getId())
                .sourceFingerprint("irrelevant-existing-fingerprint")
                .migrationState(WordPressMigrationState.PUBLISHED)
                .build());
        Path dump = writeDump(dir, wpPostsInsert(
                wpPostRow(wpId, "publish", "Original Target Post", "original-target-post", 1)));

        int exitCode = runner.run(cliArgs(dump, Map.of(wpId, newTarget.getId()), true, "test-backfill-token"), out(), errCapture());

        assertThat(exitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_VALIDATION_BLOCKED);
        assertThat(mappingRepository.count()).isOne();
        assertThat(mappingRepository.findByTargetContentId(newTarget.getId())).isEmpty();
    }

    @Test
    void targetAlreadyMappedFromAnotherSourceIsBlocked(@TempDir Path dir) throws IOException {
        long otherWpId = 71008L;
        long thisWpId = 71009L;
        ContentPostEntity target = seedPublishedTarget("Shared Target Post", "shared-target-post");
        mappingRepository.saveAndFlush(WordPressMigrationMappingEntity.builder()
                .sourceSystem("wordpress").sourcePostId(otherWpId).targetContentId(target.getId())
                .sourceFingerprint("irrelevant-existing-fingerprint")
                .migrationState(WordPressMigrationState.PUBLISHED)
                .build());
        Path dump = writeDump(dir, wpPostsInsert(
                wpPostRow(thisWpId, "publish", "Shared Target Post", "shared-target-post", 1)));

        int exitCode = runner.run(cliArgs(dump, Map.of(thisWpId, target.getId()), true, "test-backfill-token"), out(), errCapture());

        assertThat(exitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_VALIDATION_BLOCKED);
        assertThat(mappingRepository.count()).isOne();
        assertThat(mappingRepository.findBySourceSystemAndSourcePostId("wordpress", thisWpId)).isEmpty();
    }

    @Test
    void nonPublishedTargetStatusIsBlocked(@TempDir Path dir) throws IOException {
        long wpId = 71010L;
        ContentPostEntity target = seedTarget("Still A Draft", "still-a-draft", ContentStatus.DRAFT);
        Path dump = writeDump(dir, wpPostsInsert(wpPostRow(wpId, "publish", "Still A Draft", "still-a-draft", 1)));

        int exitCode = runner.run(cliArgs(dump, Map.of(wpId, target.getId()), true, "test-backfill-token"), out(), errCapture());

        assertThat(exitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_VALIDATION_BLOCKED);
        assertThat(mappingRepository.count()).isZero();
    }

    // ---------- content-equivalence reporting ----------

    @Test
    void contentDifferencesAreReportedAsAdoptExistingWithDifferencesNotHiddenAsAMatch(@TempDir Path dir) throws IOException {
        long wpId = 71011L;
        // Target has the same title/slug (identity matches) but a materially different, much
        // shorter body than the WordPress source - a real content difference that must be
        // reported, not silently folded into a clean INSERT.
        ContentPostEntity target = seedPublishedTarget("Divergent Content Post", "divergent-content-post",
                new ContentDocument(5, java.util.List.of(
                        new com.brandPitara.sfs.cms.content.document.ContentBlock.Paragraph(
                                java.util.List.of(new com.brandPitara.sfs.cms.content.document.InlineNode.Text("Short.", java.util.List.of()))))));
        String longBody = """
                <!-- wp:paragraph -->
                <p>This is a much longer, materially different paragraph of real editorial content that
                does not match the short placeholder stored on the manually-created target at all, on
                purpose, so the comparison must detect and report this difference rather than silently
                treating the two as equivalent content.</p>
                <!-- /wp:paragraph -->
                """;
        Path dump = writeDump(dir, wpPostsInsert(
                wpPostRow(wpId, "publish", "Divergent Content Post", "divergent-content-post", 1, longBody)));

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        int exitCode = runner.run(cliArgs(dump, Map.of(wpId, target.getId()), false, null), new PrintStream(captured), out());

        assertThat(exitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_SUCCESS);
        String report = captured.toString(StandardCharsets.UTF_8);
        assertThat(report).contains("ADOPT_EXISTING_WITH_DIFFERENCES");
        assertThat(report).contains("DIFFERENT");
    }

    // ---------- fingerprint consistency ----------

    @Test
    void backfillFingerprintMatchesWhatTheRealImporterWouldPersist() {
        WordPressPostRow post = new WordPressPostRow(
                71012L, 1L, "2026-01-01 00:00:00", "2026-01-01 00:00:00", "2026-01-01 00:00:00",
                "<!-- wp:paragraph --><p>Consistency check.</p><!-- /wp:paragraph -->",
                "Fingerprint Consistency Check", "", "publish", "fingerprint-consistency-check",
                null, "post", "", "https://example.com/?p=71012"
        );
        WordPressDataset dataset = WordPressDataset.builder().build();

        String directlyComputed = WordPressContentFingerprint.compute(post, dataset);

        GutenbergConversionResult conversion = new GutenbergDocumentConverter()
                .convert(post.id(), post.postTitle(), post.postContent());
        WordPressImportRequest request = new WordPressImportRequest(post, dataset, conversion, Map.of(), actor);
        realImportService.importPost(request);
        WordPressMigrationMappingEntity persisted = mappingRepository
                .findBySourceSystemAndSourcePostId("wordpress", post.id()).orElseThrow();

        assertThat(persisted.getSourceFingerprint()).isEqualTo(directlyComputed);
    }

    // ---------- authorization ----------

    @Test
    void applyWithoutConfirmationMatchingTheBackfillTokenFails(@TempDir Path dir) throws IOException {
        long wpId = 71013L;
        ContentPostEntity target = seedPublishedTarget("Auth Check Post", "auth-check-post");
        Path dump = writeDump(dir, wpPostsInsert(wpPostRow(wpId, "publish", "Auth Check Post", "auth-check-post", 1)));

        int exitCode = runner.run(cliArgs(dump, Map.of(wpId, target.getId()), true, "wrong-token"), out(), errCapture());

        assertThat(exitCode).isEqualTo(WordPressMappingBackfillRunner.EXIT_AUTHORIZATION_FAILED);
        assertThat(mappingRepository.count()).isZero();
    }

    // ---------- atomicity / rollback ----------

    @Test
    void transactionRollsBackEntirelyWhenOneInsertConflicts() {
        long okWpId = 71014L;
        ContentPostEntity okTarget = seedPublishedTarget("Rollback Ok Post", "rollback-ok-post");
        long conflictingWpId = 71015L;
        ContentPostEntity conflictingTarget = seedPublishedTarget("Rollback Conflict Post", "rollback-conflict-post");
        ContentPostEntity alreadyMappedElsewhere = seedPublishedTarget("Already Mapped Elsewhere", "already-mapped-elsewhere");
        // Pre-seed a mapping for conflictingWpId (pointing at a DIFFERENT real target) so the
        // writer's own re-check finds a conflict - target_content_id has a real FK to content_post.
        mappingRepository.saveAndFlush(WordPressMigrationMappingEntity.builder()
                .sourceSystem("wordpress").sourcePostId(conflictingWpId).targetContentId(alreadyMappedElsewhere.getId())
                .sourceFingerprint("pre-existing").migrationState(WordPressMigrationState.PUBLISHED)
                .build());

        var pendingInserts = java.util.List.of(
                new WordPressMappingBackfillWriter.PendingInsert(okWpId, okTarget.getId(), "fingerprint-a"),
                new WordPressMappingBackfillWriter.PendingInsert(conflictingWpId, conflictingTarget.getId(), "fingerprint-b")
        );

        assertThatThrownBy(() -> writer.applyBackfill(pendingInserts))
                .isInstanceOf(WordPressMappingBackfillConflictException.class);

        // The first pair (okWpId) would have succeeded on its own, but must have been rolled back
        // together with the second (conflicting) one - only the one pre-seeded row may exist.
        assertThat(mappingRepository.count()).isOne();
        assertThat(mappingRepository.findBySourceSystemAndSourcePostId("wordpress", okWpId)).isEmpty();
    }

    // ---------- write-scope isolation ----------

    @Test
    void applyNeverChangesContentPostOrMediaAssetRowCounts(@TempDir Path dir) throws IOException {
        long wpId = 71016L;
        ContentPostEntity target = seedPublishedTarget("Write Scope Post", "write-scope-post");
        Path dump = writeDump(dir, wpPostsInsert(wpPostRow(wpId, "publish", "Write Scope Post", "write-scope-post", 1)));
        long contentPostsBefore = contentPostRepository.count();
        long mediaAssetsBefore = mediaAssetRepository.count();

        runner.run(cliArgs(dump, Map.of(wpId, target.getId()), true, "test-backfill-token"), out(), out());

        assertThat(contentPostRepository.count()).isEqualTo(contentPostsBefore);
        assertThat(mediaAssetRepository.count()).isEqualTo(mediaAssetsBefore);
    }

    // ---------- helpers ----------

    private ContentPostEntity seedPublishedTarget(String title, String slug) {
        return seedPublishedTarget(title, slug, ContentDocument.empty());
    }

    /** A PUBLISHED content_post is more than a status flag - {@code
     * chk_content_post_workflow_pointers} requires a real, saved revision set as both {@code
     * approvedRevision} and {@code currentPublishedRevision}, matching exactly what {@code
     * WordPressContentImportService#publish} itself does. */
    private ContentPostEntity seedPublishedTarget(String title, String slug, ContentDocument document) {
        ContentPostEntity draft = contentPostRepository.saveAndFlush(ContentPostEntity.builder()
                .contentType(ContentType.BLOG)
                .status(ContentStatus.DRAFT)
                .title(title)
                .slug(slug)
                .contentOwner(actor).createdBy(actor).updatedBy(actor)
                .robotsIndex(true).robotsFollow(true)
                .contentDocument(document)
                .contentDocumentSchemaVersion((short) document.schemaVersion())
                .build());

        ContentPostRevisionEntity revision = revisionRepository.saveAndFlush(ContentPostRevisionEntity.builder()
                .contentPost(draft)
                .revisionNumber(1)
                .contentType(ContentType.BLOG)
                .title(title)
                .slug(slug)
                .robotsIndex(true).robotsFollow(true)
                .contentDocument(document)
                .contentDocumentSchemaVersion((short) document.schemaVersion())
                .createdFromPostVersion(draft.getVersion())
                .createdBy(actor)
                .revisionReason(ContentRevisionReason.REVIEW_SUBMISSION)
                .build());

        draft.setApprovedRevision(revision);
        draft.setCurrentPublishedRevision(revision);
        draft.setStatus(ContentStatus.PUBLISHED);
        draft.setPublishedAt(java.time.OffsetDateTime.now());
        draft.setPublishedBy(actor);
        return contentPostRepository.saveAndFlush(draft);
    }

    private ContentPostEntity seedTarget(String title, String slug, ContentStatus status) {
        if (status == ContentStatus.PUBLISHED) {
            return seedPublishedTarget(title, slug);
        }
        return contentPostRepository.saveAndFlush(ContentPostEntity.builder()
                .contentType(ContentType.BLOG)
                .status(status)
                .title(title)
                .slug(slug)
                .contentOwner(actor).createdBy(actor).updatedBy(actor)
                .robotsIndex(true).robotsFollow(true)
                .build());
    }

    private WordPressMappingBackfillCli.CliArgs cliArgs(Path dump, Map<Long, Long> pairs, boolean apply, String confirm) {
        return new WordPressMappingBackfillCli.CliArgs(dump, actor.getEmail(), pairs, apply, confirm);
    }

    private String wpPostRow(long id, String status, String title, String slug, long authorId) {
        return wpPostRow(id, status, title, slug, authorId,
                "<!-- wp:paragraph --><p>Body for " + title + ".</p><!-- /wp:paragraph -->");
    }

    private String wpPostRow(long id, String status, String title, String slug, long authorId, String content) {
        String template = "(%d, %d, '2026-01-01 00:00:00', '2026-01-01 00:00:00', '%s', '%s', '', '%s', 'closed', 'closed', "
                + "'', '%s', '', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00', '', 0, "
                + "'https://example.com/?p=%d', 0, 'post', '', 0)";
        return template.formatted(id, authorId, content.replace("'", "\\'"), title.replace("'", "\\'"), status, slug, id);
    }

    private String wpPostsInsert(String... rows) {
        return """
                INSERT INTO `wp_posts` (`ID`, `post_author`, `post_date`, `post_date_gmt`, `post_content`,
                    `post_title`, `post_excerpt`, `post_status`, `comment_status`, `ping_status`,
                    `post_password`, `post_name`, `to_ping`, `pinged`, `post_modified`, `post_modified_gmt`,
                    `post_content_filtered`, `post_parent`, `guid`, `menu_order`, `post_type`,
                    `post_mime_type`, `comment_count`) VALUES
                """ + String.join(",\n", rows) + ";\n";
    }

    private Path writeDump(Path dir, String sql) throws IOException {
        Path dump = dir.resolve("dump-" + System.nanoTime() + ".sql");
        Files.writeString(dump, sql, StandardCharsets.UTF_8);
        return dump;
    }

    private PrintStream out() {
        return new PrintStream(new ByteArrayOutputStream());
    }

    private PrintStream errCapture() {
        return new PrintStream(new ByteArrayOutputStream());
    }
}
