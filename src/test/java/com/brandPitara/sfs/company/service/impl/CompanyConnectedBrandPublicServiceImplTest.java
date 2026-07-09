package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.enums.BrandRelationType;
import com.brandPitara.sfs.brand.enums.BrandSourceType;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.company.dto.ConnectedBrandDto;
import com.brandPitara.sfs.company.entity.CompanyBrandLinkEntity;
import com.brandPitara.sfs.company.repository.CompanyBrandLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyConnectedBrandPublicServiceImplTest {

  @Mock private BrandCollaborationRepository brandCollaborationRepository;
  @Mock private CompanyBrandLinkRepository companyBrandLinkRepository;

  private CompanyConnectedBrandPublicServiceImpl service() {
    return new CompanyConnectedBrandPublicServiceImpl(brandCollaborationRepository, companyBrandLinkRepository);
  }

  private BrandEntity brand(Long id, String name, String slug) {
    return BrandEntity.builder()
        .id(id)
        .name(name)
        .slug(slug)
        .logoUrl("logo-" + id + ".png")
        .shortDescription("short " + id)
        .description("description " + id)
        .published(true)
        .active(true)
        .deleted(false)
        .build();
  }

  @Test
  void getConnectedBrands_returnsLegacyLinksWithMobileSafeSlugFields() {
    BrandEntity legacyBrand = brand(42L, "Berger", "berger-paints");
    CompanyBrandLinkEntity legacyLink = CompanyBrandLinkEntity.builder()
        .brand(legacyBrand)
        .sortOrder(3)
        .active(true)
        .deleted(false)
        .build();
    when(brandCollaborationRepository.findPublicByCompanyId(7L)).thenReturn(List.of());
    when(companyBrandLinkRepository.findPublicFallbackByCompanyId(7L))
        .thenReturn(List.of(legacyLink));

    List<ConnectedBrandDto> result = service().getConnectedBrands(7L);

    assertThat(result).hasSize(1);
    ConnectedBrandDto dto = result.get(0);
    assertThat(dto.getBrandId()).isEqualTo(42L);
    assertThat(dto.getName()).isEqualTo("Berger");
    assertThat(dto.getSlug()).isEqualTo("berger-paints");
    assertThat(dto.getLogoUrl()).isEqualTo("logo-42.png");
    assertThat(dto.getShortDescription()).isEqualTo("short 42");
    assertThat(dto.getDescription()).isEqualTo("description 42");
    assertThat(dto.isVerified()).isFalse();
    assertThat(dto.isFeatured()).isFalse();
    assertThat(dto.getDisplayOrder()).isEqualTo(3);
    verify(companyBrandLinkRepository).findPublicFallbackByCompanyId(7L);
    verify(companyBrandLinkRepository, never()).findByCompany_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(7L);
  }

  @Test
  void getConnectedBrands_prefersCanonicalCollaborationAndDeduplicatesLegacyByBrandId() {
    BrandEntity brand = brand(42L, "Berger", "berger-paints");
    BrandCollaborationEntity canonical = BrandCollaborationEntity.builder()
        .brand(brand)
        .relationType(BrandRelationType.CERTIFIED_PARTNER)
        .sourceType(BrandSourceType.SFS_VERIFIED)
        .verified(true)
        .featured(true)
        .sortOrder(1)
        .publicVisible(true)
        .active(true)
        .deleted(false)
        .build();
    CompanyBrandLinkEntity duplicateLegacy = CompanyBrandLinkEntity.builder()
        .brand(brand)
        .sortOrder(8)
        .active(true)
        .deleted(false)
        .build();
    BrandEntity legacyOnlyBrand = brand(43L, "Incor", "incor");
    CompanyBrandLinkEntity legacyOnly = CompanyBrandLinkEntity.builder()
        .brand(legacyOnlyBrand)
        .sortOrder(9)
        .active(true)
        .deleted(false)
        .build();

    when(brandCollaborationRepository.findPublicByCompanyId(7L)).thenReturn(List.of(canonical));
    when(companyBrandLinkRepository.findPublicFallbackByCompanyId(7L))
        .thenReturn(List.of(duplicateLegacy, legacyOnly));

    List<ConnectedBrandDto> result = service().getConnectedBrands(7L);

    assertThat(result).extracting(ConnectedBrandDto::getBrandId).containsExactly(42L, 43L);
    ConnectedBrandDto canonicalDto = result.get(0);
    assertThat(canonicalDto.getRelationType()).isEqualTo(BrandRelationType.CERTIFIED_PARTNER);
    assertThat(canonicalDto.getSourceType()).isEqualTo(BrandSourceType.SFS_VERIFIED);
    assertThat(canonicalDto.isVerified()).isTrue();
    assertThat(canonicalDto.isFeatured()).isTrue();
    assertThat(canonicalDto.getDisplayOrder()).isEqualTo(1);
    assertThat(result.get(1).getSlug()).isEqualTo("incor");
  }

  @Test
  void getConnectedBrands_usesStrictPublicLegacyFallbackThatExcludesUnpublishedBrands() {
    when(brandCollaborationRepository.findPublicByCompanyId(7L)).thenReturn(List.of());
    when(companyBrandLinkRepository.findPublicFallbackByCompanyId(7L)).thenReturn(List.of());

    List<ConnectedBrandDto> result = service().getConnectedBrands(7L);

    assertThat(result).isEmpty();
    verify(companyBrandLinkRepository).findPublicFallbackByCompanyId(7L);
    verify(companyBrandLinkRepository, never()).findByCompany_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(7L);
  }

  @Test
  void getConnectedBrands_usesStrictPublicLegacyFallbackThatExcludesInactiveBrands() {
    when(brandCollaborationRepository.findPublicByCompanyId(7L)).thenReturn(List.of());
    when(companyBrandLinkRepository.findPublicFallbackByCompanyId(7L)).thenReturn(List.of());

    List<ConnectedBrandDto> result = service().getConnectedBrands(7L);

    assertThat(result).isEmpty();
    verify(companyBrandLinkRepository).findPublicFallbackByCompanyId(7L);
    verify(companyBrandLinkRepository, never()).findByCompany_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(7L);
  }

  @Test
  void getConnectedBrands_usesStrictPublicLegacyFallbackThatExcludesDeletedBrands() {
    when(brandCollaborationRepository.findPublicByCompanyId(7L)).thenReturn(List.of());
    when(companyBrandLinkRepository.findPublicFallbackByCompanyId(7L)).thenReturn(List.of());

    List<ConnectedBrandDto> result = service().getConnectedBrands(7L);

    assertThat(result).isEmpty();
    verify(companyBrandLinkRepository).findPublicFallbackByCompanyId(7L);
    verify(companyBrandLinkRepository, never()).findByCompany_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(7L);
  }
}
