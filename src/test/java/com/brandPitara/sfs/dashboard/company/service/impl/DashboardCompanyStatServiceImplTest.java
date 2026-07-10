package com.brandPitara.sfs.dashboard.company.service.impl;

import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyStatEntity;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.company.repository.CompanyStatRepository;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatCreateRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatReorderRequest;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatResponse;
import com.brandPitara.sfs.dashboard.company.dto.CompanyStatUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCompanyStatServiceImplTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private CompanyStatRepository companyStatRepository;

  private DashboardCompanyStatServiceImpl service() {
    return new DashboardCompanyStatServiceImpl(companyRepository, companyStatRepository);
  }

  private CompanyEntity company() {
    return CompanyEntity.builder().id(7L).name("Morphogenesis").deleted(false).build();
  }

  private CompanyStatEntity stat(Long id) {
    return CompanyStatEntity.builder()
        .id(id).company(company())
        .label("Projects Completed").value("31+")
        .displayOrder(0).publicVisible(true).active(true).deleted(false)
        .build();
  }

  @Test
  void list_returnsStatsForCompany() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(companyStatRepository.findByCompany_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(7L))
        .thenReturn(List.of(stat(1L)));

    List<CompanyStatResponse> result = service().list(7L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLabel()).isEqualTo("Projects Completed");
    assertThat(result.get(0).getValue()).isEqualTo("31+");
  }

  @Test
  void list_throwsNotFound_whenCompanyMissing() {
    when(companyRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().list(999L)).isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void create_addsStatWithDefaults() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    when(companyStatRepository.save(any(CompanyStatEntity.class))).thenAnswer(inv -> {
      CompanyStatEntity e = inv.getArgument(0);
      e.setId(50L);
      return e;
    });

    CompanyStatCreateRequest request = CompanyStatCreateRequest.builder()
        .label("Years Experience")
        .value("9+")
        .build();

    CompanyStatResponse response = service().create(7L, request);

    assertThat(response.getId()).isEqualTo(50L);
    assertThat(response.getLabel()).isEqualTo("Years Experience");
    assertThat(response.getValue()).isEqualTo("9+");
    assertThat(response.isPublicVisible()).isTrue();
    assertThat(response.isActive()).isTrue();
    assertThat(response.getSortOrder()).isEqualTo(0);
  }

  @Test
  void create_rejectsMissingCompany() {
    when(companyRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    CompanyStatCreateRequest request = CompanyStatCreateRequest.builder().label("x").value("y").build();

    assertThatThrownBy(() -> service().create(999L, request)).isInstanceOf(EntityNotFoundException.class);
    verify(companyStatRepository, never()).save(any());
  }

  @Test
  void update_changesFields() {
    CompanyStatEntity existing = stat(50L);
    when(companyStatRepository.findByIdAndCompany_IdAndDeletedFalse(50L, 7L)).thenReturn(Optional.of(existing));
    when(companyStatRepository.save(any(CompanyStatEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    CompanyStatUpdateRequest request = CompanyStatUpdateRequest.builder()
        .value("94%")
        .iconKey("city")
        .publicVisible(false)
        .build();

    CompanyStatResponse response = service().update(7L, 50L, request);

    assertThat(response.getValue()).isEqualTo("94%");
    assertThat(response.getIconKey()).isEqualTo("city");
    assertThat(response.isPublicVisible()).isFalse();
  }

  @Test
  void delete_softDeletesRow() {
    CompanyStatEntity existing = stat(50L);
    when(companyStatRepository.findByIdAndCompany_IdAndDeletedFalse(50L, 7L)).thenReturn(Optional.of(existing));

    service().delete(7L, 50L);

    ArgumentCaptor<CompanyStatEntity> captor = ArgumentCaptor.forClass(CompanyStatEntity.class);
    verify(companyStatRepository).save(captor.capture());
    assertThat(captor.getValue().getDeleted()).isTrue();
    assertThat(captor.getValue().getActive()).isFalse();
    assertThat(captor.getValue().getPublicVisible()).isFalse();
  }

  @Test
  void reorder_updatesSortOrderForEachItem() {
    when(companyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(company()));
    CompanyStatEntity stat1 = stat(1L);
    CompanyStatEntity stat2 = stat(2L);
    when(companyStatRepository.findByIdAndCompany_IdAndDeletedFalse(1L, 7L)).thenReturn(Optional.of(stat1));
    when(companyStatRepository.findByIdAndCompany_IdAndDeletedFalse(2L, 7L)).thenReturn(Optional.of(stat2));
    when(companyStatRepository.findByCompany_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(7L))
        .thenReturn(List.of(stat1, stat2));

    CompanyStatReorderRequest request = CompanyStatReorderRequest.builder()
        .items(List.of(
            CompanyStatReorderRequest.Item.builder().statId(1L).sortOrder(1).build(),
            CompanyStatReorderRequest.Item.builder().statId(2L).sortOrder(0).build()
        ))
        .build();

    service().reorder(7L, request);

    assertThat(stat1.getDisplayOrder()).isEqualTo(1);
    assertThat(stat2.getDisplayOrder()).isEqualTo(0);
  }
}
