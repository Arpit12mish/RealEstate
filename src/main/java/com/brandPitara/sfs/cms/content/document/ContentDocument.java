package com.brandPitara.sfs.cms.content.document;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.List;

public record ContentDocument(
        int schemaVersion,
        List<ContentBlock> blocks
) {
    public static final int MINIMUM_READABLE_SCHEMA_VERSION = 1;
    /** v4 adds LAYOUT (see ContentBlock.Layout) and TABLE.title — every earlier document
     * remains readable unchanged; only a document that actually uses these declares v4. */
    public static final int CURRENT_SCHEMA_VERSION = 4;

    public static ContentDocument empty() {
        return new ContentDocument(CURRENT_SCHEMA_VERSION, List.of());
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object ignored) {
        throw new IllegalArgumentException("Unknown document property: " + property);
    }
}
