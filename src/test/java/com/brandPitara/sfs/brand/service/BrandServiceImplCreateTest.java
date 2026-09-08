package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandResponse;
import com.brandPitara.sfs.brand.dto.BrandUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandCategoryLinkEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCategoryLinkRepository;
import com.brandPitara.sfs.brand.repository.BrandDistributorRepository;
import com.brandPitara.sfs.brand.repository.BrandMediaRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.impl.BrandServiceImpl;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplCreateTest {

  @Mock private BrandRepository brandRepository;
  @Mock private BrandMediaRepository brandMediaRepository;
  @Mock private BrandDistributorRepository brandDistributorRepository;
  @Mock private BrandCategoryLinkRepository brandCategoryLinkRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private ContentVersionService contentVersionService;

  @InjectMocks private BrandServiceImpl brandService;

  private void stubSaveReturnsWithId(Long id) {
    when(brandRepository.save(any(BrandEntity.class))).thenAnswer(invocation -> {
      BrandEntity entity = invocation.getArgument(0);
      entity.setId(id);
      return entity;
    });
  }

  @Test
  void create_withMinimumPayload_succeeds() {
    BrandUpsertRequest request = BrandUpsertRequest.builder().name("Test Brand Create").build();

    when(brandRepository.findBySlug("test-brand-create")).thenReturn(Optional.empty());
    stubSaveReturnsWithId(1L);
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());

    BrandResponse response = brandService.create(request);

    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getName()).isEqualTo("Test Brand Create");
    assertThat(response.getSlug()).isEqualTo("test-brand-create");
    assertThat(response.isActive()).isTrue();
    assertThat(response.isPublished()).isFalse();
    assertThat(response.getPriority()).isEqualTo(0);
    assertThat(response.isPromoEnabled()).isFalse();
    assertThat(response.getFoundedYear()).isNull();
    assertThat(response.getCustomerRating()).isNull();
    assertThat(response.getCustomerRatingCount()).isNull();
    assertThat(response.getCategoryIds()).isEmpty();

    verifyNoInteractions(categoryRepository);
  }

  @Test
  void create_withBlankSlug_generatesSlugFromName() {
    BrandUpsertRequest request = BrandUpsertRequest.builder().name("Asian Paints").slug("").build();

    when(brandRepository.findBySlug("asian-paints")).thenReturn(Optional.empty());
    stubSaveReturnsWithId(2L);
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(2L))
        .thenReturn(List.of());

    BrandResponse response = brandService.create(request);

    assertThat(response.getSlug()).isEqualTo("asian-paints");
  }

  @Test
  void create_withSlugCollision_appendsSuffixUntilUnique() {
    BrandUpsertRequest request = BrandUpsertRequest.builder().name("Berger").build();

    when(brandRepository.findBySlug("berger")).thenReturn(Optional.of(BrandEntity.builder().id(99L).build()));
    when(brandRepository.findBySlug("berger-2")).thenReturn(Optional.empty());
    stubSaveReturnsWithId(3L);
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(3L))
        .thenReturn(List.of());

    BrandResponse response = brandService.create(request);

    assertThat(response.getSlug()).isEqualTo("berger-2");
  }

  @Test
  void create_withExplicitAvailableSlug_succeeds() {
    BrandUpsertRequest request = BrandUpsertRequest.builder().name("Berger Paints").slug("berger-paints-india").build();

    when(brandRepository.findBySlug("berger-paints-india")).thenReturn(Optional.empty());
    stubSaveReturnsWithId(4L);
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(4L))
        .thenReturn(List.of());

    BrandResponse response = brandService.create(request);

    assertThat(response.getSlug()).isEqualTo("berger-paints-india");
  }

  @Test
  void create_withDuplicateExplicitSlug_throwsConflict() {
    BrandUpsertRequest request = BrandUpsertRequest.builder().name("Berger Paints").slug("asian-paints").build();

    when(brandRepository.findBySlug("asian-paints"))
        .thenReturn(Optional.of(BrandEntity.builder().id(5L).name("Asian Paints").build()));

    assertThatThrownBy(() -> brandService.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Brand slug already exists");

    verify(brandRepository, never()).save(any());
  }

  @Test
  void create_withoutCategoryIds_doesNotTouchCategoryLinks() {
    BrandUpsertRequest request = BrandUpsertRequest.builder().name("No Category Brand").build();

    when(brandRepository.findBySlug("no-category-brand")).thenReturn(Optional.empty());
    stubSaveReturnsWithId(6L);

    BrandResponse response = brandService.create(request);

    assertThat(response.getCategoryIds()).isEmpty();
    verifyNoInteractions(categoryRepository);
    verify(brandCategoryLinkRepository, never()).findByBrand_IdOrderBySortOrderAscIdAsc(anyLong());
  }

  @Test
  void create_withEmptyCategoryIds_succeeds() {
    BrandUpsertRequest request = BrandUpsertRequest.builder().name("Empty Category Brand").categoryIds(List.of()).build();

    when(brandRepository.findBySlug("empty-category-brand")).thenReturn(Optional.empty());
    stubSaveReturnsWithId(7L);
    when(brandCategoryLinkRepository.findByBrand_IdOrderBySortOrderAscIdAsc(7L)).thenReturn(List.of());
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(7L))
        .thenReturn(List.of());

    BrandResponse response = brandService.create(request);

    assertThat(response.getCategoryIds()).isEmpty();
    verify(brandCategoryLinkRepository, never()).save(any());
  }

  @Test
  void create_withDuplicateCategoryIds_dedupesBeforeLinking() {
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Dedup Brand")
        .categoryIds(List.of(10L, 10L, 11L))
        .build();

    CategoryEntity cat10 = CategoryEntity.builder().id(10L).name("Paints").active(true).build();
    CategoryEntity cat11 = CategoryEntity.builder().id(11L).name("Tiles").active(true).build();

    when(brandRepository.findBySlug("dedup-brand")).thenReturn(Optional.empty());
    stubSaveReturnsWithId(8L);
    when(categoryRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(cat10));
    when(categoryRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(cat11));
    when(brandCategoryLinkRepository.findByBrand_IdOrderBySortOrderAscIdAsc(8L)).thenReturn(List.of());
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(8L))
        .thenReturn(List.of(link(8L, cat10), link(8L, cat11)));

    BrandResponse response = brandService.create(request);

    assertThat(response.getCategoryIds()).containsExactlyInAnyOrder(10L, 11L);
    verify(categoryRepository, times(1)).findByIdAndActiveTrue(10L);
    verify(brandCategoryLinkRepository, times(2)).save(any(BrandCategoryLinkEntity.class));
  }

  @Test
  void create_withStatsFields_persistsAndComputesYearsInIndustry() {
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Stats Brand")
        .foundedYear(2000)
        .customerRating(new BigDecimal("4.5"))
        .customerRatingCount(120)
        .build();

    when(brandRepository.findBySlug("stats-brand")).thenReturn(Optional.empty());
    stubSaveReturnsWithId(9L);
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(9L))
        .thenReturn(List.of());

    BrandResponse response = brandService.create(request);

    assertThat(response.getFoundedYear()).isEqualTo(2000);
    assertThat(response.getCustomerRating()).isEqualByComparingTo("4.5");
    assertThat(response.getCustomerRatingCount()).isEqualTo(120);
    assertThat(response.getYearsInIndustry()).isEqualTo(java.time.Year.now().getValue() - 2000);
  }

  @Test
  void create_withPromoDisabled_persistsNullPromoFields() {
    BrandUpsertRequest request = BrandUpsertRequest.builder().name("Promo Off Brand").promoEnabled(false).build();

    when(brandRepository.findBySlug("promo-off-brand")).thenReturn(Optional.empty());
    stubSaveReturnsWithId(11L);
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(11L))
        .thenReturn(List.of());

    BrandResponse response = brandService.create(request);

    assertThat(response.isPromoEnabled()).isFalse();
    assertThat(response.getPromoMediaType()).isNull();
    assertThat(response.getPromoMediaUrl()).isNull();
  }

  @Test
  void create_bumpsBrandsContentVersion() {
    BrandUpsertRequest request = BrandUpsertRequest.builder().name("Version Bump Brand").build();

    when(brandRepository.findBySlug("version-bump-brand")).thenReturn(Optional.empty());
    stubSaveReturnsWithId(12L);
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(12L))
        .thenReturn(List.of());

    brandService.create(request);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(contentVersionService).bump(keyCaptor.capture());
    assertThat(keyCaptor.getValue()).isEqualTo("BRANDS");
  }

  private BrandCategoryLinkEntity link(Long brandId, CategoryEntity category) {
    return BrandCategoryLinkEntity.builder()
        .id(category.getId() * 100)
        .brand(BrandEntity.builder().id(brandId).build())
        .category(category)
        .sortOrder(0)
        .active(true)
        .deleted(false)
        .build();
  }
}
