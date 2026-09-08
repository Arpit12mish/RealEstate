package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyProjectMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyProjectMediaRepository extends JpaRepository<CompanyProjectMediaEntity, Long> {
  List<CompanyProjectMediaEntity> findByCompanyProject_IdAndActiveTrueAndDeletedFalseAndPublicVisibleTrueOrderBySortOrderAscIdAsc(Long companyProjectId);
  List<CompanyProjectMediaEntity> findByCompanyProject_IdAndDeletedFalseOrderBySortOrderAscIdAsc(Long companyProjectId);
  Optional<CompanyProjectMediaEntity> findByIdAndCompanyProject_IdAndDeletedFalse(Long id, Long companyProjectId);
}
