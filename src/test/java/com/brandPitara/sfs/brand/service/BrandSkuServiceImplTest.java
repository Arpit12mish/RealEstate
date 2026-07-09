package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandSkuResponse;
import com.brandPitara.sfs.brand.dto.BrandSkuUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandProductCategoryEntity;
import com.brandPitara.sfs.brand.entity.BrandSkuEntity;
import com.brandPitara.sfs.brand.repository.BrandProductCategoryRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.repository.BrandSkuRepository;
import com.brandPitara.sfs.brand.service.impl.BrandSkuServiceImpl;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandSkuServiceImplTest {

  @Mock private BrandRepository brandRepository;
  @Mock private BrandSkuRepository brandSkuRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private BrandProductCategoryRepository brandProductCategoryRepository;
  @Mock private ContentVersionService contentVersionService;

  @InjectMocks private BrandSkuServiceImpl brandSkuService;

  private BrandEntity brand() {
    return BrandEntity.builder().id(1L).name("Berger").deleted(false).build();
  }

  @Test
  void create_generatesSlugFromName_whenSlugMissing() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(brandSkuRepository.findByBrand_IdAndSlugAndDeletedFalse(1L, "silk-glamour")).thenReturn(Optional.empty());
    when(brandSkuRepository.save(any(BrandSkuEntity.class))).thenAnswer(inv -> {
      BrandSkuEntity e = inv.getArgument(0);
      e.setId(10L);
      return e;
    });

    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Silk Glamour")
        .build();

    BrandSkuResponse response = brandSkuService.create(1L, request);

    assertThat(response.getSlug()).isEqualTo("silk-glamour");
    assertThat(response.getName()).isEqualTo("Silk Glamour");
    assertThat(response.getBrandId()).isEqualTo(1L);
  }

  @Test
  void create_throwsConflict_whenExplicitSlugAlreadyUsedInBrand() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(brandSkuRepository.findByBrand_IdAndSlugAndDeletedFalse(1L, "starter-kit"))
        .thenReturn(Optional.of(BrandSkuEntity.builder().id(5L).build()));

    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Starter Kit")
        .slug("starter-kit")
        .build();

    assertThatThrownBy(() -> brandSkuService.create(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409");

    verify(brandSkuRepository, never()).save(any());
  }

  @Test
  void softDelete_marksDeletedAndUnpublished() {
    BrandSkuEntity existing = BrandSkuEntity.builder()
        .id(10L)
        .brand(brand())
        .name("Silk Glamour")
        .slug("silk-glamour")
        .published(true)
        .active(true)
        .deleted(false)
        .build();

    when(brandSkuRepository.findByIdAndBrand_IdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(existing));
    when(brandSkuRepository.save(any(BrandSkuEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    brandSkuService.softDelete(1L, 10L);

    ArgumentCaptor<BrandSkuEntity> captor = ArgumentCaptor.forClass(BrandSkuEntity.class);
    verify(brandSkuRepository).save(captor.capture());
    assertThat(captor.getValue().getDeleted()).isTrue();
    assertThat(captor.getValue().getPublished()).isFalse();
  }

  @Test
  void create_throwsNotFound_whenBrandMissing() {
    when(brandRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder().name("Anything").build();

    assertThatThrownBy(() -> brandSkuService.create(99L, request))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void create_linksProductCategory_whenItBelongsToSameBrand() {
    BrandProductCategoryEntity productCategory = BrandProductCategoryEntity.builder()
        .id(50L).brand(brand()).name("Lamps").slug("lamps").deleted(false).build();

    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(brandProductCategoryRepository.findByIdAndBrand_IdAndDeletedFalse(50L, 1L))
        .thenReturn(Optional.of(productCategory));
    when(brandSkuRepository.findByBrand_IdAndSlugAndDeletedFalse(1L, "table-lamp")).thenReturn(Optional.empty());
    when(brandSkuRepository.save(any(BrandSkuEntity.class))).thenAnswer(inv -> {
      BrandSkuEntity e = inv.getArgument(0);
      e.setId(11L);
      return e;
    });

    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Table Lamp")
        .productCategoryId(50L)
        .build();

    BrandSkuResponse response = brandSkuService.create(1L, request);

    assertThat(response.getProductCategoryId()).isEqualTo(50L);
    assertThat(response.getProductCategoryName()).isEqualTo("Lamps");
  }

  @Test
  void create_throwsNotFound_whenProductCategoryBelongsToDifferentBrand() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(brandProductCategoryRepository.findByIdAndBrand_IdAndDeletedFalse(50L, 1L))
        .thenReturn(Optional.empty());

    BrandSkuUpsertRequest request = BrandSkuUpsertRequest.builder()
        .name("Table Lamp")
        .productCategoryId(50L)
        .build();

    assertThatThrownBy(() -> brandSkuService.create(1L, request))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Product category not found for this brand");

    verify(brandSkuRepository, never()).save(any());
  }
}
