package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyProjectRepository extends JpaRepository<CompanyProjectEntity, Long> {

  Page<CompanyProjectEntity> findByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(
      Long companyId, Pageable pageable
  );

  Optional<CompanyProjectEntity> findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(Long id);
}