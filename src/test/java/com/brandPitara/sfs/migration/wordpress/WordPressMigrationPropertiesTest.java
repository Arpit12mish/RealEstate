package com.brandPitara.sfs.migration.wordpress;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WordPressMigrationPropertiesTest {

    @Test
    void defaultsMatchTheVerifiedU427251222Ue8raAuthorReconciliation() {
        WordPressMigrationProperties properties = new WordPressMigrationProperties();

        assertThat(properties.getAuthors()).hasSize(3);
        assertThat(properties.getAuthors().stream().mapToLong(
                WordPressMigrationProperties.AuthorMapping::getExpectedPostCount
        ).sum()).isEqualTo(144);

        assertThat(mapping(properties, 1).getDisplayName()).isEqualTo("Square Foot Story");
        assertThat(mapping(properties, 1).getExpectedPostCount()).isEqualTo(100);
        assertThat(mapping(properties, 2).getDisplayName()).isEqualTo("Kavita Chawla");
        assertThat(mapping(properties, 2).getExpectedPostCount()).isEqualTo(16);
        assertThat(mapping(properties, 3).getDisplayName()).isEqualTo("Bhavna Satsangi");
        assertThat(mapping(properties, 3).getExpectedPostCount()).isEqualTo(28);
    }

    private WordPressMigrationProperties.AuthorMapping mapping(WordPressMigrationProperties properties, long id) {
        return properties.getAuthors().stream()
                .filter(m -> m.getWordPressAuthorId() == id)
                .findFirst()
                .orElseThrow();
    }
}
