package com.brandPitara.sfs.migration.wordpress.importer;

import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import com.brandPitara.sfs.cms.content.document.ContentBlock;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.document.ContentDocumentValidator;
import com.brandPitara.sfs.cms.content.document.ContentDocumentWordCounter;
import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.domain.ContentValidation;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.content.slug.ContentSlugService;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentCategoryEntity;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentTagEntity;
import com.brandPitara.sfs.cms.workflow.domain.ContentRevisionReason;
import com.brandPitara.sfs.cms.workflow.domain.ContentTagSnapshot;
import com.brandPitara.sfs.cms.workflow.entity.ContentPostRevisionEntity;
import com.brandPitara.sfs.cms.workflow.repository.ContentPostRevisionRepository;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.migration.wordpress.WordPressAuthorResolver;
import com.brandPitara.sfs.migration.wordpress.audit.PostClassification;
import com.brandPitara.sfs.migration.wordpress.audit.WordPressDataset;
import com.brandPitara.sfs.migration.wordpress.audit.WordPressPostClassifier;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import lombok.RequiredArgsConstructor;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Imports one already-converted WordPress post into the CMS, end to end: idempotency check,
 * author/category/tag resolution, media-ID rewriting, slug/excerpt/SEO/reading-time derivation,
 * and (only for an eligible published source) an immutable revision with
 * {@code currentPublishedRevision} set - never invoked automatically, only from an explicit
 * caller (a Testcontainers test today; a CLI in a later milestone).
 */
@Service
@RequiredArgsConstructor
public class WordPressContentImportService {

    public static final String SOURCE_SYSTEM = "wordpress";
    private static final DateTimeFormatter WP_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WordPressMigrationMappingRepository mappingRepository;
    private final WordPressAuthorResolver authorResolver;
    private final WordPressTaxonomyResolver taxonomyResolver;
    private final ContentPostRepository contentPostRepository;
    private final ContentPostRevisionRepository revisionRepository;
    private final ContentSlugService slugService;
    private final ContentDocumentValidator documentValidator;
    private final ContentDocumentWordCounter wordCounter;
    private final CmsMediaAssetRepository mediaAssetRepository;

    @Transactional
    public WordPressImportOutcome importPost(WordPressImportRequest request) {
        WordPressPostRow post = request.post();
        String fingerprint = fingerprint(request);

        Optional<WordPressMigrationMappingEntity> existing =
                mappingRepository.findBySourceSystemAndSourcePostId(SOURCE_SYSTEM, post.id());
        if (existing.isPresent()) {
            WordPressMigrationMappingEntity mapping = existing.get();
            if (mapping.getSourceFingerprint().equals(fingerprint)) {
                return new WordPressImportOutcome(
                        post.id(), mapping.getMigrationState(), mapping.getTargetContentId(), true, mapping.getErrorCode()
                );
            }
            throw WordPressImportException.fingerprintChanged(post.id(), mapping.getSourceFingerprint(), fingerprint);
        }

        PostClassification classification = WordPressPostClassifier.classify(request.conversion());
        if (classification == PostClassification.BLOCKED) {
            mappingRepository.saveAndFlush(WordPressMigrationMappingEntity.builder()
                    .sourceSystem(SOURCE_SYSTEM).sourcePostId(post.id())
                    .sourceFingerprint(fingerprint)
                    .migrationState(WordPressMigrationState.BLOCKED)
                    .errorCode("BLOCKED_SOURCE_CONTENT")
                    .build());
            return new WordPressImportOutcome(post.id(), WordPressMigrationState.BLOCKED, null, false, "BLOCKED_SOURCE_CONTENT");
        }

        return importConvertibleContent(request, fingerprint, classification);
    }

