package com.brandPitara.sfs.dashboard.companyproject.service.impl;

import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectMediaEntity;
import com.brandPitara.sfs.company.repository.CompanyProjectMediaRepository;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.dashboard.companyproject.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCompanyProjectMediaServiceImplTest {
  @Mock CompanyProjectRepository projectRepository;
  @Mock CompanyProjectMediaRepository mediaRepository;

  private DashboardCompanyProjectMediaServiceImpl service() { return new DashboardCompanyProjectMediaServiceImpl(projectRepository, mediaRepository); }

  @Test
  void mediaCrudAndReorderUseProjectScopedSoftDelete() {
    CompanyProjectEntity project = CompanyProjectEntity.builder().id(1L).name("Office").deleted(false).build();
    when(projectRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(project));
    when(mediaRepository.save(any())).thenAnswer(invocation -> { CompanyProjectMediaEntity e = invocation.getArgument(0); if (e.getId() == null) e.setId(10L); return e; });
    var created = service().create(1L, CompanyProjectMediaCreateRequest.builder().mediaUrl("one.jpg").categoryKey("WORKSTATIONS").build());
    assertThat(created.getPublicVisible()).isTrue();
    assertThat(created.getActive()).isTrue();

    CompanyProjectMediaEntity entity = CompanyProjectMediaEntity.builder().id(10L).companyProject(project).mediaUrl("one.jpg").categoryKey("WORKSTATIONS").sortOrder(0).active(true).publicVisible(true).deleted(false).build();
    when(mediaRepository.findByIdAndCompanyProject_IdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(entity));
    service().update(1L, 10L, CompanyProjectMediaUpdateRequest.builder().metricValue("2,200+ seats").build());
    assertThat(entity.getMetricValue()).isEqualTo("2,200+ seats");

    when(mediaRepository.findByCompanyProject_IdAndDeletedFalseOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(entity));
    service().reorder(1L, CompanyProjectMediaReorderRequest.builder().items(List.of(new CompanyProjectMediaReorderRequest.Item(10L, 4))).build());
    assertThat(entity.getSortOrder()).isEqualTo(4);

    service().delete(1L, 10L);
    assertThat(entity.getDeleted()).isTrue();
    assertThat(entity.getActive()).isFalse();
    assertThat(entity.getPublicVisible()).isFalse();
  }
}
