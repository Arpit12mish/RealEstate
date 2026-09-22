package com.brandPitara.sfs.migration.wordpress.dump;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

/** Opens a mysqldump export file as a buffered byte stream, transparently decompressing {@code .sql.gz}. */
public final class WordPressDumpSource {

    private WordPressDumpSource() {
    }

    public static InputStream open(Path path) throws IOException {
        InputStream raw = Files.newInputStream(path);
        String name = path.getFileName().toString();
        if (name.endsWith(".gz")) {
            return new BufferedInputStream(new GZIPInputStream(raw), 64 * 1024);
        }
        return new BufferedInputStream(raw, 64 * 1024);
    }
}
