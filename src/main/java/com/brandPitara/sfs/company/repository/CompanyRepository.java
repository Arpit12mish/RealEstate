package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {

  List<CompanyEntity> findByIdInAndActiveTrueAndPublishedTrueAndDeletedFalse(List<Long> ids);

  Optional<CompanyEntity> findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(Long id);

  Page<CompanyEntity> findByActiveTrueAndPublishedTrueAndDeletedFalse(Pageable pageable);

  Page<CompanyEntity> findByCompanyTypeAndActiveTrueAndPublishedTrueAndDeletedFalse(String companyType, Pageable pageable);

  List<CompanyEntity> findByIdInAndActiveTrueAndPublishedTrueAndDeletedFalse(Collection<Long> ids);

}




