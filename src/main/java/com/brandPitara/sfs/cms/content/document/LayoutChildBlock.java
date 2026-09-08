package com.brandPitara.sfs.cms.content.document;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The block types a LAYOUT section may contain. Deliberately a narrower sealed interface than
 * {@link ContentBlock} rather than a runtime check — {@link ContentBlock.Image} and
 * {@link ContentBlock.Table} implement this in addition to {@code ContentBlock}, and LAYOUT is
 * not in the {@code permits} list, so "no LAYOUT inside LAYOUT" and "only IMAGE/TABLE inside a
 * LAYOUT" are compile-time facts, not validator rules to remember and keep in sync (same
 * philosophy as {@link ContentBlock.Table}'s cells reusing {@link InlineNode} directly instead
 * of a wrapper — see that record's doc comment).
 * <p>
 * V1 scope is intentionally conservative (IMAGE + TABLE only, matching the reference designs);
 * widening this to CALLOUT/PARAGRAPH later is an additive permits-list change, not a rewrite.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ContentBlock.Image.class, name = "IMAGE"),
        @JsonSubTypes.Type(value = ContentBlock.Table.class, name = "TABLE")
})
public sealed interface LayoutChildBlock permits ContentBlock.Image, ContentBlock.Table {
}
