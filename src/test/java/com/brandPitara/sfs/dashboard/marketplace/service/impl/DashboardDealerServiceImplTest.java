package com.brandPitara.sfs.dashboard.marketplace.service.impl;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.marketplace.dto.DealerWorkerLinkCreateRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.DealerWorkerLinkUpdateRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.OpeningHoursReplaceRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.OfferingsReplaceRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.RecommendationReviewRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.DealerWorkerLinkResponse;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.marketplace.entity.BusinessWorkerLinkEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessOfferingType;
import com.brandPitara.sfs.marketplace.enums.WorkerRecommendationStatus;
import com.brandPitara.sfs.marketplace.repository.*;
import com.brandPitara.sfs.provider.entity.ProviderProfileEntity;
import com.brandPitara.sfs.provider.enums.ProviderType;
import com.brandPitara.sfs.provider.repository.ProviderProfileRepository;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.CityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardDealerServiceImplTest {

    @Mock private DealerRepository dealerRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private CityRepository cityRepository;
    @Mock private BusinessOpeningHoursRepository openingHoursRepository;
    @Mock private BusinessOfferingGroupRepository offeringGroupRepository;
    @Mock private BusinessMediaRepository mediaRepository;
    @Mock private BusinessWorkerLinkRepository workerLinkRepository;
    @Mock private ProviderProfileRepository providerProfileRepository;
    @Mock private DashboardCurrentUserService dashboardCurrentUserService;
    @Mock private DashboardActionAuditService auditService;

    private DashboardDealerServiceImpl service;
    private BusinessEntity dealer;
    private ProviderProfileEntity worker;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-07T06:30:00Z"), ZoneOffset.UTC);
        service = new DashboardDealerServiceImpl(dealerRepository, categoryRepository, cityRepository,
                openingHoursRepository, offeringGroupRepository, mediaRepository, workerLinkRepository,
                providerProfileRepository, dashboardCurrentUserService, auditService, clock);

        CityEntity city = new CityEntity();
        city.setId(1L);
        city.setName("Gurgaon");
        CategoryEntity category = new CategoryEntity();
        category.setId(5L);
        category.setName("Paints");
        dealer = BusinessEntity.builder().id(10L).name("Sharma Paints").city(city).category(category).active(true).build();
        worker = ProviderProfileEntity.builder().id(20L).displayName("Manish Singh")
                .providerType(ProviderType.WORKER).primaryCategory(category).build();

        when(dealerRepository.findDealerForManagement(10L)).thenReturn(Optional.of(dealer));
        when(providerProfileRepository.findWorkerForManagement(20L)).thenReturn(Optional.of(worker));
        when(workerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DashboardUserEntity me = mock(DashboardUserEntity.class);
        when(me.getId()).thenReturn(3L);
        when(dashboardCurrentUserService.getCurrentUserOrThrow()).thenReturn(me);
    }

    @Test
    void recommendationSuppliedOnLinkStartsPendingNeverVerified() {
        DealerWorkerLinkResponse link = service.linkWorker(10L, new DealerWorkerLinkCreateRequest(20L, "Trusted painter", null));

        assertThat(link.recommendationStatus()).isEqualTo(WorkerRecommendationStatus.PENDING);
        assertThat(link.recommendationReviewedBy()).isNull();
    }

    @Test
    void linkWithoutNoteHasNoRecommendation() {
        DealerWorkerLinkResponse link = service.linkWorker(10L, new DealerWorkerLinkCreateRequest(20L, "  ", null));

        assertThat(link.recommendationStatus()).isEqualTo(WorkerRecommendationStatus.NONE);
    }

    @Test
    void duplicateLinkIsConflictAndOwnListingIsRejected() {
        when(workerLinkRepository.existsByBusiness_IdAndProvider_Id(10L, 20L)).thenReturn(true);
        assertThatThrownBy(() -> service.linkWorker(10L, new DealerWorkerLinkCreateRequest(20L, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        worker.setBusiness(dealer);
        when(workerLinkRepository.existsByBusiness_IdAndProvider_Id(10L, 20L)).thenReturn(false);
        assertThatThrownBy(() -> service.linkWorker(10L, new DealerWorkerLinkCreateRequest(20L, null, null)))
                .hasMessageContaining("own listing");
    }

    @Test
    void reviewerVerificationStampsReviewerAndTime() {
        BusinessWorkerLinkEntity link = link(WorkerRecommendationStatus.PENDING, "Trusted painter");
        when(workerLinkRepository.findByIdAndBusiness_Id(30L, 10L)).thenReturn(Optional.of(link));

        DealerWorkerLinkResponse result = service.reviewRecommendation(10L, 30L,
                new RecommendationReviewRequest(WorkerRecommendationStatus.VERIFIED));

        assertThat(result.recommendationStatus()).isEqualTo(WorkerRecommendationStatus.VERIFIED);
        assertThat(result.recommendationReviewedBy()).isEqualTo(3L);
        assertThat(result.recommendationReviewedAt()).isEqualTo(OffsetDateTime.parse("2026-10-07T06:30Z"));
    }

    @Test
    void editingAVerifiedNoteSendsItBackToPendingReview() {
        BusinessWorkerLinkEntity link = link(WorkerRecommendationStatus.VERIFIED, "Trusted painter");
        link.setRecommendationReviewedBy(3L);
        link.setRecommendationReviewedAt(OffsetDateTime.parse("2026-10-01T00:00Z"));
        when(workerLinkRepository.findByIdAndBusiness_Id(30L, 10L)).thenReturn(Optional.of(link));

        DealerWorkerLinkResponse result = service.updateWorkerLink(10L, 30L,
                new DealerWorkerLinkUpdateRequest(null, "Best painter in Sector 14", null));

        assertThat(result.recommendationStatus()).isEqualTo(WorkerRecommendationStatus.PENDING);
        assertThat(result.recommendationReviewedBy()).isNull();
        assertThat(result.recommendationReviewedAt()).isNull();
    }

    @Test
    void cannotReviewALinkWithoutRecommendation() {
        when(workerLinkRepository.findByIdAndBusiness_Id(30L, 10L))
                .thenReturn(Optional.of(link(WorkerRecommendationStatus.NONE, null)));

        assertThatThrownBy(() -> service.reviewRecommendation(10L, 30L,
                new RecommendationReviewRequest(WorkerRecommendationStatus.VERIFIED)))
                .hasMessageContaining("no recommendation");
    }

    @Test
    void overlappingOpeningHoursAreRejectedBeforeAnyWrite() {
        OpeningHoursReplaceRequest request = new OpeningHoursReplaceRequest(List.of(
                new OpeningHoursReplaceRequest.Interval(1, LocalTime.of(9, 0), LocalTime.of(14, 0)),
                new OpeningHoursReplaceRequest.Interval(1, LocalTime.of(13, 0), LocalTime.of(18, 0))));

        assertThatThrownBy(() -> service.replaceOpeningHours(10L, request)).isInstanceOf(ResponseStatusException.class);
        verify(openingHoursRepository, never()).deleteByBusinessId(any());
    }

    @Test
    void duplicateProductNamesInAGroupAreRejected() {
        OfferingsReplaceRequest request = new OfferingsReplaceRequest(List.of(
                new OfferingsReplaceRequest.Group("Paints", List.of("Interior Paint", "interior paint"))));

        assertThatThrownBy(() -> service.replaceOfferings(10L, BusinessOfferingType.PRODUCT, request))
                .hasMessageContaining("Duplicate item");
        verify(offeringGroupRepository, never()).saveAll(any());
    }

    @Test
    void offeringsAreReplacedInSubmittedOrder() {
        when(offeringGroupRepository.findByBusiness_IdAndOfferingType(10L, BusinessOfferingType.PRODUCT)).thenReturn(List.of());
        OfferingsReplaceRequest request = new OfferingsReplaceRequest(List.of(
                new OfferingsReplaceRequest.Group("Paints", List.of("Interior Paint", "Exterior Paint")),
                new OfferingsReplaceRequest.Group("Hardware", List.of("Locks"))));

        service.replaceOfferings(10L, BusinessOfferingType.PRODUCT, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.brandPitara.sfs.marketplace.entity.BusinessOfferingGroupEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(offeringGroupRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting("title").containsExactly("Paints", "Hardware");
        assertThat(saved.getValue().get(0).getItems()).extracting("name", "sortOrder")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Interior Paint", 0),
                        org.assertj.core.groups.Tuple.tuple("Exterior Paint", 1));
    }

    @Test
    void phoneValidationNormalisesAndRejectsGarbage() {
        assertThat(DashboardDealerServiceImpl.validPhoneOrNull("098765 43210", "primaryPhone", true)).isEqualTo("+919876543210");
        assertThat(DashboardDealerServiceImpl.validPhoneOrNull("0124 4567890", "primaryPhone", true)).isEqualTo("+911244567890");
        assertThat(DashboardDealerServiceImpl.validPhoneOrNull("", "primaryPhone", true)).isNull();
        assertThatThrownBy(() -> DashboardDealerServiceImpl.validPhoneOrNull("0124 4567890", "whatsappPhone", false))
                .hasMessageContaining("mobile");
        assertThatThrownBy(() -> DashboardDealerServiceImpl.validZone("Nowhere/City")).hasMessageContaining("IANA");
    }

    private BusinessWorkerLinkEntity link(WorkerRecommendationStatus status, String note) {
        return BusinessWorkerLinkEntity.builder().id(30L).business(dealer).provider(worker)
                .recommendationStatus(status).recommendationNote(note).build();
    }
}
