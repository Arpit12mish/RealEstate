package com.brandPitara.sfs.migration.wordpress.importer;

import com.brandPitara.sfs.migration.wordpress.audit.WordPressDataset;
import com.brandPitara.sfs.migration.wordpress.dump.WordPressPostRow;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * The single source of truth for a WordPress post's idempotency fingerprint - extracted out of
 * {@link WordPressContentImportService} so a second caller (a mapping-backfill tool, reconciling
 * WordPress posts that were migrated by hand outside this importer before it ever ran) can compute
 * the exact same value the real importer would. Any divergence between two independently-written
 * copies of this logic would be a real bug: a backfilled row whose fingerprint doesn't match what
 * {@link WordPressContentImportService#importPost} computes looks "changed" on the next run and
 * throws, or - worse - a genuinely different post looks unchanged. There must be exactly one
 * implementation.
 */
public final class WordPressContentFingerprint {

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
    public static final int FINGERPRINT_FORMAT_VERSION = 2;

    private WordPressContentFingerprint() {
    }

    /**
     * Covers every field a source-side edit could make to a post that this importer cares about -
     * not just the body. v1 only hashed {@code postContent + postModifiedGmt}, which silently
     * missed a retitle, reslug, re-author, status change (draft published), category/tag
     * reassignment, featured-image swap, SEO-metadata edit, or an Elementor/Gutenberg toggle (via
     * {@code _elementor_edit_mode}) that changes classification without touching the body text at
     * all - any of those would have been treated as "unchanged" and silently skipped on a rerun.
     * Category/tag names are sorted before hashing so dump/database iteration order can never make
     * an unchanged post look changed.
     */
    public static String compute(WordPressPostRow post, WordPressDataset dataset) {
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
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** NUL-delimited with an explicit null marker, so e.g. title="a" + slug=null never collides
     * with title=null + slug="a" the way a plain "|"-join could if a field itself contained "|". */
    private static void appendField(StringBuilder basis, String value) {
        basis.append(value == null ? "\u0001" : value).append('\u0000');
    }
}
