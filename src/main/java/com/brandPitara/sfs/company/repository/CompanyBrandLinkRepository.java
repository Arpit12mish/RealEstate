package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyBrandLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyBrandLinkRepository extends JpaRepository<CompanyBrandLinkEntity, Long> {

  List<CompanyBrandLinkEntity> findByCompany_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long companyId);
}