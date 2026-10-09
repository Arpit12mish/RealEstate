package com.brandPitara.sfs.marketplace.service.impl;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.marketplace.dto.DealerReviewCreateRequest;
import com.brandPitara.sfs.marketplace.dto.DealerReviewModerationRequest;
import com.brandPitara.sfs.marketplace.dto.DealerReviewResponse;
import com.brandPitara.sfs.marketplace.entity.BusinessReviewEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;
import com.brandPitara.sfs.marketplace.repository.BusinessReviewRepository;
import com.brandPitara.sfs.marketplace.repository.DealerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealerReviewServiceImplTest {

    @Mock private BusinessReviewRepository reviewRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private DashboardCurrentUserService dashboardCurrentUserService;
    @Mock private DashboardActionAuditService auditService;

    private DealerReviewServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-07T06:30:00Z"), ZoneOffset.UTC);
        service = new DealerReviewServiceImpl(reviewRepository, dealerRepository, dashboardCurrentUserService, auditService, clock);
        user = new User();
        user.setId(42L);
    }

    private static DealerReviewCreateRequest request() {
        return new DealerReviewCreateRequest(5, "  Rahul Mehta ", "Gurugram", "Great paint advice and fast delivery.");
    }

    @Test
    void submissionStartsPendingAndIsNeverPublicImmediately() {
        when(dealerRepository.existsPublicDealer(7L)).thenReturn(true);
        when(reviewRepository.saveAndFlush(any())).thenAnswer(inv -> {
            BusinessReviewEntity e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });

        DealerReviewResponse response = service.submit(7L, request(), user);

        ArgumentCaptor<BusinessReviewEntity> saved = ArgumentCaptor.forClass(BusinessReviewEntity.class);
        verify(reviewRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getModerationStatus()).isEqualTo(BusinessReviewStatus.PENDING);
        assertThat(saved.getValue().getUserId()).isEqualTo(42L);
        assertThat(saved.getValue().getReviewerName()).isEqualTo("Rahul Mehta");
        assertThat(response.status()).isEqualTo(BusinessReviewStatus.PENDING);
        assertThat(response.message()).contains("pending moderation");
        verifyNoInteractions(auditService);
    }

    @Test
    void reviewingAnUnavailableDealerIs404() {
        when(dealerRepository.existsPublicDealer(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.submit(7L, request(), user)).isInstanceOf(NotFoundException.class);
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void secondReviewBySameUserIsConflict() {
        when(dealerRepository.existsPublicDealer(7L)).thenReturn(true);
        when(reviewRepository.existsByBusinessIdAndUserIdAndDeletedFalse(7L, 42L)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(7L, request(), user))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void concurrentDuplicateCaughtByUniqueIndexIsAlsoConflict() {
        when(dealerRepository.existsPublicDealer(7L)).thenReturn(true);
        when(reviewRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_business_review_user_business"));

        assertThatThrownBy(() -> service.submit(7L, request(), user))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void approvalRecomputesRatingFromApprovedReviewsOnlyUnderRowLock() {
        BusinessReviewEntity review = BusinessReviewEntity.builder()
                .id(100L).businessId(7L).userId(42L).rating((short) 4).reviewText("Good").reviewerName("A")
                .moderationStatus(BusinessReviewStatus.PENDING).build();
        BusinessEntity business = BusinessEntity.builder().id(7L).name("Gupta Colour House").avgRating(0.0).totalRatings(0).build();
        DashboardUserEntity moderator = mock(DashboardUserEntity.class);
        when(moderator.getId()).thenReturn(9L);
        when(dashboardCurrentUserService.getCurrentUserOrThrow()).thenReturn(moderator);
        when(reviewRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(review));
        when(dealerRepository.lockById(7L)).thenReturn(Optional.of(business));
        when(reviewRepository.approvedAggregate(7L)).thenReturn(List.<Object[]>of(new Object[]{3L, 4.333333}));

        var result = service.moderate(100L, new DealerReviewModerationRequest(BusinessReviewStatus.APPROVED, " ok "));

        assertThat(result.status()).isEqualTo(BusinessReviewStatus.APPROVED);
        assertThat(result.moderatedByDashboardUserId()).isEqualTo(9L);
        assertThat(result.moderationNote()).isEqualTo("ok");
        assertThat(result.dealerName()).isEqualTo("Gupta Colour House");
        assertThat(business.getTotalRatings()).isEqualTo(3);
        assertThat(business.getAvgRating()).isEqualTo(4.33);
        verify(dealerRepository).lockById(7L);
        verify(auditService).record(DashboardAuditAction.DEALER_REVIEW_MODERATED, ReviewEntityType.DEALER_REVIEW, 100L, null);
    }

    @Test
    void rejectingTheLastApprovedReviewResetsRating() {
        BusinessReviewEntity review = BusinessReviewEntity.builder()
                .id(100L).businessId(7L).moderationStatus(BusinessReviewStatus.APPROVED).rating((short) 5).build();
        BusinessEntity business = BusinessEntity.builder().id(7L).avgRating(5.0).totalRatings(1).build();
        DashboardUserEntity moderator = mock(DashboardUserEntity.class);
        when(dashboardCurrentUserService.getCurrentUserOrThrow()).thenReturn(moderator);
        when(reviewRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(review));
        when(dealerRepository.lockById(7L)).thenReturn(Optional.of(business));
        when(reviewRepository.approvedAggregate(7L)).thenReturn(List.<Object[]>of(new Object[]{0L, null}));

        service.moderate(100L, new DealerReviewModerationRequest(BusinessReviewStatus.REJECTED, null));

        assertThat(business.getTotalRatings()).isZero();
        assertThat(business.getAvgRating()).isZero();
    }

    @Test
    void moderationQueueNamesEachReviewsDealerWithOneLookup() {
        BusinessReviewEntity a = BusinessReviewEntity.builder().id(1L).businessId(7L).rating((short) 5)
                .moderationStatus(BusinessReviewStatus.PENDING).build();
        BusinessReviewEntity b = BusinessReviewEntity.builder().id(2L).businessId(8L).rating((short) 3)
                .moderationStatus(BusinessReviewStatus.PENDING).build();
        BusinessReviewEntity c = BusinessReviewEntity.builder().id(3L).businessId(7L).rating((short) 4)
                .moderationStatus(BusinessReviewStatus.PENDING).build();
        when(reviewRepository.findByModerationStatusAndDeletedFalse(eq(BusinessReviewStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(a, b, c)));
        when(dealerRepository.findAllById(List.of(7L, 8L))).thenReturn(List.of(
                BusinessEntity.builder().id(7L).name("Gupta Colour House").build(),
                BusinessEntity.builder().id(8L).name("Kapoor Paint Studio").build()));

        var page = service.moderationQueue(BusinessReviewStatus.PENDING, 0, 20);

        assertThat(page.getContent()).extracting("dealerName")
                .containsExactly("Gupta Colour House", "Kapoor Paint Studio", "Gupta Colour House");
        verify(dealerRepository, times(1)).findAllById(any());
    }
}