    private WordPressImportOutcome importConvertibleContent(
            WordPressImportRequest request, String fingerprint, PostClassification classification
    ) {
        WordPressPostRow post = request.post();
        boolean isPublished = "publish".equals(post.postStatus());

        CmsPublicAuthorEntity author = authorResolver.resolve(post.postAuthor());
        List<String> categoryNames = request.dataset().categoriesFor(post.id());
        CmsContentCategoryEntity category = categoryNames.isEmpty() ? null
                : taxonomyResolver.resolveCategory(pickPrimaryCategory(categoryNames));
        List<CmsContentTagEntity> tags = taxonomyResolver.resolveTags(request.dataset().tagsFor(post.id()));

        // A block's attachment ID can look resolved at conversion time (the id attribute is
        // present) yet still fail to resolve here - the attachment row may be missing entirely
        // from this export, or its file may be in a format the CMS doesn't accept (e.g. AVIF,
        // which WordPress can reference but CmsMediaValidator does not allow). Rather than crash
        // the whole import, the unresolvable reference is dropped and the post is forced into
        // manual review - never silently fabricated, never allowed to auto-publish with lost media.
        java.util.concurrent.atomic.AtomicBoolean droppedUnresolvedMedia = new java.util.concurrent.atomic.AtomicBoolean(false);
        ContentDocument rewritten = rewriteMediaIds(
                request.conversion().document(), request.resolvedMediaAssetIds(), droppedUnresolvedMedia
        );
        ContentDocument finalDocument = documentValidator.validateAndNormalize(rewritten);
        boolean manualReview = classification == PostClassification.MANUAL_REVIEW_REQUIRED || droppedUnresolvedMedia.get();

        Long thumbnailAttachmentId = parseLongOrNull(request.dataset().metaValue(post.id(), "_thumbnail_id"));
        CmsMediaAssetEntity cover = null;
        String coverAltText = null;
        if (thumbnailAttachmentId != null) {
            Long realId = request.resolvedMediaAssetIds().get(thumbnailAttachmentId);
            if (realId != null) {
                cover = mediaAssetRepository.findById(realId).orElse(null);
                String rawAlt = request.dataset().metaValue(thumbnailAttachmentId, "_wp_attachment_image_alt");
                coverAltText = StringUtils.hasText(rawAlt) ? decodeEntities(rawAlt) : decodeEntities(post.postTitle());
            }
        }

        String slug = slugService.resolveForCreate(
                StringUtils.hasText(post.postName()) ? post.postName() : null, decodeEntities(post.postTitle())
        );
        String excerpt = StringUtils.hasText(post.postExcerpt())
                ? truncate(decodeEntities(post.postExcerpt()), ContentValidation.EXCERPT_MAX)
                : sanitizeControlCharacters(WordPressExcerptGenerator.generate(finalDocument));

        int wordCount = wordCounter.count(finalDocument);
        int readingTime = Math.max(
                ContentValidation.READING_TIME_MIN,
                Math.min(ContentValidation.READING_TIME_MAX, (int) Math.round(wordCount / 200.0))
        );

        String rankMathTitle = request.dataset().metaValue(post.id(), "rank_math_title");
        String rankMathDescription = request.dataset().metaValue(post.id(), "rank_math_description");
        String seoTitle = StringUtils.hasText(rankMathTitle) ? decodeEntities(rankMathTitle) : decodeEntities(post.postTitle());
        String seoDescription = StringUtils.hasText(rankMathDescription) ? decodeEntities(rankMathDescription) : excerpt;

        ContentType contentType = categoryNames.contains("Interview") ? ContentType.INTERVIEW : ContentType.BLOG;

        ContentPostEntity contentPost = ContentPostEntity.builder()
                .contentType(contentType)
                .status(ContentStatus.DRAFT)
                .title(decodeEntities(post.postTitle()))
                .slug(slug)
                .excerpt(truncate(excerpt, ContentValidation.EXCERPT_MAX))
                .readingTimeMinutes(readingTime)
                .publicAuthor(author)
                .category(category)
                .tags(new LinkedHashSet<>(tags))
                .coverMediaAsset(cover)
                .coverAltText(cover == null ? null : truncate(coverAltText, 300))
                .contentOwner(request.actor())
                .createdBy(request.actor())
                .updatedBy(request.actor())
                .seoTitle(truncate(seoTitle, ContentValidation.SEO_TITLE_MAX))
                .seoDescription(truncate(seoDescription, ContentValidation.SEO_DESCRIPTION_MAX))
                .robotsIndex(true)
                .robotsFollow(true)
                .contentDocument(finalDocument)
                .contentDocumentSchemaVersion((short) finalDocument.schemaVersion())
                .build();
        contentPost = contentPostRepository.saveAndFlush(contentPost);

        WordPressMigrationState migrationState;
        if (!isPublished) {
            migrationState = WordPressMigrationState.DRAFT;
        } else if (manualReview) {
            migrationState = WordPressMigrationState.NEEDS_REVIEW;
        } else {
            migrationState = WordPressMigrationState.PUBLISHED;
            contentPost = publish(contentPost, request.actor(), post, author, category, tags, cover, coverAltText, finalDocument, readingTime, slug, excerpt, seoTitle, seoDescription, contentType);
        }

        mappingRepository.saveAndFlush(WordPressMigrationMappingEntity.builder()
                .sourceSystem(SOURCE_SYSTEM).sourcePostId(post.id())
                .targetContentId(contentPost.getId())
                .sourceFingerprint(fingerprint)
                .migrationState(migrationState)
                .build());

        return new WordPressImportOutcome(post.id(), migrationState, contentPost.getId(), false, null);
    }

