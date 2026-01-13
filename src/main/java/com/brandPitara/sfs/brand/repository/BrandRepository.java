package com.brandPitara.sfs.brand.repository;

import com.brandPitara.sfs.brand.entity.BrandEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandRepository extends JpaRepository<BrandEntity, Long> {

  Optional<BrandEntity> findByIdAndDeletedFalse(Long id);

  Page<BrandEntity> findByDeletedFalse(Pageable pageable);

  Page<BrandEntity> findByPublishedAndDeletedFalse(boolean published, Pageable pageable);

  Page<BrandEntity> findByActiveAndDeletedFalse(boolean active, Pageable pageable);

  Page<BrandEntity> findByPublishedAndActiveAndDeletedFalse(boolean published, boolean active, Pageable pageable);

  Page<BrandEntity> findByPublishedTrueAndActiveTrueAndDeletedFalse(Pageable pageable);
}
