package com.brandPitara.sfs.migration.wordpress.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordPressMediaExtractorTest {

    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02, 0x03};
    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};

    @Test
    void extractsOnlyRequestedEntriesWithChecksumAndSniffedMimeType(@TempDir Path tempDir) throws IOException {
        Path zip = buildZip(tempDir, Map.of(
                "2024/05/photo.jpg", JPEG_BYTES,
                "2024/05/photo-1024x576.jpg", JPEG_BYTES,
                "2024/06/other.png", PNG_BYTES
        ));
        Path outputDir = tempDir.resolve("extracted");

        var extractor = new WordPressMediaExtractor(10_000, 100_000);
        Map<Long, WordPressMediaExtractor.ExtractedMedia> result = extractor.extract(
                zip, Map.of(1L, "2024/05/photo.jpg"), outputDir
        );

        assertThat(result).hasSize(1);
        var media = result.get(1L);
        assertThat(media.detectedMimeType()).isEqualTo("image/jpeg");
        assertThat(media.sizeBytes()).isEqualTo(JPEG_BYTES.length);
        assertThat(media.sha256Hex()).isEqualTo(sha256Hex(JPEG_BYTES));
        assertThat(Files.exists(media.localFile())).isTrue();
        // The unrequested variant/other file is never extracted.
        assertThat(Files.exists(outputDir.resolve("2024/05/photo-1024x576.jpg"))).isFalse();
        assertThat(Files.exists(outputDir.resolve("2024/06/other.png"))).isFalse();
    }

    @Test
    void detectsMimeTypeFromMagicBytesNotFromExtension(@TempDir Path tempDir) throws IOException {
        // A file named ".jpg" that is actually PNG bytes - extension must not be trusted.
        Path zip = buildZip(tempDir, Map.of("2024/05/mislabeled.jpg", PNG_BYTES));
        var extractor = new WordPressMediaExtractor(10_000, 100_000);

        Map<Long, WordPressMediaExtractor.ExtractedMedia> result = extractor.extract(
                zip, Map.of(1L, "2024/05/mislabeled.jpg"), tempDir.resolve("out")
        );

        assertThat(result.get(1L).detectedMimeType()).isEqualTo("image/png");
    }

    @Test
    void rejectsZipEntryPathTraversal(@TempDir Path tempDir) throws IOException {
        Path zip = buildZip(tempDir, Map.of("../../etc/passwd", JPEG_BYTES));
        var extractor = new WordPressMediaExtractor(10_000, 100_000);

        assertThatThrownBy(() -> extractor.extract(
                zip, Map.of(1L, "../../etc/passwd"), tempDir.resolve("out")
        )).isInstanceOf(IOException.class).hasMessageContaining("unsafe path");
    }

    @Test
    void rejectsAnEntryExceedingThePerEntryByteLimit(@TempDir Path tempDir) throws IOException {
        byte[] big = new byte[2000];
        System.arraycopy(JPEG_BYTES, 0, big, 0, JPEG_BYTES.length);
        Path zip = buildZip(tempDir, Map.of("2024/05/big.jpg", big));
        var extractor = new WordPressMediaExtractor(1000, 100_000);

        assertThatThrownBy(() -> extractor.extract(
                zip, Map.of(1L, "2024/05/big.jpg"), tempDir.resolve("out")
        )).isInstanceOf(IOException.class).hasMessageContaining("per-entry limit");
    }

    @Test
    void rejectsWhenTotalExtractionExceedsTheConfiguredLimit(@TempDir Path tempDir) throws IOException {
        byte[] fileA = new byte[600];
        byte[] fileB = new byte[600];
        Path zip = buildZip(tempDir, Map.of("2024/05/a.jpg", fileA, "2024/05/b.jpg", fileB));
        var extractor = new WordPressMediaExtractor(10_000, 1000);

        assertThatThrownBy(() -> extractor.extract(
                zip, Map.of(1L, "2024/05/a.jpg", 2L, "2024/05/b.jpg"), tempDir.resolve("out")
        )).isInstanceOf(IOException.class).hasMessageContaining("total limit");
    }

    @Test
    void entriesNotInTheRequestedMapAreSkippedWithoutExtractingTheWholeArchive(@TempDir Path tempDir) throws IOException {
        Path zip = buildZip(tempDir, Map.of(
                "2024/05/wanted.jpg", JPEG_BYTES,
                "elementor/unrelated.css", "body{}".getBytes()
        ));
        var extractor = new WordPressMediaExtractor(10_000, 100_000);

        Map<Long, WordPressMediaExtractor.ExtractedMedia> result = extractor.extract(
                zip, Map.of(1L, "2024/05/wanted.jpg"), tempDir.resolve("out")
        );

        assertThat(result).hasSize(1);
    }

    private Path buildZip(Path tempDir, Map<String, byte[]> entries) throws IOException {
        Path zip = tempDir.resolve("uploads-" + System.nanoTime() + ".zip");
        try (var out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return zip;
    }

    private String sha256Hex(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
