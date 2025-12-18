// src/main/java/com/brandPitara/sfs/service/mapper/BusinessMapper.java
package com.brandPitara.sfs.service.mapper;

import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.entity.BusinessEntity;

import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;

public final class BusinessMapper {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private BusinessMapper() { }

    public static BusinessResponse toResponse(BusinessEntity e) {

        // 1) years in business
        Integer yearsInBusiness = null;
        if (e.getEstablishedYear() != null) {
            int currentYear = Year.now(IST).getValue();
            yearsInBusiness = Math.max(0, currentYear - e.getEstablishedYear());
        }

        // 2) is OPEN now?
        Boolean openNow = null;
        String openStatusText = null;

        if (e.getOpenTime() != null && e.getCloseTime() != null) {
            LocalTime now = LocalTime.now(IST);
            boolean isOpen = !now.isBefore(e.getOpenTime()) && now.isBefore(e.getCloseTime());
            openNow = isOpen;
            openStatusText = isOpen ? "OPEN" : "CLOSED";
        }

        // 3) call URL
        String callUrl = null;
        if (e.getPrimaryPhone() != null && !e.getPrimaryPhone().isBlank()) {
            callUrl = "tel:" + e.getPrimaryPhone().trim();
        }

        // 4) WhatsApp URL
        String whatsappUrl = null;
        if (e.getWhatsappPhone() != null && !e.getWhatsappPhone().isBlank()) {
            String sanitized = e.getWhatsappPhone().replaceAll("[^0-9]", "");
            if (!sanitized.isBlank()) {
                whatsappUrl = "https://wa.me/" + sanitized;
            }
        }

        // 5) Google Maps directions URL
        String directionsUrl = null;
        if (e.getLatitude() != null && e.getLongitude() != null) {
            directionsUrl = String.format(
                    "https://www.google.com/maps/search/?api=1&query=%s,%s",
                    e.getLatitude(), e.getLongitude()
            );
        }

        return BusinessResponse.builder()
                .id(e.getId())
                .name(e.getName())

                // phones + contact URLs
                .primaryPhone(e.getPrimaryPhone())
                .secondaryPhone(e.getSecondaryPhone())
                .whatsappPhone(e.getWhatsappPhone())
                .callUrl(callUrl)
                .whatsappUrl(whatsappUrl)
                .directionsUrl(directionsUrl)

                // address / location
                .addressLine1(e.getAddressLine1())
                .addressLine2(e.getAddressLine2())
                .landmark(e.getLandmark())
                .pincode(e.getPincode())
                .cityId(e.getCity().getId())
                .cityName(e.getCity().getName())
                .cityState(e.getCity().getState())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())

                // category
                .categoryId(e.getCategory().getId())
                .categoryName(e.getCategory().getName())
                .categorySlug(e.getCategory().getSlug())

                // timings
                .openTime(e.getOpenTime())
                .closeTime(e.getCloseTime())
                .openNow(openNow)
                .openStatusText(openStatusText)

                // badges / meta
                .establishedYear(e.getEstablishedYear())
                .yearsInBusiness(yearsInBusiness)
                .highlightBadge(e.getHighlightBadge())
                .topRated(e.getTopRated())
                .nearAndFast(e.getNearAndFast())

                // sponsored (money feature)
                .sponsored(e.getSponsored())
                .sponsoredPriority(e.getSponsoredPriority())

                // rating + status
                .avgRating(e.getAvgRating())
                .totalRatings(e.getTotalRatings())
                .active(e.getActive())

                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
