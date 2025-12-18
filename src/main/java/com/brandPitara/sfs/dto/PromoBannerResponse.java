package com.brandPitara.sfs.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class PromoBannerResponse {

    private Long id;

    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    private String title;
    private String subtitle;
    private String imageUrl;
    private String targetUrl;

    private Integer priority;
    private Boolean active;

    private OffsetDateTime startAt;
    private OffsetDateTime endAt;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
