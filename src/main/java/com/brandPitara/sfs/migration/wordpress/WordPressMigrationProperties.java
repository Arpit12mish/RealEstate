package com.brandPitara.sfs.migration.wordpress;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configurable WordPress-author-to-CMS-byline mapping for the one-time
 * u427251222_Ue8rA WordPress-to-CMS content migration. Defaults reflect that specific export's
 * verified author reconciliation (100 + 16 + 28 = 144 posts); override via
 * {@code sfs.migration.wordpress.authors} in application config for a different WordPress
 * source instead of editing this class.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sfs.migration.wordpress")
public class WordPressMigrationProperties {

    private List<AuthorMapping> authors = List.of(
            new AuthorMapping(1, "Square Foot Story", null, 100),
            new AuthorMapping(2, "Kavita Chawla", null, 16),
            new AuthorMapping(3, "Bhavna Satsangi", null, 28)
    );

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorMapping {
        private long wordPressAuthorId;
        private String displayName;
        private String designation;
        private long expectedPostCount;
    }
}
