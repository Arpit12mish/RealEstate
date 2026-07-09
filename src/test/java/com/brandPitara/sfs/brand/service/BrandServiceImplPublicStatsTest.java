package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCategoryLinkRepository;
import com.brandPitara.sfs.brand.repository.BrandDistributorRepository;
import com.brandPitara.sfs.brand.repository.BrandMediaRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.impl.BrandServiceImpl;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplPublicStatsTest {

  @Mock private BrandRepository brandRepository;
  @Mock private BrandMediaRepository brandMediaRepository;
  @Mock private BrandDistributorRepository brandDistributorRepository;
  @Mock private BrandCategoryLinkRepository brandCategoryLinkRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private ContentVersionService contentVersionService;

  @InjectMocks private BrandServiceImpl brandService;

  @Test
  void create_persistsPublicStatsFields_andResponseIncludesYearsInIndustry() {
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Ikea")
        .slug("ikea")
        .foundedYear(1943)
        .customerRating(new BigDecimal("4.7"))
        .customerRatingCount(350)
        .build();

    when(brandRepository.findBySlug("ikea")).thenReturn(Optional.empty());
    when(brandRepository.save(any(BrandEntity.class))).thenAnswer(invocation -> {
      BrandEntity saved = invocation.getArgument(0);
      saved.setId(10L);
      return saved;
    });
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(10L))
        .thenReturn(List.of());

    var response = brandService.create(request);

    ArgumentCaptor<BrandEntity> entityCaptor = ArgumentCaptor.forClass(BrandEntity.class);
    org.mockito.Mockito.verify(brandRepository).save(entityCaptor.capture());
    BrandEntity saved = entityCaptor.getValue();

    assertThat(saved.getFoundedYear()).isEqualTo(1943);
    assertThat(saved.getCustomerRating()).isEqualByComparingTo("4.7");
    assertThat(saved.getCustomerRatingCount()).isEqualTo(350);
    assertThat(response.getFoundedYear()).isEqualTo(1943);
    assertThat(response.getYearsInIndustry()).isEqualTo(Year.now().getValue() - 1943);
    assertThat(response.getCustomerRating()).isEqualByComparingTo("4.7");
    assertThat(response.getCustomerRatingCount()).isEqualTo(350);
  }

  @Test
  void update_persistsPublicStatsFields_andResponseIncludesYearsInIndustry() {
    BrandEntity existing = BrandEntity.builder()
        .id(10L)
        .name("Ikea")
        .slug("ikea")
        .active(true)
        .published(false)
        .deleted(false)
        .priority(0)
        .build();
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Ikea")
        .build();
    request.setFoundedYear(1943);
    request.setCustomerRating(new BigDecimal("4.7"));
    request.setCustomerRatingCount(350);

    when(brandRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(existing));
    when(brandRepository.save(any(BrandEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(10L))
        .thenReturn(List.of());

    var response = brandService.update(10L, request);

    assertThat(existing.getFoundedYear()).isEqualTo(1943);
    assertThat(existing.getCustomerRating()).isEqualByComparingTo("4.7");
    assertThat(existing.getCustomerRatingCount()).isEqualTo(350);
    assertThat(response.getFoundedYear()).isEqualTo(1943);
    assertThat(response.getYearsInIndustry()).isEqualTo(Year.now().getValue() - 1943);
    assertThat(response.getCustomerRating()).isEqualByComparingTo("4.7");
    assertThat(response.getCustomerRatingCount()).isEqualTo(350);
  }

  @Test
  void update_clearsPublicStatsFields_whenExplicitNullsAreProvided() {
    BrandEntity existing = BrandEntity.builder()
        .id(10L)
        .name("Ikea")
        .slug("ikea")
        .foundedYear(1943)
        .customerRating(new BigDecimal("4.7"))
        .customerRatingCount(350)
        .active(true)
        .published(false)
        .deleted(false)
        .priority(0)
        .build();
    BrandUpsertRequest request = BrandUpsertRequest.builder()
        .name("Ikea")
        .build();
    request.setFoundedYear(null);
    request.setCustomerRating(null);
    request.setCustomerRatingCount(null);

    when(brandRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(existing));
    when(brandRepository.save(any(BrandEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(10L))
        .thenReturn(List.of());

    var response = brandService.update(10L, request);

    assertThat(existing.getFoundedYear()).isNull();
    assertThat(existing.getCustomerRating()).isNull();
    assertThat(existing.getCustomerRatingCount()).isNull();
    assertThat(response.getFoundedYear()).isNull();
    assertThat(response.getYearsInIndustry()).isNull();
    assertThat(response.getCustomerRating()).isNull();
    assertThat(response.getCustomerRatingCount()).isNull();
  }
}
