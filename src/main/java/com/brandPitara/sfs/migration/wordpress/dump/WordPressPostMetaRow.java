package com.brandPitara.sfs.migration.wordpress.dump;

public record WordPressPostMetaRow(long metaId, long postId, String metaKey, String metaValue) {
}
