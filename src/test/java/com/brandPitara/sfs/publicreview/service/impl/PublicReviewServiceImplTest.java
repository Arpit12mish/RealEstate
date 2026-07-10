package com.brandPitara.sfs.publicreview.service.impl;

import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.repository.CompanyRepository;
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
import java.util.List;
import java.util.Optional;

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
                contentVersionService
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
        when(summaryRepository.findByReviewPlaceId(2L)).thenReturn(Optional.empty());
        when(reviewPlaceProvider.fetchPlaceDetails("ChIJ-broken-place"))
                .thenThrow(new IllegalStateException("Google Places API failed. status=500"));
        when(placeRepository.save(any(PublicReviewPlaceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.syncGoogleReviews(PublicReviewTargetType.PROJECT, 200L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google Places API failed");

        verify(placeRepository).save(argThat(p -> p.getFetchStatus() == GoogleReviewFetchStatus.FAILED));
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
