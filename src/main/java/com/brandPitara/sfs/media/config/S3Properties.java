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
}
