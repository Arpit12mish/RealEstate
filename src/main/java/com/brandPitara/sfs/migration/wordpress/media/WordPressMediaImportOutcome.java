package com.brandPitara.sfs.migration.wordpress.media;

public record WordPressMediaImportOutcome(
        long attachmentId,
        Long cmsMediaAssetId,
        boolean reusedExistingMapping,
        boolean reusedExistingObject,
        String objectKey,
        String errorCode
) {
}
