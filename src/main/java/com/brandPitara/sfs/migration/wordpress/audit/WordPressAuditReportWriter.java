package com.brandPitara.sfs.migration.wordpress.audit;

import com.brandPitara.sfs.migration.wordpress.gutenberg.GutenbergConversionResult;
import com.brandPitara.sfs.migration.wordpress.gutenberg.UnsupportedBlockReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Writes the audit's output as plain files under a caller-supplied directory - never inside
 * {@code src/}, and callers are expected to point this at something already git-ignored (e.g.
 * {@code target/wordpress-migration-reports}). Contains no post bodies, no credentials, no
 * WordPress user data beyond display name.
 */
public final class WordPressAuditReportWriter {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void writeAll(WordPressAuditRunner.AuditResult result, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        writeJson(outputDir.resolve("migration-summary.json"), result.summary());
        writePostsCsv(outputDir.resolve("posts.csv"), result.posts());
        writeJson(outputDir.resolve("posts.json"), result.posts());
        writeUnsupportedBlocksCsv(outputDir.resolve("unsupported-blocks.csv"), result.conversions());
        writeUnresolvedMediaCsv(outputDir.resolve("missing-media.csv"), result.conversions());
        writeUnresolvedGalleriesCsv(outputDir.resolve("unresolved-galleries.csv"), result.conversions());
        writeConversionReadinessCsv(outputDir.resolve("conversion-readiness.csv"), result.posts());
        writeManualReviewAndBlockedCsv(outputDir.resolve("manual-review-and-blocked.csv"), result.posts());
        writeJson(outputDir.resolve("elementor-report.json"), result.summary().elementorPosts());
    }

    private void writeJson(Path path, Object value) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            objectMapper.writeValue(writer, value);
        }
    }

    private void writePostsCsv(Path path, List<PostAuditEntry> posts) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("wordPressPostId,title,slug,status,authorId,authorDisplayName,categories,tags,"
                    + "targetContentType,hasFeaturedImage,blankSlug,emptyExcerpt,classification,"
                    + "warningCount,blockingErrorCount,unsupportedBlockCount,unresolvedMediaCount,"
                    + "meaningfulContentLost,reason,remediation,manualReviewRequired,"
                    + "eligibleForAutomaticPublication\n");
            for (PostAuditEntry post : posts) {
                writer.write(String.join(",",
                        csv(post.wordPressPostId()), csv(post.title()), csv(post.slug()), csv(post.status()),
                        csv(post.authorId()), csv(post.authorDisplayName()),
                        csv(String.join(";", post.categories())), csv(String.join(";", post.tags())),
                        csv(post.targetContentType()), csv(post.hasFeaturedImage()), csv(post.blankSlug()),
                        csv(post.emptyExcerpt()), csv(post.classification()), csv(post.warningCount()),
                        csv(post.blockingErrorCount()), csv(post.unsupportedBlockCount()),
                        csv(post.unresolvedMediaCount()), csv(post.meaningfulContentLost()),
                        csv(post.reason()), csv(post.remediation()),
                        csv(post.manualReviewRequired()), csv(post.eligibleForAutomaticPublication())
                ));
                writer.write("\n");
            }
        }
    }

    private void writeManualReviewAndBlockedCsv(Path path, List<PostAuditEntry> posts) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("wordPressPostId,title,status,classification,reason,unresolvedMediaCount,"
                    + "meaningfulContentLost,eligibleForAutomaticPublication,remediation\n");
            for (PostAuditEntry post : posts) {
                if (post.classification() != PostClassification.MANUAL_REVIEW_REQUIRED
                        && post.classification() != PostClassification.BLOCKED) {
                    continue;
                }
                writer.write(String.join(",",
                        csv(post.wordPressPostId()), csv(post.title()), csv(post.status()),
                        csv(post.classification()), csv(post.reason()), csv(post.unresolvedMediaCount()),
                        csv(post.meaningfulContentLost()), csv(post.eligibleForAutomaticPublication()),
                        csv(post.remediation())
                ));
                writer.write("\n");
            }
        }
    }

    private void writeUnresolvedGalleriesCsv(Path path, Map<Long, GutenbergConversionResult> conversions) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("wordPressPostId,title,path,resolvedAttachmentIds,unresolvedImageUrls,fullyUnresolved\n");
            for (GutenbergConversionResult result : conversions.values()) {
                for (var gallery : result.unresolvedGalleries()) {
                    writer.write(String.join(",",
                            csv(result.wordPressPostId()), csv(result.title()), csv(gallery.path()),
                            csv(String.join(";", gallery.resolvedAttachmentIds().stream().map(String::valueOf).toList())),
                            csv(String.join(";", gallery.unresolvedImageUrls())), csv(gallery.fullyUnresolved())
                    ));
                    writer.write("\n");
                }
            }
        }
    }

    private void writeUnsupportedBlocksCsv(Path path, Map<Long, GutenbergConversionResult> conversions) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("wordPressPostId,title,blockName,path,hadMeaningfulContent,disposition\n");
            for (GutenbergConversionResult result : conversions.values()) {
                for (UnsupportedBlockReport block : result.unsupportedBlocks()) {
                    writer.write(String.join(",",
                            csv(result.wordPressPostId()), csv(result.title()), csv(block.blockName()),
                            csv(block.path()), csv(block.hadMeaningfulContent()), csv(block.disposition())
                    ));
                    writer.write("\n");
                }
            }
        }
    }

    private void writeUnresolvedMediaCsv(Path path, Map<Long, GutenbergConversionResult> conversions) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("wordPressPostId,title,externalUrl\n");
            for (GutenbergConversionResult result : conversions.values()) {
                for (String url : result.referencedExternalUrls()) {
                    writer.write(String.join(",", csv(result.wordPressPostId()), csv(result.title()), csv(url)));
                    writer.write("\n");
                }
            }
        }
    }

    private void writeConversionReadinessCsv(Path path, List<PostAuditEntry> posts) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("wordPressPostId,title,status,classification,eligibleForAutomaticPublication\n");
            for (PostAuditEntry post : posts) {
                writer.write(String.join(",",
                        csv(post.wordPressPostId()), csv(post.title()), csv(post.status()),
                        csv(post.classification()), csv(post.eligibleForAutomaticPublication())
                ));
                writer.write("\n");
            }
        }
    }

    private String csv(Object value) {
        String text = value == null ? "" : value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
