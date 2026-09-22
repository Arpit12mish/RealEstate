package com.brandPitara.sfs.migration.wordpress.importer;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.migration.wordpress.audit.WordPressDataset;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;
import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergConversionResult;

import java.util.Map;

/**
 * @param resolvedMediaAssetIds WordPress attachment ID -> real {@code CmsMediaAssetEntity} ID,
 *                              for exactly the attachments this post's converted document and
 *                              featured image reference. An attachment with no entry here is
 *                              treated as still-unresolved (never fabricated).
 * @param actor the dashboard user this import run is attributed to (contentOwner/createdBy/
 *              updatedBy/publishedBy) - a migration run is not an interactive human editing
 *              session, but every one of those columns is NOT NULL, so an explicit actor is
 *              always required.
 */
public record WordPressImportRequest(
        WordPressPostRow post,
        WordPressDataset dataset,
        GutenbergConversionResult conversion,
        Map<Long, Long> resolvedMediaAssetIds,
        DashboardUserEntity actor
) {
}
