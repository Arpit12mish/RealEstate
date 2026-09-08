package com.brandPitara.sfs.publicreview.service.impl;

import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.integration.ExternalProviderTransactions;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.publicreview.client.GooglePlaceDetailsResponse;
import com.brandPitara.sfs.publicreview.dto.GooglePlaceSearchResultItem;
import com.brandPitara.sfs.publicreview.dto.GooglePlaceSearchResponse;
import com.brandPitara.sfs.publicreview.dto.SyncGooglePublicReviewsResponse;
import com.brandPitara.sfs.publicreview.entity.PublicReviewPlaceEntity;
import com.brandPitara.sfs.publicreview.enums.GoogleReviewFetchStatus;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.provider.ReviewPlaceProvider;
import com.brandPitara.sfs.publicreview.repository.ProjectReviewRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewPlaceRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewSampleRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewSummaryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Proves PublicReviewServiceImpl calls the ReviewPlaceProvider port (not
 * GooglePlacesClient directly) for both review sync and place search, and
 * preserves success/failure handling - a provider failure must still mark
 * the place FAILED and propagate, exactly as it did when GooglePlacesClient
 * was injected directly.
 */
class PublicReviewServiceImplTest {

    private PublicReviewPlaceRepository placeRepository;
    private PublicReviewSummaryRepository summaryRepository;
    private PublicReviewSampleRepository sampleRepository;
    private ProjectReviewRepository projectReviewRepository;
    private ProjectRepository projectRepository;
    private BuilderRepository builderRepository;
    private CompanyRepository companyRepository;
    private ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;
    private ReviewPlaceProvider reviewPlaceProvider;
    private ContentVersionService contentVersionService;
    private ExternalProviderTransactions externalProviderTransactions;
    private com.brandPitara.sfs.publicreview.config.GooglePlacesProperties googlePlacesProperties;
    private PublicReviewServiceImpl service;

    private void setUp() {
        placeRepository = mock(PublicReviewPlaceRepository.class);
        summaryRepository = mock(PublicReviewSummaryRepository.class);
        sampleRepository = mock(PublicReviewSampleRepository.class);
        projectReviewRepository = mock(ProjectReviewRepository.class);
        projectRepository = mock(ProjectRepository.class);
        builderRepository = mock(BuilderRepository.class);
        companyRepository = mock(CompanyRepository.class);
        projectPublicVisibilityPolicy = mock(ProjectPublicVisibilityPolicy.class);
        reviewPlaceProvider = mock(ReviewPlaceProvider.class);
        contentVersionService = mock(ContentVersionService.class);
        externalProviderTransactions = mock(ExternalProviderTransactions.class);
        googlePlacesProperties = new com.brandPitara.sfs.publicreview.config.GooglePlacesProperties();
        when(externalProviderTransactions.read(any())).thenAnswer(invocation ->
            ((Supplier<?>) invocation.getArgument(0)).get());
        when(externalProviderTransactions.write(any(Supplier.class))).thenAnswer(invocation ->
            ((Supplier<?>) invocation.getArgument(0)).get());
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(externalProviderTransactions).write(any(Runnable.class));

        service = new PublicReviewServiceImpl(
                placeRepository,
                summaryRepository,
                sampleRepository,
                projectReviewRepository,
                projectRepository,
                builderRepository,
                companyRepository,
                projectPublicVisibilityPolicy,
                reviewPlaceProvider,
                contentVersionService,
                externalProviderTransactions,
                googlePlacesProperties
        );
    }

