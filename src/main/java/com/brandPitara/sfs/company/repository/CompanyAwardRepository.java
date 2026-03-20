package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyAwardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyAwardRepository extends JpaRepository<CompanyAwardEntity, Long> {
  List<CompanyAwardEntity> findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(Long companyId);
}