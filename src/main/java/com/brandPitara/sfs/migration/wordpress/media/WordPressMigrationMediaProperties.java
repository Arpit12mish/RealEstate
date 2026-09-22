package com.brandPitara.sfs.migration.wordpress.media;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Explicit, all-or-nothing gate for the WordPress migration ever writing a real object to S3.
 * Every field defaults to the inert/off position - none of them are set by any profile's
 * {@code application*.yml} today, so a default application startup performs no media uploads
 * regardless of this class being on the classpath and registered as a bean (see
 * {@code WordPressMigrationInertByDefaultTest}-style guarantee for the rest of this package).
 * A real upload additionally requires {@code expectedBucket} to match the actually-configured
 * CMS media bucket, as a defense against a misconfigured environment silently writing to the
 * wrong bucket.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sfs.migration.wordpress.media")
public class WordPressMigrationMediaProperties {

    private boolean enabled = false;
    private boolean dryRun = true;
    private boolean uploadEnabled = false;
    private String expectedBucket;
    private String productionConfirmationToken;

    /** True only when every explicit gate is set - never true from defaults alone. */
    public boolean isRealUploadAuthorized(String configuredBucket) {
        return enabled
                && !dryRun
                && uploadEnabled
                && expectedBucket != null && !expectedBucket.isBlank()
                && expectedBucket.equals(configuredBucket)
                && productionConfirmationToken != null && !productionConfirmationToken.isBlank();
    }
}
