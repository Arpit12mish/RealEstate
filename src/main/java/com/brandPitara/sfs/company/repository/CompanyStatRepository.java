package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyStatRepository extends JpaRepository<CompanyStatEntity, Long> {
  List<CompanyStatEntity> findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(Long companyId);


  //batch method
  List<CompanyStatEntity> findByCompany_IdInAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(
    Collection<Long> companyIds
);

  // ---------- Phase 4.8B: dashboard admin CRUD + public-visible read ----------

  List<CompanyStatEntity> findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(
      Long companyId
  );

  // Admin view - includes inactive/hidden rows so admins can see/toggle them.
  List<CompanyStatEntity> findByCompany_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(Long companyId);

  Optional<CompanyStatEntity> findByIdAndCompany_IdAndDeletedFalse(Long id, Long companyId);

  long countByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalse(Long companyId);
}