package com.brandPitara.sfs.migration.wordpress.gutenberg;

import java.util.List;

/**
 * A structured, repairable record of a {@code core/gallery} block that could not be fully
 * resolved to CMS media - the gallery block itself is never fabricated or silently dropped from
 * the audit; this is exactly what a later remediation step needs to re-run conversion once the
 * missing images have been deliberately acquired and uploaded as {@code CmsMediaAssetEntity}
 * rows. {@code unresolvedImageUrls} are never written into a final {@code ContentDocument} -
 * they exist only here, for a human/tooling to act on.
 */
public record UnresolvedGalleryReport(
        String path,
        List<Long> resolvedAttachmentIds,
        List<String> unresolvedImageUrls
) {
    public boolean fullyUnresolved() {
        return resolvedAttachmentIds.isEmpty();
    }
}
