package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.connectivity.provider.NearbyPlaceProvider;
import com.brandPitara.sfs.project.connectivity.provider.dto.NearbyPlaceProviderResult;
import com.brandPitara.sfs.project.dto.ConnectivityProviderSearchRequest;
import com.brandPitara.sfs.project.dto.ConnectivityProviderSearchResponse;
import com.brandPitara.sfs.project.dto.ProjectConnectivityPlaceBulkUpsertRequest;
import com.brandPitara.sfs.project.dto.ProjectConnectivityPlaceUpsertRequest;
import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.entity.ProjectConnectivityEntity;
import com.brandPitara.sfs.project.entity.ProjectConnectivityPlaceEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectConnectivityPlaceRepository;
import com.brandPitara.sfs.project.repository.ProjectConnectivityRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectConnectivityServiceImplTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectConnectivityRepository connectivityRepository;
  @Mock private ProjectConnectivityPlaceRepository placeRepository;
  @Mock private ContentVersionService contentVersionService;
  @Mock private ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;
  @Mock private NearbyPlaceProvider nearbyPlaceProvider;

  @InjectMocks private ProjectConnectivityServiceImpl service;

  // ── Public endpoint ─────────────────────────────────────────────────────────

  @Test
  void publicGetReturnsSavedPlacesWithoutCallingGoogleProvider() {
    ProjectEntity project = publicProject();
    ProjectConnectivityEntity connectivity = ProjectConnectivityEntity.builder()
        .project(project)
        .title("Connectivity")
        .subtitle("Near by Connectivity")
        .defaultRadiusMeters(3000)
        .searchEnabled(true)
        .active(true)
        .deleted(false)
        .build();
    ProjectConnectivityPlaceEntity place = ProjectConnectivityPlaceEntity.builder()
        .id(101L)
        .project(project)
        .placeName("HUDA City Centre Metro Station")
        .placeType(ProjectConnectivityType.METRO)
        .category(ProjectConnectivityCategory.TRANSIT)
        .latitude(28.4592)
        .longitude(77.0728)
        .distanceMeters(1200)
        .distanceLabel("1.2 km")
        .provider("GOOGLE_PLACES")
        .externalPlaceId("google-place-id")
        .active(true)
        .deleted(false)
        .build();

    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
    when(connectivityRepository.findByProjectIdAndActiveTrueAndDeletedFalse(27L)).thenReturn(Optional.of(connectivity));
    when(placeRepository.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(27L)).thenReturn(List.of(place));

    ProjectConnectivityResponse response = service.publicGet(27L);

    assertThat(response.getProjectLatitude()).isEqualTo(28.4595);
    assertThat(response.getProjectLongitude()).isEqualTo(77.0266);
    assertThat(response.getDefaultRadiusMeters()).isEqualTo(3000);
    assertThat(response.getPlaces()).hasSize(1);
    assertThat(response.getCategories()).hasSize(1);
    assertThat(response.getCategories().get(0).getCategory()).isEqualTo(ProjectConnectivityCategory.TRANSIT);
    verifyNoInteractions(nearbyPlaceProvider);
  }

  // ── Provider search: coordinate validation ───────────────────────────────────

  @Test
  void providerSearchRequiresProjectCoordinates() {
    ProjectEntity project = publicProject();
    project.setLatitude(null);
    project.setLongitude(null);
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));

    ConnectivityProviderSearchRequest request = ConnectivityProviderSearchRequest.builder()
        .category(ProjectConnectivityCategory.TRANSIT)
        .keyword("metro station")
        .radiusMeters(3000)
        .build();

    assertThatThrownBy(() -> service.providerSearch(27L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("latitude/longitude");
    verify(nearbyPlaceProvider, never()).searchNearby(any(), any(), any(), any(), any());
  }

  // ── Provider search: radius validation ──────────────────────────────────────

  @Test
  void providerSearchRejectsRadiusExceedingConfiguredMax() {
    ProjectEntity project = publicProject();
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
    when(nearbyPlaceProvider.getMaxRadiusMeters()).thenReturn(5000);

    ConnectivityProviderSearchRequest request = ConnectivityProviderSearchRequest.builder()
        .category(ProjectConnectivityCategory.TRANSIT)
        .keyword("metro station")
        .radiusMeters(10000)
        .build();

    assertThatThrownBy(() -> service.providerSearch(27L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot exceed 5000 meters");
    verify(nearbyPlaceProvider, never()).searchNearby(any(), any(), any(), any(), any());
  }

  // ── Provider search: strict radius filtering ─────────────────────────────────

  @Test
  void providerSearchReturnsOnlyResultsWithinRadius() {
    ProjectEntity project = publicProject();
    NearbyPlaceProviderResult near = NearbyPlaceProviderResult.builder()
        .placeName("Nearby Metro")
        .placeType(ProjectConnectivityType.METRO)
        .category(ProjectConnectivityCategory.TRANSIT)
        .provider("GOOGLE_PLACES")
        .externalPlaceId("near-id")
        .distanceMeters(1000)   // 1 km — within 3000 m
        .build();
    NearbyPlaceProviderResult far = NearbyPlaceProviderResult.builder()
        .placeName("Far Away Station")
        .placeType(ProjectConnectivityType.METRO)
        .category(ProjectConnectivityCategory.TRANSIT)
        .provider("GOOGLE_PLACES")
        .externalPlaceId("far-id")
        .distanceMeters(5000)   // 5 km — outside 3000 m radius
        .build();

    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
    when(nearbyPlaceProvider.getMaxRadiusMeters()).thenReturn(10000);
    when(nearbyPlaceProvider.searchNearby(eq(28.4595), eq(77.0266), any(), eq(ProjectConnectivityCategory.TRANSIT), eq(3000)))
        .thenReturn(List.of(near, far));
    when(placeRepository.findAllProviderExternalIdKeysByProjectId(27L)).thenReturn(Set.of());
    when(placeRepository.findAllNameTypeKeysByProjectId(27L)).thenReturn(Set.of());
    when(placeRepository.findAllNameCategoryKeysByProjectId(27L)).thenReturn(Set.of());

    ConnectivityProviderSearchResponse response = service.providerSearch(27L,
        ConnectivityProviderSearchRequest.builder()
            .category(ProjectConnectivityCategory.TRANSIT)
            .keyword("metro station")
            .radiusMeters(3000)
            .build());

    assertThat(response.getResults()).hasSize(1);
    assertThat(response.getResults().get(0).getPlaceName()).isEqualTo("Nearby Metro");
    assertThat(response.getTotalFetched()).isEqualTo(2);
    assertThat(response.getTotalWithinRadius()).isEqualTo(1);
    assertThat(response.getTotalFilteredOut()).isEqualTo(1);
  }

  @Test
  void providerSearchFilters57kmResult() {
    ProjectEntity project = publicProject();
    NearbyPlaceProviderResult veryFar = NearbyPlaceProviderResult.builder()
        .placeName("Remote Station")
        .placeType(ProjectConnectivityType.RAILWAY_STATION)
        .category(ProjectConnectivityCategory.TRANSIT)
        .provider("GOOGLE_PLACES")
        .externalPlaceId("remote-id")
        .distanceMeters(57600)  // 57.6 km — the exact symptom reported
        .build();

    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
    when(nearbyPlaceProvider.getMaxRadiusMeters()).thenReturn(10000);
    when(nearbyPlaceProvider.searchNearby(eq(28.4595), eq(77.0266), any(), eq(ProjectConnectivityCategory.TRANSIT), eq(3000)))
        .thenReturn(List.of(veryFar));
    when(placeRepository.findAllProviderExternalIdKeysByProjectId(27L)).thenReturn(Set.of());
    when(placeRepository.findAllNameTypeKeysByProjectId(27L)).thenReturn(Set.of());
    when(placeRepository.findAllNameCategoryKeysByProjectId(27L)).thenReturn(Set.of());

    ConnectivityProviderSearchResponse response = service.providerSearch(27L,
        ConnectivityProviderSearchRequest.builder()
            .category(ProjectConnectivityCategory.TRANSIT)
            .keyword("metro station")
            .radiusMeters(3000)
            .build());

    assertThat(response.getResults()).isEmpty();
    assertThat(response.getTotalFetched()).isEqualTo(1);
    assertThat(response.getTotalWithinRadius()).isZero();
    assertThat(response.getTotalFilteredOut()).isEqualTo(1);
  }

  @Test
  void providerSearchFiltersNullDistanceResults() {
    ProjectEntity project = publicProject();
    // A result with null distanceMeters (no lat/lng from Google) must be excluded.
    NearbyPlaceProviderResult noLocation = NearbyPlaceProviderResult.builder()
        .placeName("Unknown Metro")
        .placeType(ProjectConnectivityType.METRO)
        .category(ProjectConnectivityCategory.TRANSIT)
        .provider("GOOGLE_PLACES")
        .externalPlaceId("no-loc-id")
        .distanceMeters(null)
        .build();

    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
    when(nearbyPlaceProvider.getMaxRadiusMeters()).thenReturn(10000);
    when(nearbyPlaceProvider.searchNearby(eq(28.4595), eq(77.0266), any(), eq(ProjectConnectivityCategory.TRANSIT), eq(3000)))
        .thenReturn(List.of(noLocation));
    when(placeRepository.findAllProviderExternalIdKeysByProjectId(27L)).thenReturn(Set.of());
    when(placeRepository.findAllNameTypeKeysByProjectId(27L)).thenReturn(Set.of());
    when(placeRepository.findAllNameCategoryKeysByProjectId(27L)).thenReturn(Set.of());

    ConnectivityProviderSearchResponse response = service.providerSearch(27L,
        ConnectivityProviderSearchRequest.builder()
            .category(ProjectConnectivityCategory.TRANSIT)
            .keyword("metro station")
            .radiusMeters(3000)
            .build());

    assertThat(response.getResults()).isEmpty();
    assertThat(response.getTotalFilteredOut()).isEqualTo(1);
  }

  // ── Provider search: duplicate detection + limit ─────────────────────────────

  @Test
  void providerSearchUsesKeywordLimitAndDuplicateFlags() {
    ProjectEntity project = publicProject();
    NearbyPlaceProviderResult duplicate = NearbyPlaceProviderResult.builder()
        .placeName("HUDA City Centre Metro Station")
        .placeType(ProjectConnectivityType.METRO)
        .category(ProjectConnectivityCategory.TRANSIT)
        .provider("GOOGLE_PLACES")
        .externalPlaceId("known-id")
        .distanceMeters(800)   // within 3000 m
        .build();
    NearbyPlaceProviderResult fresh = NearbyPlaceProviderResult.builder()
        .placeName("New Metro Station")
        .placeType(ProjectConnectivityType.METRO)
        .category(ProjectConnectivityCategory.TRANSIT)
        .provider("GOOGLE_PLACES")
        .externalPlaceId("new-id")
        .distanceMeters(1200)  // within 3000 m
        .build();

    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
    when(nearbyPlaceProvider.getMaxRadiusMeters()).thenReturn(10000);
    when(nearbyPlaceProvider.searchNearby(eq(28.4595), eq(77.0266), eq("metro station"), eq(ProjectConnectivityCategory.TRANSIT), eq(3000)))
        .thenReturn(List.of(duplicate, fresh));
    when(placeRepository.findAllProviderExternalIdKeysByProjectId(27L)).thenReturn(Set.of("GOOGLE_PLACES::known-id"));
    when(placeRepository.findAllNameTypeKeysByProjectId(27L)).thenReturn(Set.of());
    when(placeRepository.findAllNameCategoryKeysByProjectId(27L)).thenReturn(Set.of());

    var response = service.providerSearch(27L, ConnectivityProviderSearchRequest.builder()
        .category(ProjectConnectivityCategory.TRANSIT)
        .keyword("metro station")
        .radiusMeters(3000)
        .limit(1)
        .build());

    assertThat(response.getProvider()).isEqualTo("GOOGLE_PLACES");
    assertThat(response.getQuery()).isEqualTo("metro station");
    assertThat(response.getResults()).hasSize(1);
    assertThat(response.getResults().get(0).getAlreadySaved()).isTrue();
    assertThat(response.getTotalFetched()).isEqualTo(2);
    assertThat(response.getTotalWithinRadius()).isEqualTo(2);
    assertThat(response.getTotalFilteredOut()).isZero();
  }

  // ── Bulk save ────────────────────────────────────────────────────────────────

  @Test
  void bulkSaveSkipsFallbackNameCategoryDuplicates() {
    ProjectEntity project = publicProject();
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
    when(connectivityRepository.findByProjectIdAndDeletedFalse(27L)).thenReturn(Optional.empty());
    when(placeRepository.findMaxSortOrderByProjectId(27L)).thenReturn(Optional.of(0));
    when(placeRepository.findAllProviderExternalIdKeysByProjectId(27L)).thenReturn(Set.of());
    when(placeRepository.findAllNameTypeKeysByProjectId(27L)).thenReturn(Set.of());
    when(placeRepository.findAllNameCategoryKeysByProjectId(27L)).thenReturn(Set.of("city hospital::HOSPITALS"));

    ProjectConnectivityPlaceUpsertRequest duplicate = ProjectConnectivityPlaceUpsertRequest.builder()
        .placeName("City Hospital")
        .category(ProjectConnectivityCategory.HOSPITALS)
        .build();

    var response = service.bulkSavePlaces(27L, ProjectConnectivityPlaceBulkUpsertRequest.builder()
        .places(List.of(duplicate))
        .build());

    assertThat(response.getRequestedCount()).isEqualTo(1);
    assertThat(response.getSavedCount()).isZero();
    assertThat(response.getSkippedDuplicateCount()).isEqualTo(1);
    verify(placeRepository, never()).save(any());
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  private ProjectEntity publicProject() {
    return ProjectEntity.builder()
        .id(27L)
        .name("M3M Jewel")
        .addressLine("Gurugram, Haryana")
        .latitude(28.4595)
        .longitude(77.0266)
        .active(true)
        .published(true)
        .deleted(false)
        .reviewStatus(ReviewStatus.APPROVED)
        .build();
  }
}
