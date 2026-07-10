package com.brandPitara.sfs.dashboard.companyproject.service.impl;

import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectDetailResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectListItemResponse;
import com.brandPitara.sfs.entity.CityEntity;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCompanyProjectServiceImplTest {

  @Mock private CompanyProjectRepository companyProjectRepository;
  @Mock private BrandCollaborationRepository brandCollaborationRepository;

  private DashboardCompanyProjectServiceImpl service() {
    return new DashboardCompanyProjectServiceImpl(companyProjectRepository, brandCollaborationRepository);
  }

  private CompanyEntity company() {
    return CompanyEntity.builder().id(1L).name("Morphogenesis").companyType("ARCHITECT&DESIGNERS").build();
  }

  private CityEntity city() {
    return CityEntity.builder().id(9L).name("Gurgaon").build();
  }

  private CompanyProjectEntity project(Long id) {
    return CompanyProjectEntity.builder()
        .id(id)
        .name("Trump Tower Interiors")
        .company(company())
        .city(city())
        .active(true)
        .deleted(false)
        .build();
  }

  @Test
  void list_returnsNonDeletedCompanyProjectsWithCompanyAndCityFields() {
    CompanyProjectEntity project = project(88L);
    Page<CompanyProjectEntity> page = new PageImpl<>(List.of(project));
    when(companyProjectRepository.searchForDashboard(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
        .thenReturn(page);
    when(brandCollaborationRepository.countBrandsUsedByCompanyProjectIds(List.of(88L)))
        .thenReturn(List.<Object[]>of(new Object[]{88L, 3L}));

    Page<CompanyProjectListItemResponse> result =
        service().list(null, null, null, null, null, Pageable.unpaged());

    assertThat(result.getContent()).hasSize(1);
    CompanyProjectListItemResponse item = result.getContent().get(0);
    assertThat(item.getId()).isEqualTo(88L);
    assertThat(item.getCompanyName()).isEqualTo("Morphogenesis");
    assertThat(item.getCompanyType()).isEqualTo("ARCHITECT&DESIGNERS");
    assertThat(item.getCityName()).isEqualTo("Gurgaon");
    assertThat(item.getBrandsUsedCount()).isEqualTo(3L);
    assertThat(item.isDeleted()).isFalse();
  }

  @Test
  void list_usesNameSearchQueryOnlyWhenQHasText() {
    Page<CompanyProjectEntity> page = new PageImpl<>(List.of());
    when(companyProjectRepository.searchForDashboardByName(
        eq("trump"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
        .thenReturn(page);

    service().list("trump", null, null, null, null, Pageable.unpaged());

    verify(companyProjectRepository).searchForDashboardByName(
        eq("trump"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    verify(companyProjectRepository, org.mockito.Mockito.never())
        .searchForDashboard(any(), any(), any(), any(), any());
  }

  @Test
  void list_appliesCompanyIdFilter() {
    Page<CompanyProjectEntity> page = new PageImpl<>(List.of());
    when(companyProjectRepository.searchForDashboard(eq(1L), isNull(), isNull(), isNull(), any(Pageable.class)))
        .thenReturn(page);

    service().list(null, 1L, null, null, null, Pageable.unpaged());

    ArgumentCaptor<Long> companyIdCaptor = ArgumentCaptor.forClass(Long.class);
    verify(companyProjectRepository)
        .searchForDashboard(companyIdCaptor.capture(), isNull(), isNull(), isNull(), any(Pageable.class));
    assertThat(companyIdCaptor.getValue()).isEqualTo(1L);
  }

  @Test
  void getDetail_returnsCompanyNameTypeAndCityName() {
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(project(88L)));

    CompanyProjectDetailResponse response = service().getDetail(88L);

    assertThat(response.getId()).isEqualTo(88L);
    assertThat(response.getCompanyName()).isEqualTo("Morphogenesis");
    assertThat(response.getCompanyType()).isEqualTo("ARCHITECT&DESIGNERS");
    assertThat(response.getCityName()).isEqualTo("Gurgaon");
  }

  @Test
  void getDetail_throwsNotFound_forMissingOrDeletedProject() {
    when(companyProjectRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getDetail(999L)).isInstanceOf(EntityNotFoundException.class);
  }
}
