package com.brandPitara.sfs.publiccontent.dto;

public record PublicContentSeoResponse(
        String title,
        String description,
        String canonicalUrl,
        boolean robotsIndex,
        boolean robotsFollow
) {
}
