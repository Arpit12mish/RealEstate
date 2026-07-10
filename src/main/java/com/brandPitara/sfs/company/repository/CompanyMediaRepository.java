package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyMediaRepository extends JpaRepository<CompanyMediaEntity, Long> {

  List<CompanyMediaEntity> findByCompany_IdAndUsageTypeAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(
      Long companyId, String usageType
  );

  // Admin view - includes inactive/hidden rows so admins can see/toggle them.
  List<CompanyMediaEntity> findByCompany_IdAndDeletedFalseOrderBySortOrderAscIdAsc(Long companyId);

  Optional<CompanyMediaEntity> findByIdAndCompany_IdAndDeletedFalse(Long id, Long companyId);

  long countByCompany_IdAndUsageTypeAndActiveTrueAndDeletedFalse(Long companyId, String usageType);
}
