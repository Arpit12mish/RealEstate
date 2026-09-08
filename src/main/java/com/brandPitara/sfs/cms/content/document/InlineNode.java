package com.brandPitara.sfs.cms.content.document;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = InlineNode.Text.class, name = "TEXT"),
        @JsonSubTypes.Type(value = InlineNode.HardBreak.class, name = "HARD_BREAK")
})
public sealed interface InlineNode permits InlineNode.Text, InlineNode.HardBreak {

    @JsonAnySetter
    default void rejectUnknownProperty(String property, Object ignored) {
        throw new IllegalArgumentException("Unknown inline-node property: " + property);
    }

    @JsonTypeName("TEXT")
    record Text(String text, List<TextMark> marks) implements InlineNode {
    }

    @JsonTypeName("HARD_BREAK")
    record HardBreak() implements InlineNode {
    }
}
