package com.brandPitara.sfs.migration.wordpress.gutenberg;

/** A defect that must block automatic publication - either meaningful content would be lost, or the resulting document fails CMS validation. */
public record ConversionBlockingError(String blockName, String path, String message) {
}
