package com.brandPitara.sfs.marketplace.service.impl;

import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.marketplace.dto.DealerCardResponse;
import com.brandPitara.sfs.marketplace.dto.DealerDetailResponse;
import com.brandPitara.sfs.marketplace.dto.DealerReviewResponse;
import com.brandPitara.sfs.marketplace.dto.MediaItemResponse;
import com.brandPitara.sfs.marketplace.dto.NamedItemResponse;
import com.brandPitara.sfs.marketplace.dto.OfferingGroupResponse;
import com.brandPitara.sfs.marketplace.dto.RatingSummaryResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerCardResponse;
import com.brandPitara.sfs.marketplace.entity.BusinessMediaEntity;
import com.brandPitara.sfs.marketplace.entity.BusinessOfferingGroupEntity;
import com.brandPitara.sfs.marketplace.entity.BusinessWorkerLinkEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessMediaUsage;
import com.brandPitara.sfs.marketplace.enums.BusinessOfferingType;
import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;
import com.brandPitara.sfs.marketplace.repository.BusinessMediaRepository;
import com.brandPitara.sfs.marketplace.repository.BusinessOfferingGroupRepository;
import com.brandPitara.sfs.marketplace.repository.BusinessOpeningHoursRepository;
import com.brandPitara.sfs.marketplace.repository.BusinessReviewRepository;
import com.brandPitara.sfs.marketplace.repository.BusinessWorkerLinkRepository;
import com.brandPitara.sfs.marketplace.repository.DealerRepository;
import com.brandPitara.sfs.marketplace.service.DealerCardAssembler;
import com.brandPitara.sfs.marketplace.service.DealerPublicService;
import com.brandPitara.sfs.marketplace.service.DealerReviewMapper;
import com.brandPitara.sfs.marketplace.service.MarketplaceMappers;
import com.brandPitara.sfs.marketplace.service.MarketplacePaging;
import com.brandPitara.sfs.marketplace.service.OpeningHoursCalculator;
import com.brandPitara.sfs.marketplace.service.WorkerCardAssembler;
import com.brandPitara.sfs.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DealerPublicServiceImpl implements DealerPublicService {

    private final DealerRepository dealerRepository;
    private final BusinessMediaRepository mediaRepository;
    private final BusinessOfferingGroupRepository offeringGroupRepository;
    private final BusinessOpeningHoursRepository openingHoursRepository;
    private final BusinessWorkerLinkRepository workerLinkRepository;
    private final BusinessReviewRepository reviewRepository;
    private final CategoryRepository categoryRepository;
    private final DealerCardAssembler dealerCardAssembler;
    private final WorkerCardAssembler workerCardAssembler;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public DealerDetailResponse getDealer(Long dealerId) {
        BusinessEntity b = requirePublicDealer(dealerId);
        Instant now = clock.instant();
        ZoneId zone = OpeningHoursCalculator.resolveZone(b.getTimezone());

        List<BusinessMediaEntity> media = mediaRepository.findPublicByBusinessIds(List.of(b.getId()));
        List<BusinessOfferingGroupEntity> groups = offeringGroupRepository.findWithItemsByBusinessId(b.getId());

        List<OfferingGroupResponse> productGroups = groups.stream()
                .filter(g -> g.getOfferingType() == BusinessOfferingType.PRODUCT)
                .map(g -> new OfferingGroupResponse(g.getId(), g.getTitle(), g.getItems().stream()
                        .map(i -> new NamedItemResponse(i.getId(), i.getName()))
                        .toList()))
                .filter(g -> !g.items().isEmpty())
                .toList();

        List<NamedItemResponse> services = groups.stream()
                .filter(g -> g.getOfferingType() == BusinessOfferingType.SERVICE)
                .flatMap(g -> g.getItems().stream())
                .map(i -> new NamedItemResponse(i.getId(), i.getName()))
                .toList();

        Set<String> tags = new LinkedHashSet<>();
        tags.add(b.getCategory().getName());
        productGroups.forEach(g -> tags.add(g.title()));

        return DealerDetailResponse.builder()
                .id(b.getId())
                .name(b.getName())
                .categoryId(b.getCategory().getId())
                .categoryName(b.getCategory().getName())
                .categorySlug(b.getCategory().getSlug())
                .tags(new ArrayList<>(tags))
                .locality(b.getLocality())
                .cityId(b.getCity().getId())
                .cityName(b.getCity().getName())
                .locationText(MarketplaceMappers.locationText(b))
                .addressLine1(b.getAddressLine1())
                .addressLine2(b.getAddressLine2())
                .landmark(b.getLandmark())
                .pincode(b.getPincode())
                .latitude(b.getLatitude())
                .longitude(b.getLongitude())
                .description(b.getDescription())
                .heroMedia(mediaOf(media, BusinessMediaUsage.HERO))
                .galleryMedia(mediaOf(media, BusinessMediaUsage.GALLERY))
                .openingHours(OpeningHoursCalculator.evaluate(
                        DealerCardAssembler.intervals(b, openingHoursRepository.findByBusinessIds(List.of(b.getId()))),
                        zone, now))
                .establishedYear(b.getEstablishedYear())
                .yearsInBusiness(OpeningHoursCalculator.yearsInBusiness(b.getEstablishedYear(), zone, now))
                .productGroups(productGroups)
                .services(services)
                .contact(MarketplaceMappers.contact(b.getPrimaryPhone(), b.getWhatsappPhone()))
                .rating(rating(b))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealerCardResponse> listDealers(Long cityId, Long categoryId, int page, int size) {
        Page<BusinessEntity> result = dealerRepository.findDealers(cityId, categoryId, MarketplacePaging.page(page, size));
        return MarketplacePaging.response(result, dealerCardAssembler.toCards(result.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealerCardResponse> similarDealers(Long dealerId, int page, int size) {
        BusinessEntity base = requirePublicDealer(dealerId);
        Page<BusinessEntity> result = dealerRepository.findSimilarDealers(
                base.getId(),
                base.getCity().getId(),
                base.getCategory().getId(),
                similarCategoryIds(base.getCategory()),
                MarketplacePaging.page(page, size)
        );
        return MarketplacePaging.response(result, dealerCardAssembler.toCards(result.getContent()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WorkerCardResponse> connectedWorkers(Long dealerId, int page, int size) {
        requirePublicDealerExists(dealerId);
        Page<BusinessWorkerLinkEntity> links = workerLinkRepository.findPublicConnectedWorkers(
                dealerId, MarketplacePaging.page(page, size));
        return MarketplacePaging.response(links, workerCardAssembler.toCards(
                links.getContent().stream().map(BusinessWorkerLinkEntity::getProvider).toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealerReviewResponse> approvedReviews(Long dealerId, int page, int size) {
        requirePublicDealerExists(dealerId);
        var result = reviewRepository.findByBusinessIdAndModerationStatusAndDeletedFalse(
                dealerId,
                BusinessReviewStatus.APPROVED,
                MarketplacePaging.page(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
        );
        return MarketplacePaging.response(result, result.getContent().stream()
                .map(r -> DealerReviewMapper.toResponse(r, null))
                .toList());
    }

    // ---------- helpers ----------

    private BusinessEntity requirePublicDealer(Long dealerId) {
        return dealerRepository.findPublicDealer(dealerId)
                .orElseThrow(() -> new NotFoundException("Dealer not found: " + dealerId));
    }

    private void requirePublicDealerExists(Long dealerId) {
        if (!dealerRepository.existsPublicDealer(dealerId)) {
            throw new NotFoundException("Dealer not found: " + dealerId);
        }
    }

    /** The dealer's own category plus its siblings under the same parent. */
    private List<Long> similarCategoryIds(CategoryEntity category) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(category.getId());
        if (category.getParent() != null) {
            categoryRepository.findByParentIdAndActiveTrueOrderByPriorityAsc(category.getParent().getId())
                    .forEach(c -> ids.add(c.getId()));
        }
        return new ArrayList<>(ids);
    }

    private static List<MediaItemResponse> mediaOf(List<BusinessMediaEntity> media, BusinessMediaUsage usage) {
        return media.stream()
                .filter(m -> m.getUsageType() == usage)
                .map(m -> new MediaItemResponse(m.getId(), m.getMediaUrl(), m.getAltText()))
                .toList();
    }

    private static RatingSummaryResponse rating(BusinessEntity b) {
        int count = b.getTotalRatings() == null ? 0 : b.getTotalRatings();
        if (count == 0 || b.getAvgRating() == null) return new RatingSummaryResponse(null, 0);
        return new RatingSummaryResponse(BigDecimal.valueOf(b.getAvgRating()).setScale(1, RoundingMode.HALF_UP), count);
    }
}
