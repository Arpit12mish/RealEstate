package com.brandPitara.sfs.migration.wordpress.media;

import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;

import java.nio.file.Path;

/**
 * @param sourceUploadPath the WordPress attachment's {@code _wp_attached_file} value, exactly as
 *                          it appears as a zip entry name in {@code zipPath}.
 * @param outputDir         a temp directory the caller owns and cleans up; only this one
 *                           attachment's bytes are ever extracted into it by this request.
 */
public record WordPressMediaImportRequest(
        long attachmentId,
        Path zipPath,
        String sourceUploadPath,
        Path outputDir,
        DashboardUserEntity actor
) {
}
