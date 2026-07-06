package com.brandPitara.sfs.brand.repository;

import com.brandPitara.sfs.brand.entity.BrandCertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandCertificateRepository extends JpaRepository<BrandCertificateEntity, Long> {

  List<BrandCertificateEntity> findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long brandId);

  List<BrandCertificateEntity> findByBrand_IdAndDeletedFalseOrderBySortOrderAscIdAsc(Long brandId);

  Optional<BrandCertificateEntity> findByIdAndBrand_IdAndDeletedFalse(Long id, Long brandId);
}
