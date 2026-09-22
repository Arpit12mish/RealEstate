package com.brandPitara.sfs.migration.wordpress.gutenberg;

import com.brandPitara.sfs.cms.content.document.ContentDocument;

import java.util.List;
import java.util.Set;

/**
 * The full outcome of converting one WordPress post's {@code post_content} to a schema-v5
 * {@link ContentDocument}. {@code document} may still reference media only by WordPress
 * attachment ID (see {@link #referencedAttachmentIds()}) - it is not production-ready until a
 * later media-resolution step replaces those with real {@code CmsMediaAssetEntity} IDs.
 */
public record GutenbergConversionResult(
        long wordPressPostId,
        String title,
        ContentDocument document,
        List<ConversionWarning> warnings,
        List<ConversionBlockingError> blockingErrors,
        List<UnsupportedBlockReport> unsupportedBlocks,
        List<UnresolvedGalleryReport> unresolvedGalleries,
        Set<Long> referencedAttachmentIds,
        Set<String> referencedExternalUrls,
        boolean manualReviewRequired,
        boolean eligibleForAutomaticPublication
) {
}
