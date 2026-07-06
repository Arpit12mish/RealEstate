package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.BrandResponse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplAdminListTest {

  @Mock private BrandRepository brandRepository;
  @Mock private BrandMediaRepository brandMediaRepository;
  @Mock private BrandDistributorRepository brandDistributorRepository;
  @Mock private BrandCategoryLinkRepository brandCategoryLinkRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private ContentVersionService contentVersionService;

  @InjectMocks private BrandServiceImpl brandService;

  private BrandEntity brand(Long id, String name) {
    return BrandEntity.builder().id(id).name(name).deleted(false).active(true).published(true).priority(0).build();
  }

  private BrandCategoryLinkEntity link(BrandEntity brand, Long categoryId, String categoryName) {
    CategoryEntity category = CategoryEntity.builder().id(categoryId).name(categoryName).slug(categoryName.toLowerCase()).active(true).build();
    return BrandCategoryLinkEntity.builder().id(categoryId * 100).brand(brand).category(category).sortOrder(0).active(true).deleted(false).build();
  }

  @Test
  void adminList_batchesCategoryLinkLookup_insteadOfQueryingPerRow() {
    BrandEntity brand1 = brand(1L, "Berger");
    BrandEntity brand2 = brand(2L, "Asian Paints");
    Pageable pageable = PageRequest.of(0, 20);
    Page<BrandEntity> page = new PageImpl<>(List.of(brand1, brand2), pageable, 2);

    when(brandRepository.findByDeletedFalse(pageable)).thenReturn(page);
    when(brandCategoryLinkRepository.findByBrand_IdInAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(anyCollection()))
        .thenReturn(List.of(
            link(brand1, 9L, "Paints"),
            link(brand2, 9L, "Paints"),
            link(brand2, 10L, "Building Materials")
        ));

    Page<BrandResponse> result = brandService.adminList(null, null, pageable);

    // One batched call for the whole page...
    verify(brandCategoryLinkRepository, times(1))
        .findByBrand_IdInAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(anyCollection());
    // ...and never the per-brand lookup, which would mean an N+1 across the page.
    verify(brandCategoryLinkRepository, never())
        .findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(any());

    BrandResponse response1 = result.getContent().stream().filter(r -> r.getId().equals(1L)).findFirst().orElseThrow();
    BrandResponse response2 = result.getContent().stream().filter(r -> r.getId().equals(2L)).findFirst().orElseThrow();

    assertThat(response1.getCategoryIds()).containsExactly(9L);
    assertThat(response2.getCategoryIds()).containsExactly(9L, 10L);
  }

  @Test
  void adminList_handlesEmptyPage_withoutCallingBatchLookup() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<BrandEntity> emptyPage = new PageImpl<>(List.<BrandEntity>of(), pageable, 0);

    when(brandRepository.findByDeletedFalse(pageable)).thenReturn(emptyPage);
    when(brandCategoryLinkRepository.findByBrand_IdInAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(anyCollection()))
        .thenReturn(List.of());

    Page<BrandResponse> result = brandService.adminList(null, null, pageable);

    assertThat(result.getContent()).isEmpty();
  }
}
