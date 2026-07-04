package com.brandPitara.sfs.instagram.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "sfs.instagram.meta")
public class InstagramMetaProperties {

    private String graphApiVersion = "v25.0";
    private String appId;
    private String appSecret;
    private String accessToken;
    private String facebookPageId;
    private String instagramBusinessAccountId;
    private boolean syncEnabled = false;
    private String syncCron = "0 0 * * * *";
    private int publicLimit = 10;
    private int requestTimeoutSeconds = 15;

    public int effectivePublicLimit() {
        return Math.min(Math.max(publicLimit, 1), 20);
    }

    public int effectiveRequestTimeoutSeconds() {
        return Math.max(requestTimeoutSeconds, 1);
    }

    public boolean hasRequiredSyncConfig() {
        return hasText(accessToken) && hasText(instagramBusinessAccountId) && hasText(graphApiVersion);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
