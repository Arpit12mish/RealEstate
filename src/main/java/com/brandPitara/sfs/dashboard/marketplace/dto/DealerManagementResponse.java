package com.brandPitara.sfs.dashboard.marketplace.dto;

import com.brandPitara.sfs.marketplace.dto.OfferingGroupResponse;
import com.brandPitara.sfs.marketplace.dto.OpeningHoursResponse;
import lombok.Builder;

import java.util.List;

/** Dashboard view of a dealer: every editable field plus its child collections. */
@Builder
public record DealerManagementResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        Long cityId,
        String cityName,
        String description,
        String primaryPhone,
        String whatsappPhone,
        String email,
        String website,
        String addressLine1,
        String addressLine2,
        String landmark,
        String locality,
        String pincode,
        Double latitude,
        Double longitude,
        Integer establishedYear,
        String timezone,
        boolean active,
        Double avgRating,
        Integer totalRatings,
        OpeningHoursResponse openingHours,
        List<OfferingGroupResponse> productGroups,
        List<OfferingGroupResponse> serviceGroups,
        List<DealerMediaResponse> media,
        List<DealerWorkerLinkResponse> workerLinks
) {
}
