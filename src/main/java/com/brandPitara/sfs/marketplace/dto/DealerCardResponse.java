package com.brandPitara.sfs.marketplace.dto;

import com.brandPitara.sfs.marketplace.enums.OpenStatus;
import lombok.Builder;

import java.util.List;

/** Compact dealer projection used by listings and "Similar Stores". */
@Builder
public record DealerCardResponse(
        Long id,
        String name,
        String coverImageUrl,
        int photoCount,
        /** Up to four product names, for the chip row. */
        List<String> productChips,
        String locationText,
        OpenStatus openStatus,
        String openStatusText,
        Integer yearsInBusiness,
        PublicContactResponse contact
) {
}
