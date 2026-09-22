package com.brandPitara.sfs.migration.wordpress.media;

/**
 * {@code sha256Hex} is a real, independently-verifiable checksum (S3's optional
 * {@code x-amz-checksum-sha256}, requested at PUT time and read back via
 * {@code ChecksumMode.ENABLED} on HEAD) - not the general-purpose ETag
 * {@link com.brandPitara.sfs.media.service.StoredObjectMetadata} carries elsewhere in the CMS,
 * which for a simple PUT happens to be an MD5 and is never compared against a SHA-256 source hash.
 */
public record WordPressStoredObjectMetadata(long contentLength, String contentType, String sha256Hex) {
}
