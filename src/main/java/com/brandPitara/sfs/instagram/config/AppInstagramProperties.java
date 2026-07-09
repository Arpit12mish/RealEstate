package com.brandPitara.sfs.instagram.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.instagram")
public class AppInstagramProperties {

    private String fallbackThumbnailUrl = "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/banner/Hero/M3M-Jacob-%26-Co-1.webp";
    private int thumbnailCacheTtlHours = 24;
    private int assetDownloadTimeoutSeconds = 10;
    private long maxThumbnailBytes = 5L * 1024L * 1024L;

    public int effectiveThumbnailCacheTtlHours() {
        return Math.max(thumbnailCacheTtlHours, 1);
    }

    public int effectiveAssetDownloadTimeoutSeconds() {
        return Math.max(assetDownloadTimeoutSeconds, 1);
    }

    public long effectiveMaxThumbnailBytes() {
        return Math.max(maxThumbnailBytes, 1024L);
    }
}
