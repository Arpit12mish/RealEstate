package com.brandPitara.sfs.migration.wordpress.gutenberg;

/**
 * One entry per Gutenberg block this converter could not (or deliberately does not) represent in
 * the target schema. {@code path} is a breadcrumb of block names/indices from the document root
 * (e.g. {@code "[2]/columns[0]/column[1]/qligg:box[0]"}), so the same block name appearing
 * several times in one post is still individually locatable.
 */
public record UnsupportedBlockReport(
        String blockName,
        String path,
        boolean hadMeaningfulContent,
        Disposition disposition
) {
    public enum Disposition {
        OMITTED_NO_CONTENT,
        OMITTED_BY_POLICY,
        MANUAL_REVIEW_REQUIRED
    }
}
