package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandCategoryLinkResponse;
import com.brandPitara.sfs.brand.dto.BrandCategoryLinkUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandCategoryLinkEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCategoryLinkRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.impl.BrandCategoryLinkServiceImpl;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandCategoryLinkServiceImplTest {

  @Mock private BrandRepository brandRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private BrandCategoryLinkRepository brandCategoryLinkRepository;
  @Mock private ContentVersionService contentVersionService;

  @InjectMocks private BrandCategoryLinkServiceImpl brandCategoryLinkService;

  private BrandEntity brand() {
    return BrandEntity.builder().id(1L).name("Berger").deleted(false).build();
  }

  private CategoryEntity category() {
    return CategoryEntity.builder().id(9L).name("Paint & Finishes").slug("paints-finishes").active(true).build();
  }

  @Test
  void upsert_createsNewLink_whenNoneExists() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(categoryRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.of(category()));
    when(brandCategoryLinkRepository.findByBrand_IdAndCategory_Id(1L, 9L)).thenReturn(Optional.empty());
    when(brandCategoryLinkRepository.save(any(BrandCategoryLinkEntity.class))).thenAnswer(inv -> {
      BrandCategoryLinkEntity e = inv.getArgument(0);
      e.setId(100L);
      return e;
    });

    BrandCategoryLinkUpsertRequest request = BrandCategoryLinkUpsertRequest.builder()
        .categoryId(9L)
        .displayOrder(1)
        .active(true)
        .build();

    BrandCategoryLinkResponse response = brandCategoryLinkService.upsert(1L, request);

    assertThat(response.getCategoryId()).isEqualTo(9L);
    assertThat(response.getCategoryName()).isEqualTo("Paint & Finishes");
    assertThat(response.getDisplayOrder()).isEqualTo(1);
    verify(brandCategoryLinkRepository).save(any(BrandCategoryLinkEntity.class));
  }

  @Test
  void upsert_updatesExistingLink_insteadOfCreatingDuplicate() {
    BrandCategoryLinkEntity existing = BrandCategoryLinkEntity.builder()
        .id(55L)
        .brand(brand())
        .category(category())
        .sortOrder(0)
        .active(true)
        .deleted(false)
        .build();

    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(categoryRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.of(category()));
    when(brandCategoryLinkRepository.findByBrand_IdAndCategory_Id(1L, 9L)).thenReturn(Optional.of(existing));
    when(brandCategoryLinkRepository.save(any(BrandCategoryLinkEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BrandCategoryLinkUpsertRequest request = BrandCategoryLinkUpsertRequest.builder()
        .categoryId(9L)
        .displayOrder(3)
        .build();

    BrandCategoryLinkResponse response = brandCategoryLinkService.upsert(1L, request);

    ArgumentCaptor<BrandCategoryLinkEntity> captor = ArgumentCaptor.forClass(BrandCategoryLinkEntity.class);
    verify(brandCategoryLinkRepository).save(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(55L);
    assertThat(response.getDisplayOrder()).isEqualTo(3);
    // Same existing row is reused, not a second insert - satisfies the upsert convention.
    verify(brandCategoryLinkRepository, times(1)).save(any());
  }

  @Test
  void upsert_throwsNotFound_whenCategoryInactiveOrMissing() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(categoryRepository.findByIdAndActiveTrue(9L)).thenReturn(Optional.empty());

    BrandCategoryLinkUpsertRequest request = BrandCategoryLinkUpsertRequest.builder().categoryId(9L).build();

    assertThatThrownBy(() -> brandCategoryLinkService.upsert(1L, request))
        .isInstanceOf(EntityNotFoundException.class);
  }
}
