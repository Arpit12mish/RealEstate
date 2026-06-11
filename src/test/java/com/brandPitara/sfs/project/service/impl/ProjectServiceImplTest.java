package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.dto.ProjectPatchRequest;
import com.brandPitara.sfs.project.dto.ProjectUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectDetailComposer;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

  @Mock private BuilderRepository builderRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectFloorPlanRepository projectFloorPlanRepository;
  @Mock private ContentVersionService contentVersionService;
  @Mock private ProjectMediaRepository projectMediaRepository;
  @Mock private ProjectFavoriteService projectFavoriteService;
  @Mock private ProjectDetailComposer projectDetailComposer;
  @Mock private ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;

  @InjectMocks private ProjectServiceImpl service;

  @Test
  void createRejectsDuplicateSlugBeforeSaving() {
    ProjectUpsertRequest request = ProjectUpsertRequest.builder()
        .name("M3M Antalya Hills 3")
        .slug("m3m-antalya-hills")
        .build();

    when(projectRepository.findBySlug("m3m-antalya-hills"))
        .thenReturn(Optional.of(project(12L, "m3m-antalya-hills")));

    assertThatThrownBy(() -> service.create(7L, request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          ResponseStatusException rse = (ResponseStatusException) ex;
          assertThat(rse.getStatusCode().value()).isEqualTo(409);
          assertThat(rse.getReason()).contains("m3m-antalya-hills", "projectId=12");
        });

    verify(projectRepository, never()).save(org.mockito.ArgumentMatchers.any(ProjectEntity.class));
  }

  @Test
  void updateRejectsSlugUsedByAnotherProjectBeforeSaving() {
    ProjectUpsertRequest request = ProjectUpsertRequest.builder()
        .name("M3M Antalya Hills 3")
        .slug("m3m-antalya-hills")
        .status(ProjectStatus.UNDER_CONSTRUCTION)
        .build();

    when(projectRepository.findByIdAndDeletedFalse(35L))
        .thenReturn(Optional.of(project(35L, "m3m-antalya-hills-3")));
    when(projectRepository.findBySlugAndIdNot("m3m-antalya-hills", 35L))
        .thenReturn(Optional.of(project(12L, "m3m-antalya-hills")));

    assertThatThrownBy(() -> service.update(35L, request))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          ResponseStatusException rse = (ResponseStatusException) ex;
          assertThat(rse.getStatusCode().value()).isEqualTo(409);
          assertThat(rse.getReason()).contains("m3m-antalya-hills", "projectId=12");
        });

    verify(projectRepository, never()).save(org.mockito.ArgumentMatchers.any(ProjectEntity.class));
  }

  @Test
  void updateAllowsCurrentProjectToKeepItsOwnSlug() {
    ProjectEntity existing = project(35L, "m3m-antalya-hills");
    ProjectUpsertRequest request = ProjectUpsertRequest.builder()
        .name("M3M Antalya Hills 3")
        .slug("m3m-antalya-hills")
        .status(ProjectStatus.UNDER_CONSTRUCTION)
        .build();

    when(projectRepository.findByIdAndDeletedFalse(35L)).thenReturn(Optional.of(existing));
    when(projectRepository.findBySlugAndIdNot("m3m-antalya-hills", 35L)).thenReturn(Optional.empty());
    when(projectRepository.save(existing)).thenReturn(existing);

    service.update(35L, request);

    verify(projectRepository).save(existing);
  }

  @Test
  void updateAcceptsCompletedProjectStatus() {
    ProjectEntity existing = project(45L, "ats-pristine-golf-villas");
    existing.setStatus(ProjectStatus.READY_TO_MOVE);
    ProjectUpsertRequest request = ProjectUpsertRequest.builder()
        .name("ATS PRISTINE GOLF VILLAS ( Phase -1 )")
        .slug("ats-pristine-golf-villas")
        .status(ProjectStatus.COMPLETED)
        .build();

    when(projectRepository.findByIdAndDeletedFalse(45L)).thenReturn(Optional.of(existing));
    when(projectRepository.findBySlugAndIdNot("ats-pristine-golf-villas", 45L)).thenReturn(Optional.empty());
    when(projectRepository.save(existing)).thenReturn(existing);

    service.update(45L, request);

    assertThat(existing.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
    verify(projectRepository).save(existing);
  }

  @Test
  void patchStatusOnlyDoesNotValidateSlug() {
    ProjectEntity existing = project(45L, "ats-pristine-golf-villas");
    existing.setStatus(ProjectStatus.READY_TO_MOVE);
    ProjectPatchRequest request = ProjectPatchRequest.builder()
        .status(ProjectStatus.COMPLETED)
        .build();

    when(projectRepository.findByIdAndDeletedFalse(45L)).thenReturn(Optional.of(existing));
    when(projectRepository.save(existing)).thenReturn(existing);

    service.patch(45L, request);

    assertThat(existing.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
    verify(projectRepository, never()).findBySlugAndIdNot(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
    verify(projectRepository).save(existing);
  }

  @Test
  void setPublishedRejectsPublishingDraftProject() {
    ProjectEntity existing = project(35L, "alameda-central");
    existing.setReviewStatus(ReviewStatus.DRAFT);

    when(projectRepository.findByIdAndDeletedFalse(35L)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.setPublished(35L, true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must be APPROVED");

    verify(projectRepository, never()).save(org.mockito.ArgumentMatchers.any(ProjectEntity.class));
  }

  @Test
  void setPublishedAllowsApprovedProject() {
    ProjectEntity existing = project(27L, "m3m-jewel");
    existing.setReviewStatus(ReviewStatus.APPROVED);

    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(existing));
    when(projectRepository.save(existing)).thenReturn(existing);

    service.setPublished(27L, true);

    assertThat(existing.getPublished()).isTrue();
    verify(projectRepository).save(existing);
  }

  @Test
  void setPublishedAllowsUnpublishingUnapprovedProject() {
    ProjectEntity existing = project(35L, "alameda-central");
    existing.setReviewStatus(ReviewStatus.DRAFT);
    existing.setPublished(true);

    when(projectRepository.findByIdAndDeletedFalse(35L)).thenReturn(Optional.of(existing));
    when(projectRepository.save(existing)).thenReturn(existing);

    service.setPublished(35L, false);

    assertThat(existing.getPublished()).isFalse();
    verify(projectRepository).save(existing);
  }

  private ProjectEntity project(Long id, String slug) {
    ProjectEntity entity = new ProjectEntity();
    entity.setId(id);
    entity.setName("Project " + id);
    entity.setSlug(slug);
    entity.setActive(true);
    entity.setPublished(false);
    entity.setDeleted(false);
    entity.setPriority(0);
    entity.setReviewStatus(ReviewStatus.DRAFT);
    return entity;
  }
}
