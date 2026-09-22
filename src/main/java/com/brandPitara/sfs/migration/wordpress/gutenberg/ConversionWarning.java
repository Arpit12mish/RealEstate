package com.brandPitara.sfs.migration.wordpress.gutenberg;

/** A non-blocking note about a lossy or best-effort conversion decision - the post can still be published. */
public record ConversionWarning(String blockName, String path, String message) {
}
