package com.brandPitara.sfs.cms.content.document;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextMark.Bold.class, name = "BOLD"),
        @JsonSubTypes.Type(value = TextMark.Italic.class, name = "ITALIC"),
        @JsonSubTypes.Type(value = TextMark.Underline.class, name = "UNDERLINE"),
        @JsonSubTypes.Type(value = TextMark.Link.class, name = "LINK")
})
public sealed interface TextMark permits
        TextMark.Bold,
        TextMark.Italic,
        TextMark.Underline,
        TextMark.Link {

    @JsonAnySetter
    default void rejectUnknownProperty(String property, Object ignored) {
        throw new IllegalArgumentException("Unknown text-mark property: " + property);
    }

    @JsonTypeName("BOLD")
    record Bold() implements TextMark {
    }

    @JsonTypeName("ITALIC")
    record Italic() implements TextMark {
    }

    @JsonTypeName("UNDERLINE")
    record Underline() implements TextMark {
    }

    @JsonTypeName("LINK")
    record Link(
            String href,
            boolean openInNewTab,
            boolean nofollow,
            boolean sponsored
    ) implements TextMark {
    }
}
