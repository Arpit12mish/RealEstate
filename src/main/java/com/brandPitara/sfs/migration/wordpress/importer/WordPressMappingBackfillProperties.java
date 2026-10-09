package com.brandPitara.sfs.migration.wordpress.importer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Its own explicit, all-or-nothing gate for the mapping-backfill tool - deliberately separate from
 * {@code WordPressMigrationMediaProperties}. Backfilling never uploads media or writes {@code
 * content_post}, so it has no reason to share the media-upload authorization token: a token leak
 * or misconfiguration on one tool must never grant authority over the other. Every field defaults
 * to the inert/off position - a default application startup never authorizes a backfill write
 * regardless of this class being on the classpath.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sfs.migration.wordpress.mapping-backfill")
public class WordPressMappingBackfillProperties {

    private boolean enabled = false;
    private String confirmationToken;

    /** True only when every explicit gate is set - never true from defaults alone. */
    public boolean isAuthorized(String suppliedConfirm) {
        return enabled
                && confirmationToken != null && !confirmationToken.isBlank()
                && suppliedConfirm != null && suppliedConfirm.equals(confirmationToken);
    }
}
