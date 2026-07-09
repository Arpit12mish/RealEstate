package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.company.dto.CompanyProjectResponse;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyProjectPublicServiceImplTest {

  @Mock private CompanyProjectRepository companyProjectRepository;
  @Mock private BrandCollaborationRepository brandCollaborationRepository;

  private CompanyProjectPublicServiceImpl service() {
    return new CompanyProjectPublicServiceImpl(companyProjectRepository, brandCollaborationRepository);
  }

  @Test
  void publicGet_returnsBrandsUsedWithMobileSafeBrandFields() {
    CompanyEntity company = CompanyEntity.builder().id(5L).name("Knowledge Center").logoUrl("company.png").build();
    CompanyProjectEntity project = CompanyProjectEntity.builder()
        .id(88L)
        .name("Immersive Hub")
        .slug("immersive-hub")
        .company(company)
        .published(true)
        .active(true)
        .deleted(false)
        .build();
    BrandEntity brand = BrandEntity.builder()
        .id(42L)
        .name("Berger")
        .slug("berger-paints")
        .logoUrl("berger.png")
        .shortDescription("Paints and coatings")
        .published(true)
        .active(true)
        .deleted(false)
        .build();
    BrandCollaborationEntity collaboration = BrandCollaborationEntity.builder()
        .brand(brand)
        .verified(true)
        .featured(true)
        .sortOrder(2)
        .publicVisible(true)
        .active(true)
        .deleted(false)
        .build();
    when(companyProjectRepository
        .findByIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndCompany_PublishedTrueAndCompany_ActiveTrueAndCompany_DeletedFalse(88L))
        .thenReturn(Optional.of(project));
    when(brandCollaborationRepository.findPublicByCompanyProjectId(eq(88L), org.mockito.Mockito.any(Pageable.class)))
        .thenReturn(List.of(collaboration));

    CompanyProjectResponse response = service().publicGet(88L);

    assertThat(response.getBrandsUsed()).hasSize(1);
    assertThat(response.getBrandsUsed().get(0).getBrandId()).isEqualTo(42L);
    assertThat(response.getBrandsUsed().get(0).getSlug()).isEqualTo("berger-paints");
    assertThat(response.getBrandsUsed().get(0).getName()).isEqualTo("Berger");
    assertThat(response.getBrandsUsed().get(0).getLogoUrl()).isEqualTo("berger.png");
    assertThat(response.getBrandsUsed().get(0).getShortDescription()).isEqualTo("Paints and coatings");
    assertThat(response.getBrandsUsed().get(0).isVerified()).isTrue();
    assertThat(response.getBrandsUsed().get(0).isFeatured()).isTrue();
    assertThat(response.getBrandsUsed().get(0).getDisplayOrder()).isEqualTo(2);
  }

  @Test
  void publicGet_usesPublicRepositoryFilterForBrandsUsed() {
    CompanyProjectEntity project = CompanyProjectEntity.builder()
        .id(88L)
        .name("Immersive Hub")
        .published(true)
        .active(true)
        .deleted(false)
        .build();
    when(companyProjectRepository
        .findByIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndCompany_PublishedTrueAndCompany_ActiveTrueAndCompany_DeletedFalse(88L))
        .thenReturn(Optional.of(project));
    when(brandCollaborationRepository.findPublicByCompanyProjectId(eq(88L), org.mockito.Mockito.any(Pageable.class)))
        .thenReturn(List.of());

    CompanyProjectResponse response = service().publicGet(88L);

    assertThat(response.getBrandsUsed()).isEmpty();
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    org.mockito.Mockito.verify(brandCollaborationRepository).findPublicByCompanyProjectId(eq(88L), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
  }

  @Test
  void publicGet_throwsNotFound_whenCompanyProjectIsNotPublic() {
    when(companyProjectRepository
        .findByIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndCompany_PublishedTrueAndCompany_ActiveTrueAndCompany_DeletedFalse(88L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().publicGet(88L))
        .isInstanceOf(ResponseStatusException.class);
  }
}
