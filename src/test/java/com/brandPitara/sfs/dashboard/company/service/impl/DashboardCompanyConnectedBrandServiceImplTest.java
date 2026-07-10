package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyConnectedBrandUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCompanyConnectedBrandServiceImplTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private BrandRepository brandRepository;
  @Mock private BrandCollaborationRepository brandCollaborationRepository;
  @Mock private ContentVersionService contentVersionService;

  private DashboardCompanyConnectedBrandServiceImpl service() {
    return new DashboardCompanyConnectedBrandServiceImpl(
        companyRepository, brandRepository, brandCollaborationRepository, contentVersionService
    );
  }

  private CompanyEntity company() {
    return CompanyEntity.builder().id(7L).name("Morphogenesis").deleted(false).build();
  }

  private BrandEntity publishedBrand() {
    return BrandEntity.builder()
        .id(1L).name("Samsung").slug("samsung").logoUrl("samsung.png")
        .published(true).active(true).deleted(false)
        .build();
  }

  @Test
  void list_returnsOnlyCompanyTargetRows() {
    CompanyEntity company = company();
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company));
    BrandCollaborationEntity row = BrandCollaborationEntity.builder()
        .id(50L).brand(publishedBrand()).company(company)
        .targetType(BrandCollaborationTargetType.COMPANY)
        .publicVisible(true).active(true).deleted(false).sortOrder(0)
        .build();
    when(brandCollaborationRepository
        .findByCompany_IdAndTargetTypeAndDeletedFalseOrderBySortOrderAscIdAsc(7L, BrandCollaborationTargetType.COMPANY))
        .thenReturn(List.of(row));

    List<CompanyConnectedBrandResponse> result = service().list(7L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getBrandId()).isEqualTo(1L);
    assertThat(result.get(0).getCompanyId()).isEqualTo(7L);
  }

  @Test
  void create_addsValidPublishedBrand() {
    CompanyEntity company = company();
    BrandEntity brand = publishedBrand();
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company));
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand));
    when(brandCollaborationRepository.existsByBrand_IdAndCompany_IdAndDeletedFalse(1L, 7L)).thenReturn(false);
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class)))
        .thenAnswer(inv -> {
          BrandCollaborationEntity e = inv.getArgument(0);
          e.setId(99L);
          return e;
        });

    CompanyConnectedBrandCreateRequest request = CompanyConnectedBrandCreateRequest.builder()
        .brandId(1L)
        .featured(true)
        .build();

    CompanyConnectedBrandResponse response = service().create(7L, request);

    assertThat(response.getId()).isEqualTo(99L);
    assertThat(response.isPublicVisible()).isTrue();
    assertThat(response.isFeatured()).isTrue();
    verify(contentVersionService).bump("BRANDS");
  }

  @Test
  void create_rejectsDuplicateActiveConnection() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(publishedBrand()));
    when(brandCollaborationRepository.existsByBrand_IdAndCompany_IdAndDeletedFalse(1L, 7L)).thenReturn(true);

    CompanyConnectedBrandCreateRequest request = CompanyConnectedBrandCreateRequest.builder().brandId(1L).build();

    assertThatThrownBy(() -> service().create(7L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already connected");
    verify(brandCollaborationRepository, never()).save(any());
  }

  @Test
  void delete_softDeletesRow() {
    BrandCollaborationEntity existing = BrandCollaborationEntity.builder()
        .id(50L).brand(publishedBrand()).company(company())
        .targetType(BrandCollaborationTargetType.COMPANY)
        .active(true).deleted(false).publicVisible(true)
        .build();
    when(brandCollaborationRepository
        .findByIdAndCompany_IdAndTargetTypeAndDeletedFalse(50L, 7L, BrandCollaborationTargetType.COMPANY))
        .thenReturn(Optional.of(existing));

    service().delete(7L, 50L);

    assertThat(existing.getDeleted()).isTrue();
    assertThat(existing.getActive()).isFalse();
    assertThat(existing.getPublicVisible()).isFalse();
  }

  @Test
  void update_throwsNotFound_whenCollaborationDoesNotBelongToCompany() {
    when(brandCollaborationRepository
        .findByIdAndCompany_IdAndTargetTypeAndDeletedFalse(50L, 7L, BrandCollaborationTargetType.COMPANY))
        .thenReturn(Optional.empty());

    CompanyConnectedBrandUpdateRequest request = CompanyConnectedBrandUpdateRequest.builder().featured(true).build();

    assertThatThrownBy(() -> service().update(7L, 50L, request)).isInstanceOf(EntityNotFoundException.class);
  }
}
