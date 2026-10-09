package com.brandPitara.sfs.migration.wordpress.importer;

import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.document.ContentDocumentWordCounter;
import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentTagEntity;
import com.brandPitara.sfs.migration.wordpress.audit.WordPressDataset;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressUserRow;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergConversionResult;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergDocumentConverter;
import lombok.RequiredArgsConstructor;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every check for one WordPress-post-id/content-post-id pair - a real {@code @Service} (not a
 * plain method on the CLI runner) specifically so {@link #validateAll} is a genuine
 * {@code @Transactional(readOnly = true)} boundary: {@link ContentPostEntity#getTags()}, {@code
 * getPublicAuthor()}, and {@code getCategory()} are all lazy associations, and reading them
 * outside an open Hibernate session throws {@code LazyInitializationException} - the runner
 * itself is a plain, non-Spring-managed object and cannot open one.
 */
@Service
@RequiredArgsConstructor
public class WordPressMappingBackfillValidator {

    private static final String SOURCE_SYSTEM = "wordpress";

    private final WordPressMigrationMappingRepository mappingRepository;
    private final ContentPostRepository contentPostRepository;
    private final ContentDocumentWordCounter wordCounter;

    @Transactional(readOnly = true)
    public List<WordPressMappingBackfillRow> validateAll(Map<Long, Long> pairs, WordPressDataset dataset) {
        List<WordPressMappingBackfillRow> rows = new ArrayList<>();
        for (Map.Entry<Long, Long> pair : pairs.entrySet()) {
            rows.add(validate(pair.getKey(), pair.getValue(), dataset));
        }
        return rows;
    }

    private WordPressMappingBackfillRow validate(long wpId, long targetId, WordPressDataset dataset) {
        WordPressPostRow wpPost = dataset.postsById().get(wpId);
        if (wpPost == null || !"post".equals(wpPost.postType())) {
            return blocked(wpId, targetId, null, null, null, null, null, null,
                    "WordPress source post " + wpId + " was not found in the dump (or is not post_type=post).");
        }

        ContentPostEntity target = contentPostRepository.findById(targetId).orElse(null);
        if (target == null) {
            return blocked(wpId, targetId, wpPost.postTitle(), null, wpPost.postName(), null, null, null,
                    "Target content_post " + targetId + " does not exist.");
        }

        String targetStatusName = target.getStatus() == null ? null : target.getStatus().name();
        String fingerprint = WordPressContentFingerprint.compute(wpPost, dataset);

        WordPressMappingBackfillRow.IdentityResult identity = identityOf(wpPost, target);
        if (identity == WordPressMappingBackfillRow.IdentityResult.NO_MATCH) {
            return blocked(wpId, targetId, wpPost.postTitle(), target.getTitle(), wpPost.postName(), target.getSlug(),
                    targetStatusName, fingerprint,
                    "Neither slug nor normalized title matches between the WordPress source and the target - "
                            + "this pair looks wrong and was refused rather than guessed.");
        }

        if (target.getStatus() != ContentStatus.PUBLISHED) {
            return blocked(wpId, targetId, wpPost.postTitle(), target.getTitle(), wpPost.postName(), target.getSlug(),
                    targetStatusName, fingerprint,
                    "Target status " + targetStatusName + " has no defined backfill rule yet (only PUBLISHED does) - "
                            + "needs an explicit human decision, not an automatic one.");
        }

        Optional<WordPressMigrationMappingEntity> existingBySource =
                mappingRepository.findBySourceSystemAndSourcePostId(SOURCE_SYSTEM, wpId);
        if (existingBySource.isPresent()) {
            WordPressMigrationMappingEntity existing = existingBySource.get();
            if (!Objects.equals(existing.getTargetContentId(), targetId)) {
                return blocked(wpId, targetId, wpPost.postTitle(), target.getTitle(), wpPost.postName(), target.getSlug(),
                        targetStatusName, fingerprint,
                        "WordPress post " + wpId + " is already mapped to a DIFFERENT content_post ("
                                + existing.getTargetContentId() + ").");
            }
            if (!existing.getSourceFingerprint().equals(fingerprint)) {
                return blocked(wpId, targetId, wpPost.postTitle(), target.getTitle(), wpPost.postName(), target.getSlug(),
                        targetStatusName, fingerprint,
                        "WordPress post " + wpId + " is already mapped to this target, but with a different "
                                + "fingerprint - the source content may have changed since that mapping was written.");
            }
            WordPressMappingBackfillRow.ContentComparison comparison = compare(wpPost, dataset, target);
            return new WordPressMappingBackfillRow(wpId, targetId, wpPost.postTitle(), target.getTitle(), wpPost.postName(),
                    target.getSlug(), targetStatusName, fingerprint, identity, comparison,
                    WordPressMappingBackfillRow.PlannedAction.ALREADY_MAPPED, null);
        }

        Optional<WordPressMigrationMappingEntity> existingByTarget = mappingRepository.findByTargetContentId(targetId);
        if (existingByTarget.isPresent()) {
            return blocked(wpId, targetId, wpPost.postTitle(), target.getTitle(), wpPost.postName(), target.getSlug(),
                    targetStatusName, fingerprint,
                    "content_post " + targetId + " is already mapped from a DIFFERENT WordPress post ("
                            + existingByTarget.get().getSourcePostId() + ").");
        }

        WordPressMappingBackfillRow.ContentComparison comparison = compare(wpPost, dataset, target);
        WordPressMappingBackfillRow.PlannedAction action = comparison.isFullyEquivalent()
                ? WordPressMappingBackfillRow.PlannedAction.INSERT
                : WordPressMappingBackfillRow.PlannedAction.ADOPT_EXISTING_WITH_DIFFERENCES;

        return new WordPressMappingBackfillRow(wpId, targetId, wpPost.postTitle(), target.getTitle(), wpPost.postName(),
                target.getSlug(), targetStatusName, fingerprint, identity, comparison, action, null);
    }

    private WordPressMappingBackfillRow blocked(
            long wpId, long targetId, String wpTitle, String targetTitle, String wpSlug, String targetSlug,
            String targetStatus, String fingerprint, String reason
    ) {
        return new WordPressMappingBackfillRow(wpId, targetId, wpTitle, targetTitle, wpSlug, targetSlug, targetStatus,
                fingerprint, WordPressMappingBackfillRow.IdentityResult.NO_MATCH, null,
                WordPressMappingBackfillRow.PlannedAction.BLOCKED, reason);
    }

    private WordPressMappingBackfillRow.IdentityResult identityOf(WordPressPostRow wpPost, ContentPostEntity target) {
        String wpSlug = normalize(wpPost.postName());
        String targetSlug = normalize(target.getSlug());
        if (!wpSlug.isBlank() && wpSlug.equals(targetSlug)) {
            return WordPressMappingBackfillRow.IdentityResult.SLUG_EXACT_MATCH;
        }
        String wpTitle = normalizeTitle(wpPost.postTitle());
        String targetTitle = normalizeTitle(target.getTitle());
        if (!wpTitle.isBlank() && wpTitle.equals(targetTitle)) {
            return WordPressMappingBackfillRow.IdentityResult.TITLE_NORMALIZED_MATCH;
        }
        return WordPressMappingBackfillRow.IdentityResult.NO_MATCH;
    }

    /**
     * Identity (slug/title) proves this is the same article, never that the manually-created
     * target contains the complete migrated content. Every dimension here is reported; the
     * document/word-count check is a structural proxy (block-type sequence plus a word-count
     * tolerance reusing the already-tested {@link ContentDocumentWordCounter}), not a
     * byte-for-byte diff - deliberately honest about that limit rather than claiming a precision
     * this doesn't have.
     */
    private WordPressMappingBackfillRow.ContentComparison compare(
            WordPressPostRow wpPost, WordPressDataset dataset, ContentPostEntity target
    ) {
        List<String> differences = new ArrayList<>();

        boolean titleMatches = normalizeTitle(wpPost.postTitle()).equals(normalizeTitle(target.getTitle()));
        if (!titleMatches) differences.add("title");

        boolean slugMatches = normalize(wpPost.postName()).equals(normalize(target.getSlug()));
        if (!slugMatches) differences.add("slug");

        String wpAuthorName = wpPost.postAuthor() == null ? null
                : Optional.ofNullable(dataset.userById(wpPost.postAuthor())).map(WordPressUserRow::displayName).orElse(null);
        String targetAuthorName = target.getPublicAuthor() == null ? null : target.getPublicAuthor().getDisplayName();
        boolean authorMatches = namesEqualIgnoringCase(wpAuthorName, targetAuthorName);
        if (!authorMatches) differences.add("author");

        boolean interviewCategory = dataset.categoriesFor(wpPost.id()).stream()
                .anyMatch(name -> name.equalsIgnoreCase("Interview"));
        ContentType expectedType = interviewCategory ? ContentType.INTERVIEW : ContentType.BLOG;
        boolean contentTypeMatches = target.getContentType() == expectedType;
        if (!contentTypeMatches) differences.add("contentType");

        boolean sourcePublished = "publish".equals(wpPost.postStatus());
        boolean statusMatches = sourcePublished && target.getStatus() == ContentStatus.PUBLISHED;
        if (!statusMatches) differences.add("status");

        List<String> wpCategories = dataset.categoriesFor(wpPost.id());
        String targetCategoryName = target.getCategory() == null ? null : target.getCategory().getName();
        boolean categoriesMatch = !wpCategories.isEmpty() && targetCategoryName != null
                && wpCategories.stream().anyMatch(c -> c.equalsIgnoreCase(targetCategoryName));
        if (!wpCategories.isEmpty() && !categoriesMatch) differences.add("category");

        Set<String> wpTags = dataset.tagsFor(wpPost.id()).stream()
                .map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> targetTags = target.getTags().stream()
                .map(CmsContentTagEntity::getName).filter(Objects::nonNull)
                .map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        boolean tagsMatch = wpTags.isEmpty() || wpTags.equals(targetTags);
        if (!wpTags.isEmpty() && !tagsMatch) differences.add("tags");

        boolean wpHasFeatured = dataset.metaValue(wpPost.id(), "_thumbnail_id") != null;
        boolean targetHasFeatured = target.getCoverMediaAsset() != null;
        boolean featuredMediaMatches = wpHasFeatured == targetHasFeatured;
        if (!featuredMediaMatches) differences.add("featuredMedia");

        String wpSeoTitle = dataset.metaValue(wpPost.id(), "rank_math_title");
        String wpSeoDescription = dataset.metaValue(wpPost.id(), "rank_math_description");
        boolean seoMatches = presencesRoughlyMatch(wpSeoTitle, target.getSeoTitle())
                && presencesRoughlyMatch(wpSeoDescription, target.getSeoDescription());
        if (!seoMatches) differences.add("seo");

        GutenbergConversionResult conversion = new GutenbergDocumentConverter()
                .convert(wpPost.id(), wpPost.postTitle(), wpPost.postContent());
        boolean documentEquivalent = documentsRoughlyEquivalent(conversion.document(), target.getContentDocument());
        if (!documentEquivalent) differences.add("document");

        return new WordPressMappingBackfillRow.ContentComparison(titleMatches, slugMatches, authorMatches,
                contentTypeMatches, statusMatches, categoriesMatch, tagsMatch, featuredMediaMatches, seoMatches,
                documentEquivalent, differences);
    }

    private boolean documentsRoughlyEquivalent(ContentDocument source, ContentDocument target) {
        if (target == null) {
            return source.blocks().isEmpty();
        }
        List<String> sourceBlockTypes = source.blocks().stream().map(b -> b.getClass().getSimpleName()).toList();
        List<String> targetBlockTypes = target.blocks().stream().map(b -> b.getClass().getSimpleName()).toList();
        boolean blockStructureMatches = sourceBlockTypes.equals(targetBlockTypes);

        int sourceWords = wordCounter.count(source);
        int targetWords = wordCounter.count(target);
        boolean wordCountClose = targetWords == 0
                ? sourceWords == 0
                : Math.abs(sourceWords - targetWords) <= Math.max(5, targetWords / 10);

        return blockStructureMatches && wordCountClose;
    }

    private boolean presencesRoughlyMatch(String source, String target) {
        boolean sourcePresent = source != null && !source.isBlank();
        boolean targetPresent = target != null && !target.isBlank();
        return sourcePresent == targetPresent;
    }

    private boolean namesEqualIgnoringCase(String a, String b) {
        return Objects.equals(
                a == null ? null : a.trim().toLowerCase(Locale.ROOT),
                b == null ? null : b.trim().toLowerCase(Locale.ROOT)
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /** Decodes HTML entities (WordPress titles often carry {@code &#8217;} etc.) then collapses
     * everything but letters/digits to single spaces, so punctuation-only differences (a colon,
     * a curly vs. straight apostrophe, a dropped parenthetical) never cause a false NO_MATCH. */
    private String normalizeTitle(String value) {
        if (value == null) {
            return "";
        }
        String decoded = Parser.unescapeEntities(value, false);
        return decoded.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
