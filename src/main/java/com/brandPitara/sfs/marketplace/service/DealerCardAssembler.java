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
                .build();
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