    private ContentPostEntity publish(
            ContentPostEntity contentPost, DashboardUserEntity actor, WordPressPostRow post,
            CmsPublicAuthorEntity author, CmsContentCategoryEntity category, List<CmsContentTagEntity> tags,
            CmsMediaAssetEntity cover, String coverAltText, ContentDocument document, int readingTime,
            String slug, String excerpt, String seoTitle, String seoDescription, ContentType contentType
    ) {
        long postVersionAtSnapshot = contentPost.getVersion();
        ContentPostRevisionEntity revision = ContentPostRevisionEntity.builder()
                .contentPost(contentPost)
                .revisionNumber(1)
                .contentType(contentType)
                .title(contentPost.getTitle())
                .slug(slug)
                .excerpt(excerpt == null ? null : truncate(excerpt, ContentValidation.EXCERPT_MAX))
                .readingTimeMinutes(readingTime)
                .publicAuthorId(author.getId())
                .publicAuthorName(author.getDisplayName())
                .publicAuthorSlug(author.getSlug())
                .publicAuthorDesignation(author.getDesignation())
                .publicAuthorProfileMediaAssetId(author.getProfileMediaAsset() == null ? null : author.getProfileMediaAsset().getId())
                .categoryId(category == null ? null : category.getId())
                .categoryName(category == null ? null : category.getName())
                .categorySlug(category == null ? null : category.getSlug())
                .tagSnapshots(tags.stream().map(t -> new ContentTagSnapshot(t.getId(), t.getName(), t.getSlug())).toList())
                .coverMediaAssetId(cover == null ? null : cover.getId())
                .coverAltText(cover == null ? null : truncate(coverAltText, 300))
                .seoTitle(truncate(seoTitle, ContentValidation.SEO_TITLE_MAX))
                .seoDescription(truncate(seoDescription, ContentValidation.SEO_DESCRIPTION_MAX))
                .robotsIndex(true)
                .robotsFollow(true)
                .contentDocument(document)
                .contentDocumentSchemaVersion((short) document.schemaVersion())
                .createdFromPostVersion(postVersionAtSnapshot)
                .createdBy(actor)
                .revisionReason(ContentRevisionReason.REVIEW_SUBMISSION)
                .build();
        revision = revisionRepository.saveAndFlush(revision);

        contentPost.setApprovedRevision(revision);
        contentPost.setCurrentPublishedRevision(revision);
        contentPost.setStatus(ContentStatus.PUBLISHED);
        contentPost.setPublishedAt(parseWpDate(post.postDateGmt(), post.postDate()));
        contentPost.setPublishedBy(actor);
        return contentPostRepository.saveAndFlush(contentPost);
    }

    /**
     * Replaces WordPress-attachment-ID placeholders in IMAGE/VIDEO/GALLERY blocks with real
     * CmsMediaAssetEntity IDs - never invents one for a still-unresolved reference. A block (or,
     * for a gallery, just the one unresolved image within it) whose attachment ID has no entry in
     * {@code resolvedMediaAssetIds} is dropped from the document rather than failing the whole
     * import, and {@code droppedUnresolvedMedia} is set so the caller forces manual review.
     */
    private ContentDocument rewriteMediaIds(
            ContentDocument document, java.util.Map<Long, Long> resolvedMediaAssetIds,
            java.util.concurrent.atomic.AtomicBoolean droppedUnresolvedMedia
    ) {
        List<ContentBlock> rewritten = new ArrayList<>(document.blocks().size());
        for (ContentBlock block : document.blocks()) {
            ContentBlock result = rewriteBlock(block, resolvedMediaAssetIds, droppedUnresolvedMedia);
            if (result != null) {
                rewritten.add(result);
            }
        }
        return new ContentDocument(document.schemaVersion(), List.copyOf(rewritten));
    }

