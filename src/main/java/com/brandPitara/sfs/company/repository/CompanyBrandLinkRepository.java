package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyBrandLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanyBrandLinkRepository extends JpaRepository<CompanyBrandLinkEntity, Long> {

  List<CompanyBrandLinkEntity> findByCompany_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long companyId);

  @Query("""
      select link
      from CompanyBrandLinkEntity link
        join fetch link.brand brand
      where link.company.id = :companyId
        and link.active = true
        and link.deleted = false
        and brand.published = true
        and brand.active = true
        and brand.deleted = false
      order by link.sortOrder asc, link.id asc
      """)
  List<CompanyBrandLinkEntity> findPublicFallbackByCompanyId(@Param("companyId") Long companyId);
}
