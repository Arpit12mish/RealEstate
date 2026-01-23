package com.brandPitara.sfs.brand.repository;

import com.brandPitara.sfs.brand.entity.BrandMediaEntity;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity.Placement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandMediaRepository extends JpaRepository<BrandMediaEntity, Long> {

  List<BrandMediaEntity> findByBrandIdAndPlacementAndDeletedFalseAndActiveTrueOrderBySortOrderAsc(
      Long brandId, Placement placement
  );
}