    /** @return the rewritten block, or {@code null} if it must be dropped (its media never resolved). */
    private ContentBlock rewriteBlock(
            ContentBlock block, java.util.Map<Long, Long> resolvedMediaAssetIds,
            java.util.concurrent.atomic.AtomicBoolean droppedUnresolvedMedia
    ) {
        if (block instanceof ContentBlock.Image image) {
            Long realId = resolveOrFlagDropped(image.mediaAssetId(), resolvedMediaAssetIds, droppedUnresolvedMedia);
            if (realId == null) {
                return null;
            }
            return new ContentBlock.Image(realId, image.decorative(), image.altText(), image.caption(), image.layout(), image.link());
        }
        if (block instanceof ContentBlock.Video video) {
            Long realId = resolveOrFlagDropped(video.mediaAssetId(), resolvedMediaAssetIds, droppedUnresolvedMedia);
            if (realId == null) {
                return null;
            }
            Long posterRealId = video.posterMediaAssetId() == null ? null
                    : resolveOrFlagDropped(video.posterMediaAssetId(), resolvedMediaAssetIds, droppedUnresolvedMedia);
            return new ContentBlock.Video(realId, posterRealId, video.caption());
        }
        if (block instanceof ContentBlock.Gallery gallery) {
            List<ContentBlock.GalleryImage> images = new ArrayList<>();
            for (ContentBlock.GalleryImage img : gallery.images()) {
                Long realId = resolveOrFlagDropped(img.mediaAssetId(), resolvedMediaAssetIds, droppedUnresolvedMedia);
                if (realId != null) {
                    images.add(new ContentBlock.GalleryImage(realId, img.decorative(), img.altText(), img.caption()));
                }
            }
            if (images.isEmpty()) {
                return null;
            }
            return new ContentBlock.Gallery(gallery.columns(), images);
        }
        if (block instanceof ContentBlock.Layout layout) {
            List<com.brandPitara.sfs.cms.content.document.LayoutChildBlock> children = new ArrayList<>();
            for (var child : layout.children()) {
                if (child instanceof ContentBlock.Image image) {
                    Long realId = resolveOrFlagDropped(image.mediaAssetId(), resolvedMediaAssetIds, droppedUnresolvedMedia);
                    if (realId != null) {
                        children.add(new ContentBlock.Image(
                                realId, image.decorative(), image.altText(), image.caption(), image.layout(), image.link()
                        ));
                    }
                } else {
                    children.add(child);
                }
            }
            if (children.isEmpty()) {
                return null;
            }
            return new ContentBlock.Layout(layout.columns(), children);
        }
        return block;
    }

    private Long resolveOrFlagDropped(
            Long wordPressAttachmentId, java.util.Map<Long, Long> resolvedMediaAssetIds,
            java.util.concurrent.atomic.AtomicBoolean droppedUnresolvedMedia
    ) {
        Long realId = resolvedMediaAssetIds.get(wordPressAttachmentId);
        if (realId == null) {
            droppedUnresolvedMedia.set(true);
        }
        return realId;
    }

    private String pickPrimaryCategory(List<String> categoryNames) {
        if (categoryNames.size() > 1) {
            for (String name : categoryNames) {
                if (!"Uncategorized".equals(name)) {
                    return name;
                }
            }
        }
        return categoryNames.get(0);
    }

    /**
     * Bump whenever the fingerprint basis below changes shape. Every already-migrated post's
     * stored {@code source_fingerprint} was computed under whatever version was active at import
     * time; changing the basis without bumping this makes every unchanged source post look
     * "changed" on the next run (a spurious {@link WordPressImportException#fingerprintChanged}),
     * or - worse, the reverse problem this whole field exists to prevent - makes a genuinely
     * changed post look unchanged because the old basis didn't cover the field that changed. A
     * version bump is a deliberate, visible event: every previously-migrated post will fail
     * fingerprint comparison on its next run and require an explicit decision (re-import,
     * one-time re-fingerprint backfill, or leave as-is), not a silent behavior change.
     */
    private static final int FINGERPRINT_FORMAT_VERSION = 2;

