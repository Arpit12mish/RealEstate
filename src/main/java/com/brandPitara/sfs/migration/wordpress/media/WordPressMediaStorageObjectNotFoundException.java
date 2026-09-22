package com.brandPitara.sfs.migration.wordpress.media;

public class WordPressMediaStorageObjectNotFoundException extends RuntimeException {
    public WordPressMediaStorageObjectNotFoundException(String bucket, String key) {
        super("No object at " + bucket + "/" + key);
    }
}
