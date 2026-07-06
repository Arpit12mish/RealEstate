package com.brandPitara.sfs.brand.repository;

import com.brandPitara.sfs.brand.entity.BrandFaqEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandFaqRepository extends JpaRepository<BrandFaqEntity, Long> {

  List<BrandFaqEntity> findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long brandId);

  List<BrandFaqEntity> findByBrand_IdAndDeletedFalseOrderBySortOrderAscIdAsc(Long brandId);

  Optional<BrandFaqEntity> findByIdAndBrand_IdAndDeletedFalse(Long id, Long brandId);
}
