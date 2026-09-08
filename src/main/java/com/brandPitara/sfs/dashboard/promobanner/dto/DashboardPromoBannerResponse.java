package com.brandPitara.sfs.dashboard.promobanner.dto;

import com.brandPitara.sfs.enums.PromoBannerMediaType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class DashboardPromoBannerResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String slotKey;
    private String title;
    private String subtitle;
    private PromoBannerMediaType mediaType;
    private String mediaUrl;
    private String imageUrl;
    private String targetUrl;
    private Integer priority;
    private Boolean active;
    private Integer displayDurationMs;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
