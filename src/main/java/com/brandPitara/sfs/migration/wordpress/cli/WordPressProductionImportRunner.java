package com.brandPitara.sfs.migration.wordpress.cli;

import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.migration.wordpress.audit.PostClassification;
import com.brandPitara.sfs.migration.wordpress.audit.WordPressDataset;
import com.brandPitara.sfs.migration.wordpress.audit.WordPressPostClassifier;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressDumpReader;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergConversionResult;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergDocumentConverter;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressContentImportService;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressImportOutcome;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressImportRequest;
import com.brandPitara.sfs.migration.wordpress.importer.WordPressMigrationMappingRepository;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaExtractor;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaImportOutcome;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaImportRequest;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaImportService;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaMappingRepository;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaMigrationState;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMediaObjectKeyFactory;
import com.brandPitara.sfs.migration.wordpress.media.WordPressMigrationMediaProperties;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The actual orchestration logic behind {@link WordPressProductionImportCli} - deliberately a
 * plain class (not a Spring bean), constructed either from an already-booted {@link
 * ApplicationContext} (production usage, via {@link #fromContext}) or directly from its explicit
 * dependencies (test usage, with mocks - see the primary constructor), so the exact same
 * production wiring the deployed app uses is what runs in practice. Returns an exit code from
 * {@link #run} instead of calling {@link System#exit} itself, so tests can assert on outcomes
 * without terminating the test JVM; only {@link WordPressProductionImportCli#main} translates the
 * returned code into a real process exit.
 */
final class WordPressProductionImportRunner {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_ACTOR_NOT_FOUND = 3;
    static final int EXIT_AUTHORIZATION_FAILED = 4;
    static final int EXIT_DUMP_READ_FAILED = 5;
    static final int EXIT_TEMP_DIR_FAILED = 6;

    private static final String SOURCE_SYSTEM = "wordpress";

    private final WordPressContentImportService contentImportService;
    private final WordPressMediaImportService mediaImportService;
    private final WordPressMigrationMappingRepository mappingRepository;
    private final WordPressMediaMappingRepository mediaMappingRepository;
    private final DashboardUserRepository dashboardUserRepository;
    private final ContentPostRepository contentPostRepository;
    private final CmsMediaAssetRepository mediaAssetRepository;
    private final CmsMediaProperties cmsMediaProperties;
    private final WordPressMigrationMediaProperties migrationMediaProperties;

    WordPressProductionImportRunner(
            WordPressContentImportService contentImportService,
            WordPressMediaImportService mediaImportService,
            WordPressMigrationMappingRepository mappingRepository,
            WordPressMediaMappingRepository mediaMappingRepository,
            DashboardUserRepository dashboardUserRepository,
            ContentPostRepository contentPostRepository,
            CmsMediaAssetRepository mediaAssetRepository,
            CmsMediaProperties cmsMediaProperties,
            WordPressMigrationMediaProperties migrationMediaProperties
    ) {
        this.contentImportService = contentImportService;
        this.mediaImportService = mediaImportService;
        this.mappingRepository = mappingRepository;
        this.mediaMappingRepository = mediaMappingRepository;
        this.dashboardUserRepository = dashboardUserRepository;
        this.contentPostRepository = contentPostRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.cmsMediaProperties = cmsMediaProperties;
        this.migrationMediaProperties = migrationMediaProperties;
    }

    static WordPressProductionImportRunner fromContext(ApplicationContext context) {
        return new WordPressProductionImportRunner(
                context.getBean(WordPressContentImportService.class),
                context.getBean(WordPressMediaImportService.class),
                context.getBean(WordPressMigrationMappingRepository.class),
                context.getBean(WordPressMediaMappingRepository.class),
                context.getBean(DashboardUserRepository.class),
                context.getBean(ContentPostRepository.class),
                context.getBean(CmsMediaAssetRepository.class),
                context.getBean(CmsMediaProperties.class),
                context.getBean(WordPressMigrationMediaProperties.class)
        );
    }

    int run(WordPressProductionImportCli.CliArgs args, PrintStream out, PrintStream err) {
        DashboardUserEntity actor = dashboardUserRepository.findByEmailIgnoreCase(args.actorEmail())
                .orElse(null);
        if (actor == null) {
            err.println("No dashboard user found with email '" + args.actorEmail()
                    + "' - refusing to attribute a migration run to a fabricated account. Aborting.");
            return EXIT_ACTOR_NOT_FOUND;
        }
        out.println("Resolved actor: " + actor.getName() + " (id=" + actor.getId() + ")");

        if (args.apply()) {
            boolean authorized = migrationMediaProperties.isRealUploadAuthorized(cmsMediaProperties.getBucket());
            boolean confirmMatches = args.confirm() != null
                    && args.confirm().equals(migrationMediaProperties.getProductionConfirmationToken());
            boolean fullCorpusConfirmMatches = !args.allPosts()
                    || (args.confirmFullCorpus() != null
                            && args.confirmFullCorpus().equals(migrationMediaProperties.getProductionConfirmationToken()));
            if (!authorized || !confirmMatches || !fullCorpusConfirmMatches) {
                err.println("Real-write authorization check failed:");
                err.println("  sfs.migration.wordpress.media.* gate authorized: " + authorized);
                err.println("  --confirm matches the configured production-confirmation-token: " + confirmMatches);
                if (args.allPosts()) {
                    err.println("  --confirm-full-corpus matches the configured production-confirmation-token: "
                            + fullCorpusConfirmMatches);
                }
                err.println("Aborting - no database or S3 writes were attempted.");
                return EXIT_AUTHORIZATION_FAILED;
            }
            out.println("Real-write authorization confirmed. Proceeding with APPLY mode, scope="
                    + (args.allPosts() ? "ALL POSTS" : args.postIds()));
        }

        out.println("Reading WordPress dump: " + args.dumpPath());
        WordPressDataset dataset;
        try {
            WordPressDataset.Builder builder = WordPressDataset.builder();
            new WordPressDumpReader().read(args.dumpPath(), builder);
            dataset = builder.build();
        } catch (IOException e) {
            err.println("Failed to read the WordPress dump: " + e.getMessage());
            return EXIT_DUMP_READ_FAILED;
        }
        out.println("Dataset loaded: " + dataset.postsById().size() + " total rows.");

        Map<Long, WordPressPostRow> posts = new LinkedHashMap<>();
        for (WordPressPostRow row : dataset.postsById().values()) {
            if (!"post".equals(row.postType())) continue;
            if (!("publish".equals(row.postStatus()) || "draft".equals(row.postStatus()))) continue;
            if (args.postIds() != null && !args.postIds().contains(row.id())) continue;
            posts.put(row.id(), row);
        }
        out.println("Posts in scope: " + posts.size()
                + (args.postIds() != null ? " (scoped to " + args.postIds() + ")" : " (full corpus)"));
        if (posts.isEmpty()) {
            out.println("Nothing to do.");
            return EXIT_SUCCESS;
        }

        Map<Long, GutenbergConversionResult> conversions = new LinkedHashMap<>();
        for (var entry : posts.entrySet()) {
            WordPressPostRow post = entry.getValue();
            conversions.put(entry.getKey(), new GutenbergDocumentConverter()
                    .convert(post.id(), post.postTitle(), post.postContent()));
        }

        Map<Long, String> attachmentPaths = new LinkedHashMap<>();
        for (Long postId : posts.keySet()) {
            for (Long attachmentId : conversions.get(postId).referencedAttachmentIds()) {
                addAttachmentPath(dataset, attachmentId, attachmentPaths);
            }
            String thumbnailIdRaw = dataset.metaValue(postId, "_thumbnail_id");
            if (thumbnailIdRaw != null && !thumbnailIdRaw.isBlank()) {
                try {
                    addAttachmentPath(dataset, Long.parseLong(thumbnailIdRaw.trim()), attachmentPaths);
                } catch (NumberFormatException ignored) {
                    // reported per-post if it ends up mattering; not fatal here
                }
            }
        }
        out.println("Distinct referenced attachment IDs: " + attachmentPaths.size());

        if (!args.apply()) {
            runDryRun(posts, attachmentPaths, args, out, err);
            return EXIT_SUCCESS;
        }
        runApply(dataset, posts, conversions, attachmentPaths, actor, args, out);
        return EXIT_SUCCESS;
    }

    private void runDryRun(
            Map<Long, WordPressPostRow> posts, Map<Long, String> attachmentPaths,
            WordPressProductionImportCli.CliArgs args, PrintStream out, PrintStream err
    ) {
        out.println("========== DRY RUN - no database or S3 writes will be made ==========");
        long published = 0, draft = 0, needsReview = 0, blocked = 0;
        long alreadyMigrated = 0;
        for (var entry : posts.entrySet()) {
            WordPressPostRow post = entry.getValue();
            GutenbergConversionResult conversion = new GutenbergDocumentConverter()
                    .convert(post.id(), post.postTitle(), post.postContent());
            PostClassification classification = WordPressPostClassifier.classify(conversion);
            boolean isPublished = "publish".equals(post.postStatus());
            String intendedState = classification == PostClassification.BLOCKED ? "BLOCKED"
                    : !isPublished ? "DRAFT"
                    : classification == PostClassification.MANUAL_REVIEW_REQUIRED ? "NEEDS_REVIEW" : "PUBLISHED";
            switch (intendedState) {
                case "PUBLISHED" -> published++;
                case "DRAFT" -> draft++;
                case "NEEDS_REVIEW" -> needsReview++;
                case "BLOCKED" -> blocked++;
                default -> { }
            }
            Optional<?> existing = mappingRepository.findBySourceSystemAndSourcePostId(SOURCE_SYSTEM, post.id());
            if (existing.isPresent()) {
                alreadyMigrated++;
            }
            out.println("post=" + post.id() + " title='" + post.postTitle() + "' sourceStatus=" + post.postStatus()
                    + " intendedState=" + intendedState + " alreadyMigrated=" + existing.isPresent());
        }
        out.println("Intended outcome breakdown: PUBLISHED=" + published + " DRAFT=" + draft
                + " NEEDS_REVIEW=" + needsReview + " BLOCKED=" + blocked
                + " (already-migrated posts among these: " + alreadyMigrated + ")");

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("wp-production-dryrun-media-");
        } catch (IOException e) {
            err.println("Could not create a temp directory for dry-run checksums: " + e.getMessage());
            return;
        }
        try {
            long resolvableCount = 0, unresolvableCount = 0, alreadyStoredCount = 0;
            long totalBytes = 0;
            WordPressMediaExtractor extractor = new WordPressMediaExtractor(512L * 1024 * 1024, Long.MAX_VALUE);
            for (var entry : attachmentPaths.entrySet()) {
                long attachmentId = entry.getKey();
                try {
                    var extracted = extractor.extractOne(args.uploadsZipPath(), attachmentId, entry.getValue(), tempDir);
                    String mimeType = extracted.detectedMimeType();
                    CmsMediaType mediaType = mimeType.startsWith("image/") ? CmsMediaType.IMAGE
                            : mimeType.startsWith("video/") ? CmsMediaType.VIDEO : null;
                    if (mediaType == null) {
                        unresolvableCount++;
                        out.println("  attachment " + attachmentId + ": UNRESOLVABLE (disallowed MIME " + mimeType + ")");
                        continue;
                    }
                    String key = WordPressMediaObjectKeyFactory.build(
                            mediaType, mimeType, extracted.sha256Hex(), OffsetDateTime.now(ZoneOffset.UTC)
                    );
                    boolean alreadyMapped = mediaMappingRepository
                            .findBySourceSystemAndWordPressAttachmentId(SOURCE_SYSTEM, attachmentId)
                            .map(m -> m.getMigrationState() == WordPressMediaMigrationState.COMPLETED)
                            .orElse(false);
                    if (alreadyMapped) {
                        alreadyStoredCount++;
                    }
                    resolvableCount++;
                    totalBytes += extracted.sizeBytes();
                    Files.deleteIfExists(extracted.localFile());
                } catch (Exception unresolvable) {
                    unresolvableCount++;
                    out.println("  attachment " + attachmentId + ": UNRESOLVABLE (" + unresolvable.getMessage() + ")");
                }
            }
            out.println("Media: " + resolvableCount + " resolvable, " + unresolvableCount + " unresolvable, "
                    + alreadyStoredCount + " already completed in a prior run, " + totalBytes + " bytes total to upload.");
        } finally {
            deleteRecursively(tempDir);
        }
        out.println("=======================================================================");
        out.println("No writes were made. Re-run with --apply --confirm=<token> plus an explicit "
                + "--post-ids=<...> or --all-posts --confirm-full-corpus=<token> scope to perform the real import.");
    }

    private void runApply(
            WordPressDataset dataset, Map<Long, WordPressPostRow> posts, Map<Long, GutenbergConversionResult> conversions,
            Map<Long, String> attachmentPaths, DashboardUserEntity actor, WordPressProductionImportCli.CliArgs args,
            PrintStream out
    ) {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("wp-production-import-media-");
        } catch (IOException e) {
            out.println("Could not create a temp directory for media extraction: " + e.getMessage());
            return;
        }
        try {
            Map<Long, Long> resolvedMediaAssetIds = new LinkedHashMap<>();
            Map<Long, Exception> mediaFailures = new LinkedHashMap<>();
            for (var entry : attachmentPaths.entrySet()) {
                WordPressMediaImportRequest request = new WordPressMediaImportRequest(
                        entry.getKey(), args.uploadsZipPath(), entry.getValue(), tempDir, actor
                );
                try {
                    WordPressMediaImportOutcome outcome = mediaImportService.importAttachment(request);
                    resolvedMediaAssetIds.put(entry.getKey(), outcome.cmsMediaAssetId());
                } catch (Exception failure) {
                    mediaFailures.put(entry.getKey(), failure);
                    out.println("Media import failed for attachment " + entry.getKey() + ": " + failure.getMessage());
                }
            }
            out.println("Media resolved: " + resolvedMediaAssetIds.size() + " of " + attachmentPaths.size()
                    + " (" + mediaFailures.size() + " unresolved - their posts will be forced to manual review, never fabricated).");

            Map<Long, WordPressImportOutcome> outcomes = new LinkedHashMap<>();
            Map<Long, Exception> contentFailures = new LinkedHashMap<>();
            for (var entry : posts.entrySet()) {
                long postId = entry.getKey();
                WordPressImportRequest request = new WordPressImportRequest(
                        entry.getValue(), dataset, conversions.get(postId), resolvedMediaAssetIds, actor
                );
                try {
                    outcomes.put(postId, contentImportService.importPost(request));
                } catch (Exception failure) {
                    contentFailures.put(postId, failure);
                    out.println("Content import failed for post " + postId + ": " + failure.getMessage());
                }
            }

            long published = 0, draft = 0, needsReview = 0, blocked = 0;
            for (WordPressImportOutcome outcome : outcomes.values()) {
                switch (outcome.migrationState()) {
                    case PUBLISHED -> published++;
                    case DRAFT -> draft++;
                    case NEEDS_REVIEW -> needsReview++;
                    case BLOCKED -> blocked++;
                    default -> { }
                }
            }

            out.println("========== APPLY complete ==========");
            out.println("Posts processed: " + outcomes.size() + " of " + posts.size()
                    + " (" + contentFailures.size() + " failed)");
            out.println("Outcome breakdown: PUBLISHED=" + published + " DRAFT=" + draft
                    + " NEEDS_REVIEW=" + needsReview + " BLOCKED=" + blocked);
            out.println("content_post rows now in DB: " + contentPostRepository.count());
            out.println("cms_media_asset rows now in DB: " + mediaAssetRepository.count());
            out.println("=====================================");
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private void addAttachmentPath(WordPressDataset dataset, Long attachmentId, Map<Long, String> out) {
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

    private void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (var walk = Files.walk(directory)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // best-effort cleanup of a temp directory
        }
    }
}
