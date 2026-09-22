package com.brandPitara.sfs.migration.wordpress.dump;

public record WordPressTermTaxonomyRow(long termTaxonomyId, long termId, String taxonomy, Long parent) {
}
