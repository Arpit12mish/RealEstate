package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMasterPlanEntity;
import com.brandPitara.sfs.project.enums.MasterPlanAreaUnit;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectMasterPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMasterPlanServiceImplTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectMasterPlanRepository masterPlanRepository;
  @Mock private ContentVersionService contentVersionService;
  @Mock private ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;

  @InjectMocks private ProjectMasterPlanServiceImpl service;

  @Test
  void upsertCreatesMasterPlanForProject() {
    ProjectEntity project = project(11L);
    when(projectRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(project));
    when(masterPlanRepository.findByProjectIdAndDeletedFalse(11L)).thenReturn(Optional.empty());
    when(masterPlanRepository.save(any(ProjectMasterPlanEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ProjectMasterPlanResponse response = service.upsert(11L, request("https://cdn.example.com/master.webp"));

    assertThat(response.getProjectId()).isEqualTo(11L);
    assertThat(response.getImageUrl()).isEqualTo("https://cdn.example.com/master.webp");
    assertThat(response.getStats()).extracting("key").contains("TOTAL_UNITS", "PARK_AREA");
    verify(contentVersionService).bump("PROJECTS");
  }

  @Test
  void upsertUpdatesExistingNonDeletedMasterPlanInsteadOfCreatingDuplicate() {
    ProjectEntity project = project(11L);
    ProjectMasterPlanEntity existing = ProjectMasterPlanEntity.builder()
        .id(5L)
        .project(project)
        .active(true)
        .deleted(false)
        .build();

    when(projectRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(project));
    when(masterPlanRepository.findByProjectIdAndDeletedFalse(11L)).thenReturn(Optional.of(existing));
    when(masterPlanRepository.save(any(ProjectMasterPlanEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    service.upsert(11L, request("https://cdn.example.com/updated.webp"));

    ArgumentCaptor<ProjectMasterPlanEntity> captor = ArgumentCaptor.forClass(ProjectMasterPlanEntity.class);
    verify(masterPlanRepository).save(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(5L);
    assertThat(captor.getValue().getMasterPlanImageUrl()).isEqualTo("https://cdn.example.com/updated.webp");
  }

  @Test
  void softDeleteMarksDeletedAndInactive() {
    ProjectEntity project = project(11L);
    ProjectMasterPlanEntity existing = ProjectMasterPlanEntity.builder()
        .id(5L)
        .project(project)
        .active(true)
        .deleted(false)
        .build();

    when(masterPlanRepository.findByProjectIdAndDeletedFalse(11L)).thenReturn(Optional.of(existing));

    service.softDelete(11L);

    assertThat(existing.getDeleted()).isTrue();
    assertThat(existing.getActive()).isFalse();
    verify(masterPlanRepository).save(existing);
  }

  @Test
  void setActiveTogglesVisibility() {
    ProjectEntity project = project(11L);
    ProjectMasterPlanEntity existing = ProjectMasterPlanEntity.builder()
        .id(5L)
        .project(project)
        .active(false)
        .deleted(false)
        .build();

    when(masterPlanRepository.findByProjectIdAndDeletedFalse(11L)).thenReturn(Optional.of(existing));
    when(masterPlanRepository.save(existing)).thenReturn(existing);

    ProjectMasterPlanResponse response = service.setActive(11L, true);

    assertThat(existing.getActive()).isTrue();
    assertThat(response.getActive()).isTrue();
  }

  @Test
  void publicGetReturnsNullForInactiveOrEmptyMasterPlan() {
    ProjectEntity project = project(11L);
    when(projectRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(project));
    when(masterPlanRepository.findByProjectIdAndActiveTrueAndDeletedFalse(11L))
        .thenReturn(Optional.of(ProjectMasterPlanEntity.builder().active(true).deleted(false).build()));

    ProjectMasterPlanResponse response = service.publicGet(11L);

    assertThat(response).isNull();
    verify(projectPublicVisibilityPolicy).assertPubliclyVisible(project, 11L);
  }

  private ProjectMasterPlanUpsertRequest request(String imageUrl) {
    return ProjectMasterPlanUpsertRequest.builder()
        .title("Master Plan")
        .subtitle("Site layout, towers & open spaces")
        .masterPlanImageUrl(imageUrl)
        .totalUnits(1520)
        .parkAreaValue(new BigDecimal("2.70"))
        .parkAreaUnit(MasterPlanAreaUnit.ACRE)
        .active(true)
        .build();
  }

  private ProjectEntity project(Long id) {
    ProjectEntity entity = new ProjectEntity();
    entity.setId(id);
    entity.setName("Project " + id);
    entity.setActive(true);
    entity.setPublished(true);
    entity.setDeleted(false);
    entity.setReviewStatus(ReviewStatus.APPROVED);
    return entity;
  }
}
