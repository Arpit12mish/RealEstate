package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandFaqResponse;
import com.brandPitara.sfs.brand.dto.BrandFaqUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandFaqEntity;
import com.brandPitara.sfs.brand.repository.BrandFaqRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.impl.BrandFaqServiceImpl;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandFaqServiceImplTest {

  @Mock private BrandRepository brandRepository;
  @Mock private BrandFaqRepository brandFaqRepository;
  @Mock private ContentVersionService contentVersionService;

  @InjectMocks private BrandFaqServiceImpl brandFaqService;

  private BrandEntity brand() {
    return BrandEntity.builder().id(1L).name("Berger").deleted(false).build();
  }

  @Test
  void create_savesFaqForBrand() {
    when(brandRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(brand()));
    when(brandFaqRepository.save(any(BrandFaqEntity.class))).thenAnswer(inv -> {
      BrandFaqEntity e = inv.getArgument(0);
      e.setId(30L);
      return e;
    });

    BrandFaqUpsertRequest request = BrandFaqUpsertRequest.builder()
        .question("Is Silk Glamour washable?")
        .answer("Yes, it is washable and stain resistant.")
        .build();

    BrandFaqResponse response = brandFaqService.create(1L, request);

    assertThat(response.getQuestion()).isEqualTo("Is Silk Glamour washable?");
    assertThat(response.isActive()).isTrue();
  }

  @Test
  void update_appliesPartialChanges() {
    BrandFaqEntity existing = BrandFaqEntity.builder()
        .id(30L)
        .brand(brand())
        .question("Is Silk Glamour washable?")
        .answer("Yes.")
        .active(true)
        .deleted(false)
        .build();

    when(brandFaqRepository.findByIdAndBrand_IdAndDeletedFalse(30L, 1L)).thenReturn(Optional.of(existing));
    when(brandFaqRepository.save(any(BrandFaqEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    BrandFaqUpsertRequest request = BrandFaqUpsertRequest.builder()
        .answer("Yes, it is washable and stain resistant.")
        .build();

    BrandFaqResponse response = brandFaqService.update(1L, 30L, request);

    assertThat(response.getAnswer()).isEqualTo("Yes, it is washable and stain resistant.");
    assertThat(response.getQuestion()).isEqualTo("Is Silk Glamour washable?"); // untouched
  }

  @Test
  void softDelete_marksDeletedAndInactive() {
    BrandFaqEntity existing = BrandFaqEntity.builder()
        .id(30L)
        .brand(brand())
        .question("Q")
        .answer("A")
        .active(true)
        .deleted(false)
        .build();

    when(brandFaqRepository.findByIdAndBrand_IdAndDeletedFalse(30L, 1L)).thenReturn(Optional.of(existing));
    when(brandFaqRepository.save(any(BrandFaqEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    brandFaqService.softDelete(1L, 30L);

    ArgumentCaptor<BrandFaqEntity> captor = ArgumentCaptor.forClass(BrandFaqEntity.class);
    verify(brandFaqRepository).save(captor.capture());
    assertThat(captor.getValue().getDeleted()).isTrue();
    assertThat(captor.getValue().getActive()).isFalse();
  }

  @Test
  void create_throwsNotFound_whenBrandMissing() {
    when(brandRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

    BrandFaqUpsertRequest request = BrandFaqUpsertRequest.builder().question("Q").answer("A").build();

    assertThatThrownBy(() -> brandFaqService.create(99L, request))
        .isInstanceOf(EntityNotFoundException.class);
  }
}
