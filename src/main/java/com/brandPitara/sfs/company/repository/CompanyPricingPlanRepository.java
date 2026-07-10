package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyPricingPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyPricingPlanRepository extends JpaRepository<CompanyPricingPlanEntity, Long> {

  List<CompanyPricingPlanEntity> findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(
      Long companyId
  );

  // Admin view - includes inactive/hidden rows so admins can see/toggle them.
  List<CompanyPricingPlanEntity> findByCompany_IdAndDeletedFalseOrderBySortOrderAscIdAsc(Long companyId);

  Optional<CompanyPricingPlanEntity> findByIdAndCompany_IdAndDeletedFalse(Long id, Long companyId);
}
