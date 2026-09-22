package com.brandPitara.sfs.migration.wordpress.media;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Accumulates objects this run could not safely delete itself (because they might be shared with
 * an existing mapping, or the delete attempt itself failed) so a human can review and clean them
 * up explicitly - see the recovery process documented on {@link WordPressMediaImportService}.
 * Never deletes anything itself.
 */
@Component
public class WordPressMediaOrphanCleanupReport {

    public record Entry(long attachmentId, String bucket, String key, String reason) {
    }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void record(long attachmentId, String bucket, String key, String reason) {
        entries.add(new Entry(attachmentId, bucket, key, reason));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }
}
