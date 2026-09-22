package com.brandPitara.sfs.migration.wordpress.media;

import com.brandPitara.sfs.cms.media.domain.CmsMediaType;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Builds content-addressed object keys under the same {@code cms/images/}/{@code cms/videos/}
 * managed roots {@code CloudFrontPublicMediaUrlResolver} requires - the existing dashboard-upload
 * key builder ({@code CmsMediaServiceImpl.buildStorageKey}) uses a random UUID instead, since a
 * human-initiated upload has no natural content-addressing need. This migration's key is derived
 * purely from the SHA-256 of the bytes (never the filename) so two WordPress attachments with
 * byte-identical content - even under different filenames, e.g. the same "Download SFS App" image
 * re-exported under two different names - resolve to the exact same key and are therefore never
 * uploaded twice; see {@link WordPressMediaImportService} for how that physical-object dedup is
 * used. Never derives any part of the key from a WordPress URL or filename.
 */
public final class WordPressMediaObjectKeyFactory {

    private WordPressMediaObjectKeyFactory() {
    }

    public static String build(CmsMediaType mediaType, String contentType, String sha256Hex, OffsetDateTime now) {
        String family = mediaType == CmsMediaType.IMAGE ? "images" : "videos";
        String extension = extensionFor(contentType);
        String checksum = sha256Hex.substring(0, Math.min(32, sha256Hex.length()));
        return "cms/" + family + "/wordpress/" + now.getYear() + "/"
                + String.format("%02d", now.getMonthValue())
                + "/wp-" + checksum + "." + extension;
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "video/mp4" -> "mp4";
            default -> throw new IllegalStateException(
                    "Unsupported content type reached key generation: " + contentType
                            + " - it must be rejected before this point.");
        };
    }

    /**
     * Strips any directory component, keeps only the filename stem, and reduces it to a small
     * safe alphabet - no {@code ..}, no absolute path, no backslash, no path separator at all can
     * survive. Used for the {@code CmsMediaAssetEntity.originalFilename} column, never for the
     * object key itself.
     */
    public static String sanitizeFilename(String filename) {
        String base = filename == null ? "" : filename;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        int dot = base.lastIndexOf('.');
        String stem = dot > 0 ? base.substring(0, dot) : base;
        String sanitized = stem.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-_]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (sanitized.isBlank()) {
            sanitized = "file";
        }
        return sanitized.length() > 80 ? sanitized.substring(0, 80) : sanitized;
    }
}
