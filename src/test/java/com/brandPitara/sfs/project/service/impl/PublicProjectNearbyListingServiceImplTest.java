package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.project.repository.ProjectNearbyListingProjection;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PublicProjectNearbyListingServiceImplTest {

  @Test
  void listNearbyUsesDistanceQueryWhenCoordinatesAreValid() {
    ProjectRepository projectRepository = mock(ProjectRepository.class);
    ProjectFavoriteService projectFavoriteService = mock(ProjectFavoriteService.class);
    PublicProjectNearbyListingServiceImpl service = new PublicProjectNearbyListingServiceImpl(projectRepository, projectFavoriteService);

    when(projectRepository.findNearbyPublicProjectCards(28.6139, 77.2090, 5))
        .thenReturn(List.of(row(101L)));

    assertThat(service.listNearby(28.6139, 77.2090, 99L, 5))
        .extracting("projectId")
        .containsExactly(101L);

    verify(projectRepository).findNearbyPublicProjectCards(28.6139, 77.2090, 5);
    verify(projectFavoriteService).enrichNearbyListingCards(anyList());
    verify(projectRepository, never()).findPublicProjectCardsByCity(anyLong(), anyInt());
    verify(projectRepository, never()).findFallbackPublicProjectCards(anyInt());
  }

  @Test
  void listNearbyUsesCityFallbackWhenCoordinatesAreMissing() {
    ProjectRepository projectRepository = mock(ProjectRepository.class);
    ProjectFavoriteService projectFavoriteService = mock(ProjectFavoriteService.class);
    PublicProjectNearbyListingServiceImpl service = new PublicProjectNearbyListingServiceImpl(projectRepository, projectFavoriteService);

    when(projectRepository.findPublicProjectCardsByCity(7L, 5))
        .thenReturn(List.of(row(202L)));

    assertThat(service.listNearby(null, null, 7L, 5))
        .extracting("projectId")
        .containsExactly(202L);

    verify(projectRepository).findPublicProjectCardsByCity(7L, 5);
  }

  @Test
  void listNearbyFallsBackGloballyWhenCityHasNoProjects() {
    ProjectRepository projectRepository = mock(ProjectRepository.class);
    ProjectFavoriteService projectFavoriteService = mock(ProjectFavoriteService.class);
    PublicProjectNearbyListingServiceImpl service = new PublicProjectNearbyListingServiceImpl(projectRepository, projectFavoriteService);

    when(projectRepository.findPublicProjectCardsByCity(7L, 5))
        .thenReturn(List.of());
    when(projectRepository.findFallbackPublicProjectCards(5))
        .thenReturn(List.of(row(303L)));

    assertThat(service.listNearby(null, null, 7L, 5))
        .extracting("projectId")
        .containsExactly(303L);

    verify(projectRepository).findPublicProjectCardsByCity(7L, 5);
    verify(projectRepository).findFallbackPublicProjectCards(5);
  }

  @Test
  void listNearbyUsesGlobalFallbackAndCapsLimit() {
    ProjectRepository projectRepository = mock(ProjectRepository.class);
    ProjectFavoriteService projectFavoriteService = mock(ProjectFavoriteService.class);
    PublicProjectNearbyListingServiceImpl service = new PublicProjectNearbyListingServiceImpl(projectRepository, projectFavoriteService);

    when(projectRepository.findFallbackPublicProjectCards(10))
        .thenReturn(List.of(row(303L)));

    assertThat(service.listNearby(200.0, 77.2090, null, 99))
        .extracting("projectId")
        .containsExactly(303L);

    verify(projectRepository).findFallbackPublicProjectCards(10);
  }

  private static ProjectNearbyListingProjection row(Long projectId) {
    return new ProjectNearbyListingProjection() {
      @Override public Long getProjectId() { return projectId; }
      @Override public String getProjectName() { return "Project " + projectId; }
      @Override public String getProjectSlug() { return "project-" + projectId; }
      @Override public String getCoverImageUrl() { return null; }
      @Override public String getAddressLine() { return null; }
      @Override public String getCityName() { return null; }
      @Override public Long getBuilderId() { return 1L; }
      @Override public String getBuilderName() { return "Builder"; }
      @Override public String getBuilderLogoUrl() { return null; }
      @Override public Long getPriceMin() { return null; }
      @Override public Long getPriceMax() { return null; }
      @Override public Double getLatitude() { return null; }
      @Override public Double getLongitude() { return null; }
      @Override public Double getDistanceKm() { return null; }
      @Override public Boolean getVerified() { return false; }
      @Override public String getUnitConfigurationType() { return null; }
      @Override public String getUnitLabel() { return null; }
      @Override public Integer getBedrooms() { return null; }
      @Override public BigDecimal getSaleableAreaSqft() { return null; }
      @Override public BigDecimal getSuperAreaSqft() { return null; }
      @Override public BigDecimal getCarpetAreaSqft() { return null; }
    };
  }
}