    /**
     * Covers every field a source-side edit could make to a post that this importer cares about -
     * not just the body. v1 only hashed {@code postContent + postModifiedGmt}, which silently
     * missed a retitle, reslug, re-author, status change (draft published), category/tag
     * reassignment, featured-image swap, SEO-metadata edit, or an Elementor/Gutenberg toggle (via
     * {@code _elementor_edit_mode}) that changes classification without touching the body text at
     * all - any of those would have been treated as "unchanged" and silently skipped on a rerun.
     * Category/tag names are sorted before hashing (see class doc) so dump/database iteration
     * order can never make an unchanged post look changed.
     */
    private String fingerprint(WordPressImportRequest request) {
        WordPressPostRow post = request.post();
        WordPressDataset dataset = request.dataset();

        List<String> sortedCategories = dataset.categoriesFor(post.id()).stream().sorted().toList();
        List<String> sortedTags = dataset.tagsFor(post.id()).stream().sorted().toList();

        StringBuilder basis = new StringBuilder(512);
        basis.append(FINGERPRINT_FORMAT_VERSION).append('\u0000');
        appendField(basis, String.valueOf(post.id()));
        appendField(basis, post.postTitle());
        appendField(basis, post.postName());
        appendField(basis, post.postAuthor() == null ? null : String.valueOf(post.postAuthor()));
        appendField(basis, post.postStatus());
        appendField(basis, post.postDate());
        appendField(basis, post.postModifiedGmt());
        appendField(basis, post.postContent());
        appendField(basis, post.postExcerpt());
        appendField(basis, dataset.metaValue(post.id(), "_thumbnail_id"));
        appendField(basis, String.join(",", sortedCategories));
        appendField(basis, String.join(",", sortedTags));
        appendField(basis, dataset.metaValue(post.id(), "rank_math_title"));
        appendField(basis, dataset.metaValue(post.id(), "rank_math_description"));
        appendField(basis, dataset.metaValue(post.id(), "_elementor_edit_mode"));
        appendField(basis, dataset.metaValue(post.id(), "_elementor_version"));
        appendField(basis, dataset.metaValue(post.id(), "_elementor_data"));

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(basis.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** NUL-delimited with an explicit null marker, so e.g. title="a" + slug=null never collides
     * with title=null + slug="a" the way a plain "|"-join could if a field itself contained "|". */
    private void appendField(StringBuilder basis, String value) {
        basis.append(value == null ? "\u0001" : value).append('\u0000');
    }

    private OffsetDateTime parseWpDate(String primary, String fallback) {
        OffsetDateTime parsed = parseWpDateTime(primary);
        if (parsed != null) {
            return parsed;
        }
        parsed = parseWpDateTime(fallback);
        return parsed != null ? parsed : OffsetDateTime.now(ZoneOffset.UTC);
    }

    private OffsetDateTime parseWpDateTime(String value) {
        if (value == null || value.isBlank() || value.startsWith("0000")) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, WP_DATETIME).atOffset(ZoneOffset.UTC);
        } catch (Exception malformed) {
            return null;
        }
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException notNumeric) {
            return null;
        }
    }

    /**
     * Decodes HTML entities and strips ISO control characters (a literal newline/tab embedded in
     * raw WordPress postmeta - e.g. {@code rank_math_description} - would otherwise violate the
     * {@code chk_content_post_seo_description}-style CHECK constraints, which forbid control
     * characters in every plain-text CMS column). Every metadata field that flows into one of
     * those columns (title, excerpt, SEO title/description, alt text) must go through this.
     */
    private String decodeEntities(String value) {
        return value == null ? null : sanitizeControlCharacters(Parser.unescapeEntities(value, false));
    }

    /** Replaces ISO control characters (literal newline/tab/etc.) with a space, then collapses whitespace. */
    private String sanitizeControlCharacters(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder cleaned = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            cleaned.append(Character.isISOControl(c) ? ' ' : c);
        }
        return cleaned.toString().trim().replaceAll("\\s+", " ");
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
