package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyStatRepository extends JpaRepository<CompanyStatEntity, Long> {
  List<CompanyStatEntity> findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(Long companyId);
}