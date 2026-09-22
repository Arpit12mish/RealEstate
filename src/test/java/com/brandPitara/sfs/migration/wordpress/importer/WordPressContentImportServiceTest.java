package com.brandPitara.sfs.migration.wordpress.importer;

import com.brandPitara.sfs.SfsApplication;
import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.document.ContentBlock;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.workflow.repository.ContentPostRevisionRepository;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.migration.wordpress.audit.WordPressDataset;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostMetaRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressTermRelationshipRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressTermRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressTermTaxonomyRow;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergConversionResult;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergDocumentConverter;
import com.brandPitara.sfs.publiccontent.exception.PublicContentApiException;
import com.brandPitara.sfs.publiccontent.service.PublicContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hermetic Testcontainers coverage for {@link WordPressContentImportService} using compact,
 * hand-built fixtures - never the real WordPress dump or {@code uploads.zip} (see
 * {@code WordPressRepresentativeImportVerification} for the explicit opt-in test that imports the
 * ten real sample post IDs from local files). The container is disposed with the JVM; no volume
 * is mounted and no production/staging database is touched.
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
                "jwt.secret=wp-import-test-mobile-secret-wp-import-test-mobile-secret",
                "dashboard.jwt.secret=wp-import-test-dashboard-secret-wp-import-test-dashboard-secret",
                "dashboard.seed.enabled=false",
                "sfs.search.enabled=false",
                "google.maps.places.enabled=false",
                "sfs.instagram.meta.sync-enabled=false",
                "sfs.log.dir=target/test-logs",
                "app.cms.media.public-delivery.base-url=https://media.example.test"
        }
)
@ActiveProfiles({"test", "local-staging"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WordPressContentImportServiceTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_wordpress_import_test")
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
    private WordPressContentImportService importService;
    @Autowired
    private WordPressMigrationMappingRepository mappingRepository;
    @Autowired
    private ContentPostRepository contentPostRepository;
    @Autowired
    private ContentPostRevisionRepository revisionRepository;
    @Autowired
    private DashboardUserRepository dashboardUserRepository;
    @Autowired
    private CmsMediaAssetRepository mediaAssetRepository;
    @Autowired
    private PublicContentService publicContentService;

    private DashboardUserEntity actor;

    @BeforeEach
    void createActor() {
        actor = dashboardUserRepository.saveAndFlush(DashboardUserEntity.builder()
                .name("Migration Runner")
                .email("migration-runner-" + UUID.randomUUID() + "@example.com")
                .passwordHash("encoded")
                .role(DashboardRole.CONTENT_STAFF)
                .active(true)
                .build());
    }

    @Test
    void publishedEligiblePostBecomesPublishedWithCurrentPublishedRevisionSet() {
        long wpId = 90001L;
        WordPressPostRow post = post(wpId, "publish", "Gurgaon Skyline Views", """
                <!-- wp:paragraph -->
                <p>A clean paragraph with no unsupported markup.</p>
                <!-- /wp:paragraph -->
                """);
        WordPressImportOutcome outcome = importService.importPost(request(post, WordPressDataset.builder().build()));

        assertThat(outcome.migrationState()).isEqualTo(WordPressMigrationState.PUBLISHED);
        assertThat(outcome.skippedIdempotent()).isFalse();
        assertThat(outcome.targetContentId()).isNotNull();

        ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(saved.getCurrentPublishedRevision()).isNotNull();
        assertThat(saved.getApprovedRevision()).isNotNull();
        assertThat(saved.getPublishedAt()).isNotNull();
        assertThat(saved.getPublishedBy()).isNotNull();
        assertThat(revisionRepository.findMaximumRevisionNumber(saved.getId())).isEqualTo(1);

        WordPressMigrationMappingEntity mapping = mappingRepository
                .findBySourceSystemAndSourcePostId(WordPressContentImportService.SOURCE_SYSTEM, wpId).orElseThrow();
        assertThat(mapping.getMigrationState()).isEqualTo(WordPressMigrationState.PUBLISHED);
        assertThat(mapping.getTargetContentId()).isEqualTo(saved.getId());
    }

    @Test
    void publishedPostRequiringManualReviewBecomesDraftNeverPublicWithNeedsReviewMappingState() {
        long wpId = 90002L;
        // An <img> with no resolvable wp-image-<id> class forces manualReviewRequired=true.
        WordPressPostRow post = post(wpId, "publish", "Unresolvable Hotlinked Image", """
                <!-- wp:image -->
                <figure class="wp-block-image"><img src="https://external.example/hotlinked.jpg" alt="External"/></figure>
                <!-- /wp:image -->

                <!-- wp:paragraph -->
                <p>Some real text so the document is not empty.</p>
                <!-- /wp:paragraph -->
                """);
        WordPressImportOutcome outcome = importService.importPost(request(post, WordPressDataset.builder().build()));

        assertThat(outcome.migrationState()).isEqualTo(WordPressMigrationState.NEEDS_REVIEW);
        ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(saved.getCurrentPublishedRevision()).isNull();
        assertThat(saved.getPublishedAt()).isNull();
        assertThat(revisionRepository.findMaximumRevisionNumber(saved.getId())).isZero();

        assertThatThrownBy(() -> publicContentService.getBySlug(saved.getSlug(), null))
                .isInstanceOf(PublicContentApiException.class);
    }

    @Test
    void draftSourcePostAlwaysBecomesDraftWithNoRevisionEvenWhenContentIsClean() {
        long wpId = 90003L;
        WordPressPostRow post = post(wpId, "draft", "A Clean Draft", """
                <!-- wp:paragraph -->
                <p>Clean content that would otherwise be eligible for publication.</p>
                <!-- /wp:paragraph -->
                """);
        WordPressImportOutcome outcome = importService.importPost(request(post, WordPressDataset.builder().build()));

        assertThat(outcome.migrationState()).isEqualTo(WordPressMigrationState.DRAFT);
        ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(saved.getCurrentPublishedRevision()).isNull();
        assertThat(revisionRepository.findMaximumRevisionNumber(saved.getId())).isZero();
    }

    @Test
    void blockedPostRecordsMappingWithoutInsertingAContentPostRow() {
        long wpId = 90004L;
        WordPressPostRow post = post(wpId, "publish", "Empty Blocked Post", "");
        long postsBefore = contentPostRepository.count();

        WordPressImportOutcome outcome = importService.importPost(request(post, WordPressDataset.builder().build()));

        assertThat(outcome.migrationState()).isEqualTo(WordPressMigrationState.BLOCKED);
        assertThat(outcome.targetContentId()).isNull();
        assertThat(outcome.errorCode()).isEqualTo("BLOCKED_SOURCE_CONTENT");
        assertThat(contentPostRepository.count()).isEqualTo(postsBefore);

        WordPressMigrationMappingEntity mapping = mappingRepository
                .findBySourceSystemAndSourcePostId(WordPressContentImportService.SOURCE_SYSTEM, wpId).orElseThrow();
        assertThat(mapping.getTargetContentId()).isNull();
        assertThat(mapping.getMigrationState()).isEqualTo(WordPressMigrationState.BLOCKED);
    }

    @Test
    void reimportingTheSamePostIsIdempotentAndCreatesNoDuplicateRows() {
        long wpId = 90005L;
        WordPressPostRow post = post(wpId, "publish", "Idempotency Check", """
                <!-- wp:paragraph -->
                <p>Stable content across reruns.</p>
                <!-- /wp:paragraph -->
                """);
        WordPressImportOutcome first = importService.importPost(request(post, WordPressDataset.builder().build()));
        long postsAfterFirst = contentPostRepository.count();
        long revisionsAfterFirst = revisionRepository.count();

        WordPressImportOutcome second = importService.importPost(request(post, WordPressDataset.builder().build()));

        assertThat(second.skippedIdempotent()).isTrue();
        assertThat(second.targetContentId()).isEqualTo(first.targetContentId());
        assertThat(second.migrationState()).isEqualTo(first.migrationState());
        assertThat(contentPostRepository.count()).isEqualTo(postsAfterFirst);
        assertThat(revisionRepository.count()).isEqualTo(revisionsAfterFirst);
        assertThat(mappingRepository.findBySourceSystemAndSourcePostId(WordPressContentImportService.SOURCE_SYSTEM, wpId))
                .isPresent();
    }

    @Test
    void changedFingerprintOnRerunFailsSafelyInsteadOfSilentlyOverwriting() {
        long wpId = 90006L;
        WordPressPostRow original = post(wpId, "publish", "Original Title", """
                <!-- wp:paragraph -->
                <p>Original content.</p>
                <!-- /wp:paragraph -->
                """);
        importService.importPost(request(original, WordPressDataset.builder().build()));

        WordPressPostRow changed = new WordPressPostRow(
                wpId, original.postAuthor(), original.postDate(), original.postDateGmt(),
                "2026-09-18 00:00:00", "<!-- wp:paragraph --><p>Materially different content.</p><!-- /wp:paragraph -->",
                original.postTitle(), original.postExcerpt(), original.postStatus(), original.postName(),
                original.postParent(), original.postType(), original.postMimeType(), original.guid()
        );

        assertThatThrownBy(() -> importService.importPost(request(changed, WordPressDataset.builder().build())))
                .isInstanceOf(WordPressImportException.class);
    }

    /**
     * v1 of the fingerprint only hashed {@code postContent + postModifiedGmt}. Each case here
     * changes exactly one other field a source-side edit could make and proves it is still
     * detected - i.e. never a false idempotent skip. See
     * {@link WordPressContentImportService#FINGERPRINT_FORMAT_VERSION}'s doc for why this
     * matters: a field left out of the basis is a silently-lost edit on every future rerun, not a
     * one-time bug.
     */
    @Test
    void titleChangeIsDetectedOnRerun() {
        long wpId = 90101L;
        WordPressPostRow original = post(wpId, "publish", "Original Title", stableBody());
        importService.importPost(request(original, WordPressDataset.builder().build()));

        WordPressPostRow changed = withTitle(original, "A Materially Different Title");
        assertThatThrownBy(() -> importService.importPost(request(changed, WordPressDataset.builder().build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void slugChangeIsDetectedOnRerun() {
        long wpId = 90102L;
        WordPressPostRow original = post(wpId, "publish", "Slug Test", stableBody());
        importService.importPost(request(original, WordPressDataset.builder().build()));

        WordPressPostRow changed = new WordPressPostRow(
                original.id(), original.postAuthor(), original.postDate(), original.postDateGmt(),
                original.postModifiedGmt(), original.postContent(), original.postTitle(), original.postExcerpt(),
                original.postStatus(), "a-materially-different-slug", original.postParent(), original.postType(),
                original.postMimeType(), original.guid()
        );
        assertThatThrownBy(() -> importService.importPost(request(changed, WordPressDataset.builder().build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void authorChangeIsDetectedOnRerun() {
        long wpId = 90103L;
        WordPressPostRow original = post(wpId, "publish", "Author Test", stableBody());
        importService.importPost(request(original, WordPressDataset.builder().build()));

        WordPressPostRow changed = new WordPressPostRow(
                original.id(), 99L, original.postDate(), original.postDateGmt(),
                original.postModifiedGmt(), original.postContent(), original.postTitle(), original.postExcerpt(),
                original.postStatus(), original.postName(), original.postParent(), original.postType(),
                original.postMimeType(), original.guid()
        );
        assertThatThrownBy(() -> importService.importPost(request(changed, WordPressDataset.builder().build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void statusChangeIsDetectedOnRerunEvenWithIdenticalBody() {
        long wpId = 90104L;
        WordPressPostRow original = post(wpId, "draft", "Status Test", stableBody());
        importService.importPost(request(original, WordPressDataset.builder().build()));

        WordPressPostRow changed = new WordPressPostRow(
                original.id(), original.postAuthor(), original.postDate(), original.postDateGmt(),
                original.postModifiedGmt(), original.postContent(), original.postTitle(), original.postExcerpt(),
                "publish", original.postName(), original.postParent(), original.postType(),
                original.postMimeType(), original.guid()
        );
        assertThatThrownBy(() -> importService.importPost(request(changed, WordPressDataset.builder().build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void postDateChangeIsDetectedEvenWhenModifiedGmtIsUnchanged() {
        long wpId = 90105L;
        WordPressPostRow original = post(wpId, "publish", "Date Test", stableBody());
        importService.importPost(request(original, WordPressDataset.builder().build()));

        WordPressPostRow changed = new WordPressPostRow(
                original.id(), original.postAuthor(), "2026-03-15 00:00:00", original.postDateGmt(),
                original.postModifiedGmt(), original.postContent(), original.postTitle(), original.postExcerpt(),
                original.postStatus(), original.postName(), original.postParent(), original.postType(),
                original.postMimeType(), original.guid()
        );
        assertThatThrownBy(() -> importService.importPost(request(changed, WordPressDataset.builder().build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void excerptChangeIsDetectedOnRerun() {
        long wpId = 90106L;
        WordPressPostRow original = new WordPressPostRow(
                wpId, 1L, "2026-01-01 00:00:00", "2026-01-01 00:00:00", "2026-01-01 00:00:00",
                stableBody(), "Excerpt Test", "Original excerpt.", "publish", "wp-import-test-" + wpId,
                null, "post", "", "https://example.com/?p=" + wpId
        );
        importService.importPost(request(original, WordPressDataset.builder().build()));

        WordPressPostRow changed = new WordPressPostRow(
                original.id(), original.postAuthor(), original.postDate(), original.postDateGmt(),
                original.postModifiedGmt(), original.postContent(), original.postTitle(), "A materially different excerpt.",
                original.postStatus(), original.postName(), original.postParent(), original.postType(),
                original.postMimeType(), original.guid()
        );
        assertThatThrownBy(() -> importService.importPost(request(changed, WordPressDataset.builder().build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void featuredAttachmentChangeIsDetectedOnRerunEvenWithIdenticalBody() {
        long wpId = 90107L;
        WordPressPostRow post = post(wpId, "publish", "Featured Image Test", stableBody());

        WordPressDataset.Builder firstRunDataset = WordPressDataset.builder();
        firstRunDataset.onPostMeta(new WordPressPostMetaRow(1L, wpId, "_thumbnail_id", "481"));
        importService.importPost(request(post, firstRunDataset.build()));

        WordPressDataset.Builder secondRunDataset = WordPressDataset.builder();
        secondRunDataset.onPostMeta(new WordPressPostMetaRow(1L, wpId, "_thumbnail_id", "482"));
        assertThatThrownBy(() -> importService.importPost(request(post, secondRunDataset.build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void categoryReassignmentIsDetectedOnRerunEvenWithIdenticalBody() {
        long wpId = 90108L;
        WordPressPostRow post = post(wpId, "publish", "Category Reassignment Test", stableBody());

        WordPressDataset.Builder firstRunDataset = WordPressDataset.builder();
        firstRunDataset.onTerm(new WordPressTermRow(1L, "Interview", "interview"));
        firstRunDataset.onTermTaxonomy(new WordPressTermTaxonomyRow(1L, 1L, "category", null));
        firstRunDataset.onTermRelationship(new WordPressTermRelationshipRow(wpId, 1L));
        importService.importPost(request(post, firstRunDataset.build()));

        WordPressDataset.Builder secondRunDataset = WordPressDataset.builder();
        secondRunDataset.onTerm(new WordPressTermRow(2L, "Market Update", "market-update"));
        secondRunDataset.onTermTaxonomy(new WordPressTermTaxonomyRow(2L, 2L, "category", null));
        secondRunDataset.onTermRelationship(new WordPressTermRelationshipRow(wpId, 2L));
        assertThatThrownBy(() -> importService.importPost(request(post, secondRunDataset.build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void tagReassignmentIsDetectedOnRerunEvenWithIdenticalBody() {
        long wpId = 90109L;
        WordPressPostRow post = post(wpId, "publish", "Tag Reassignment Test", stableBody());

        WordPressDataset.Builder firstRunDataset = WordPressDataset.builder();
        firstRunDataset.onTerm(new WordPressTermRow(3L, "Gurgaon", "gurgaon"));
        firstRunDataset.onTermTaxonomy(new WordPressTermTaxonomyRow(3L, 3L, "post_tag", null));
        firstRunDataset.onTermRelationship(new WordPressTermRelationshipRow(wpId, 3L));
        importService.importPost(request(post, firstRunDataset.build()));

        WordPressDataset.Builder secondRunDataset = WordPressDataset.builder();
        secondRunDataset.onTerm(new WordPressTermRow(4L, "Noida", "noida"));
        secondRunDataset.onTermTaxonomy(new WordPressTermTaxonomyRow(4L, 4L, "post_tag", null));
        secondRunDataset.onTermRelationship(new WordPressTermRelationshipRow(wpId, 4L));
        assertThatThrownBy(() -> importService.importPost(request(post, secondRunDataset.build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void rankMathSeoMetadataChangeIsDetectedOnRerunEvenWithIdenticalBody() {
        long wpId = 90110L;
        WordPressPostRow post = post(wpId, "publish", "SEO Metadata Test", stableBody());

        WordPressDataset.Builder firstRunDataset = WordPressDataset.builder();
        firstRunDataset.onPostMeta(new WordPressPostMetaRow(1L, wpId, "rank_math_title", "Original SEO Title"));
        firstRunDataset.onPostMeta(new WordPressPostMetaRow(2L, wpId, "rank_math_description", "Original SEO description."));
        importService.importPost(request(post, firstRunDataset.build()));

        WordPressDataset.Builder secondRunDataset = WordPressDataset.builder();
        secondRunDataset.onPostMeta(new WordPressPostMetaRow(1L, wpId, "rank_math_title", "A Materially Different SEO Title"));
        secondRunDataset.onPostMeta(new WordPressPostMetaRow(2L, wpId, "rank_math_description", "Original SEO description."));
        assertThatThrownBy(() -> importService.importPost(request(post, secondRunDataset.build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void elementorEditModeChangeIsDetectedOnRerunEvenWithIdenticalBody() {
        long wpId = 90111L;
        WordPressPostRow post = post(wpId, "publish", "Elementor Toggle Test", stableBody());

        WordPressDataset.Builder firstRunDataset = WordPressDataset.builder();
        firstRunDataset.onPostMeta(new WordPressPostMetaRow(1L, wpId, "_elementor_edit_mode", "builder"));
        importService.importPost(request(post, firstRunDataset.build()));

        WordPressDataset.Builder secondRunDataset = WordPressDataset.builder();
        secondRunDataset.onPostMeta(new WordPressPostMetaRow(1L, wpId, "_elementor_edit_mode", ""));
        assertThatThrownBy(() -> importService.importPost(request(post, secondRunDataset.build())))
                .isInstanceOf(WordPressImportException.class);
    }

    @Test
    void categoryOrderingAloneNeverChangesTheFingerprint() {
        long wpId = 90112L;
        WordPressPostRow post = post(wpId, "publish", "Category Order Stability Test", stableBody());

        WordPressDataset.Builder firstRunDataset = WordPressDataset.builder();
        firstRunDataset.onTerm(new WordPressTermRow(5L, "Alpha", "alpha"));
        firstRunDataset.onTerm(new WordPressTermRow(6L, "Beta", "beta"));
        firstRunDataset.onTermTaxonomy(new WordPressTermTaxonomyRow(5L, 5L, "category", null));
        firstRunDataset.onTermTaxonomy(new WordPressTermTaxonomyRow(6L, 6L, "category", null));
        firstRunDataset.onTermRelationship(new WordPressTermRelationshipRow(wpId, 5L));
        firstRunDataset.onTermRelationship(new WordPressTermRelationshipRow(wpId, 6L));
        WordPressImportOutcome first = importService.importPost(request(post, firstRunDataset.build()));

        // Same two categories, relationships inserted in the opposite order - a rerun must still
        // be recognized as unchanged (deterministic sort before hashing), not misfire as a change.
        WordPressDataset.Builder secondRunDataset = WordPressDataset.builder();
        secondRunDataset.onTerm(new WordPressTermRow(5L, "Alpha", "alpha"));
        secondRunDataset.onTerm(new WordPressTermRow(6L, "Beta", "beta"));
        secondRunDataset.onTermTaxonomy(new WordPressTermTaxonomyRow(5L, 5L, "category", null));
        secondRunDataset.onTermTaxonomy(new WordPressTermTaxonomyRow(6L, 6L, "category", null));
        secondRunDataset.onTermRelationship(new WordPressTermRelationshipRow(wpId, 6L));
        secondRunDataset.onTermRelationship(new WordPressTermRelationshipRow(wpId, 5L));
        WordPressImportOutcome second = importService.importPost(request(post, secondRunDataset.build()));

        assertThat(second.skippedIdempotent()).isTrue();
        assertThat(second.targetContentId()).isEqualTo(first.targetContentId());
    }

    @Test
    void imageBlockAttachmentIdIsRewrittenToTheRealMediaAssetIdNeverLeakingTheWordPressId() {
        long wpId = 90007L;
        long wpAttachmentId = 481L;
        CmsMediaAssetEntity media = mediaAssetRepository.saveAndFlush(CmsMediaAssetEntity.builder()
                .mediaType(CmsMediaType.IMAGE).status(CmsMediaStatus.READY)
                .storageBucket("private-test").storageKey("wp-import-test/" + UUID.randomUUID() + ".jpg")
                .originalFilename("skyline.jpg").contentType("image/jpeg")
                .declaredSizeBytes(1024L).sizeBytes(1024L).width(1200).height(630)
                .createdBy(actor).readyAt(OffsetDateTime.now()).build());

        WordPressPostRow post = post(wpId, "publish", "Post With A Real Image", """
                <!-- wp:image {"id":481,"sizeSlug":"large"} -->
                <figure class="wp-block-image size-large"><img src="https://squarefootstory.com/x.jpg" alt="Skyline" class="wp-image-481"/><figcaption>Gurgaon skyline</figcaption></figure>
                <!-- /wp:image -->
                """);
        WordPressImportRequest request = request(post, WordPressDataset.builder().build(), Map.of(wpAttachmentId, media.getId()));
        WordPressImportOutcome outcome = importService.importPost(request);

        ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
        ContentBlock.Image image = (ContentBlock.Image) saved.getContentDocument().blocks().get(0);
        assertThat(image.mediaAssetId()).isEqualTo(media.getId());
        assertThat(image.mediaAssetId()).isNotEqualTo(wpAttachmentId);
    }

    /**
     * Regression test for a real bug the full-144-post corpus run surfaced: an IMAGE nested
     * inside a LAYOUT block (WordPress "columns" of images) was never rewritten at all - the raw
     * WordPress attachment ID leaked straight into the persisted, published document, which then
     * failed public API media validation ("Referenced CMS media asset was not found").
     */
    @Test
    void imageNestedInsideALayoutBlockIsRewrittenTheSameAsATopLevelImage() {
        long wpId = 90009L;
        long resolvedAttachmentId = 601L;
        // Attachment 602 (the second column's image) is deliberately left unresolved.
        CmsMediaAssetEntity media = mediaAssetRepository.saveAndFlush(CmsMediaAssetEntity.builder()
                .mediaType(CmsMediaType.IMAGE).status(CmsMediaStatus.READY)
                .storageBucket("private-test").storageKey("wp-import-test/" + UUID.randomUUID() + ".jpg")
                .originalFilename("tower-a.jpg").contentType("image/jpeg")
                .declaredSizeBytes(1024L).sizeBytes(1024L).width(1200).height(630)
                .createdBy(actor).readyAt(OffsetDateTime.now()).build());

        WordPressPostRow post = post(wpId, "publish", "Post With A Two-Column Image Layout", """
                <!-- wp:columns -->
                <div class="wp-block-columns"><!-- wp:column -->
                <div class="wp-block-column"><!-- wp:image {"id":601} -->
                <figure><img src="a.jpg" alt="Tower A" class="wp-image-601"/></figure>
                <!-- /wp:image --></div>
                <!-- /wp:column -->

                <!-- wp:column -->
                <div class="wp-block-column"><!-- wp:image {"id":602} -->
                <figure><img src="b.jpg" alt="Tower B" class="wp-image-602"/></figure>
                <!-- /wp:image --></div>
                <!-- /wp:column --></div>
                <!-- /wp:columns -->
                """);
        WordPressImportRequest request = request(
                post, WordPressDataset.builder().build(), Map.of(resolvedAttachmentId, media.getId())
        );

        WordPressImportOutcome outcome = importService.importPost(request);

        // The unresolved second image forces manual review - never auto-published with lost media.
        assertThat(outcome.migrationState()).isEqualTo(WordPressMigrationState.NEEDS_REVIEW);
        ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
        ContentBlock.Layout layout = (ContentBlock.Layout) saved.getContentDocument().blocks().get(0);
        assertThat(layout.children()).hasSize(1);
        ContentBlock.Image remaining = (ContentBlock.Image) layout.children().get(0);
        assertThat(remaining.mediaAssetId())
                .as("the resolved image's WordPress attachment ID must be rewritten to the real CMS media ID")
                .isEqualTo(media.getId());
        assertThat(mediaAssetRepository.findById(remaining.mediaAssetId())).isPresent();
    }

    @Test
    void unresolvedMediaReferenceIsNeverFabricatedButDropsTheBlockAndForcesManualReview() {
        long wpId = 90008L;
        WordPressPostRow post = post(wpId, "publish", "Post With Unresolved Media", """
                <!-- wp:paragraph --><p>Real text that survives.</p><!-- /wp:paragraph -->

                <!-- wp:image {"id":999999,"sizeSlug":"large"} -->
                <figure class="wp-block-image size-large"><img src="https://squarefootstory.com/x.jpg" alt="Skyline" class="wp-image-999999"/></figure>
                <!-- /wp:image -->
                """);
        WordPressImportRequest request = request(post, WordPressDataset.builder().build(), Map.of());

        WordPressImportOutcome outcome = importService.importPost(request);

        assertThat(outcome.migrationState()).isEqualTo(WordPressMigrationState.NEEDS_REVIEW);
        ContentPostEntity saved = contentPostRepository.findById(outcome.targetContentId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(saved.getContentDocument().blocks())
                .as("the unresolvable image block must be dropped, never fabricated with an invented ID")
                .noneMatch(block -> block instanceof ContentBlock.Image);
        assertThat(saved.getContentDocument().blocks())
                .as("the rest of the document must survive")
                .anyMatch(block -> block instanceof ContentBlock.Paragraph);
    }

    @Test
    void categoryAndTagResolutionIsIdempotentAcrossTwoDifferentPostsSharingATaxonomy() {
        WordPressDataset.Builder builder = WordPressDataset.builder();
        builder.onTerm(new WordPressTermRow(1L, "Interview", "interview"));
        builder.onTermTaxonomy(new WordPressTermTaxonomyRow(1L, 1L, "category", null));

        long wpIdA = 90010L;
        long wpIdB = 90011L;
        builder.onTermRelationship(new WordPressTermRelationshipRow(wpIdA, 1L));
        builder.onTermRelationship(new WordPressTermRelationshipRow(wpIdB, 1L));
        WordPressDataset dataset = builder.build();

        WordPressPostRow postA = post(wpIdA, "publish", "Interview One", """
                <!-- wp:paragraph --><p>Interview content A.</p><!-- /wp:paragraph -->
                """);
        WordPressPostRow postB = post(wpIdB, "publish", "Interview Two", """
                <!-- wp:paragraph --><p>Interview content B.</p><!-- /wp:paragraph -->
                """);

        WordPressImportOutcome outcomeA = importService.importPost(request(postA, dataset));
        WordPressImportOutcome outcomeB = importService.importPost(request(postB, dataset));

        ContentPostEntity savedA = contentPostRepository.findById(outcomeA.targetContentId()).orElseThrow();
        ContentPostEntity savedB = contentPostRepository.findById(outcomeB.targetContentId()).orElseThrow();
        assertThat(savedA.getCategory()).isNotNull();
        assertThat(savedA.getCategory().getId()).isEqualTo(savedB.getCategory().getId());
    }

    private String stableBody() {
        return """
                <!-- wp:paragraph -->
                <p>Stable content shared by every fingerprint-field test.</p>
                <!-- /wp:paragraph -->
                """;
    }

    private WordPressPostRow withTitle(WordPressPostRow post, String title) {
        return new WordPressPostRow(
                post.id(), post.postAuthor(), post.postDate(), post.postDateGmt(), post.postModifiedGmt(),
                post.postContent(), title, post.postExcerpt(), post.postStatus(), post.postName(),
                post.postParent(), post.postType(), post.postMimeType(), post.guid()
        );
    }

    private WordPressPostRow post(long id, String status, String title, String content) {
        return new WordPressPostRow(
                id, 1L, "2026-01-01 00:00:00", "2026-01-01 00:00:00", "2026-01-01 00:00:00",
                content, title, "", status, "wp-import-test-" + id, null, "post", "", "https://example.com/?p=" + id
        );
    }

    private WordPressImportRequest request(WordPressPostRow post, WordPressDataset dataset) {
        return request(post, dataset, Map.of());
    }

    private WordPressImportRequest request(WordPressPostRow post, WordPressDataset dataset, Map<Long, Long> resolvedMediaAssetIds) {
        GutenbergConversionResult conversion = new GutenbergDocumentConverter()
                .convert(post.id(), post.postTitle(), post.postContent());
        return new WordPressImportRequest(post, dataset, conversion, resolvedMediaAssetIds, actor);
    }
}
