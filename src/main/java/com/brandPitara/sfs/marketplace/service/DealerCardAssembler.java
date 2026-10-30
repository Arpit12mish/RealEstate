package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.marketplace.dto.DealerCardResponse;
import com.brandPitara.sfs.marketplace.dto.OpeningHoursResponse;
import com.brandPitara.sfs.marketplace.entity.BusinessMediaEntity;
import com.brandPitara.sfs.marketplace.entity.BusinessOpeningHoursEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessMediaUsage;
import com.brandPitara.sfs.marketplace.enums.BusinessOfferingType;
import com.brandPitara.sfs.marketplace.repository.BusinessMediaRepository;
import com.brandPitara.sfs.marketplace.repository.BusinessOfferingGroupRepository;
import com.brandPitara.sfs.marketplace.repository.BusinessOpeningHoursRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds dealer cards for a page of businesses with a fixed number of batched queries (media,
 * product chips, opening hours) regardless of page size.
 */
@Component
@RequiredArgsConstructor
public class DealerCardAssembler {

    static final int MAX_CHIPS = 4;
    static final int NEW_LISTING_DAYS = 30;
    static final double TOP_RATED_MIN_RATING = 4.5;
    static final int TOP_RATED_MIN_REVIEWS = 10;

    private final BusinessMediaRepository mediaRepository;
    private final BusinessOfferingGroupRepository offeringGroupRepository;
    private final BusinessOpeningHoursRepository openingHoursRepository;
    private final Clock clock;

    public List<DealerCardResponse> toCards(List<BusinessEntity> businesses) {
        if (businesses.isEmpty()) return List.of();
        List<Long> ids = businesses.stream().map(BusinessEntity::getId).toList();

        Map<Long, List<BusinessMediaEntity>> mediaByBusiness = mediaRepository.findPublicByBusinessIds(ids).stream()
                .collect(Collectors.groupingBy(m -> m.getBusiness().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<String>> chipsByBusiness = new LinkedHashMap<>();
        for (Object[] row : offeringGroupRepository.findItemNamesByBusinessIds(ids, BusinessOfferingType.PRODUCT)) {
            List<String> chips = chipsByBusiness.computeIfAbsent((Long) row[0], k -> new ArrayList<>());
            if (chips.size() < MAX_CHIPS) chips.add((String) row[1]);
        }

        Map<Long, List<BusinessOpeningHoursEntity>> hoursByBusiness = openingHoursRepository.findByBusinessIds(ids).stream()
                .collect(Collectors.groupingBy(h -> h.getBusiness().getId()));

        Instant now = clock.instant();
        return businesses.stream()
                .map(b -> toCard(b,
                        mediaByBusiness.getOrDefault(b.getId(), List.of()),
                        chipsByBusiness.getOrDefault(b.getId(), List.of()),
                        hoursByBusiness.getOrDefault(b.getId(), List.of()),
                        now))
                .toList();
    }

    private DealerCardResponse toCard(
            BusinessEntity b,
            List<BusinessMediaEntity> media,
            List<String> chips,
            List<BusinessOpeningHoursEntity> hours,
            Instant now
    ) {
        ZoneId zone = OpeningHoursCalculator.resolveZone(b.getTimezone());
        OpeningHoursResponse opening = OpeningHoursCalculator.evaluate(intervals(b, hours), zone, now);
        String cover = media.stream()
                .sorted(Comparator.comparing((BusinessMediaEntity m) -> m.getUsageType() == BusinessMediaUsage.HERO ? 0 : 1)
                        .thenComparingInt(BusinessMediaEntity::getSortOrder)
                        .thenComparing(BusinessMediaEntity::getId))
                .map(BusinessMediaEntity::getMediaUrl)
                .findFirst()
                .orElse(null);

        return DealerCardResponse.builder()
                .id(b.getId())
                .name(b.getName())
                .coverImageUrl(cover)
                .photoCount(media.size())
                .productChips(chips)
                .locationText(MarketplaceMappers.locationText(b))
                .openStatus(opening.status())
                .openStatusText(opening.statusText())
                .yearsInBusiness(OpeningHoursCalculator.yearsInBusiness(b.getEstablishedYear(), zone, now))
                .contact(MarketplaceMappers.contact(b.getPrimaryPhone(), b.getWhatsappPhone()))
                .averageRating(averageRating(b))
                .reviewCount(reviewCount(b))
                .badge(badge(b, now))
                .build();
    }

    static int reviewCount(BusinessEntity b) {
        return b.getTotalRatings() == null ? 0 : Math.max(b.getTotalRatings(), 0);
    }

    static Double averageRating(BusinessEntity b) {
        if (reviewCount(b) == 0 || b.getAvgRating() == null) return null;
        return Math.round(b.getAvgRating() * 10.0) / 10.0;
    }

    /** "Newly onboarded" for listings created in the last 30 days; otherwise "Top rated" for well-reviewed stores. */
    static String badge(BusinessEntity b, Instant now) {
        if (b.getCreatedAt() != null && b.getCreatedAt().toInstant().isAfter(now.minus(NEW_LISTING_DAYS, ChronoUnit.DAYS))) {
            return "Newly onboarded";
        }
        Double rating = averageRating(b);
        if (rating != null && rating >= TOP_RATED_MIN_RATING && reviewCount(b) >= TOP_RATED_MIN_REVIEWS) {
            return "Top rated";
        }
        return null;
    }

    /** Explicit weekly rows win; legacy single open/close times apply to every day otherwise. */
    public static List<OpeningHoursCalculator.Interval> intervals(BusinessEntity b, List<BusinessOpeningHoursEntity> rows) {
        if (!rows.isEmpty()) {
            return rows.stream()
                    .map(h -> new OpeningHoursCalculator.Interval(h.getDayOfWeek(), h.getOpensAt(), h.getClosesAt()))
                    .toList();
        }
        return OpeningHoursCalculator.legacyDaily(b.getOpenTime(), b.getCloseTime());
    }
}
