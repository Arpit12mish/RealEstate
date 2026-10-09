package com.brandPitara.sfs.dashboard.marketplace.service.impl;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.marketplace.dto.*;
import com.brandPitara.sfs.dashboard.marketplace.service.DashboardDealerService;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.marketplace.dto.NamedItemResponse;
import com.brandPitara.sfs.marketplace.dto.OfferingGroupResponse;
import com.brandPitara.sfs.marketplace.entity.*;
import com.brandPitara.sfs.marketplace.enums.BusinessOfferingType;
import com.brandPitara.sfs.marketplace.enums.WorkerRecommendationStatus;
import com.brandPitara.sfs.marketplace.repository.*;
import com.brandPitara.sfs.marketplace.service.DealerCardAssembler;
import com.brandPitara.sfs.marketplace.service.MarketplacePaging;
import com.brandPitara.sfs.marketplace.service.OpeningHoursCalculator;
import com.brandPitara.sfs.marketplace.service.OpeningHoursValidator;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.util.PhoneNumberNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardDealerServiceImpl implements DashboardDealerService {

    private final DealerRepository dealerRepository;
    private final CategoryRepository categoryRepository;
    private final CityRepository cityRepository;
    private final BusinessOpeningHoursRepository openingHoursRepository;
    private final BusinessOfferingGroupRepository offeringGroupRepository;
    private final BusinessMediaRepository mediaRepository;
    private final BusinessWorkerLinkRepository workerLinkRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final DashboardCurrentUserService dashboardCurrentUserService;
    private final DashboardActionAuditService auditService;
    private final Clock clock;

    static final int MAX_MEDIA_PER_DEALER = 40;

    // ---------- dealer profile ----------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealerListItemResponse> list(String query, int page, int size) {
        Page<BusinessEntity> result = dealerRepository.searchDealersForManagement(
                StringUtils.hasText(query) ? query.trim() : "", MarketplacePaging.page(page, size));
        return MarketplacePaging.response(result, result.getContent().stream()
                .map(b -> new DealerListItemResponse(b.getId(), b.getName(), b.getCategory().getName(),
                        b.getCity().getName(), b.getLocality(), Boolean.TRUE.equals(b.getActive())))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DealerManagementResponse get(Long dealerId) {
        return toResponse(requireDealer(dealerId));
    }

    @Override
    @Transactional
    public DealerManagementResponse create(DealerCreateRequest request) {
        BusinessEntity dealer = BusinessEntity.builder()
                .name(request.name().trim())
                .category(requireCategory(request.categoryId()))
                .city(requireCity(request.cityId()))
                .active(false) // published explicitly once the profile is filled in
                .build();
        BusinessEntity saved = dealerRepository.save(dealer);
        auditService.record(DashboardAuditAction.DEALER_CREATED, ReviewEntityType.DEALER, saved.getId(), null);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DealerManagementResponse update(Long dealerId, DealerUpdateRequest r) {
        BusinessEntity b = requireDealer(dealerId);
        if (r.name() != null) {
            if (!StringUtils.hasText(r.name())) throw badRequest("name cannot be blank");
            b.setName(r.name().trim());
        }
        if (r.categoryId() != null) b.setCategory(requireCategory(r.categoryId()));
        if (r.cityId() != null) b.setCity(requireCity(r.cityId()));
        if (r.description() != null) b.setDescription(blankToNull(r.description()));
        if (r.primaryPhone() != null) b.setPrimaryPhone(validPhoneOrNull(r.primaryPhone(), "primaryPhone", true));
        if (r.whatsappPhone() != null) b.setWhatsappPhone(validPhoneOrNull(r.whatsappPhone(), "whatsappPhone", false));
        if (r.email() != null) b.setEmail(blankToNull(r.email()));
        if (r.website() != null) b.setWebsite(blankToNull(r.website()));
        if (r.addressLine1() != null) b.setAddressLine1(blankToNull(r.addressLine1()));
        if (r.addressLine2() != null) b.setAddressLine2(blankToNull(r.addressLine2()));
        if (r.landmark() != null) b.setLandmark(blankToNull(r.landmark()));
        if (r.locality() != null) b.setLocality(blankToNull(r.locality()));
        if (r.pincode() != null) b.setPincode(blankToNull(r.pincode()));
        if (r.latitude() != null) b.setLatitude(r.latitude());
        if (r.longitude() != null) b.setLongitude(r.longitude());
        if (r.establishedYear() != null) {
            int currentYear = clock.instant().atZone(OpeningHoursCalculator.resolveZone(b.getTimezone())).getYear();
            if (r.establishedYear() > currentYear) throw badRequest("establishedYear cannot be in the future");
            b.setEstablishedYear(r.establishedYear());
        }
        if (r.timezone() != null) b.setTimezone(validZone(r.timezone()));
        if (r.active() != null) b.setActive(r.active());

        dealerRepository.save(b);
        auditService.record(DashboardAuditAction.DEALER_UPDATED, ReviewEntityType.DEALER, b.getId(), null);
        return toResponse(b);
    }

    // ---------- opening hours ----------

    @Override
    @Transactional
    public DealerManagementResponse replaceOpeningHours(Long dealerId, OpeningHoursReplaceRequest request) {
        BusinessEntity b = requireDealer(dealerId);
        List<OpeningHoursCalculator.Interval> intervals = request.intervals().stream()
                .map(i -> new OpeningHoursCalculator.Interval(i.dayOfWeek(), i.opensAt(), i.closesAt()))
                .toList();
        OpeningHoursValidator.validate(intervals);

        openingHoursRepository.deleteByBusinessId(b.getId());
        openingHoursRepository.flush();
        openingHoursRepository.saveAll(intervals.stream()
                .map(i -> BusinessOpeningHoursEntity.builder()
                        .business(b)
                        .dayOfWeek((short) i.dayOfWeek())
                        .opensAt(i.opensAt())
                        .closesAt(i.closesAt())
                        .build())
                .toList());
        auditService.record(DashboardAuditAction.DEALER_OPENING_HOURS_REPLACED, ReviewEntityType.DEALER, b.getId(), null);
        return toResponse(b);
    }

    // ---------- products / services ----------

    @Override
    @Transactional
    public DealerManagementResponse replaceOfferings(Long dealerId, BusinessOfferingType type, OfferingsReplaceRequest request) {
        BusinessEntity b = requireDealer(dealerId);
        Set<String> titles = new HashSet<>();
        List<BusinessOfferingGroupEntity> groups = new ArrayList<>();
        int groupOrder = 0;
        for (OfferingsReplaceRequest.Group g : request.groups()) {
            String title = g.title().trim();
            if (!titles.add(title.toLowerCase(Locale.ROOT))) throw badRequest("Duplicate group title: " + title);
            BusinessOfferingGroupEntity group = BusinessOfferingGroupEntity.builder()
                    .business(b).offeringType(type).title(title).sortOrder(groupOrder++).build();
            Set<String> names = new HashSet<>();
            int itemOrder = 0;
            for (String raw : g.items()) {
                String name = raw.trim();
                if (!names.add(name.toLowerCase(Locale.ROOT))) {
                    throw badRequest("Duplicate item '" + name + "' in group " + title);
                }
                group.getItems().add(BusinessOfferingItemEntity.builder()
                        .group(group).name(name).sortOrder(itemOrder++).build());
            }
            groups.add(group);
        }

        // Remove through the entity graph so item rows cascade, then flush before re-inserting
        // because (business, type, lower(title)) is unique.
        offeringGroupRepository.deleteAll(offeringGroupRepository.findByBusiness_IdAndOfferingType(b.getId(), type));
        offeringGroupRepository.flush();
        offeringGroupRepository.saveAll(groups);
        auditService.record(DashboardAuditAction.DEALER_OFFERINGS_REPLACED, ReviewEntityType.DEALER, b.getId(), null);
        return toResponse(b);
    }

    // ---------- media ----------

    @Override
    @Transactional
    public DealerMediaResponse addMedia(Long dealerId, DealerMediaCreateRequest request) {
        BusinessEntity b = requireDealer(dealerId);
        if (mediaRepository.countByBusiness_IdAndDeletedFalse(b.getId()) >= MAX_MEDIA_PER_DEALER) {
            throw badRequest("A dealer can have at most " + MAX_MEDIA_PER_DEALER + " images");
        }
        BusinessMediaEntity saved = mediaRepository.save(BusinessMediaEntity.builder()
                .business(b)
                .usageType(request.usageType())
                .mediaUrl(request.mediaUrl().trim())
                .storageKey(blankToNull(request.storageKey()))
                .altText(blankToNull(request.altText()))
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .active(true)
                .deleted(false)
                .build());
        auditService.record(DashboardAuditAction.DEALER_MEDIA_ADDED, ReviewEntityType.DEALER_MEDIA, saved.getId(), null);
        return toMedia(saved);
    }

    @Override
    @Transactional
    public DealerMediaResponse updateMedia(Long dealerId, Long mediaId, DealerMediaUpdateRequest request) {
        BusinessMediaEntity m = requireMedia(dealerId, mediaId);
        if (request.usageType() != null) m.setUsageType(request.usageType());
        if (request.altText() != null) m.setAltText(blankToNull(request.altText()));
        if (request.sortOrder() != null) m.setSortOrder(request.sortOrder());
        if (request.active() != null) m.setActive(request.active());
        mediaRepository.save(m);
        auditService.record(DashboardAuditAction.DEALER_MEDIA_UPDATED, ReviewEntityType.DEALER_MEDIA, m.getId(), null);
        return toMedia(m);
    }

    @Override
    @Transactional
    public void deleteMedia(Long dealerId, Long mediaId) {
        BusinessMediaEntity m = requireMedia(dealerId, mediaId);
        m.setDeleted(true);
        m.setActive(false);
        mediaRepository.save(m);
        auditService.record(DashboardAuditAction.DEALER_MEDIA_DELETED, ReviewEntityType.DEALER_MEDIA, m.getId(), null);
    }

    // ---------- worker links & recommendations ----------

    @Override
    @Transactional
    public DealerWorkerLinkResponse linkWorker(Long dealerId, DealerWorkerLinkCreateRequest request) {
        BusinessEntity b = requireDealer(dealerId);
        ProviderProfileEntity worker = providerProfileRepository.findWorkerForManagement(request.workerId())
                .orElseThrow(() -> new NotFoundException("Worker not found: " + request.workerId()));
        if (worker.getBusiness() != null && worker.getBusiness().getId().equals(b.getId())) {
            throw badRequest("A worker cannot be linked to its own listing");
        }
        if (workerLinkRepository.existsByBusiness_IdAndProvider_Id(b.getId(), worker.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Worker is already linked to this dealer");
        }
        String note = blankToNull(request.recommendationNote());
        BusinessWorkerLinkEntity saved = workerLinkRepository.save(BusinessWorkerLinkEntity.builder()
                .business(b)
                .provider(worker)
                .recommendationNote(note)
                // Never trusted on entry: a recommendation always starts PENDING review.
                .recommendationStatus(note == null ? WorkerRecommendationStatus.NONE : WorkerRecommendationStatus.PENDING)
                .createdByDashboardUserId(currentDashboardUserId())
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .build());
        auditService.record(DashboardAuditAction.DEALER_WORKER_LINK_CREATED, ReviewEntityType.DEALER_WORKER_LINK, saved.getId(), null);
        return toLink(saved);
    }

    @Override
    @Transactional
    public DealerWorkerLinkResponse updateWorkerLink(Long dealerId, Long linkId, DealerWorkerLinkUpdateRequest request) {
        BusinessWorkerLinkEntity link = requireLink(dealerId, linkId);
        if (request.status() != null) link.setStatus(request.status());
        if (request.sortOrder() != null) link.setSortOrder(request.sortOrder());
        if (request.recommendationNote() != null) {
            String note = blankToNull(request.recommendationNote());
            if (!Objects.equals(note, link.getRecommendationNote())) {
                link.setRecommendationNote(note);
                link.setRecommendationStatus(note == null ? WorkerRecommendationStatus.NONE : WorkerRecommendationStatus.PENDING);
                link.setRecommendationReviewedBy(null);
                link.setRecommendationReviewedAt(null);
            }
        }
        workerLinkRepository.save(link);
        auditService.record(DashboardAuditAction.DEALER_WORKER_LINK_UPDATED, ReviewEntityType.DEALER_WORKER_LINK, link.getId(), null);
        return toLink(link);
    }

    @Override
    @Transactional
    public void deleteWorkerLink(Long dealerId, Long linkId) {
        BusinessWorkerLinkEntity link = requireLink(dealerId, linkId);
        workerLinkRepository.delete(link);
        auditService.record(DashboardAuditAction.DEALER_WORKER_LINK_DELETED, ReviewEntityType.DEALER_WORKER_LINK, linkId, null);
    }

    @Override
    @Transactional
    public DealerWorkerLinkResponse reviewRecommendation(Long dealerId, Long linkId, RecommendationReviewRequest request) {
        BusinessWorkerLinkEntity link = requireLink(dealerId, linkId);
        WorkerRecommendationStatus target = request.status();
        if (target == WorkerRecommendationStatus.NONE) {
            throw badRequest("Use the link update to withdraw a recommendation");
        }
        if (link.getRecommendationNote() == null) {
            throw badRequest("This link has no recommendation to review");
        }
        link.setRecommendationStatus(target);
        if (target == WorkerRecommendationStatus.PENDING) {
            link.setRecommendationReviewedBy(null);
            link.setRecommendationReviewedAt(null);
        } else {
            link.setRecommendationReviewedBy(currentDashboardUserId());
            link.setRecommendationReviewedAt(OffsetDateTime.now(clock));
        }
        workerLinkRepository.save(link);
        auditService.record(DashboardAuditAction.DEALER_WORKER_RECOMMENDATION_REVIEWED,
                ReviewEntityType.DEALER_WORKER_LINK, link.getId(), null);
        return toLink(link);
    }

    // ---------- helpers ----------

    private BusinessEntity requireDealer(Long dealerId) {
        return dealerRepository.findDealerForManagement(dealerId)
                .orElseThrow(() -> new NotFoundException("Dealer not found: " + dealerId));
    }

    private CategoryEntity requireCategory(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    private CityEntity requireCity(Long id) {
        return cityRepository.findById(id).orElseThrow(() -> new NotFoundException("City not found: " + id));
    }

    private BusinessMediaEntity requireMedia(Long dealerId, Long mediaId) {
        requireDealer(dealerId);
        return mediaRepository.findByIdAndBusiness_IdAndDeletedFalse(mediaId, dealerId)
                .orElseThrow(() -> new NotFoundException("Dealer media not found: " + mediaId));
    }

    private BusinessWorkerLinkEntity requireLink(Long dealerId, Long linkId) {
        requireDealer(dealerId);
        return workerLinkRepository.findByIdAndBusiness_Id(linkId, dealerId)
                .orElseThrow(() -> new NotFoundException("Dealer worker link not found: " + linkId));
    }

    private Long currentDashboardUserId() {
        return dashboardCurrentUserService.getCurrentUserOrThrow().getId();
    }

    /**
     * Stores the normalised number. Calls also accept an STD landline; WhatsApp needs a mobile.
     */
    static String validPhoneOrNull(String raw, String field, boolean allowLandline) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            return PhoneNumberNormalizer.normalize(raw);
        } catch (ResponseStatusException ex) {
            String digits = raw.replaceAll("\\D", "");
            if (allowLandline && digits.length() == 11 && digits.startsWith("0") && digits.charAt(1) != '0') {
                return "+91" + digits.substring(1);
            }
            throw badRequest(field + " must be a valid " + (allowLandline ? "phone" : "mobile") + " number");
        }
    }

    static String validZone(String timezone) {
        try {
            return ZoneId.of(timezone.trim()).getId();
        } catch (DateTimeException ex) {
            throw badRequest("timezone must be an IANA zone id such as Asia/Kolkata");
        }
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private DealerManagementResponse toResponse(BusinessEntity b) {
        List<BusinessOfferingGroupEntity> groups = offeringGroupRepository.findWithItemsByBusinessId(b.getId());
        return DealerManagementResponse.builder()
                .id(b.getId())
                .name(b.getName())
                .categoryId(b.getCategory().getId())
                .categoryName(b.getCategory().getName())
                .cityId(b.getCity().getId())
                .cityName(b.getCity().getName())
                .description(b.getDescription())
                .primaryPhone(b.getPrimaryPhone())
                .whatsappPhone(b.getWhatsappPhone())
                .email(b.getEmail())
                .website(b.getWebsite())
                .addressLine1(b.getAddressLine1())
                .addressLine2(b.getAddressLine2())
                .landmark(b.getLandmark())
                .locality(b.getLocality())
                .pincode(b.getPincode())
                .latitude(b.getLatitude())
                .longitude(b.getLongitude())
                .establishedYear(b.getEstablishedYear())
                .timezone(b.getTimezone())
                .active(Boolean.TRUE.equals(b.getActive()))
                .avgRating(b.getAvgRating())
                .totalRatings(b.getTotalRatings())
                .openingHours(OpeningHoursCalculator.evaluate(
                        DealerCardAssembler.intervals(b, openingHoursRepository.findByBusinessIds(List.of(b.getId()))),
                        OpeningHoursCalculator.resolveZone(b.getTimezone()),
                        clock.instant()))
                .productGroups(groupsOf(groups, BusinessOfferingType.PRODUCT))
                .serviceGroups(groupsOf(groups, BusinessOfferingType.SERVICE))
                .media(mediaRepository.findByBusiness_IdAndDeletedFalseOrderByUsageTypeAscSortOrderAscIdAsc(b.getId()).stream()
                        .map(DashboardDealerServiceImpl::toMedia).toList())
                .workerLinks(workerLinkRepository.findAllForManagement(b.getId()).stream()
                        .map(DashboardDealerServiceImpl::toLink).toList())
                .build();
    }

    private static List<OfferingGroupResponse> groupsOf(List<BusinessOfferingGroupEntity> groups, BusinessOfferingType type) {
        return groups.stream()
                .filter(g -> g.getOfferingType() == type)
                .map(g -> new OfferingGroupResponse(g.getId(), g.getTitle(), g.getItems().stream()
                        .map(i -> new NamedItemResponse(i.getId(), i.getName())).toList()))
                .toList();
    }

    private static DealerMediaResponse toMedia(BusinessMediaEntity m) {
        return new DealerMediaResponse(m.getId(), m.getUsageType(), m.getMediaUrl(), m.getStorageKey(),
                m.getAltText(), m.getSortOrder(), Boolean.TRUE.equals(m.getActive()));
    }

    static DealerWorkerLinkResponse toLink(BusinessWorkerLinkEntity l) {
        ProviderProfileEntity p = l.getProvider();
        return new DealerWorkerLinkResponse(
                l.getId(), l.getBusiness().getId(), p.getId(), p.getDisplayName(),
                p.getPrimaryCategory() == null ? null : p.getPrimaryCategory().getName(),
                p.getVerificationStatus(), l.getStatus(), l.getRecommendationStatus(), l.getRecommendationNote(),
                l.getRecommendationReviewedBy(), l.getRecommendationReviewedAt(), l.getSortOrder());
    }
}
