package com.brandPitara.sfs.home.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComparePropertiesCardDto {
    private final String mediaType;
    private final String mediaUrl;
    private final String ctaText;
    private final String actionType;
    private final String actionValue;
}
