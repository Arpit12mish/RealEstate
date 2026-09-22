package com.brandPitara.sfs.migration.wordpress.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts only the specific media entries a representative import actually needs, from
 * {@code uploads.zip}, into a caller-supplied temporary directory - never the whole 2 GB
 * archive. Uses {@link ZipFile} (central-directory based random access) rather than a linear
 * {@code ZipInputStream} scan, so each requested entry is looked up directly by name instead of
 * scanning every entry in the archive - this is both faster and correct against archives whose
 * per-entry local headers use a streaming/EXT-descriptor framing {@code ZipInputStream} rejects.
 * Enforces per-entry and total byte limits and rejects any entry name that could escape the
 * output directory (path traversal). MIME type is sniffed from the file's own magic bytes, never
 * trusted from the filename extension.
 */
public final class WordPressMediaExtractor {

    public record ExtractedMedia(
            long attachmentId, String relativePath, Path localFile, String sha256Hex,
            String detectedMimeType, long sizeBytes
    ) {
    }

    private final long maxEntryBytes;
    private final long maxTotalBytes;

    public WordPressMediaExtractor(long maxEntryBytes, long maxTotalBytes) {
        this.maxEntryBytes = maxEntryBytes;
        this.maxTotalBytes = maxTotalBytes;
    }

    /**
     * @param attachmentIdToRelativePath WordPress attachment ID -> its {@code _wp_attached_file}
     *                                   value (e.g. {@code "2024/05/photo.jpg"}), exactly as it
     *                                   appears as a zip entry name.
     */
    public Map<Long, ExtractedMedia> extract(
            Path zipPath, Map<Long, String> attachmentIdToRelativePath, Path outputDir
    ) throws IOException {
        Files.createDirectories(outputDir);
        for (String name : attachmentIdToRelativePath.values()) {
            rejectPathTraversal(name);
        }

        Map<Long, ExtractedMedia> results = new HashMap<>();
        AtomicLong totalExtracted = new AtomicLong();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            for (Map.Entry<Long, String> request : attachmentIdToRelativePath.entrySet()) {
                long attachmentId = request.getKey();
                String name = request.getValue();
                ZipEntry entry = zipFile.getEntry(name);
                if (entry == null || entry.isDirectory()) {
                    continue;
                }
                Path destination = safeResolve(outputDir, name);
                results.put(attachmentId, streamEntry(zipFile, entry, attachmentId, name, destination, totalExtracted));
            }
        }
        return results;
    }

    /**
     * Strict single-attachment resolution used by the real media-import lifecycle: fails closed
     * (rather than silently omitting the attachment, as the batch {@link #extract} does) when the
     * entry is missing, or when its name collides with another entry case-insensitively or
     * literally (a malformed archive containing two entries with the same name).
     */
    public ExtractedMedia extractOne(
            Path zipPath, long attachmentId, String relativePath, Path outputDir
    ) throws IOException {
        rejectPathTraversal(relativePath);
        Path destination = safeResolve(outputDir, relativePath);

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            int exactMatches = 0;
            List<String> caseInsensitiveMatches = new ArrayList<>();
            String lowerTarget = relativePath.toLowerCase(Locale.ROOT);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry candidate = entries.nextElement();
                if (candidate.isDirectory()) {
                    continue;
                }
                if (candidate.getName().equals(relativePath)) {
                    exactMatches++;
                }
                if (candidate.getName().toLowerCase(Locale.ROOT).equals(lowerTarget)) {
                    caseInsensitiveMatches.add(candidate.getName());
                }
            }
            if (exactMatches == 0) {
                throw WordPressMediaImportException.missingZipEntry(attachmentId, relativePath);
            }
            if (exactMatches > 1 || caseInsensitiveMatches.size() > 1) {
                throw WordPressMediaImportException.ambiguousZipEntry(attachmentId, relativePath);
            }

            ZipEntry entry = zipFile.getEntry(relativePath);
            return streamEntry(zipFile, entry, attachmentId, relativePath, destination, new AtomicLong());
        }
    }

    private ExtractedMedia streamEntry(
            ZipFile zipFile, ZipEntry entry, long attachmentId, String name, Path destination,
            AtomicLong totalExtracted
    ) throws IOException {
        Files.createDirectories(destination.getParent());
        MessageDigest digest = sha256();
        long entrySize = 0;
        byte[] buffer = new byte[8192];
        byte[] header = new byte[16];
        int headerLength = 0;
        try (
                InputStream in = zipFile.getInputStream(entry);
                var out = Files.newOutputStream(
                        destination, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                )
        ) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                entrySize += read;
                long runningTotal = totalExtracted.addAndGet(read);
                if (entrySize > maxEntryBytes) {
                    throw new IOException("Zip entry '" + name + "' exceeds the per-entry limit of "
                            + maxEntryBytes + " bytes.");
                }
                if (runningTotal > maxTotalBytes) {
                    throw new IOException("Selective extraction exceeded the total limit of "
                            + maxTotalBytes + " bytes.");
                }
                out.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                if (headerLength < header.length) {
                    int toCopy = Math.min(read, header.length - headerLength);
                    System.arraycopy(buffer, 0, header, headerLength, toCopy);
                    headerLength += toCopy;
                }
            }
        }
        return new ExtractedMedia(
                attachmentId, name, destination, toHex(digest.digest()),
                sniffMimeType(header, headerLength, name), entrySize
        );
    }

    private void rejectPathTraversal(String entryName) throws IOException {
        if (entryName.startsWith("/") || entryName.contains("..")) {
            throw new IOException("Rejected zip entry with unsafe path: " + entryName);
        }
    }

    private Path safeResolve(Path outputDir, String entryName) throws IOException {
        Path resolved = outputDir.resolve(entryName).normalize();
        if (!resolved.startsWith(outputDir.normalize())) {
            throw new IOException("Rejected zip entry escaping the output directory: " + entryName);
        }
        return resolved;
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** Magic-byte sniffing for the formats WordPress media actually uses - never trusts the extension. */
    private String sniffMimeType(byte[] header, int length, String fallbackNameForLogging) {
        if (length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (length >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
            return "image/png";
        }
        if (length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "image/webp";
        }
        if (length >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F'
                && header[3] == '8' && (header[4] == '7' || header[4] == '9') && header[5] == 'a') {
            return "image/gif";
        }
        if (length >= 12 && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
            return "video/mp4";
        }
        return "application/octet-stream";
    }
}
