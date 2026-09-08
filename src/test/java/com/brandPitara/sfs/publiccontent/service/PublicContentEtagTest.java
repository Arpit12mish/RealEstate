package com.brandPitara.sfs.publiccontent.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PublicContentEtagTest {
    private final PublicContentEtag etags = new PublicContentEtag();

    @Test
    void etagIsDeterministicAndChangesForRevisionOrPublicationEvent() {
        OffsetDateTime published = OffsetDateTime.parse("2026-08-19T12:00:00+05:30");
        String first = etags.create(10L, published);

        assertThat(etags.create(10L, published)).isEqualTo(first);
        assertThat(etags.create(11L, published)).isNotEqualTo(first);
        assertThat(etags.create(10L, published.plusSeconds(1))).isNotEqualTo(first);
        assertThat(first).startsWith("\"cms-").endsWith("\"");
    }

    @Test
    void conditionalMatchingSupportsListsWeakTagsAndWildcard() {
        String etag = etags.create(10L, OffsetDateTime.parse("2026-08-19T12:00:00Z"));

        assertThat(etags.matches(etag, etag)).isTrue();
        assertThat(etags.matches("\"other\", W/" + etag, etag)).isTrue();
        assertThat(etags.matches("*", etag)).isTrue();
        assertThat(etags.matches("\"other\"", etag)).isFalse();
        assertThat(etags.matches(null, etag)).isFalse();
    }
}
