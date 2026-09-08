package com.brandPitara.sfs.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Response for GET /api/public/cities/{citySlug} (GAP-017). Deliberately
 * distinct from {@link CityResponse}: omits {@code active} and
 * {@code homepageFeatured}, which are admin/curation-oriented fields not
 * present on {@link TrendingCityCardResponse} either (the other genuinely
 * public-facing city DTO) — matching that existing convention rather than
 * reusing CityResponse as-is. Omits {@code description} entirely: no such
 * field exists on CityEntity today, so it is not invented here.
 */
@Data
@Builder
public class PublicCityDetailResponse {
    private Long id;
    private String slug;
    private String name;
    private String state;
    private String countryCode;
    private String coverImageUrl;
    private Double growthPercent;
    private OffsetDateTime updatedAt;
}
