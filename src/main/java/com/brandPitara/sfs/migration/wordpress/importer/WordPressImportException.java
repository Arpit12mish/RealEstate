package com.brandPitara.sfs.migration.wordpress.importer;

/** Thrown when an import cannot proceed safely - never caught to silently overwrite published content. */
public class WordPressImportException extends RuntimeException {

    public WordPressImportException(String message) {
        super(message);
    }

    public static WordPressImportException fingerprintChanged(long sourcePostId, String previous, String current) {
        return new WordPressImportException(
                "WordPress post " + sourcePostId + " has a different source fingerprint than its last import "
                        + "(" + previous + " -> " + current + "). Refusing to silently overwrite an existing "
                        + "migration result - use an explicit update operation if this change is intentional."
        );
    }
}
