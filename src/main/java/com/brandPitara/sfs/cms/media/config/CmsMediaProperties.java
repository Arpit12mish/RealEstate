package com.brandPitara.sfs.cms.media.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.cms.media")
public class CmsMediaProperties {

    public static final long DEFAULT_MAX_IMAGE_BYTES = 15L * 1024 * 1024;
    public static final long DEFAULT_MAX_VIDEO_BYTES = 250L * 1024 * 1024;
    public static final String IMMUTABLE_CACHE_CONTROL = "public, max-age=31536000, immutable";

    /**
     * Private CMS bucket (CMS_MEDIA_S3_BUCKET). Required — there is no fallback to
     * app.media.s3.bucket (that bucket serves unrelated, publicly-readable project media
     * and has a different access policy). A blank value makes every CMS media endpoint
     * fail closed with 503 CMS_MEDIA_STORAGE_UNAVAILABLE via CmsMediaServiceImpl#configuredBucket.
     */
    private String bucket;
    private long maxImageBytes = DEFAULT_MAX_IMAGE_BYTES;
    private long maxVideoBytes = DEFAULT_MAX_VIDEO_BYTES;
    private int validationPrefixBytes = 256 * 1024;
    private int pendingRetentionHours = 24;
    private int failedRetentionDays = 7;
    private int cleanupBatchSize = 100;
}
