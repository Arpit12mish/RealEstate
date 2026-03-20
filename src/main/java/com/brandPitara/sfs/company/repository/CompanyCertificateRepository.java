package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyCertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyCertificateRepository extends JpaRepository<CompanyCertificateEntity, Long> {
  List<CompanyCertificateEntity> findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(Long companyId);
}