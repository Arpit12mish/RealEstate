package com.brandPitara.sfs.migration.wordpress.media;

import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.media.service.MediaObjectStorageService;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the production storage path is inert by default: a real S3 write is refused unless
 * every explicit gate in {@link WordPressMigrationMediaProperties} is set, matching requirement
 * "the real S3 implementation must be inert unless migration mode is explicitly enabled." Never
 * contacts real AWS - {@code S3Client}/{@code MediaObjectStorageService} are mocked.
 */
class S3WordPressMigrationMediaStorageTest {

    @Test
    void storeIsRefusedByDefaultConfiguration(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        S3Client s3Client = mock(S3Client.class);
        MediaObjectStorageService delegate = mock(MediaObjectStorageService.class);
        CmsMediaProperties cmsMediaProperties = new CmsMediaProperties();
        cmsMediaProperties.setBucket("real-cms-bucket");
        WordPressMigrationMediaProperties migrationProperties = new WordPressMigrationMediaProperties();
        var storage = new S3WordPressMigrationMediaStorage(s3Client, delegate, cmsMediaProperties, migrationProperties);
        Path file = tempDir.resolve("x.jpg");
        Files.write(file, new byte[]{1, 2, 3});

        assertThatThrownBy(() -> storage.store("real-cms-bucket", "cms/images/wordpress/2026/09/wp-x.jpg", file, "image/jpeg"))
                .isInstanceOf(WordPressMediaUploadNotAuthorizedException.class);
        verify(s3Client, never()).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void deleteOrphanIsRefusedByDefaultConfiguration() {
        S3Client s3Client = mock(S3Client.class);
        MediaObjectStorageService delegate = mock(MediaObjectStorageService.class);
        CmsMediaProperties cmsMediaProperties = new CmsMediaProperties();
        cmsMediaProperties.setBucket("real-cms-bucket");
        WordPressMigrationMediaProperties migrationProperties = new WordPressMigrationMediaProperties();
        var storage = new S3WordPressMigrationMediaStorage(s3Client, delegate, cmsMediaProperties, migrationProperties);

        assertThatThrownBy(() -> storage.deleteOrphan("real-cms-bucket", "cms/images/wordpress/2026/09/wp-x.jpg"))
                .isInstanceOf(WordPressMediaUploadNotAuthorizedException.class);
        verify(delegate, never()).delete(any(), any());
    }

    @Test
    void headAndExistsAreNeverGatedEvenWithDefaultConfiguration() {
        S3Client s3Client = mock(S3Client.class);
        MediaObjectStorageService delegate = mock(MediaObjectStorageService.class);
        CmsMediaProperties cmsMediaProperties = new CmsMediaProperties();
        cmsMediaProperties.setBucket("real-cms-bucket");
        WordPressMigrationMediaProperties migrationProperties = new WordPressMigrationMediaProperties();
        HeadObjectResponse response = HeadObjectResponse.builder()
                .contentLength(10L).contentType("image/jpeg").build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(response);
        var storage = new S3WordPressMigrationMediaStorage(s3Client, delegate, cmsMediaProperties, migrationProperties);

        assertThat(storage.exists("real-cms-bucket", "key")).isTrue();
    }

    @Test
    void storeSucceedsOnlyWhenEveryExplicitGateIsSetAndBucketMatches(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        S3Client s3Client = mock(S3Client.class);
        MediaObjectStorageService delegate = mock(MediaObjectStorageService.class);
        CmsMediaProperties cmsMediaProperties = new CmsMediaProperties();
        cmsMediaProperties.setBucket("real-cms-bucket");
        WordPressMigrationMediaProperties migrationProperties = new WordPressMigrationMediaProperties();
        migrationProperties.setEnabled(true);
        migrationProperties.setDryRun(false);
        migrationProperties.setUploadEnabled(true);
        migrationProperties.setExpectedBucket("real-cms-bucket");
        migrationProperties.setProductionConfirmationToken("confirmed-migration-2026");
        var storage = new S3WordPressMigrationMediaStorage(s3Client, delegate, cmsMediaProperties, migrationProperties);
        Path file = tempDir.resolve("x.jpg");
        Files.write(file, new byte[]{1, 2, 3});

        storage.store("real-cms-bucket", "cms/images/wordpress/2026/09/wp-x.jpg", file, "image/jpeg");

        verify(s3Client).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void storeIsRefusedWhenExpectedBucketDoesNotMatchTheConfiguredBucket(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        S3Client s3Client = mock(S3Client.class);
        MediaObjectStorageService delegate = mock(MediaObjectStorageService.class);
        CmsMediaProperties cmsMediaProperties = new CmsMediaProperties();
        cmsMediaProperties.setBucket("real-cms-bucket");
        WordPressMigrationMediaProperties migrationProperties = new WordPressMigrationMediaProperties();
        migrationProperties.setEnabled(true);
        migrationProperties.setDryRun(false);
        migrationProperties.setUploadEnabled(true);
        migrationProperties.setExpectedBucket("wrong-bucket");
        migrationProperties.setProductionConfirmationToken("confirmed-migration-2026");
        var storage = new S3WordPressMigrationMediaStorage(s3Client, delegate, cmsMediaProperties, migrationProperties);
        Path file = tempDir.resolve("x.jpg");
        Files.write(file, new byte[]{1, 2, 3});

        assertThatThrownBy(() -> storage.store("real-cms-bucket", "cms/images/wordpress/2026/09/wp-x.jpg", file, "image/jpeg"))
                .isInstanceOf(WordPressMediaUploadNotAuthorizedException.class);
    }
}
