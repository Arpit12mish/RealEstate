package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyProjectRepository extends JpaRepository<CompanyProjectEntity, Long> {

  Page<CompanyProjectEntity> findByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(
      Long companyId, Pageable pageable
  );

  Optional<CompanyProjectEntity> findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(Long id);

  Optional<CompanyProjectEntity> findByIdAndPublishedTrueAndActiveTrueAndDeletedFalseAndCompany_PublishedTrueAndCompany_ActiveTrueAndCompany_DeletedFalse(Long id);

  Optional<CompanyProjectEntity> findByIdAndDeletedFalse(Long id);

  List<CompanyProjectEntity> findByCompany_IdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(
      Collection<Long> companyIds
  );

  List<CompanyProjectEntity> findTop10ByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(
      Long companyId
  );

  // ---------- dashboard admin read paths (Company Project list/detail) ----------

  // Never pass a null/blank q into lower(:q)-based query - Postgres can't infer a type for
  // a null bound only inside lower(...)/concat(...) (see BrandPublicServiceImpl). The service
  // layer picks this variant only when q has text, and searchForDashboard otherwise.
  @Query(value = """
      select cp
      from CompanyProjectEntity cp
        join fetch cp.company c
        left join fetch cp.city ci
      where cp.deleted = false
        and (:companyId is null or c.id = :companyId)
        and (:companyType is null or c.companyType = :companyType)
        and (:cityId is null or ci.id = :cityId)
        and (:active is null or cp.active = :active)
        and lower(cp.name) like lower(concat('%', :q, '%'))
      order by cp.id desc
      """,
      countQuery = """
      select count(cp)
      from CompanyProjectEntity cp
        join cp.company c
      where cp.deleted = false
        and (:companyId is null or c.id = :companyId)
        and (:companyType is null or c.companyType = :companyType)
        and (:cityId is null or cp.city.id = :cityId)
        and (:active is null or cp.active = :active)
        and lower(cp.name) like lower(concat('%', :q, '%'))
      """)
  Page<CompanyProjectEntity> searchForDashboardByName(
      @Param("q") String q,
      @Param("companyId") Long companyId,
      @Param("companyType") String companyType,
      @Param("cityId") Long cityId,
      @Param("active") Boolean active,
      Pageable pageable
  );

  @Query(value = """
      select cp
      from CompanyProjectEntity cp
        join fetch cp.company c
        left join fetch cp.city ci
      where cp.deleted = false
        and (:companyId is null or c.id = :companyId)
        and (:companyType is null or c.companyType = :companyType)
        and (:cityId is null or ci.id = :cityId)
        and (:active is null or cp.active = :active)
      order by cp.id desc
      """,
      countQuery = """
      select count(cp)
      from CompanyProjectEntity cp
        join cp.company c
      where cp.deleted = false
        and (:companyId is null or c.id = :companyId)
        and (:companyType is null or c.companyType = :companyType)
        and (:cityId is null or cp.city.id = :cityId)
        and (:active is null or cp.active = :active)
      """)
  Page<CompanyProjectEntity> searchForDashboard(
      @Param("companyId") Long companyId,
      @Param("companyType") String companyType,
      @Param("cityId") Long cityId,
      @Param("active") Boolean active,
      Pageable pageable
  );
}
