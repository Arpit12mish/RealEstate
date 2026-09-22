package com.brandPitara.sfs.migration.wordpress.audit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A read-only index of {@code uploads.zip}'s entry paths, built by streaming ZIP metadata only -
 * never extracting file contents (see {@code ZipInputStream}, which exposes each entry's name
 * without materializing its bytes unless explicitly read). Real uploads live directly under
 * {@code 2024/}, {@code 2025/}, {@code 2026/}; plugin/theme directories (elementor, rank-math,
 * template-kits, astra-docs, ...) are indexed too but excluded from "real media" queries unless
 * a caller explicitly asks for them.
 */
public final class WordPressMediaArchiveIndex {

    private final Set<String> entries;

    private WordPressMediaArchiveIndex(Set<String> entries) {
        this.entries = entries;
    }

    public static WordPressMediaArchiveIndex build(Path zipPath) throws IOException {
        Set<String> entries = new LinkedHashSet<>();
        try (InputStream in = java.nio.file.Files.newInputStream(zipPath);
             ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.add(entry.getName());
                }
                zip.closeEntry();
            }
        }
        return new WordPressMediaArchiveIndex(entries);
    }

    public boolean exists(String relativePath) {
        return entries.contains(relativePath);
    }

    public boolean isUnderMediaYearDirectory(String relativePath) {
        return relativePath.matches("^20(2[4-9]|[3-9]\\d)/.*");
    }

    public int totalEntryCount() {
        return entries.size();
    }

    public Set<String> entries() {
        return entries;
    }
}
