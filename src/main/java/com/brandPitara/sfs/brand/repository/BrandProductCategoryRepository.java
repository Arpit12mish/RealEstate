package com.brandPitara.sfs.brand.repository;

import com.brandPitara.sfs.brand.entity.BrandProductCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandProductCategoryRepository extends JpaRepository<BrandProductCategoryEntity, Long> {

  Page<BrandProductCategoryEntity> findByBrand_IdAndDeletedFalse(Long brandId, Pageable pageable);

  List<BrandProductCategoryEntity> findByBrand_IdAndActiveTrueAndPublicVisibleTrueAndDeletedFalseOrderBySortOrderAscIdAsc(
      Long brandId
  );

  Optional<BrandProductCategoryEntity> findByIdAndBrand_IdAndDeletedFalse(Long id, Long brandId);

  Optional<BrandProductCategoryEntity> findByBrand_IdAndSlugAndDeletedFalse(Long brandId, String slug);

  Optional<BrandProductCategoryEntity> findByBrand_IdAndSlugAndIdNotAndDeletedFalse(Long brandId, String slug, Long id);
}
