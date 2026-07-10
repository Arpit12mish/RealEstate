package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyCertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyCertificateRepository extends JpaRepository<CompanyCertificateEntity, Long> {
  List<CompanyCertificateEntity> findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(Long companyId);

  // ---------- Phase 4.8B: dashboard admin CRUD + public-visible read ----------

  List<CompanyCertificateEntity> findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(
      Long companyId
  );

  // Admin view - includes inactive/hidden rows so admins can see/toggle them.
  List<CompanyCertificateEntity> findByCompany_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(Long companyId);

  Optional<CompanyCertificateEntity> findByIdAndCompany_IdAndDeletedFalse(Long id, Long companyId);
}