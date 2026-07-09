package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandProductCategoryResponse;
import com.brandPitara.sfs.brand.dto.BrandProductCategoryUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandProductCategoryEntity;
import com.brandPitara.sfs.brand.repository.BrandProductCategoryRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.impl.BrandProductCategoryServiceImpl;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandProductCategoryServiceImplTest {

  @Mock private BrandRepository brandRepository;
  @Mock private BrandProductCategoryRepository brandProductCategoryRepository;
  @Mock private ContentVersionService contentVersionService;

  @InjectMocks private BrandProductCategoryServiceImpl brandProductCategoryService;

  private BrandEntity brand() {
    return BrandEntity.builder().id(1L).name("Berger").deleted(false).build();
  }

  @Test
  void create_generatesSlugFromName_whenSlugMissing() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(brandProductCategoryRepository.findByBrand_IdAndSlugAndDeletedFalse(1L, "lamps")).thenReturn(Optional.empty());
    when(brandProductCategoryRepository.save(any(BrandProductCategoryEntity.class))).thenAnswer(inv -> {
      BrandProductCategoryEntity e = inv.getArgument(0);
      e.setId(20L);
      return e;
    });

    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("Lamps")
        .externalUrl("https://berger.com/lamps")
        .build();

    BrandProductCategoryResponse response = brandProductCategoryService.create(1L, request);

    assertThat(response.getSlug()).isEqualTo("lamps");
    assertThat(response.getName()).isEqualTo("Lamps");
    assertThat(response.getExternalUrl()).isEqualTo("https://berger.com/lamps");
    assertThat(response.isActive()).isTrue();
    assertThat(response.isPublicVisible()).isTrue();
  }

  @Test
  void create_throwsConflict_whenExplicitSlugAlreadyUsedInBrand() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(brandProductCategoryRepository.findByBrand_IdAndSlugAndDeletedFalse(1L, "lamps"))
        .thenReturn(Optional.of(BrandProductCategoryEntity.builder().id(5L).build()));

    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("Lamps")
        .slug("lamps")
        .build();

    assertThatThrownBy(() -> brandProductCategoryService.create(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409");

    verify(brandProductCategoryRepository, never()).save(any());
  }

  @Test
  void create_throwsNotFound_whenBrandMissing() {
    when(brandRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder().name("Lamps").build();

    assertThatThrownBy(() -> brandProductCategoryService.create(99L, request))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void update_appliesPartialChanges() {
    BrandProductCategoryEntity existing = BrandProductCategoryEntity.builder()
        .id(20L).brand(brand()).name("Lamps").slug("lamps")
        .active(true).publicVisible(true).deleted(false)
        .build();

    when(brandProductCategoryRepository.findByIdAndBrand_IdAndDeletedFalse(20L, 1L)).thenReturn(Optional.of(existing));
    when(brandProductCategoryRepository.save(any(BrandProductCategoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder()
        .name("Decorative Lamps")
        .build();

    BrandProductCategoryResponse response = brandProductCategoryService.update(1L, 20L, request);

    assertThat(response.getName()).isEqualTo("Decorative Lamps");
    assertThat(response.getSlug()).isEqualTo("lamps"); // untouched field preserved
  }

  @Test
  void update_throwsNotFound_whenCategoryMissing() {
    when(brandProductCategoryRepository.findByIdAndBrand_IdAndDeletedFalse(99L, 1L)).thenReturn(Optional.empty());

    BrandProductCategoryUpsertRequest request = BrandProductCategoryUpsertRequest.builder().name("x").build();

    assertThatThrownBy(() -> brandProductCategoryService.update(1L, 99L, request))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void softDelete_marksDeletedAndInactive() {
    BrandProductCategoryEntity existing = BrandProductCategoryEntity.builder()
        .id(20L).brand(brand()).name("Lamps").slug("lamps")
        .active(true).publicVisible(true).deleted(false)
        .build();

    when(brandProductCategoryRepository.findByIdAndBrand_IdAndDeletedFalse(20L, 1L)).thenReturn(Optional.of(existing));
    when(brandProductCategoryRepository.save(any(BrandProductCategoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    brandProductCategoryService.softDelete(1L, 20L);

    ArgumentCaptor<BrandProductCategoryEntity> captor = ArgumentCaptor.forClass(BrandProductCategoryEntity.class);
    verify(brandProductCategoryRepository).save(captor.capture());
    assertThat(captor.getValue().getDeleted()).isTrue();
    assertThat(captor.getValue().getActive()).isFalse();
  }
}
