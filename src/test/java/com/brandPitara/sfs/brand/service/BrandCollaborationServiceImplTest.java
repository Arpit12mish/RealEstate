package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandCollaborationResponse;
import com.brandPitara.sfs.brand.dto.BrandCollaborationUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import com.brandPitara.sfs.brand.enums.BrandRelationType;
import com.brandPitara.sfs.brand.enums.BrandSourceType;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.impl.BrandCollaborationServiceImpl;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.repository.BusinessRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandCollaborationServiceImplTest {

  @Mock private BrandRepository brandRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private BuilderRepository builderRepository;
  @Mock private CompanyRepository companyRepository;
  @Mock private CompanyProjectRepository companyProjectRepository;
  @Mock private BusinessRepository businessRepository;
  @Mock private BrandCollaborationRepository brandCollaborationRepository;
  @Mock private ContentVersionService contentVersionService;

  @InjectMocks private BrandCollaborationServiceImpl brandCollaborationService;

  private BrandEntity brand() {
    return BrandEntity.builder().id(1L).name("Berger").deleted(false).build();
  }

  private ProjectEntity project() {
    return ProjectEntity.builder().id(27L).name("Skyline Residency").build();
  }

  private CompanyProjectEntity companyProject() {
    return CompanyProjectEntity.builder().id(88L).name("Knowledge Center Hub").deleted(false).build();
  }

  @Test
  void create_projectTarget_setsOnlyProjectFk() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project()));
    when(brandCollaborationRepository.existsByBrand_IdAndProject_IdAndDeletedFalse(1L, 27L)).thenReturn(false);
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class))).thenAnswer(inv -> {
      BrandCollaborationEntity e = inv.getArgument(0);
      e.setId(500L);
      return e;
    });

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.PROJECT)
        .targetId(27L)
        .relationType(BrandRelationType.USED_IN_PROJECT)
        .build();

    BrandCollaborationResponse response = brandCollaborationService.create(1L, request);

    ArgumentCaptor<BrandCollaborationEntity> captor = ArgumentCaptor.forClass(BrandCollaborationEntity.class);
    verify(brandCollaborationRepository).save(captor.capture());
    BrandCollaborationEntity saved = captor.getValue();

    assertThat(saved.getTargetType()).isEqualTo(BrandCollaborationTargetType.PROJECT);
    assertThat(saved.getProject()).isNotNull();
    assertThat(saved.getProject().getId()).isEqualTo(27L);
    assertThat(saved.getBuilder()).isNull();
    assertThat(saved.getCompany()).isNull();
    assertThat(saved.getBusiness()).isNull();
    assertThat(saved.getCompanyProject()).isNull();
    assertThat(saved.getRelationType()).isEqualTo(BrandRelationType.USED_IN_PROJECT);

    assertThat(response.getTargetType()).isEqualTo(BrandCollaborationTargetType.PROJECT);
    assertThat(response.getTargetId()).isEqualTo(27L);
    assertThat(response.getTargetName()).isEqualTo("Skyline Residency");
    assertThat(response.getRelationType()).isEqualTo(BrandRelationType.USED_IN_PROJECT);

    verifyNoInteractions(builderRepository, companyRepository, companyProjectRepository, businessRepository);
  }

  @Test
  void create_companyProjectTarget_setsOnlyCompanyProjectFk() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(companyProject()));
    when(brandCollaborationRepository.existsByBrand_IdAndCompanyProject_IdAndDeletedFalse(1L, 88L)).thenReturn(false);
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class))).thenAnswer(inv -> {
      BrandCollaborationEntity e = inv.getArgument(0);
      e.setId(700L);
      return e;
    });

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.COMPANY_PROJECT)
        .targetId(88L)
        .relationType(BrandRelationType.USED_IN_PROJECT)
        .publicVisible(true)
        .build();

    BrandCollaborationResponse response = brandCollaborationService.create(1L, request);

    ArgumentCaptor<BrandCollaborationEntity> captor = ArgumentCaptor.forClass(BrandCollaborationEntity.class);
    verify(brandCollaborationRepository).save(captor.capture());
    BrandCollaborationEntity saved = captor.getValue();

    assertThat(saved.getTargetType()).isEqualTo(BrandCollaborationTargetType.COMPANY_PROJECT);
    assertThat(saved.getCompanyProject()).isNotNull();
    assertThat(saved.getCompanyProject().getId()).isEqualTo(88L);
    assertThat(saved.getProject()).isNull();
    assertThat(saved.getBuilder()).isNull();
    assertThat(saved.getCompany()).isNull();
    assertThat(saved.getBusiness()).isNull();

    assertThat(response.getTargetType()).isEqualTo(BrandCollaborationTargetType.COMPANY_PROJECT);
    assertThat(response.getTargetId()).isEqualTo(88L);
    assertThat(response.getTargetName()).isEqualTo("Knowledge Center Hub");
    assertThat(response.isPublicVisible()).isTrue();
  }

  @Test
  void create_throwsNotFound_whenTargetProjectDoesNotExist() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(projectRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.PROJECT)
        .targetId(999L)
        .build();

    assertThatThrownBy(() -> brandCollaborationService.create(1L, request))
        .isInstanceOf(EntityNotFoundException.class);

    verify(brandCollaborationRepository, never()).save(any());
  }

  @Test
  void create_throwsBadRequest_whenTargetTypeMissing() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetId(27L)
        .relationType(BrandRelationType.USED_IN_PROJECT)
        .build();

    assertThatThrownBy(() -> brandCollaborationService.create(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400")
        .hasMessageContaining("targetType is required");

    verify(brandCollaborationRepository, never()).save(any());
    verifyNoInteractions(projectRepository, builderRepository, companyRepository, companyProjectRepository, businessRepository);
  }

  @Test
  void create_throwsBadRequest_whenTargetIdMissing() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.PROJECT)
        .relationType(BrandRelationType.USED_IN_PROJECT)
        .build();

    assertThatThrownBy(() -> brandCollaborationService.create(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400")
        .hasMessageContaining("targetId is required");

    verify(brandCollaborationRepository, never()).save(any());
    verifyNoInteractions(projectRepository, builderRepository, companyRepository, companyProjectRepository, businessRepository);
  }

  @Test
  void create_throwsConflict_whenActiveCollaborationAlreadyExistsForSameTarget() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project()));
    when(brandCollaborationRepository.existsByBrand_IdAndProject_IdAndDeletedFalse(1L, 27L)).thenReturn(true);

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.PROJECT)
        .targetId(27L)
        .build();

    assertThatThrownBy(() -> brandCollaborationService.create(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409");

    verify(brandCollaborationRepository, never()).save(any());
  }

  /**
   * Pairs with V121: the *ExistsAndDeletedFalse repository check (which this test mocks)
   * is what the corrected partial unique index now matches at the DB level too - a
   * soft-deleted row no longer counts as "existing" for either layer, so recreating the
   * same brand+target pair after a delete must succeed.
   */
  @Test
  void create_allowsRecreation_whenOnlyExistingCollaborationForTargetWasSoftDeleted() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project()));
    // The only row for this brand+project pair is soft-deleted, so the DeletedFalse-scoped
    // exists check correctly reports no live conflict.
    when(brandCollaborationRepository.existsByBrand_IdAndProject_IdAndDeletedFalse(1L, 27L)).thenReturn(false);
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class))).thenAnswer(inv -> {
      BrandCollaborationEntity e = inv.getArgument(0);
      e.setId(501L);
      return e;
    });

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.PROJECT)
        .targetId(27L)
        .relationType(BrandRelationType.USED_IN_PROJECT)
        .build();

    BrandCollaborationResponse response = brandCollaborationService.create(1L, request);

    assertThat(response.getId()).isEqualTo(501L);
    verify(brandCollaborationRepository).save(any(BrandCollaborationEntity.class));
  }

  @Test
  void create_companyProject_allowsRecreation_whenOnlyExistingCollaborationWasSoftDeleted() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(companyProject()));
    when(brandCollaborationRepository.existsByBrand_IdAndCompanyProject_IdAndDeletedFalse(1L, 88L)).thenReturn(false);
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class))).thenAnswer(inv -> {
      BrandCollaborationEntity e = inv.getArgument(0);
      e.setId(701L);
      return e;
    });

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.COMPANY_PROJECT)
        .targetId(88L)
        .relationType(BrandRelationType.USED_IN_PROJECT)
        .build();

    BrandCollaborationResponse response = brandCollaborationService.create(1L, request);

    assertThat(response.getId()).isEqualTo(701L);
    verify(brandCollaborationRepository).save(any(BrandCollaborationEntity.class));
  }

  @Test
  void create_companyProject_throwsConflict_whenActiveCollaborationAlreadyExists() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(companyProject()));
    when(brandCollaborationRepository.existsByBrand_IdAndCompanyProject_IdAndDeletedFalse(1L, 88L)).thenReturn(true);

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.COMPANY_PROJECT)
        .targetId(88L)
        .relationType(BrandRelationType.USED_IN_PROJECT)
        .build();

    assertThatThrownBy(() -> brandCollaborationService.create(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409");

    verify(brandCollaborationRepository, never()).save(any());
  }

  @Test
  void update_companyProjectTarget_replacesTargetAndReturnsCompanyProjectSummary() {
    BrandCollaborationEntity existing = BrandCollaborationEntity.builder()
        .id(900L)
        .brand(brand())
        .project(project())
        .targetType(BrandCollaborationTargetType.PROJECT)
        .deleted(false)
        .active(true)
        .build();
    when(brandCollaborationRepository.findByIdAndBrand_IdAndDeletedFalse(900L, 1L)).thenReturn(Optional.of(existing));
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(companyProject()));
    when(brandCollaborationRepository.existsByBrand_IdAndCompanyProject_IdAndDeletedFalseAndIdNot(1L, 88L, 900L))
        .thenReturn(false);
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.COMPANY_PROJECT)
        .targetId(88L)
        .relationType(BrandRelationType.USED_IN_PROJECT)
        .build();

    BrandCollaborationResponse response = brandCollaborationService.update(1L, 900L, request);

    assertThat(existing.getProject()).isNull();
    assertThat(existing.getCompanyProject()).isNotNull();
    assertThat(response.getTargetType()).isEqualTo(BrandCollaborationTargetType.COMPANY_PROJECT);
    assertThat(response.getTargetId()).isEqualTo(88L);
    assertThat(response.getTargetName()).isEqualTo("Knowledge Center Hub");
  }

  @Test
  void create_defaultsSourceTypeToAdminEntered_whenOmitted() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project()));
    when(brandCollaborationRepository.existsByBrand_IdAndProject_IdAndDeletedFalse(1L, 27L)).thenReturn(false);
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.PROJECT)
        .targetId(27L)
        .relationType(BrandRelationType.USED_IN_PROJECT)
        .build();

    BrandCollaborationResponse response = brandCollaborationService.create(1L, request);

    assertThat(response.getSourceType()).isEqualTo(BrandSourceType.ADMIN_ENTERED);
  }

  @Test
  void create_keepsExplicitSourceType_whenProvided() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(projectRepository.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project()));
    when(brandCollaborationRepository.existsByBrand_IdAndProject_IdAndDeletedFalse(1L, 27L)).thenReturn(false);
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.PROJECT)
        .targetId(27L)
        .relationType(BrandRelationType.CERTIFIED_PARTNER)
        .sourceType(BrandSourceType.SFS_VERIFIED)
        .build();

    BrandCollaborationResponse response = brandCollaborationService.create(1L, request);

    assertThat(response.getSourceType()).isEqualTo(BrandSourceType.SFS_VERIFIED);
    assertThat(response.getRelationType()).isEqualTo(BrandRelationType.CERTIFIED_PARTNER);
  }

  @Test
  void adminList_throwsNotFound_whenBrandMissing() {
    when(brandRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

    Pageable pageable = PageRequest.of(0, 20);

    assertThatThrownBy(() -> brandCollaborationService.adminList(99L, pageable))
        .isInstanceOf(EntityNotFoundException.class);

    verifyNoInteractions(brandCollaborationRepository);
  }

  @Test
  void create_throwsNotFound_whenTargetBusinessMissingOrInactive() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    // findByIdAndActiveTrue returning empty covers both "doesn't exist" and
    // "exists but is inactive" - BusinessEntity has no separate deleted flag.
    when(businessRepository.findByIdAndActiveTrue(42L)).thenReturn(Optional.empty());

    BrandCollaborationUpsertRequest request = BrandCollaborationUpsertRequest.builder()
        .targetType(BrandCollaborationTargetType.BUSINESS)
        .targetId(42L)
        .relationType(BrandRelationType.ASSOCIATED_VENDOR)
        .build();

    assertThatThrownBy(() -> brandCollaborationService.create(1L, request))
        .isInstanceOf(EntityNotFoundException.class);

    verify(brandCollaborationRepository, never()).save(any());
    verifyNoInteractions(projectRepository, builderRepository, companyRepository, companyProjectRepository);
  }
}
