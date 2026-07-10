package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyMediaEntity;
import com.brandPitara.sfs.company.repository.CompanyMediaRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyMediaUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class DashboardCompanyMediaServiceImplTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private CompanyMediaRepository companyMediaRepository;

  private DashboardCompanyMediaServiceImpl service() {
    return new DashboardCompanyMediaServiceImpl(companyRepository, companyMediaRepository);
  }

  private CompanyEntity company() {
    return CompanyEntity.builder().id(7L).name("Morphogenesis").deleted(false).build();
  }

  private CompanyMediaEntity media(Long id, String usageType) {
    return CompanyMediaEntity.builder()
        .id(id).company(company())
        .mediaUrl("https://cdn/x.jpg").mediaType("IMAGE").usageType(usageType)
        .sortOrder(0).publicVisible(true).active(true).deleted(false)
        .build();
  }

  @Test
  void create_addsHeroImage() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(companyMediaRepository.save(any(CompanyMediaEntity.class))).thenAnswer(inv -> {
      CompanyMediaEntity e = inv.getArgument(0);
      e.setId(50L);
      return e;
    });

    CompanyMediaCreateRequest request = CompanyMediaCreateRequest.builder()
        .mediaUrl("https://cdn/hero1.jpg")
        .usageType("hero")
        .build();

    CompanyMediaResponse response = service().create(7L, request);

    assertThat(response.getId()).isEqualTo(50L);
    assertThat(response.getUsageType()).isEqualTo("HERO");
    assertThat(response.getMediaType()).isEqualTo("IMAGE");
    assertThat(response.isPublicVisible()).isTrue();
  }

  @Test
  void create_rejectsInvalidUsageType() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));

    CompanyMediaCreateRequest request = CompanyMediaCreateRequest.builder()
        .mediaUrl("https://cdn/x.jpg")
        .usageType("BANNER")
        .build();

    assertThatThrownBy(() -> service().create(7L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("usageType must be one of");
    verify(companyMediaRepository, never()).save(any());
  }

  @Test
  void create_rejectsMissingCompany() {
    when(companyRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    CompanyMediaCreateRequest request = CompanyMediaCreateRequest.builder()
        .mediaUrl("https://cdn/x.jpg")
        .usageType("HERO")
        .build();

    assertThatThrownBy(() -> service().create(999L, request)).isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void update_changesUsageTypeAndSortOrder() {
    CompanyMediaEntity existing = media(50L, "HERO");
    when(companyMediaRepository.findByIdAndCompany_IdAndDeletedFalse(50L, 7L)).thenReturn(Optional.of(existing));
    when(companyMediaRepository.save(any(CompanyMediaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    CompanyMediaUpdateRequest request = CompanyMediaUpdateRequest.builder()
        .usageType("gallery")
        .sortOrder(3)
        .build();

    CompanyMediaResponse response = service().update(7L, 50L, request);

    assertThat(response.getUsageType()).isEqualTo("GALLERY");
    assertThat(response.getSortOrder()).isEqualTo(3);
  }

  @Test
  void delete_softDeletesRow() {
    CompanyMediaEntity existing = media(50L, "HERO");
    when(companyMediaRepository.findByIdAndCompany_IdAndDeletedFalse(50L, 7L)).thenReturn(Optional.of(existing));

    service().delete(7L, 50L);

    ArgumentCaptor<CompanyMediaEntity> captor = ArgumentCaptor.forClass(CompanyMediaEntity.class);
    verify(companyMediaRepository).save(captor.capture());
    assertThat(captor.getValue().getDeleted()).isTrue();
    assertThat(captor.getValue().getActive()).isFalse();
  }

  @Test
  void list_returnsMediaForCompany() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(companyMediaRepository.findByCompany_IdAndDeletedFalseOrderBySortOrderAscIdAsc(7L))
        .thenReturn(List.of(media(1L, "HERO")));

    List<CompanyMediaResponse> result = service().list(7L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUsageType()).isEqualTo("HERO");
  }
}
