package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.dto.ProjectPatchRequest;
import com.brandPitara.sfs.project.dto.ProjectPublicResponse;
import com.brandPitara.sfs.project.dto.ProjectUpsertRequest;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.project.service.ProjectPublicCoreService;
import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
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
  @Mock private ProjectPublicCoreService projectPublicCoreService;
  @Mock private ProjectPublicCacheEvictionPublisher cacheEvictionPublisher;
  @Spy private ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy = new ProjectPublicVisibilityPolicy();

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
    verify(cacheEvictionPublisher).publish(35L, ProjectCacheEvictionReason.PROJECT_UPDATED);
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
    BuilderEntity builder = publicBuilder();
    existing.setBuilder(builder);

    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(existing));
    when(builderRepository.findByIdAndDeletedFalseForUpdate(7L)).thenReturn(Optional.of(builder));
    when(projectRepository.save(existing)).thenReturn(existing);

    service.setPublished(27L, true);

    assertThat(existing.getPublished()).isTrue();
    verify(projectRepository).save(existing);
    verify(cacheEvictionPublisher).publish(27L, ProjectCacheEvictionReason.VISIBILITY_CHANGED);
  }

  @Test
  void setPublishedRejectsApprovedProjectWithUnpublishedBuilder() {
    ProjectEntity existing = project(27L, "m3m-jewel");
    existing.setReviewStatus(ReviewStatus.APPROVED);
    BuilderEntity builder = publicBuilder();
    builder.setPublished(false);
    existing.setBuilder(builder);

    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(existing));
    when(builderRepository.findByIdAndDeletedFalseForUpdate(7L)).thenReturn(Optional.of(builder));

    assertThatThrownBy(() -> service.setPublished(27L, true))
        .isInstanceOf(com.brandPitara.sfs.project.exception.PublicationConflictException.class)
        .satisfies(ex -> assertThat(
            ((com.brandPitara.sfs.project.exception.PublicationConflictException) ex).getCode())
            .isEqualTo("PROJECT_BUILDER_NOT_PUBLISHED"));

    assertThat(existing.getPublished()).isFalse();
    verify(projectRepository, never()).save(any(ProjectEntity.class));
  }

  @Test
  void setPublishedRejectsApprovedProjectWithInactiveBuilder() {
    ProjectEntity existing = project(27L, "m3m-jewel");
    existing.setReviewStatus(ReviewStatus.APPROVED);
    BuilderEntity builder = publicBuilder();
    builder.setActive(false);
    existing.setBuilder(builder);

    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(existing));
    when(builderRepository.findByIdAndDeletedFalseForUpdate(7L)).thenReturn(Optional.of(builder));

    assertThatThrownBy(() -> service.setPublished(27L, true))
        .isInstanceOf(com.brandPitara.sfs.project.exception.PublicationConflictException.class)
        .satisfies(ex -> assertThat(
            ((com.brandPitara.sfs.project.exception.PublicationConflictException) ex).getCode())
            .isEqualTo("PROJECT_BUILDER_INACTIVE"));

    assertThat(existing.getPublished()).isFalse();
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
    verify(cacheEvictionPublisher).publish(35L, ProjectCacheEvictionReason.VISIBILITY_CHANGED);
  }

  @Test
  void softDeletePublishesPriorityDeletionEviction() {
    ProjectEntity existing = project(27L, "m3m-jewel");
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(existing));
    when(projectRepository.save(existing)).thenReturn(existing);

    service.softDelete(27L);

    verify(cacheEvictionPublisher).publish(27L, ProjectCacheEvictionReason.PROJECT_DELETED);
  }

  @Test
  void publicBrowseWithManualCityFilterCanReturnEmptyWithoutGlobalFallback() {
    PageRequest pageable = PageRequest.of(0, 10);

    when(projectRepository.findByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
        7L,
        ReviewStatus.APPROVED,
        pageable
    )).thenReturn(Page.empty(pageable));

    Page<?> page = service.publicBrowse(null, 7L, pageable);

    assertThat(page.getContent()).isEmpty();
    verify(projectRepository).findByCityIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
        7L,
        ReviewStatus.APPROVED,
        pageable
    );
    verify(projectRepository, never()).findByPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(
        any(),
        any()
    );
  }

  @Test
  void publicGetUsesSharedCoreAndAddsMembershipWithoutRecounting() {
    ProjectPublicCoreData core = ProjectPublicCoreData.builder()
        .id(27L)
        .name("Project 27")
        .favoriteCount(9L)
        .propertyTypes(java.util.Set.of())
        .build();
    when(projectPublicCoreService.getById(27L)).thenReturn(core);
    when(projectFavoriteService.isCurrentViewerFavorite(27L)).thenReturn(true);

    ProjectPublicResponse result = service.publicGet(27L);

    assertThat(result.getFavoriteCount()).isEqualTo(9L);
    assertThat(result.getIsFavorite()).isTrue();
    verify(projectFavoriteService, never()).enrichPublicProject(any());
    verify(projectFavoriteService, never()).getProjectFavoriteCount(any());
  }

  // --- publicGetBySlug (GAP-001) ---

  @Test
  void publicGetBySlugReturnsComposedResponseForVisibleProject() {
    ProjectPublicCoreData core = ProjectPublicCoreData.builder()
        .id(51L)
        .slug("m3m-antalya-hills")
        .name("M3M Antalya Hills")
        .favoriteCount(4L)
        .propertyTypes(java.util.Set.of())
        .build();
    when(projectPublicCoreService.getBySlug("m3m-antalya-hills")).thenReturn(core);
    when(projectFavoriteService.isCurrentViewerFavorite(51L)).thenReturn(false);

    ProjectPublicResponse result = service.publicGetBySlug("m3m-antalya-hills");

    assertThat(result.getSlug()).isEqualTo("m3m-antalya-hills");
    assertThat(result.getFavoriteCount()).isEqualTo(4L);
    assertThat(result.getIsFavorite()).isFalse();
    verify(projectFavoriteService).isCurrentViewerFavorite(51L);
  }

  @Test
  void publicGetBySlugThrowsNotFoundForUnknownSlug() {
    when(projectPublicCoreService.getBySlug("does-not-exist"))
        .thenThrow(new NotFoundException("Project not found: does-not-exist"));

    assertThatThrownBy(() -> service.publicGetBySlug("does-not-exist"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("does-not-exist");

    verify(projectFavoriteService, never()).isCurrentViewerFavorite(any());
  }

  @Test
  void publicGetBySlugThrowsNotFoundForDraftProject() {
    when(projectPublicCoreService.getBySlug("draft-project"))
        .thenThrow(new NotFoundException("Project not found: draft-project"));

    assertThatThrownBy(() -> service.publicGetBySlug("draft-project"))
        .isInstanceOf(NotFoundException.class);

    verify(projectFavoriteService, never()).isCurrentViewerFavorite(any());
  }

  @Test
  void publicGetBySlugThrowsNotFoundForInactiveProject() {
    when(projectPublicCoreService.getBySlug("inactive-project"))
        .thenThrow(new NotFoundException("Project not found: inactive-project"));

    assertThatThrownBy(() -> service.publicGetBySlug("inactive-project"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void publicGetBySlugThrowsNotFoundForDeletedProject() {
    // findBySlugAndDeletedFalse itself excludes deleted rows at the query
    // level - a deleted project's slug simply never matches, mirroring
    // findByIdAndDeletedFalse's existing behavior for publicGet(Long).
    when(projectPublicCoreService.getBySlug("deleted-project"))
        .thenThrow(new NotFoundException("Project not found: deleted-project"));

    assertThatThrownBy(() -> service.publicGetBySlug("deleted-project"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void publicGetBySlugIsCaseSensitiveNonCanonicalCasingIsNotFound() {
    // No IgnoreCase lookup exists for Project slugs (unlike City's
    // findBySlugIgnoreCaseAndActiveTrue()) - a differently-cased request
    // is indistinguishable from an unknown slug and must 404 the same way,
    // matching BrandPublicServiceImpl's own established precedent.
    when(projectPublicCoreService.getBySlug("M3M-Antalya-Hills"))
        .thenThrow(new NotFoundException("Project not found: M3M-Antalya-Hills"));

    assertThatThrownBy(() -> service.publicGetBySlug("M3M-Antalya-Hills"))
        .isInstanceOf(NotFoundException.class);

    verify(projectPublicCoreService, never()).getBySlug("m3m-antalya-hills");
  }

  @Test
  void publicGetBySlugTreatsMalformedSlugAsNotFoundWithoutSeparateValidation() {
    // No server-side slug format validation exists (matching Brand's own
    // precedent of not special-casing format) - a malformed string simply
    // matches no row and 404s the same way any other unknown slug would.
    String malformed = "not a valid slug!! ??";

    when(projectPublicCoreService.getBySlug(malformed))
        .thenThrow(new NotFoundException("Project not found: " + malformed));

    assertThatThrownBy(() -> service.publicGetBySlug(malformed))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void publicGetBySlugDoesNotScanTheFullProjectList() {
    when(projectPublicCoreService.getBySlug("m3m-antalya-hills"))
        .thenThrow(new NotFoundException("Project not found: m3m-antalya-hills"));

    assertThatThrownBy(() -> service.publicGetBySlug("m3m-antalya-hills"))
        .isInstanceOf(NotFoundException.class);

    verify(projectRepository, never()).findAll();
    verify(projectRepository, never()).findByDeletedFalse(any());
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

  private BuilderEntity publicBuilder() {
    return BuilderEntity.builder()
        .id(7L)
        .name("Max Estates")
        .published(true)
        .active(true)
        .deleted(false)
        .build();
  }
}
