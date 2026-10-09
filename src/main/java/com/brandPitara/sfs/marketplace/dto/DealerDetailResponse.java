package com.brandPitara.sfs.marketplace.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record DealerDetailResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        String categorySlug,
        /** Category followed by product group titles, e.g. ["Paints & Finishes", "Paints", "Hardware"]. */
        List<String> tags,
        String locality,
        Long cityId,
        String cityName,
        /** "Sector 26, Gurgaon" */
        String locationText,
        String addressLine1,
        String addressLine2,
        String landmark,
        String pincode,
        Double latitude,
        Double longitude,
        String description,
        List<MediaItemResponse> heroMedia,
        List<MediaItemResponse> galleryMedia,
        OpeningHoursResponse openingHours,
        Integer establishedYear,
        Integer yearsInBusiness,
        List<OfferingGroupResponse> productGroups,
        List<NamedItemResponse> services,
        PublicContactResponse contact,
        RatingSummaryResponse rating
) {
}
