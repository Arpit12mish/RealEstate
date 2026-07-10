package com.brandPitara.sfs.dashboard.companyproject.service.impl;

import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedCreateRequest;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedReorderRequest;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedResponse;
import com.brandPitara.sfs.dashboard.companyproject.dto.CompanyProjectBrandUsedUpdateRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardCompanyProjectBrandServiceImplTest {

  @Mock private CompanyProjectRepository companyProjectRepository;
  @Mock private BrandRepository brandRepository;
  @Mock private BrandCollaborationRepository brandCollaborationRepository;
  @Mock private ContentVersionService contentVersionService;

  private DashboardCompanyProjectBrandServiceImpl service() {
    return new DashboardCompanyProjectBrandServiceImpl(
        companyProjectRepository, brandRepository, brandCollaborationRepository, contentVersionService
    );
  }

  private CompanyProjectEntity project() {
    return CompanyProjectEntity.builder().id(88L).name("Immersive Hub").deleted(false).build();
  }

  private BrandEntity publishedBrand() {
    return BrandEntity.builder()
        .id(1L).name("Samsung").slug("samsung").logoUrl("samsung.png")
        .published(true).active(true).deleted(false)
        .build();
  }

  private BrandEntity unpublishedBrand() {
    return BrandEntity.builder()
        .id(2L).name("Unlisted Co").slug("unlisted-co").logoUrl(null)
        .published(false).active(true).deleted(false)
        .build();
  }

  private BrandCollaborationEntity collaboration(Long id, BrandEntity brand, CompanyProjectEntity project) {
    return BrandCollaborationEntity.builder()
        .id(id)
        .brand(brand)
        .companyProject(project)
        .targetType(BrandCollaborationTargetType.COMPANY_PROJECT)
        .publicVisible(true)
        .verified(false)
        .featured(false)
        .sortOrder(0)
        .active(true)
        .deleted(false)
        .build();
  }

  @Test
  void list_returnsOnlyCompanyProjectRowsForThatProject() {
    CompanyProjectEntity project = project();
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(project));
    BrandCollaborationEntity row = collaboration(10L, publishedBrand(), project);
    when(brandCollaborationRepository
        .findByCompanyProject_IdAndTargetTypeAndDeletedFalseOrderBySortOrderAscIdAsc(88L, BrandCollaborationTargetType.COMPANY_PROJECT))
        .thenReturn(List.of(row));

    List<CompanyProjectBrandUsedResponse> result = service().list(88L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getBrandId()).isEqualTo(1L);
    assertThat(result.get(0).getCompanyProjectId()).isEqualTo(88L);
    verify(brandCollaborationRepository)
        .findByCompanyProject_IdAndTargetTypeAndDeletedFalseOrderBySortOrderAscIdAsc(88L, BrandCollaborationTargetType.COMPANY_PROJECT);
  }

  @Test
  void list_throwsNotFound_whenCompanyProjectMissing() {
    when(companyProjectRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().list(999L)).isInstanceOf(EntityNotFoundException.class);
    verifyNoInteractions(brandCollaborationRepository);
  }

  @Test
  void create_addsValidPublishedBrand() {
    CompanyProjectEntity project = project();
    BrandEntity brand = publishedBrand();
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(project));
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand));
    when(brandCollaborationRepository.existsByBrand_IdAndCompanyProject_IdAndDeletedFalse(1L, 88L)).thenReturn(false);
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class)))
        .thenAnswer(inv -> {
          BrandCollaborationEntity e = inv.getArgument(0);
          e.setId(50L);
          return e;
        });

    CompanyProjectBrandUsedCreateRequest request = CompanyProjectBrandUsedCreateRequest.builder()
        .brandId(1L)
        .featured(true)
        .sortOrder(3)
        .title("Featured in living room")
        .build();

    CompanyProjectBrandUsedResponse response = service().create(88L, request);

    assertThat(response.getId()).isEqualTo(50L);
    assertThat(response.getBrandId()).isEqualTo(1L);
    assertThat(response.getBrandSlug()).isEqualTo("samsung");
    assertThat(response.isPublicVisible()).isTrue();
    assertThat(response.isFeatured()).isTrue();
    assertThat(response.getSortOrder()).isEqualTo(3);
    assertThat(response.getTitle()).isEqualTo("Featured in living room");

    ArgumentCaptor<BrandCollaborationEntity> captor = ArgumentCaptor.forClass(BrandCollaborationEntity.class);
    verify(brandCollaborationRepository).save(captor.capture());
    BrandCollaborationEntity saved = captor.getValue();
    assertThat(saved.getTargetType()).isEqualTo(BrandCollaborationTargetType.COMPANY_PROJECT);
    assertThat(saved.getCompanyProject()).isEqualTo(project);
    assertThat(saved.getCompany()).isNull();
    assertThat(saved.getProject()).isNull();
    assertThat(saved.getBuilder()).isNull();
    assertThat(saved.getBusiness()).isNull();
    verify(contentVersionService).bump("BRANDS");
    verify(contentVersionService).bump("HOME");
  }

  @Test
  void create_rejectsDuplicateActiveCollaboration() {
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(project()));
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(publishedBrand()));
    when(brandCollaborationRepository.existsByBrand_IdAndCompanyProject_IdAndDeletedFalse(1L, 88L)).thenReturn(true);

    CompanyProjectBrandUsedCreateRequest request = CompanyProjectBrandUsedCreateRequest.builder().brandId(1L).build();

    assertThatThrownBy(() -> service().create(88L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already attached");
    verify(brandCollaborationRepository, never()).save(any());
  }

  @Test
  void create_rejectsMissingCompanyProject() {
    when(companyProjectRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

    CompanyProjectBrandUsedCreateRequest request = CompanyProjectBrandUsedCreateRequest.builder().brandId(1L).build();

    assertThatThrownBy(() -> service().create(999L, request)).isInstanceOf(EntityNotFoundException.class);
    verifyNoInteractions(brandRepository);
  }

  @Test
  void create_rejectsMissingBrand() {
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(project()));
    when(brandRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

    CompanyProjectBrandUsedCreateRequest request = CompanyProjectBrandUsedCreateRequest.builder().brandId(404L).build();

    assertThatThrownBy(() -> service().create(88L, request)).isInstanceOf(EntityNotFoundException.class);
    verify(brandCollaborationRepository, never()).save(any());
  }

  @Test
  void create_rejectsPublicVisibleTrueForUnpublishedBrand() {
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(project()));
    when(brandRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(unpublishedBrand()));

    CompanyProjectBrandUsedCreateRequest request = CompanyProjectBrandUsedCreateRequest.builder()
        .brandId(2L)
        .publicVisible(true)
        .build();

    assertThatThrownBy(() -> service().create(88L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("published and active");
    verify(brandCollaborationRepository, never()).save(any());
  }

  @Test
  void create_allowsPublicVisibleFalseForUnpublishedBrand() {
    CompanyProjectEntity project = project();
    BrandEntity brand = unpublishedBrand();
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(project));
    when(brandRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(brand));
    when(brandCollaborationRepository.existsByBrand_IdAndCompanyProject_IdAndDeletedFalse(2L, 88L)).thenReturn(false);
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    CompanyProjectBrandUsedCreateRequest request = CompanyProjectBrandUsedCreateRequest.builder()
        .brandId(2L)
        .publicVisible(false)
        .build();

    CompanyProjectBrandUsedResponse response = service().create(88L, request);

    assertThat(response.isPublicVisible()).isFalse();
  }

  @Test
  void update_changesSortOrderPublicVisibleFeaturedVerifiedTitleDescription() {
    CompanyProjectEntity project = project();
    BrandEntity brand = publishedBrand();
    BrandCollaborationEntity existing = collaboration(50L, brand, project);
    when(brandCollaborationRepository
        .findByIdAndCompanyProject_IdAndTargetTypeAndDeletedFalse(50L, 88L, BrandCollaborationTargetType.COMPANY_PROJECT))
        .thenReturn(Optional.of(existing));
    when(brandCollaborationRepository.save(any(BrandCollaborationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    CompanyProjectBrandUsedUpdateRequest request = CompanyProjectBrandUsedUpdateRequest.builder()
        .sortOrder(5)
        .publicVisible(true)
        .featured(true)
        .verified(true)
        .title("Updated title")
        .description("Updated description")
        .build();

    CompanyProjectBrandUsedResponse response = service().update(88L, 50L, request);

    assertThat(response.getSortOrder()).isEqualTo(5);
    assertThat(response.isPublicVisible()).isTrue();
    assertThat(response.isFeatured()).isTrue();
    assertThat(response.isVerified()).isTrue();
    assertThat(response.getTitle()).isEqualTo("Updated title");
    assertThat(response.getDescription()).isEqualTo("Updated description");
    verify(contentVersionService).bump("BRANDS");
    verify(contentVersionService).bump("HOME");
  }

  @Test
  void update_rejectsPublicVisibleTrue_whenBrandNoLongerPublishable() {
    CompanyProjectEntity project = project();
    BrandEntity brand = unpublishedBrand();
    BrandCollaborationEntity existing = collaboration(50L, brand, project);
    existing.setPublicVisible(false);
    when(brandCollaborationRepository
        .findByIdAndCompanyProject_IdAndTargetTypeAndDeletedFalse(50L, 88L, BrandCollaborationTargetType.COMPANY_PROJECT))
        .thenReturn(Optional.of(existing));

    CompanyProjectBrandUsedUpdateRequest request = CompanyProjectBrandUsedUpdateRequest.builder()
        .publicVisible(true)
        .build();

    assertThatThrownBy(() -> service().update(88L, 50L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("published and active");
    verify(brandCollaborationRepository, never()).save(any());
  }

  @Test
  void update_throwsNotFound_whenCollaborationDoesNotBelongToProject() {
    when(brandCollaborationRepository
        .findByIdAndCompanyProject_IdAndTargetTypeAndDeletedFalse(50L, 88L, BrandCollaborationTargetType.COMPANY_PROJECT))
        .thenReturn(Optional.empty());

    CompanyProjectBrandUsedUpdateRequest request = CompanyProjectBrandUsedUpdateRequest.builder().sortOrder(1).build();

    assertThatThrownBy(() -> service().update(88L, 50L, request)).isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void delete_softDeletesRow() {
    CompanyProjectEntity project = project();
    BrandCollaborationEntity existing = collaboration(50L, publishedBrand(), project);
    when(brandCollaborationRepository
        .findByIdAndCompanyProject_IdAndTargetTypeAndDeletedFalse(50L, 88L, BrandCollaborationTargetType.COMPANY_PROJECT))
        .thenReturn(Optional.of(existing));

    service().delete(88L, 50L);

    ArgumentCaptor<BrandCollaborationEntity> captor = ArgumentCaptor.forClass(BrandCollaborationEntity.class);
    verify(brandCollaborationRepository).save(captor.capture());
    BrandCollaborationEntity saved = captor.getValue();
    assertThat(saved.getDeleted()).isTrue();
    assertThat(saved.getActive()).isFalse();
    assertThat(saved.getPublicVisible()).isFalse();
    verify(contentVersionService).bump("BRANDS");
    verify(contentVersionService).bump("HOME");
  }

  @Test
  void reorder_updatesSortOrderForEachItemAndReturnsOrderedList() {
    CompanyProjectEntity project = project();
    BrandCollaborationEntity row1 = collaboration(10L, publishedBrand(), project);
    BrandCollaborationEntity row2 = collaboration(11L, publishedBrand(), project);
    when(companyProjectRepository.findByIdAndDeletedFalse(88L)).thenReturn(Optional.of(project));
    when(brandCollaborationRepository
        .findByIdAndCompanyProject_IdAndTargetTypeAndDeletedFalse(10L, 88L, BrandCollaborationTargetType.COMPANY_PROJECT))
        .thenReturn(Optional.of(row1));
    when(brandCollaborationRepository
        .findByIdAndCompanyProject_IdAndTargetTypeAndDeletedFalse(11L, 88L, BrandCollaborationTargetType.COMPANY_PROJECT))
        .thenReturn(Optional.of(row2));
    when(brandCollaborationRepository
        .findByCompanyProject_IdAndTargetTypeAndDeletedFalseOrderBySortOrderAscIdAsc(88L, BrandCollaborationTargetType.COMPANY_PROJECT))
        .thenReturn(List.of(row1, row2));

    CompanyProjectBrandUsedReorderRequest request = CompanyProjectBrandUsedReorderRequest.builder()
        .items(List.of(
            CompanyProjectBrandUsedReorderRequest.Item.builder().collaborationId(10L).sortOrder(1).build(),
            CompanyProjectBrandUsedReorderRequest.Item.builder().collaborationId(11L).sortOrder(0).build()
        ))
        .build();

    service().reorder(88L, request);

    assertThat(row1.getSortOrder()).isEqualTo(1);
    assertThat(row2.getSortOrder()).isEqualTo(0);
    verify(brandCollaborationRepository, times(2)).save(any(BrandCollaborationEntity.class));
    verify(contentVersionService).bump("BRANDS");
  }
}