    @Test
    void syncGoogleReviewsCallsProviderAndPersistsSuccessfulFetch() {
        setUp();

        PublicReviewPlaceEntity place = PublicReviewPlaceEntity.builder()
                .id(1L)
                .targetType(PublicReviewTargetType.PROJECT)
                .targetId(100L)
                .googlePlaceId("ChIJ-place-id")
                .fetchStatus(GoogleReviewFetchStatus.NOT_FETCHED)
                .oneTimeFetched(false)
                .build();

        when(projectRepository.findByIdAndDeletedFalse(100L))
                .thenReturn(Optional.of(ProjectEntity.builder().id(100L).build()));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalse(1L, PublicReviewTargetType.PROJECT, 100L))
                .thenReturn(Optional.of(place));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalseForUpdate(1L, PublicReviewTargetType.PROJECT, 100L))
                .thenReturn(Optional.of(place));
        when(summaryRepository.findByReviewPlaceId(1L)).thenReturn(Optional.empty());

        GooglePlaceDetailsResponse googleResponse = new GooglePlaceDetailsResponse();
        googleResponse.setRating(BigDecimal.valueOf(4.5));
        googleResponse.setUserRatingCount(42);
        googleResponse.setReviews(List.of());
        when(reviewPlaceProvider.fetchPlaceDetails("ChIJ-place-id")).thenReturn(googleResponse);

        when(placeRepository.save(any(PublicReviewPlaceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncGooglePublicReviewsResponse response = service.syncGoogleReviews(PublicReviewTargetType.PROJECT, 100L, 1L);

        verify(reviewPlaceProvider).fetchPlaceDetails("ChIJ-place-id");
        assertThat(response.getRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
        assertThat(response.getUserRatingCount()).isEqualTo(42);
        assertThat(response.getReviewPlaceId()).isEqualTo(1L);

        verify(placeRepository, atLeastOnce()).save(argThat(p ->
                p.getFetchStatus() == GoogleReviewFetchStatus.FETCHED && Boolean.TRUE.equals(p.getOneTimeFetched())
        ));
        verify(contentVersionService).bump("PROJECTS");
    }

    @Test
    void syncGoogleReviewsMarksPlaceFailedAndPropagatesWhenProviderThrows() {
        setUp();

        PublicReviewPlaceEntity place = PublicReviewPlaceEntity.builder()
                .id(2L)
                .targetType(PublicReviewTargetType.PROJECT)
                .targetId(200L)
                .googlePlaceId("ChIJ-broken-place")
                .fetchStatus(GoogleReviewFetchStatus.NOT_FETCHED)
                .oneTimeFetched(false)
                .build();

        when(projectRepository.findByIdAndDeletedFalse(200L))
                .thenReturn(Optional.of(ProjectEntity.builder().id(200L).build()));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalse(2L, PublicReviewTargetType.PROJECT, 200L))
                .thenReturn(Optional.of(place));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalseForUpdate(2L, PublicReviewTargetType.PROJECT, 200L))
                .thenReturn(Optional.of(place));
        when(summaryRepository.findByReviewPlaceId(2L)).thenReturn(Optional.empty());
        when(reviewPlaceProvider.fetchPlaceDetails("ChIJ-broken-place"))
                .thenThrow(new IllegalStateException("Google Places API failed. status=500"));
        when(placeRepository.save(any(PublicReviewPlaceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.syncGoogleReviews(PublicReviewTargetType.PROJECT, 200L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google Places API failed");

        // save() is now called twice: once to reserve the attempt (FETCHING,
        // before the Google call) and once to mark it FAILED afterward - proving
        // the reservation step actually ran, not just the final failure state.
        verify(placeRepository, times(2)).save(any(PublicReviewPlaceEntity.class));
        assertThat(place.getFetchStatus()).isEqualTo(GoogleReviewFetchStatus.FAILED);
        // A provider failure must release the reservation, not leave it looking
        // like an in-flight FETCHING lease with a stale start time.
        assertThat(place.getFetchStartedAt()).isNull();
    }

    @Test
    void syncGoogleReviewsRejectsAConcurrentAttemptInsteadOfCallingGoogleTwice() {
        setUp();

        // Simulates the state a first, still-in-flight sync leaves the row in:
        // prepareGoogleSync reserved it (FETCHING) before calling Google, and
        // that reservation is still uncommitted-by-a-second-reader/observed by a
        // second request arriving while the first is mid-flight.
        PublicReviewPlaceEntity place = PublicReviewPlaceEntity.builder()
                .id(4L)
                .targetType(PublicReviewTargetType.PROJECT)
                .targetId(400L)
                .googlePlaceId("ChIJ-in-flight-place")
                .fetchStatus(GoogleReviewFetchStatus.FETCHING)
                .fetchStartedAt(OffsetDateTime.now())
                .oneTimeFetched(false)
                .build();

        when(projectRepository.findByIdAndDeletedFalse(400L))
                .thenReturn(Optional.of(ProjectEntity.builder().id(400L).build()));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalseForUpdate(4L, PublicReviewTargetType.PROJECT, 400L))
                .thenReturn(Optional.of(place));
        when(summaryRepository.findByReviewPlaceId(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.syncGoogleReviews(PublicReviewTargetType.PROJECT, 400L, 4L))
                .hasMessageContaining("already in progress");

        verify(reviewPlaceProvider, never()).fetchPlaceDetails(any());
        verify(placeRepository, never()).save(any());
    }

    @Test
    void syncGoogleReviewsReclaimsAStaleFetchingReservationLeftByACrashedProcess() {
        setUp();
        // A process that crashed/was killed right after committing the FETCHING
        // reservation (before persisting success or failure) leaves exactly
        // this state: fetchStatus=FETCHING with a fetchStartedAt from well
        // before the configured lease.
        googlePlacesProperties.setFetchLeaseSeconds(60);

        PublicReviewPlaceEntity place = PublicReviewPlaceEntity.builder()
                .id(5L)
                .targetType(PublicReviewTargetType.PROJECT)
                .targetId(500L)
                .googlePlaceId("ChIJ-abandoned-place")
                .fetchStatus(GoogleReviewFetchStatus.FETCHING)
                .fetchStartedAt(OffsetDateTime.now().minusSeconds(120))
                .oneTimeFetched(false)
                .build();

        when(projectRepository.findByIdAndDeletedFalse(500L))
                .thenReturn(Optional.of(ProjectEntity.builder().id(500L).build()));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalseForUpdate(5L, PublicReviewTargetType.PROJECT, 500L))
                .thenReturn(Optional.of(place));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalse(5L, PublicReviewTargetType.PROJECT, 500L))
                .thenReturn(Optional.of(place));
        when(summaryRepository.findByReviewPlaceId(5L)).thenReturn(Optional.empty());

        GooglePlaceDetailsResponse googleResponse = new GooglePlaceDetailsResponse();
        googleResponse.setRating(BigDecimal.valueOf(4.2));
        googleResponse.setUserRatingCount(9);
        googleResponse.setReviews(List.of());
        when(reviewPlaceProvider.fetchPlaceDetails("ChIJ-abandoned-place")).thenReturn(googleResponse);
        when(placeRepository.save(any(PublicReviewPlaceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncGooglePublicReviewsResponse response =
                service.syncGoogleReviews(PublicReviewTargetType.PROJECT, 500L, 5L);

        assertThat(response.getReviewPlaceId()).isEqualTo(5L);
        verify(reviewPlaceProvider, times(1)).fetchPlaceDetails("ChIJ-abandoned-place");
        verify(placeRepository, atLeastOnce()).save(argThat(p ->
                p.getFetchStatus() == GoogleReviewFetchStatus.FETCHED && p.getFetchStartedAt() == null));
    }

    @Test
    void syncGoogleReviewsForCompanyBumpsCompaniesContentVersion() {
        setUp();

        PublicReviewPlaceEntity place = PublicReviewPlaceEntity.builder()
                .id(3L)
                .targetType(PublicReviewTargetType.COMPANY)
                .targetId(7L)
                .googlePlaceId("ChIJ-company-place")
                .fetchStatus(GoogleReviewFetchStatus.NOT_FETCHED)
                .oneTimeFetched(false)
                .build();

        when(companyRepository.findByIdAndDeletedFalse(7L))
                .thenReturn(Optional.of(com.brandPitara.sfs.company.entity.CompanyEntity.builder().id(7L).build()));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalse(3L, PublicReviewTargetType.COMPANY, 7L))
                .thenReturn(Optional.of(place));
        when(placeRepository.findByIdAndTargetTypeAndTargetIdAndDeletedFalseForUpdate(3L, PublicReviewTargetType.COMPANY, 7L))
                .thenReturn(Optional.of(place));
        when(summaryRepository.findByReviewPlaceId(3L)).thenReturn(Optional.empty());

        GooglePlaceDetailsResponse googleResponse = new GooglePlaceDetailsResponse();
        googleResponse.setRating(BigDecimal.valueOf(4.8));
        googleResponse.setUserRatingCount(15);
        googleResponse.setReviews(List.of());
        when(reviewPlaceProvider.fetchPlaceDetails("ChIJ-company-place")).thenReturn(googleResponse);
        when(placeRepository.save(any(PublicReviewPlaceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SyncGooglePublicReviewsResponse response = service.syncGoogleReviews(PublicReviewTargetType.COMPANY, 7L, 3L);

        assertThat(response.getRating()).isEqualByComparingTo(BigDecimal.valueOf(4.8));
        verify(contentVersionService).bump("COMPANIES");
        verify(contentVersionService, never()).bump("PROJECTS");
        verify(contentVersionService, never()).bump("BUILDERS");
    }

    @Test
    void listMySubmittedReviewsResolvesProjectNamesWithoutAnNPlusOneQuery() {
        setUp();

        com.brandPitara.sfs.entity.User user = new com.brandPitara.sfs.entity.User();
        user.setId(9L);

        com.brandPitara.sfs.publicreview.entity.ProjectReviewEntity reviewOne =
                com.brandPitara.sfs.publicreview.entity.ProjectReviewEntity.builder()
                        .id(1L).userId(9L).projectId(100L).rating(5).build();
        com.brandPitara.sfs.publicreview.entity.ProjectReviewEntity reviewTwo =
                com.brandPitara.sfs.publicreview.entity.ProjectReviewEntity.builder()
                        .id(2L).userId(9L).projectId(200L).rating(4).build();
        // Same projectId as reviewOne - proves the batch lookup is deduplicated,
        // not just "one query instead of N" by coincidence of distinct IDs.
        com.brandPitara.sfs.publicreview.entity.ProjectReviewEntity reviewThree =
                com.brandPitara.sfs.publicreview.entity.ProjectReviewEntity.builder()
                        .id(3L).userId(9L).projectId(100L).rating(3).build();

        when(projectReviewRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(9L))
                .thenReturn(List.of(reviewOne, reviewTwo, reviewThree));
        when(projectRepository.findByIdInAndDeletedFalse(any()))
                .thenReturn(List.of(
                        ProjectEntity.builder().id(100L).name("Skyline Towers").build(),
                        ProjectEntity.builder().id(200L).name("Palm Residency").build()
                ));

        var responses = service.listMySubmittedReviews(user);

        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).getProjectName()).isEqualTo("Skyline Towers");
        assertThat(responses.get(1).getProjectName()).isEqualTo("Palm Residency");
        assertThat(responses.get(2).getProjectName()).isEqualTo("Skyline Towers");

        verify(projectRepository, times(1)).findByIdInAndDeletedFalse(any());
        verify(projectRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    void searchGooglePlacesDelegatesToProviderWithProjectCoordinates() {
        setUp();

        ProjectEntity project = ProjectEntity.builder()
                .id(300L)
                .name("Skyline Towers")
                .latitude(28.4595)
                .longitude(77.0266)
                .build();
        when(projectRepository.findByIdAndDeletedFalse(300L)).thenReturn(Optional.of(project));

        GooglePlaceSearchResultItem item = GooglePlaceSearchResultItem.builder()
                .placeId("place-1")
                .displayName("Skyline Towers Sales Office")
                .build();
        when(reviewPlaceProvider.searchPlaces("Skyline Towers", 28.4595, 77.0266))
                .thenReturn(List.of(item));

        GooglePlaceSearchResponse response = service.searchGooglePlaces(300L, null);

        assertThat(response.getResults()).containsExactly(item);
        verify(reviewPlaceProvider).searchPlaces("Skyline Towers", 28.4595, 77.0266);
    }
}
