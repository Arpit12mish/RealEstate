package com.brandPitara.sfs.brand.repository;

import com.brandPitara.sfs.brand.entity.BrandSkuEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BrandSkuRepository extends JpaRepository<BrandSkuEntity, Long> {

  Page<BrandSkuEntity> findByBrand_IdAndDeletedFalse(Long brandId, Pageable pageable);

  Page<BrandSkuEntity> findByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(Long brandId, Pageable pageable);

  long countByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(Long brandId);

  Optional<BrandSkuEntity> findByIdAndBrand_IdAndDeletedFalse(Long id, Long brandId);

  Optional<BrandSkuEntity> findByBrand_IdAndSlugAndDeletedFalse(Long brandId, String slug);

  Optional<BrandSkuEntity> findByBrand_IdAndSlugAndIdNotAndDeletedFalse(Long brandId, String slug, Long id);

  // "productCategories" grouping on the brand detail page - distinct categories actually
  // used by this brand's public-visible SKUs.
  @Query("""
      select distinct s.category
      from BrandSkuEntity s
      where s.brand.id = :brandId
        and s.published = true and s.active = true and s.deleted = false
        and s.category is not null
      """)
  List<CategoryEntity> findDistinctPublicCategoriesByBrandId(@Param("brandId") Long brandId);

  // Batch product counts for a page of brands - one query instead of one per row.
  // Each row is [brandId, count].
  @Query("""
      select s.brand.id, count(s)
      from BrandSkuEntity s
      where s.brand.id in :brandIds
        and s.published = true and s.active = true and s.deleted = false
      group by s.brand.id
      """)
  List<Object[]> countPublicByBrandIds(@Param("brandIds") Collection<Long> brandIds);
}
