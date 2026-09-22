package com.brandPitara.sfs.migration.wordpress;

import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import com.brandPitara.sfs.cms.author.repository.CmsPublicAuthorRepository;
import com.brandPitara.sfs.cms.content.slug.ContentSlugService;
import com.brandPitara.sfs.cms.metadata.dto.CmsAuthorRequest;
import com.brandPitara.sfs.cms.metadata.dto.CmsAuthorResponse;
import com.brandPitara.sfs.cms.metadata.service.CmsMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Resolves a WordPress {@code post_author} ID to a {@link CmsPublicAuthorEntity} byline, never a
 * login {@code DashboardUserEntity} - no WordPress credentials are ever imported. Idempotent:
 * looks up the deterministic slug derived from the configured display name before creating
 * anything, so re-running the migration reuses the same author profile instead of duplicating it.
 */
@Service
@RequiredArgsConstructor
public class WordPressAuthorResolver {

    private final WordPressMigrationProperties properties;
    private final CmsMetadataService metadataService;
    private final CmsPublicAuthorRepository authorRepository;
    private final ContentSlugService slugService;

    @Transactional
    public CmsPublicAuthorEntity resolve(long wordPressAuthorId) {
        WordPressMigrationProperties.AuthorMapping mapping = mappingFor(wordPressAuthorId);
        String slug = slugService.normalize(mapping.getDisplayName());
        return authorRepository.findBySlug(slug)
                .map(existing -> verifyIdentityConsistency(mapping, existing))
                .orElseGet(() -> createAuthor(mapping, slug));
    }

    /**
     * An existing author found by the deterministic slug must actually BE the configured author,
     * not a coincidental slug collision with unrelated content - reusing it blindly would
     * silently misattribute posts to the wrong byline. displayName is what the slug was derived
     * from, so any mismatch there is a hard conflict; designation is only compared when this
     * migration's config actually specifies one, so a human-added designation on an already
     * "correct" author profile is never treated as a conflict.
     */
    private CmsPublicAuthorEntity verifyIdentityConsistency(
            WordPressMigrationProperties.AuthorMapping mapping, CmsPublicAuthorEntity existing
    ) {
        boolean displayNameMatches = mapping.getDisplayName().equals(existing.getDisplayName());
        boolean designationConflicts = mapping.getDesignation() != null
                && !mapping.getDesignation().equals(existing.getDesignation());
        if (!displayNameMatches || designationConflicts) {
            throw WordPressAuthorResolutionException.authorIdentityConflict(
                    mapping.getWordPressAuthorId(), mapping.getDisplayName(),
                    existing.getId(), existing.getDisplayName()
            );
        }
        return existing;
    }

    /**
     * Validates that every WordPress author ID present in the source data is configured, that
     * each author's actual post count matches the configured expectation, and that the grand
     * total reconciles. Call once the full audit has counted every one of the 144 posts by
     * author - never partial data, so a genuinely missing author's absence is still caught by
     * the total-mismatch check even if no per-author entry exists to compare against.
     */
    public void reconcile(Map<Long, Long> wordPressAuthorIdToActualPostCount) {
        long expectedTotal = 0;
        for (WordPressMigrationProperties.AuthorMapping mapping : properties.getAuthors()) {
            expectedTotal += mapping.getExpectedPostCount();
        }

        long actualTotal = 0;
        for (Map.Entry<Long, Long> entry : wordPressAuthorIdToActualPostCount.entrySet()) {
            WordPressMigrationProperties.AuthorMapping mapping = mappingFor(entry.getKey());
            long actual = entry.getValue();
            actualTotal += actual;
            if (mapping.getExpectedPostCount() != actual) {
                throw WordPressAuthorResolutionException.postCountMismatch(
                        mapping.getWordPressAuthorId(), mapping.getDisplayName(),
                        mapping.getExpectedPostCount(), actual
                );
            }
        }

        if (expectedTotal != actualTotal) {
            throw WordPressAuthorResolutionException.totalMismatch(expectedTotal, actualTotal);
        }
    }

    private CmsPublicAuthorEntity createAuthor(WordPressMigrationProperties.AuthorMapping mapping, String slug) {
        CmsAuthorResponse created = metadataService.createAuthor(new CmsAuthorRequest(
                mapping.getDisplayName(), slug, null, mapping.getDesignation(), null, true, null
        ));
        return authorRepository.findWithProfileMediaById(created.id())
                .orElseThrow(() -> new WordPressAuthorResolutionException(
                        "Author profile disappeared immediately after creation: id=" + created.id()
                ));
    }

    private WordPressMigrationProperties.AuthorMapping mappingFor(long wordPressAuthorId) {
        return properties.getAuthors().stream()
                .filter(mapping -> mapping.getWordPressAuthorId() == wordPressAuthorId)
                .findFirst()
                .orElseThrow(() -> WordPressAuthorResolutionException.unknownAuthor(wordPressAuthorId));
    }
}
