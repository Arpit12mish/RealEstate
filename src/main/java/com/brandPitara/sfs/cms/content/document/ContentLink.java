package com.brandPitara.sfs.cms.content.document;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public record ContentLink(
        String href,
        boolean openInNewTab,
        boolean nofollow,
        boolean sponsored
) {
    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object ignored) {
        throw new IllegalArgumentException("Unknown content-link property: " + property);
    }
}
