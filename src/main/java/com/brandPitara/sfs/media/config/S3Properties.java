package com.brandPitara.sfs.media.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.media.s3")
public class S3Properties {
    private String bucket;
    private String region;
    private String publicBaseUrl; // CloudFront base URL later
    private int presignExpirySeconds = 300;
    private long maxPromoBannerVideoBytes = 25L * 1024 * 1024;

    /**
     * Bounded call timeouts for S3Client's real network operations (putObject/
     * deleteObject - InstagramAssetCacheServiceImpl, S3ObjectService). Without
     * these, the AWS SDK v2 default has no overall apiCallTimeout, so an S3-side
     * hang can block the calling thread far longer than a request should wait.
     * S3Presigner never makes a network call (presigning is local crypto), so
     * it does not need these.
     */
    private int apiCallTimeoutMs = 10000;
    private int apiCallAttemptTimeoutMs = 5000;
}
