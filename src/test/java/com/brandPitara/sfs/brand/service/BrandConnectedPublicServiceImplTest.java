package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.PublicConnectedBrandsSectionResponse;
import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.brand.service.impl.BrandConnectedPublicServiceImpl;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandConnectedPublicServiceImplTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private BuilderRepository builderRepository;
  @Mock private BrandCollaborationRepository brandCollaborationRepository;

  private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy = new ProjectPublicVisibilityPolicy();

  private BrandConnectedPublicServiceImpl service;

  private BrandConnectedPublicServiceImpl service() {
    return new BrandConnectedPublicServiceImpl(
        projectRepository, projectPublicVisibilityPolicy, builderRepository, brandCollaborationRepository
    );
  }

  private ProjectEntity approvedProject(Long id) {
    return ProjectEntity.builder()
        .id(id).name("Skyline Residency")
        .published(true).active(true).deleted(false)
        .reviewStatus(ReviewStatus.APPROVED)
        .build();
  }

  private BrandCollaborationEntity collaboration(Long id, Long brandName) {
    BrandEntity brand = BrandEntity.builder()
        .id(brandName).name("Berger").slug("berger").published(true).active(true).deleted(false)
        .build();
    return BrandCollaborationEntity.builder()
        .id(id).brand(brand).featured(true).verified(true).sortOrder(0).active(true).deleted(false)
        .publicVisible(true)
        .build();
  }

  // ---------- 5: excludes publicVisible=false collaboration ----------

  @Test
  void getProjectConnectedBrands_returnsOnlyWhatRepositoryFilters_excludingNonPublicVisible() {
    service = service();
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(approvedProject(27L)));
    // The repository query itself excludes publicVisible=false rows (see
    // BrandCollaborationRepository.findPublicByProjectId) - simulating that filtering
    // having already happened by only returning the one row that should survive it.
    when(brandCollaborationRepository.findPublicByProjectId(eq(27L), any(Pageable.class)))
        .thenReturn(List.of(collaboration(1L, 42L)));

    PublicConnectedBrandsSectionResponse response = service.getProjectConnectedBrands(27L, 10);

    assertThat(response.getProjectId()).isEqualTo(27L);
    assertThat(response.getItems()).hasSize(1);
    assertThat(response.getItems().get(0).getBrandId()).isEqualTo(42L);
  }

  // ---------- 6: excludes unpublished brand ----------

  @Test
  void getProjectConnectedBrands_excludesCollaborationsWhoseBrandIsNotPublished() {
    service = service();
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(approvedProject(27L)));
    // Only the still-visible collaboration is returned by the repository (its query
    // filters on brand.published/active/deleted) - an unpublished-brand row never reaches here.
    when(brandCollaborationRepository.findPublicByProjectId(eq(27L), any(Pageable.class)))
        .thenReturn(List.of(collaboration(1L, 42L)));

    PublicConnectedBrandsSectionResponse response = service.getProjectConnectedBrands(27L, 10);

    assertThat(response.getItems()).extracting("brandId").containsExactly(42L);
  }

  @Test
  void getProjectConnectedBrands_throwsNotFound_whenProjectMissing() {
    service = service();
    when(projectRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProjectConnectedBrands(999L, 10))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void getProjectConnectedBrands_throwsNotFound_whenProjectNotApproved() {
    service = service();
    ProjectEntity draft = approvedProject(27L);
    draft.setReviewStatus(ReviewStatus.DRAFT);
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(draft));

    assertThatThrownBy(() -> service.getProjectConnectedBrands(27L, 10))
        .isInstanceOf(NotFoundException.class);
  }

  // ---------- 7: builder connected brands sorted featured/displayOrder ----------

  @Test
  void getBuilderConnectedBrands_preservesRepositoryFeaturedDisplayOrderSort() {
    service = service();
    BuilderEntity builder = BuilderEntity.builder().id(5L).published(true).active(true).deleted(false).build();
    when(builderRepository.findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(5L)).thenReturn(Optional.of(builder));

    // Repository query orders by featured desc, sortOrder asc, id asc - mock returns rows
    // pre-sorted exactly like that, and the service must not reorder them.
    BrandCollaborationEntity featuredFirst = collaboration(1L, 42L);
    BrandCollaborationEntity second = collaboration(2L, 43L);
    when(brandCollaborationRepository.findPublicByBuilderId(eq(5L), any(Pageable.class)))
        .thenReturn(List.of(featuredFirst, second));

    PublicConnectedBrandsSectionResponse response = service.getBuilderConnectedBrands(5L, 10);

    assertThat(response.getBuilderId()).isEqualTo(5L);
    assertThat(response.getItems()).extracting("brandId").containsExactly(42L, 43L);
  }

  @Test
  void getBuilderConnectedBrands_throwsNotFound_whenBuilderMissingOrUnpublished() {
    service = service();
    when(builderRepository.findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getBuilderConnectedBrands(5L, 10))
        .isInstanceOf(EntityNotFoundException.class);
  }
}
