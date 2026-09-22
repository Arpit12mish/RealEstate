package com.brandPitara.sfs.migration.wordpress.media;

/** COMPLETED always has a verified, READY {@code cms_media_asset_id}; FAILED never does. */
public enum WordPressMediaMigrationState {
    COMPLETED,
    FAILED
}
