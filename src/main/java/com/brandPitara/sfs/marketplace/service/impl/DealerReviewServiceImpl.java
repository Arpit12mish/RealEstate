package com.brandPitara.sfs.marketplace.service.impl;

import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.marketplace.dto.DealerReviewCreateRequest;
import com.brandPitara.sfs.marketplace.dto.DealerReviewModerationRequest;
import com.brandPitara.sfs.marketplace.dto.DealerReviewModerationResponse;
import com.brandPitara.sfs.marketplace.dto.DealerReviewResponse;
import com.brandPitara.sfs.marketplace.entity.BusinessReviewEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;
import com.brandPitara.sfs.marketplace.repository.BusinessReviewRepository;
import com.brandPitara.sfs.marketplace.repository.DealerRepository;
import com.brandPitara.sfs.marketplace.service.DealerReviewMapper;
import com.brandPitara.sfs.marketplace.service.DealerReviewService;
import com.brandPitara.sfs.marketplace.service.MarketplacePaging;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DealerReviewServiceImpl implements DealerReviewService {

    static final String DUPLICATE_MESSAGE = "You have already submitted a review for this store.";

    private final BusinessReviewRepository reviewRepository;
    private final DealerRepository dealerRepository;
    private final DashboardCurrentUserService dashboardCurrentUserService;
    private final DashboardActionAuditService auditService;
    private final Clock clock;

    @Override
    @Transactional
    public DealerReviewResponse submit(Long dealerId, DealerReviewCreateRequest request, User currentUser) {
        if (!dealerRepository.existsPublicDealer(dealerId)) {
            throw new NotFoundException("Dealer not found: " + dealerId);
        }
        if (reviewRepository.existsByBusinessIdAndUserIdAndDeletedFalse(dealerId, currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
        }

        BusinessReviewEntity entity = BusinessReviewEntity.builder()
                .businessId(dealerId)
                .userId(currentUser.getId())
                .reviewerName(clean(request.reviewerName()))
                .reviewerLocation(clean(request.reviewerLocation()))
                .rating(request.rating().shortValue())
                .reviewText(clean(request.reviewText()))
                .moderationStatus(BusinessReviewStatus.PENDING)
                .deleted(false)
                .build();

        try {
            BusinessReviewEntity saved = reviewRepository.saveAndFlush(entity);
            return DealerReviewMapper.toResponse(saved, "Review submitted successfully and is pending moderation.");
        } catch (DataIntegrityViolationException ex) {
            // Concurrent double-submit raced past the exists check; the partial unique index wins.
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_MESSAGE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DealerReviewResponse> findMine(Long dealerId, User currentUser) {
        return reviewRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDescIdDesc(currentUser.getId()).stream()
                .filter(r -> r.getBusinessId().equals(dealerId))
                .findFirst()
                .map(r -> DealerReviewMapper.toResponse(r, null));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DealerReviewModerationResponse> moderationQueue(BusinessReviewStatus status, int page, int size) {
        Page<BusinessReviewEntity> result = reviewRepository.findByModerationStatusAndDeletedFalse(
                status == null ? BusinessReviewStatus.PENDING : status,
                MarketplacePaging.page(page, size, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"))));
        // One lookup for the page's dealer names so moderators can see which store a review is for.
        Map<Long, String> dealerNames = dealerRepository.findAllById(
                        result.getContent().stream().map(BusinessReviewEntity::getBusinessId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(BusinessEntity::getId, BusinessEntity::getName, (a, b) -> a));
        return MarketplacePaging.response(result, result.getContent().stream()
                .map(r -> toModeration(r, dealerNames.get(r.getBusinessId())))
                .toList());
    }

    @Override
    @Transactional
    public DealerReviewModerationResponse moderate(Long reviewId, DealerReviewModerationRequest request) {
        BusinessReviewEntity review = reviewRepository.findByIdAndDeletedFalse(reviewId)
                .orElseThrow(() -> new NotFoundException("Dealer review not found: " + reviewId));
        Long moderatorId = dashboardCurrentUserService.getCurrentUserOrThrow().getId();

        // Lock the business first so the aggregate below is computed from a serialised view.
        BusinessEntity business = dealerRepository.lockById(review.getBusinessId())
                .orElseThrow(() -> new NotFoundException("Dealer not found: " + review.getBusinessId()));

        review.setModerationStatus(request.status());
        review.setModerationNote(clean(request.note()));
        review.setModeratedByDashboardUserId(moderatorId);
        review.setModeratedAt(OffsetDateTime.now(clock));
        reviewRepository.saveAndFlush(review);

        recomputeRating(business);
        auditService.record(DashboardAuditAction.DEALER_REVIEW_MODERATED, ReviewEntityType.DEALER_REVIEW, review.getId(), null);
        return toModeration(review, business.getName());
    }

    /** business.avg_rating / total_ratings come only from APPROVED reviews. */
    void recomputeRating(BusinessEntity business) {
        List<Object[]> rows = reviewRepository.approvedAggregate(business.getId());
        Object[] row = rows.isEmpty() ? new Object[]{0L, null} : rows.get(0);
        long count = row[0] == null ? 0L : ((Number) row[0]).longValue();
        double avg = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
        business.setTotalRatings((int) count);
        business.setAvgRating(count == 0 ? 0.0 : Math.round(avg * 100.0) / 100.0);
        dealerRepository.save(business);
    }

    private static DealerReviewModerationResponse toModeration(BusinessReviewEntity r, String dealerName) {
        return new DealerReviewModerationResponse(
                r.getId(), r.getBusinessId(), dealerName, r.getUserId(), r.getReviewerName(), r.getReviewerLocation(),
                r.getRating(), r.getReviewText(), r.getModerationStatus(), r.getModerationNote(),
                r.getModeratedByDashboardUserId(), r.getModeratedAt(), r.getCreatedAt());
    }

    private static String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
