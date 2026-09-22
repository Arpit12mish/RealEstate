package com.brandPitara.sfs.migration.wordpress.media;

import com.brandPitara.sfs.SfsApplication;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hermetic Testcontainers coverage for the real media-upload lifecycle
 * ({@link WordPressMediaImportService}), using {@link InMemoryWordPressMigrationMediaStorage} in
 * place of {@link S3WordPressMigrationMediaStorage} - never real AWS. Every test that reaches
 * storage implicitly proves the transaction-boundary requirement, since the fake asserts
 * {@code TransactionSynchronizationManager.isActualTransactionActive() == false} on every call.
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
                "jwt.secret=wp-media-test-mobile-secret-wp-media-test-mobile-secret",
                "dashboard.jwt.secret=wp-media-test-dashboard-secret-wp-media-test-dashboard-secret",
                "dashboard.seed.enabled=false",
                "sfs.search.enabled=false",
                "google.maps.places.enabled=false",
                "sfs.instagram.meta.sync-enabled=false",
                "sfs.log.dir=target/test-logs",
                "app.cms.media.public-delivery.base-url=https://media.example.test",
                "app.cms.media.bucket=test-cms-media-bucket"
        }
)
@Import(WordPressMediaImportServiceTest.FakeStorageConfig.class)
@ActiveProfiles({"test", "local-staging"})
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WordPressMediaImportServiceTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sfs_wordpress_media_import_test")
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

    // CmsMediaValidator.jpegDimensions requires at least 12 bytes (its scan loop checks p+9 < length
    // with p starting at 2) - an 11-byte buffer looks plausible but is silently rejected as corrupt.
    private static final byte[] MINIMAL_JPEG_100X50 = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xC0, 0x00, 0x07, 0x08, 0x00, 0x32, 0x00, 0x64, 0x00
    };
    private static final byte[] MINIMAL_PNG_100X50 = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x64, 0x00, 0x00, 0x00, 0x32
    };
    private static final byte[] MINIMAL_MP4 = {
            0x00, 0x00, 0x00, 0x10, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6F, 0x6D, 0x00, 0x00, 0x02, 0x00
    };

    /**
     * Object keys are content-addressed (see {@link WordPressMediaObjectKeyFactory}), and the
     * database is shared - not rolled back - across every test method in this class, so two
     * different tests must never coincidentally upload byte-identical content or they collide on
     * the same key against leftover rows from an earlier test. Each test that isn't specifically
     * testing within-test dedup should call this for a fresh, unique image. The trailing tag is
     * never read by {@code CmsMediaValidator.jpegDimensions} (it returns as soon as it decodes the
     * fixed-size SOF0 header, before reaching these extra bytes).
     */
    private byte[] uniqueJpeg() {
        byte[] base = MINIMAL_JPEG_100X50;
        byte[] tag = UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] unique = java.util.Arrays.copyOf(base, base.length + tag.length);
        System.arraycopy(tag, 0, unique, base.length, tag.length);
        return unique;
    }

    @Autowired
    private WordPressMediaImportService importService;
    @Autowired
    private WordPressMediaFinalizationService finalizationService;
    @Autowired
    private WordPressMediaMappingRepository mappingRepository;
    @Autowired
    private CmsMediaAssetRepository mediaAssetRepository;
    @Autowired
    private DashboardUserRepository dashboardUserRepository;
    @Autowired
    private WordPressMigrationMediaStorage storage;
    @Autowired
    private WordPressMediaOrphanCleanupReport orphanReport;

    private InMemoryWordPressMigrationMediaStorage fakeStorage;
    private DashboardUserEntity actor;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        fakeStorage = (InMemoryWordPressMigrationMediaStorage) storage;
        fakeStorage.reset();
        actor = dashboardUserRepository.saveAndFlush(DashboardUserEntity.builder()
                .name("Media Migration Runner")
                .email("media-migration-runner-" + UUID.randomUUID() + "@example.com")
                .passwordHash("encoded")
                .role(DashboardRole.CONTENT_STAFF)
                .active(true)
                .build());
        tempDir = Files.createTempDirectory("wp-media-import-test-");
    }

    @Test
    void successfulImageUploadTransitionsToReadyWithStoredBytesMatchingSource() throws IOException {
        byte[] jpeg = uniqueJpeg();
        Path zip = buildZip(Map.of("2026/05/skyline.jpg", jpeg));
        WordPressMediaImportOutcome outcome = importService.importAttachment(
                request(501L, zip, "2026/05/skyline.jpg"));

        assertThat(outcome.errorCode()).isNull();
        assertThat(outcome.cmsMediaAssetId()).isNotNull();
        assertThat(outcome.objectKey()).startsWith("cms/images/wordpress/");

        CmsMediaAssetEntity asset = mediaAssetRepository.findById(outcome.cmsMediaAssetId()).orElseThrow();
        assertThat(asset.getStatus()).isEqualTo(CmsMediaStatus.READY);
        assertThat(asset.getWidth()).isEqualTo(100);
        assertThat(asset.getHeight()).isEqualTo(50);

        byte[] stored = fakeStorage.storedBytes("test-cms-media-bucket", outcome.objectKey());
        assertThat(stored).isEqualTo(jpeg);
        assertThat(fakeStorage.storeInvocationCount()).isEqualTo(1);
    }

    @Test
    void pngImageDecodesRealDimensionsFromItsOwnBytes() throws IOException {
        Path zip = buildZip(Map.of("2026/05/banner.png", MINIMAL_PNG_100X50));
        WordPressMediaImportOutcome outcome = importService.importAttachment(
                request(519L, zip, "2026/05/banner.png"));

        CmsMediaAssetEntity asset = mediaAssetRepository.findById(outcome.cmsMediaAssetId()).orElseThrow();
        assertThat(asset.getWidth()).isEqualTo(100);
        assertThat(asset.getHeight()).isEqualTo(50);
        assertThat(fakeStorage.storedBytes("test-cms-media-bucket", outcome.objectKey())).isEqualTo(MINIMAL_PNG_100X50);
    }

    @Test
    void managedVideoKeyIsUsedAndVideoIsAcceptedWithoutDimensionDecoding() throws IOException {
        Path zip = buildZip(Map.of("2026/05/walkthrough.mp4", MINIMAL_MP4));
        WordPressMediaImportOutcome outcome = importService.importAttachment(
                request(502L, zip, "2026/05/walkthrough.mp4"));

        assertThat(outcome.objectKey()).startsWith("cms/videos/wordpress/");
        CmsMediaAssetEntity asset = mediaAssetRepository.findById(outcome.cmsMediaAssetId()).orElseThrow();
        assertThat(asset.getStatus()).isEqualTo(CmsMediaStatus.READY);
        assertThat(asset.getWidth()).isNull();
    }

    @Test
    void repeatedAttachmentReferenceReusesTheSameCmsMediaIdWithoutReupload() throws IOException {
        Path zip = buildZip(Map.of("2026/05/skyline.jpg", uniqueJpeg()));
        WordPressMediaImportRequest req = request(503L, zip, "2026/05/skyline.jpg");

        WordPressMediaImportOutcome first = importService.importAttachment(req);
        WordPressMediaImportOutcome second = importService.importAttachment(req);

        assertThat(second.cmsMediaAssetId()).isEqualTo(first.cmsMediaAssetId());
        assertThat(second.reusedExistingMapping()).isTrue();
        assertThat(fakeStorage.storeInvocationCount()).isEqualTo(1);
        assertThat(mappingRepository.findBySourceSystemAndWordPressAttachmentId(
                WordPressMediaImportService.SOURCE_SYSTEM, 503L)).isPresent();
    }

    @Test
    void twoDistinctAttachmentIdsWithIdenticalBytesShareOneCmsMediaRowAndOneStoredObject() throws IOException {
        byte[] jpeg = uniqueJpeg();
        Path zip = buildZip(Map.of(
                "2026/05/a.jpg", jpeg,
                "2026/06/b.jpg", jpeg
        ));
        WordPressMediaImportOutcome outcomeA = importService.importAttachment(request(504L, zip, "2026/05/a.jpg"));
        WordPressMediaImportOutcome outcomeB = importService.importAttachment(request(505L, zip, "2026/06/b.jpg"));

        assertThat(outcomeA.objectKey()).isEqualTo(outcomeB.objectKey());
        assertThat(outcomeA.cmsMediaAssetId()).isEqualTo(outcomeB.cmsMediaAssetId());
        assertThat(fakeStorage.storeInvocationCount()).isEqualTo(1);
        assertThat(fakeStorage.objectCount()).isEqualTo(1);
    }

    @Test
    void uploadFailureLeavesNoReadyRowAndRecordsAFailedMapping() throws IOException {
        Path zip = buildZip(Map.of("2026/05/skyline.jpg", uniqueJpeg()));
        fakeStorage.failNextStore(new RuntimeException("simulated S3 outage"));

        assertThatThrownBy(() -> importService.importAttachment(request(506L, zip, "2026/05/skyline.jpg")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("UPLOAD_FAILED");

        assertThat(fakeStorage.objectCount()).isZero();
        var mapping = mappingRepository.findBySourceSystemAndWordPressAttachmentId(
                WordPressMediaImportService.SOURCE_SYSTEM, 506L).orElseThrow();
        assertThat(mapping.getMigrationState()).isEqualTo(WordPressMediaMigrationState.FAILED);
        assertThat(mapping.getErrorCode()).isEqualTo("UPLOAD_FAILED");
        assertThat(mapping.getCmsMediaAsset()).isNull();
    }

    @Test
    void verificationFailureAfterUploadDeletesTheOrphanAndLeavesNoReadyRow() throws IOException {
        Path zip = buildZip(Map.of("2026/05/skyline.jpg", uniqueJpeg()));
        fakeStorage.failNextHead(new WordPressMediaStorageObjectNotFoundException("test-cms-media-bucket", "whatever"));

        assertThatThrownBy(() -> importService.importAttachment(request(507L, zip, "2026/05/skyline.jpg")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("STORED_OBJECT_VERIFICATION_FAILED");

        // The object WAS written by store() before head() was made to fail, so it must have been cleaned up.
        assertThat(fakeStorage.objectCount()).isZero();
        var mapping = mappingRepository.findBySourceSystemAndWordPressAttachmentId(
                WordPressMediaImportService.SOURCE_SYSTEM, 507L).orElseThrow();
        assertThat(mapping.getMigrationState()).isEqualTo(WordPressMediaMigrationState.FAILED);
        assertThat(mapping.getCmsMediaAsset()).isNull();
        assertThat(orphanReport.entries()).noneMatch(e -> e.attachmentId() == 507L);
    }

    @Test
    void checksumMismatchReportedByStorageFailsTheImportAndCleansUp() throws IOException {
        Path zip = buildZip(Map.of("2026/05/skyline.jpg", uniqueJpeg()));
        fakeStorage.corruptNextStoredChecksum();

        assertThatThrownBy(() -> importService.importAttachment(request(508L, zip, "2026/05/skyline.jpg")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("CHECKSUM_MISMATCH");
        assertThat(mappingRepository.findBySourceSystemAndWordPressAttachmentId(
                WordPressMediaImportService.SOURCE_SYSTEM, 508L).orElseThrow().getCmsMediaAsset()).isNull();
    }

    @Test
    void dbFinalizationFailureAfterUploadRecordsFailureAndCleansUpTheOrphan() throws IOException {
        Path zip = buildZip(Map.of("2026/05/skyline.jpg", uniqueJpeg()));
        DashboardUserEntity ghost = DashboardUserEntity.builder()
                .name("Ghost").email("ghost-" + UUID.randomUUID() + "@example.com")
                .passwordHash("x").role(DashboardRole.CONTENT_STAFF).active(true).build();
        ghost.setId(999_999_999L);

        WordPressMediaImportRequest req = new WordPressMediaImportRequest(509L, zip, "2026/05/skyline.jpg", tempDir, ghost);
        assertThatThrownBy(() -> importService.importAttachment(req))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("DB_FINALIZATION_FAILED");

        assertThat(fakeStorage.objectCount()).isZero();
        var mapping = mappingRepository.findBySourceSystemAndWordPressAttachmentId(
                WordPressMediaImportService.SOURCE_SYSTEM, 509L).orElseThrow();
        assertThat(mapping.getMigrationState()).isEqualTo(WordPressMediaMigrationState.FAILED);
        assertThat(mapping.getErrorCode()).isEqualTo("DB_FINALIZATION_FAILED");
        assertThat(mapping.getCmsMediaAsset()).isNull();
    }

    @Test
    void objectNotCreatedByThisOperationIsRecordedInTheOrphanReportRatherThanDeleted() throws IOException {
        long attachmentId = 520L;
        String bucket = "test-cms-media-bucket";
        byte[] jpeg = uniqueJpeg();
        Path zip = buildZip(Map.of("2026/05/skyline.jpg", jpeg));
        String key = WordPressMediaObjectKeyFactory.build(
                com.brandPitara.sfs.cms.media.domain.CmsMediaType.IMAGE, "image/jpeg",
                sha256Hex(jpeg), java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
        );
        // Simulate a leftover object from a previous crashed run: present in storage, but with no
        // CmsMediaAssetEntity row yet - this run must not treat it as "created by this operation".
        fakeStorage.seed(bucket, key, jpeg, "image/jpeg");
        fakeStorage.failNextHead(new RuntimeException("simulated transient storage failure"));

        assertThatThrownBy(() -> importService.importAttachment(request(attachmentId, zip, "2026/05/skyline.jpg")))
                .isInstanceOf(WordPressMediaImportException.class);

        assertThat(fakeStorage.storeInvocationCount()).isZero();
        assertThat(orphanReport.entries())
                .anyMatch(e -> e.attachmentId() == attachmentId && e.reason().startsWith("not-created-by-this-operation"));
        assertThat(fakeStorage.hasObject(bucket, key)).isTrue();
    }

    /**
     * Reproduces the race {@code recordFailureAndMaybeCleanup}'s re-check exists for, without real
     * threads: content-addressed keys mean a DIFFERENT attachment with byte-identical bytes can
     * finish its own full store+verify+finalize flow in the narrow window between THIS operation's
     * own store() and its own verification - {@code runBeforeNextHead} lands that concurrent
     * finalize() at exactly that interleaving point. {@code createAndMarkReady}'s own
     * DataIntegrityViolationException handling only protects the two concurrent DB inserts from
     * each other; it does nothing for a cleanup triggered by this operation's own (unrelated)
     * verification failure, which is exactly what this test would catch a regression of.
     */
    @Test
    void concurrentWinnerAdoptingTheSameKeyIsNeverDeletedByThisOperationsCleanup() throws IOException {
        long attachmentId = 521L;
        byte[] jpeg = uniqueJpeg();
        Path zip = buildZip(Map.of("2026/05/skyline.jpg", jpeg));
        String bucket = "test-cms-media-bucket";
        String key = WordPressMediaObjectKeyFactory.build(
                com.brandPitara.sfs.cms.media.domain.CmsMediaType.IMAGE, "image/jpeg",
                sha256Hex(jpeg), java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
        );

        fakeStorage.runBeforeNextHead(() -> finalizationService.createAndMarkReady(
                com.brandPitara.sfs.cms.media.domain.CmsMediaType.IMAGE, bucket, key,
                "concurrent-winner.jpg", "image/jpeg", jpeg.length, actor, sha256Hex(jpeg),
                new com.brandPitara.sfs.cms.media.validation.CmsMediaValidationResult(100, 50, null)
        ));
        fakeStorage.failNextHead(new WordPressMediaStorageObjectNotFoundException(bucket, key));

        assertThatThrownBy(() -> importService.importAttachment(request(attachmentId, zip, "2026/05/skyline.jpg")))
                .isInstanceOf(WordPressMediaImportException.class);

        assertThat(fakeStorage.hasObject(bucket, key))
                .as("a shared content-addressed object must never be deleted while a concurrent "
                        + "import's READY asset depends on it")
                .isTrue();
        assertThat(orphanReport.entries())
                .anyMatch(e -> e.attachmentId() == attachmentId && e.reason().startsWith("adopted-by-concurrent-import"));

        var mapping = mappingRepository.findBySourceSystemAndWordPressAttachmentId(
                WordPressMediaImportService.SOURCE_SYSTEM, attachmentId).orElseThrow();
        assertThat(mapping.getMigrationState()).isEqualTo(WordPressMediaMigrationState.FAILED);
        assertThat(mapping.getCmsMediaAsset()).isNull();
    }

    @Test
    void existingCompletedMappingWithAMissingObjectFailsSafelyInsteadOfSilentlyReuploading() throws IOException {
        Path zip = buildZip(Map.of("2026/05/skyline.jpg", uniqueJpeg()));
        WordPressMediaImportOutcome first = importService.importAttachment(request(510L, zip, "2026/05/skyline.jpg"));
        fakeStorage.removeQuietly("test-cms-media-bucket", first.objectKey());

        assertThatThrownBy(() -> importService.importAttachment(request(510L, zip, "2026/05/skyline.jpg")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("STORED_OBJECT_MISSING");
    }

    @Test
    void sourceFingerprintChangeOnRerunFailsSafelyRatherThanSilentlyOverwriting() throws IOException {
        Path zipA = buildZip(Map.of("2026/05/skyline.jpg", uniqueJpeg()));
        importService.importAttachment(request(511L, zipA, "2026/05/skyline.jpg"));

        byte[] differentJpeg = {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xC0, 0x00, 0x07, 0x08, 0x00, 0x14, 0x00, 0x28, 0x00
        };
        Path zipB = buildZip(Map.of("2026/05/skyline.jpg", differentJpeg));

        assertThatThrownBy(() -> importService.importAttachment(request(511L, zipB, "2026/05/skyline.jpg")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("SOURCE_FINGERPRINT_CHANGED");
    }

    @Test
    void duplicateZipEntryNameIsRejectedAsAmbiguous() throws IOException {
        Path zip = buildZipWithRawEntries(new String[]{"2026/05/dup.jpg", "2026/05/dup.jpg"},
                new byte[][]{MINIMAL_JPEG_100X50, MINIMAL_JPEG_100X50});

        assertThatThrownBy(() -> importService.importAttachment(request(512L, zip, "2026/05/dup.jpg")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("ZIP_ENTRY_AMBIGUOUS");
    }

    @Test
    void caseInsensitiveZipEntryCollisionIsRejectedAsAmbiguous() throws IOException {
        Path zip = buildZipWithRawEntries(new String[]{"2026/05/Photo.jpg", "2026/05/photo.jpg"},
                new byte[][]{MINIMAL_JPEG_100X50, MINIMAL_JPEG_100X50});

        assertThatThrownBy(() -> importService.importAttachment(request(513L, zip, "2026/05/photo.jpg")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("ZIP_ENTRY_AMBIGUOUS");
    }

    @Test
    void pathTraversalInSourceUploadPathIsRejected() throws IOException {
        Path zip = buildZip(Map.of("2026/05/skyline.jpg", MINIMAL_JPEG_100X50));

        assertThatThrownBy(() -> importService.importAttachment(request(514L, zip, "../../etc/passwd")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("ZIP_EXTRACTION_FAILED");
    }

    @Test
    void corruptImageThatPassesMagicByteSniffingButHasNoDecodableStructureIsRejected() throws IOException {
        byte[] corrupt = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02, 0x03, 0x04, 0x05};
        Path zip = buildZip(Map.of("2026/05/broken.jpg", corrupt));

        assertThatThrownBy(() -> importService.importAttachment(request(515L, zip, "2026/05/broken.jpg")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("CORRUPT_MEDIA");
        assertThat(mappingRepository.findBySourceSystemAndWordPressAttachmentId(
                WordPressMediaImportService.SOURCE_SYSTEM, 515L)).isEmpty();
        assertThat(fakeStorage.storeInvocationCount()).isZero();
    }

    @Test
    void disallowedMimeTypeIsRejectedBeforeAnyUpload() throws IOException {
        byte[] gifBytes = {'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0};
        Path zip = buildZip(Map.of("2026/05/animated.gif", gifBytes));

        assertThatThrownBy(() -> importService.importAttachment(request(516L, zip, "2026/05/animated.gif")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("DISALLOWED_MIME_TYPE");
        assertThat(fakeStorage.storeInvocationCount()).isZero();
    }

    @Test
    void extensionMimeMismatchIsRejected() throws IOException {
        // Real JPEG bytes under a ".png" name.
        Path zip = buildZip(Map.of("2026/05/mislabeled.png", MINIMAL_JPEG_100X50));

        assertThatThrownBy(() -> importService.importAttachment(request(517L, zip, "2026/05/mislabeled.png")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("EXTENSION_MIME_MISMATCH");
    }

    @Test
    void sizeLimitIsEnforcedBeforeStoring() throws IOException {
        byte[] oversized = new byte[16 * 1024 * 1024];
        System.arraycopy(MINIMAL_JPEG_100X50, 0, oversized, 0, MINIMAL_JPEG_100X50.length);
        Path zip = buildZip(Map.of("2026/05/huge.jpg", oversized));

        assertThatThrownBy(() -> importService.importAttachment(request(518L, zip, "2026/05/huge.jpg")))
                .isInstanceOf(WordPressMediaImportException.class)
                .extracting(e -> ((WordPressMediaImportException) e).errorCode())
                .isEqualTo("SIZE_LIMIT_EXCEEDED");
        assertThat(fakeStorage.storeInvocationCount()).isZero();
    }

    @Test
    void concurrentMappingRaceReusesTheWinnersRowInsteadOfFailing() {
        String key = "cms/images/wordpress/2026/05/wp-race-test.jpg";
        CmsMediaAssetEntity winner = finalizationService.createAndMarkReady(
                com.brandPitara.sfs.cms.media.domain.CmsMediaType.IMAGE, "test-cms-media-bucket", key,
                "race.jpg", "image/jpeg", 11L, actor, "racehash",
                new com.brandPitara.sfs.cms.media.validation.CmsMediaValidationResult(100, 50, null)
        );

        CmsMediaAssetEntity result = finalizationService.createAndMarkReady(
                com.brandPitara.sfs.cms.media.domain.CmsMediaType.IMAGE, "test-cms-media-bucket", key,
                "race.jpg", "image/jpeg", 11L, actor, "racehash",
                new com.brandPitara.sfs.cms.media.validation.CmsMediaValidationResult(100, 50, null)
        );

        assertThat(result.getId()).isEqualTo(winner.getId());
        assertThat(mediaAssetRepository.findAllByIdIn(java.util.Set.of(winner.getId()))).hasSize(1);
    }

    private WordPressMediaImportRequest request(long attachmentId, Path zip, String sourceUploadPath) {
        return new WordPressMediaImportRequest(attachmentId, zip, sourceUploadPath, tempDir, actor);
    }

    private String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private Path buildZip(Map<String, byte[]> entries) throws IOException {
        Path zip = tempDir.resolve("uploads-" + UUID.randomUUID() + ".zip");
        try (var out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return zip;
    }

    /**
     * {@link ZipOutputStream} refuses a second entry with a name already written
     * ({@code ZipException: duplicate entry}), so a raw PKZIP writer (STORED entries, no data
     * descriptors) is used here to build the deliberately-malformed archives the
     * duplicate/case-insensitive-collision tests need.
     */
    private Path buildZipWithRawEntries(String[] names, byte[][] contents) throws IOException {
        Path zip = tempDir.resolve("uploads-" + UUID.randomUUID() + ".zip");
        try (var out = new java.io.ByteArrayOutputStream()) {
            java.util.List<Integer> localHeaderOffsets = new java.util.ArrayList<>();
            for (int i = 0; i < names.length; i++) {
                localHeaderOffsets.add(out.size());
                writeLocalFileHeader(out, names[i], contents[i]);
            }
            int centralDirectoryStart = out.size();
            for (int i = 0; i < names.length; i++) {
                writeCentralDirectoryRecord(out, names[i], contents[i], localHeaderOffsets.get(i));
            }
            int centralDirectorySize = out.size() - centralDirectoryStart;
            writeEndOfCentralDirectory(out, names.length, centralDirectorySize, centralDirectoryStart);
            Files.write(zip, out.toByteArray());
        }
        return zip;
    }

    private void writeLocalFileHeader(java.io.ByteArrayOutputStream out, String name, byte[] content) throws IOException {
        byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long crc = crc32(content);
        writeLE32(out, 0x04034b50);
        writeLE16(out, 20);
        writeLE16(out, 0);
        writeLE16(out, 0);
        writeLE16(out, 0);
        writeLE16(out, 0);
        writeLE32(out, crc);
        writeLE32(out, content.length);
        writeLE32(out, content.length);
        writeLE16(out, nameBytes.length);
        writeLE16(out, 0);
        out.write(nameBytes);
        out.write(content);
    }

    private void writeCentralDirectoryRecord(
            java.io.ByteArrayOutputStream out, String name, byte[] content, int localHeaderOffset
    ) throws IOException {
        byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long crc = crc32(content);
        writeLE32(out, 0x02014b50);
        writeLE16(out, 20);
        writeLE16(out, 20);
        writeLE16(out, 0);
        writeLE16(out, 0);
        writeLE16(out, 0);
        writeLE16(out, 0);
        writeLE32(out, crc);
        writeLE32(out, content.length);
        writeLE32(out, content.length);
        writeLE16(out, nameBytes.length);
        writeLE16(out, 0);
        writeLE16(out, 0);
        writeLE16(out, 0);
        writeLE16(out, 0);
        writeLE32(out, 0);
        writeLE32(out, localHeaderOffset);
        out.write(nameBytes);
    }

    private void writeEndOfCentralDirectory(
            java.io.ByteArrayOutputStream out, int entryCount, int centralDirectorySize, int centralDirectoryOffset
    ) throws IOException {
        writeLE32(out, 0x06054b50);
        writeLE16(out, 0);
        writeLE16(out, 0);
        writeLE16(out, entryCount);
        writeLE16(out, entryCount);
        writeLE32(out, centralDirectorySize);
        writeLE32(out, centralDirectoryOffset);
        writeLE16(out, 0);
    }

    private long crc32(byte[] content) {
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        crc32.update(content);
        return crc32.getValue();
    }

    private void writeLE16(java.io.ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
    }

    private void writeLE32(java.io.ByteArrayOutputStream out, long value) {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 24) & 0xFF));
    }
}
