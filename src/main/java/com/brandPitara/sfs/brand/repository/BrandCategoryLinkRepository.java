package com.brandPitara.sfs.brand.repository;

import com.brandPitara.sfs.brand.entity.BrandCategoryLinkEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BrandCategoryLinkRepository extends JpaRepository<BrandCategoryLinkEntity, Long> {

  List<BrandCategoryLinkEntity> findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long brandId);

  // Batch variant for list/page responses - one query for every brand on the page
  // instead of one query per row (see BrandServiceImpl.adminList).
  List<BrandCategoryLinkEntity> findByBrand_IdInAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Collection<Long> brandIds);

  List<BrandCategoryLinkEntity> findByCategory_IdAndActiveTrueAndDeletedFalse(Long categoryId);

  List<BrandCategoryLinkEntity> findByBrand_IdAndDeletedFalseOrderBySortOrderAscIdAsc(Long brandId);

  Optional<BrandCategoryLinkEntity> findByBrand_IdAndCategory_Id(Long brandId, Long categoryId);

  Optional<BrandCategoryLinkEntity> findByBrand_IdAndCategory_IdAndDeletedFalse(Long brandId, Long categoryId);

  // Mobile filter chips: only categories linked to at least one public-visible brand.
  @Query("""
      select distinct l.category
      from BrandCategoryLinkEntity l
        join l.brand b
      where l.active = true and l.deleted = false
        and l.category.active = true
        and b.published = true and b.active = true and b.deleted = false
      order by l.category.priority asc, l.category.name asc
      """)
  List<CategoryEntity> findDistinctPublicCategories();
}
