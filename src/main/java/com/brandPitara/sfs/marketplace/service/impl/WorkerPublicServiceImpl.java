package com.brandPitara.sfs.marketplace.service.impl;

import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.marketplace.dto.NamedItemResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerDetailResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerRecommendationResponse;
import com.brandPitara.sfs.marketplace.entity.BusinessMediaEntity;
import com.brandPitara.sfs.marketplace.entity.BusinessWorkerLinkEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessMediaUsage;
import com.brandPitara.sfs.marketplace.repository.BusinessMediaRepository;
import com.brandPitara.sfs.marketplace.repository.BusinessWorkerLinkRepository;
import com.brandPitara.sfs.marketplace.service.MarketplaceMappers;
import com.brandPitara.sfs.marketplace.service.MarketplacePaging;
import com.brandPitara.sfs.marketplace.service.WorkerCardAssembler;
import com.brandPitara.sfs.marketplace.service.WorkerPublicService;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.entity.ProviderServiceAreaEntity;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.provider.repository.ProviderServiceAreaRepository;
import com.brandPitara.sfs.provider.repository.ProviderServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkerPublicServiceImpl implements WorkerPublicService {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderServiceAreaRepository serviceAreaRepository;
    private final ProviderServiceOfferingRepository serviceOfferingRepository;
    private final BusinessWorkerLinkRepository workerLinkRepository;
    private final BusinessMediaRepository businessMediaRepository;
    private final WorkerCardAssembler workerCardAssembler;

    @Override
    @Transactional(readOnly = true)
    public WorkerDetailResponse getWorker(Long workerId) {
        ProviderProfileEntity p = requirePublicWorker(workerId);
        Set<Long> ids = Set.of(p.getId());

        List<ProviderServiceAreaEntity> areas = serviceAreaRepository.findByProviderIdIn(ids).stream()
                .sorted(Comparator.comparing(ProviderServiceAreaEntity::getId))
                .toList();
        List<WorkerDetailResponse.ServiceAreaResponse> areaResponses = areas.stream()
                .map(a -> new WorkerDetailResponse.ServiceAreaResponse(
                        a.getCity().getId(),
                        a.getCity().getName(),
                        a.getLocality(),
                        StringUtils.hasText(a.getLocality()) ? a.getLocality().trim() : a.getCity().getName()))
                .collect(Collectors.toMap(
                        WorkerDetailResponse.ServiceAreaResponse::label,
                        a -> a,
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .values().stream().toList();

        return WorkerDetailResponse.builder()
                .id(p.getId())
                .displayName(p.getDisplayName())
                .initials(MarketplaceMappers.initials(p.getDisplayName()))
                .avatarUrl(workerCardAssembler.avatarUrls(ids).get(p.getId()))
                .trade(p.getPrimaryCategory().getName())
                .tradeCategoryId(p.getPrimaryCategory().getId())
                .headline(p.getHeadline())
                .bio(p.getBio())
                .areaSummary(areaSummary(areaResponses))
                .availability(p.getAvailabilityStatus())
                .availabilityUpdatedAt(p.getAvailabilityUpdatedAt())
                .experienceYears(p.getExperienceYears())
                .rates(workerCardAssembler.ratesByWorker(ids).getOrDefault(p.getId(), List.of()).stream()
                        .map(MarketplaceMappers::rate)
                        .toList())
                .services(serviceOfferingRepository.findByProvider_IdOrderBySortOrderAscIdAsc(p.getId()).stream()
                        .map(s -> new NamedItemResponse(s.getId(), s.getName()))
                        .toList())
                .serviceAreas(areaResponses)
                .contact(WorkerCardAssembler.contact(p))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WorkerRecommendationResponse> recommendations(Long workerId, int page, int size) {
        requirePublicWorker(workerId);
        Page<BusinessWorkerLinkEntity> links = workerLinkRepository.findVerifiedRecommendations(
                workerId, MarketplacePaging.page(page, size));
        List<Long> businessIds = links.getContent().stream().map(l -> l.getBusiness().getId()).toList();

        Map<Long, String> coverByBusiness = businessIds.isEmpty() ? Map.of() : businessMediaRepository
                .findPublicByBusinessIds(businessIds).stream()
                .sorted(Comparator.comparing((BusinessMediaEntity m) -> m.getUsageType() == BusinessMediaUsage.HERO ? 0 : 1)
                        .thenComparingInt(BusinessMediaEntity::getSortOrder)
                        .thenComparing(BusinessMediaEntity::getId))
                .collect(Collectors.toMap(m -> m.getBusiness().getId(), BusinessMediaEntity::getMediaUrl,
                        (first, ignored) -> first));

        return MarketplacePaging.response(links, links.getContent().stream()
                .map(l -> {
                    BusinessEntity b = l.getBusiness();
                    return new WorkerRecommendationResponse(
                            b.getId(),
                            b.getName(),
                            coverByBusiness.get(b.getId()),
                            MarketplaceMappers.locationText(b),
                            l.getRecommendationNote()
                    );
                })
                .toList());
    }

    private ProviderProfileEntity requirePublicWorker(Long workerId) {
        return providerProfileRepository.findPublicWorker(workerId)
                .orElseThrow(() -> new NotFoundException("Worker not found: " + workerId));
    }

    /** "Serves Sector 14 & nearby" from the first listed area. */
    static String areaSummary(List<WorkerDetailResponse.ServiceAreaResponse> areas) {
        if (areas.isEmpty()) return null;
        String first = areas.get(0).label();
        return areas.size() > 1 ? "Serves " + first + " & nearby" : "Serves " + first;
    }
}
