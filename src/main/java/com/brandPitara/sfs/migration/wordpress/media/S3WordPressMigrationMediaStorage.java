package com.brandPitara.sfs.migration.wordpress.media;

import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.media.service.MediaObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Path;
import java.util.Base64;

/**
 * Production {@link WordPressMigrationMediaStorage}: delegates to the application's existing
 * {@code S3Client} bean (no new AWS config, no new credentials, no duplicated bucket/region
 * properties) for the one capability {@link MediaObjectStorageService} does not expose - a
 * direct, non-presigned PUT with a real, independently-verifiable SHA-256 checksum - and reuses
 * {@link MediaObjectStorageService#delete} for orphan cleanup. {@link WordPressMigrationMediaProperties}
 * gates every mutating call so this class is inert unless migration mode is explicitly, fully
 * enabled (see {@link WordPressMigrationMediaProperties#isRealUploadAuthorized}).
 */
@Component
@RequiredArgsConstructor
public class S3WordPressMigrationMediaStorage implements WordPressMigrationMediaStorage {

    private final S3Client s3Client;
    private final MediaObjectStorageService delegate;
    private final CmsMediaProperties cmsMediaProperties;
    private final WordPressMigrationMediaProperties migrationProperties;

    @Override
    public void store(String bucket, String key, Path localFile, String contentType) {
        requireUploadAuthorized();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .checksumAlgorithm(ChecksumAlgorithm.SHA256)
                .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                        .putHeader("If-None-Match", "*")
                        .build())
                .build();
        s3Client.putObject(request, RequestBody.fromFile(localFile));
    }

    @Override
    public WordPressStoredObjectMetadata head(String bucket, String key) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket).key(key).checksumMode(ChecksumMode.ENABLED).build());
            String sha256Hex = response.checksumSHA256() == null ? null : toHex(Base64.getDecoder().decode(response.checksumSHA256()));
            return new WordPressStoredObjectMetadata(response.contentLength(), response.contentType(), sha256Hex);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new WordPressMediaStorageObjectNotFoundException(bucket, key);
            }
            throw exception;
        }
    }

    @Override
    public boolean exists(String bucket, String key) {
        try {
            head(bucket, key);
            return true;
        } catch (WordPressMediaStorageObjectNotFoundException notFound) {
            return false;
        }
    }

    @Override
    public void deleteOrphan(String bucket, String key) {
        requireUploadAuthorized();
        delegate.delete(bucket, key);
    }

    private void requireUploadAuthorized() {
        if (!migrationProperties.isRealUploadAuthorized(cmsMediaProperties.getBucket())) {
            throw new WordPressMediaUploadNotAuthorizedException();
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
