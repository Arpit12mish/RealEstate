package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyProjectRepository extends JpaRepository<CompanyProjectEntity, Long> {

  // company/city are eagerly graphed - the public card mapper reads both past .getId(),
  // and this method is called outside any pre-existing session (see ArchitectDesignerPublicServiceImpl,
  // CompanyProjectPublicServiceImpl), so leaving them LAZY here throws LazyInitializationException.
  @EntityGraph(attributePaths = {"company", "city"})
  Page<CompanyProjectEntity> findByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(
      Long companyId, Pageable pageable
  );

  Optional<CompanyProjectEntity> findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(Long id);

  // Public detail read: join fetch (not @EntityGraph) because the Company_PublishedTrue/ActiveTrue/
  // DeletedFalse filter already needs a join on `company` - reusing that join for the fetch avoids a
  // redundant second self-join to `company` that @EntityGraph would add alongside it. Same pattern as
  // searchForDashboard/searchForDashboardByName below.
  @Query("""
      select cp
      from CompanyProjectEntity cp
        join fetch cp.company c
        left join fetch cp.city ci
      where cp.id = :id
        and cp.published = true and cp.active = true and cp.deleted = false
        and c.published = true and c.active = true and c.deleted = false
      """)
  Optional<CompanyProjectEntity> findPublicByIdWithCompanyAndCity(@Param("id") Long id);

  Optional<CompanyProjectEntity> findByIdAndDeletedFalse(Long id);

  Optional<CompanyProjectEntity> findBySlug(String slug);

  Optional<CompanyProjectEntity> findBySlugAndIdNot(String slug, Long id);

  List<CompanyProjectEntity> findByCompany_IdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(
      Collection<Long> companyIds
  );

  @Query("""
      select cp
      from CompanyProjectEntity cp
      where cp.company.id in :companyIds
        and cp.published = true
        and cp.active = true
        and cp.deleted = false
        and not exists (
          select earlier.id
          from CompanyProjectEntity earlier
          where earlier.company.id = cp.company.id
            and earlier.published = true
            and earlier.active = true
            and earlier.deleted = false
            and (
              earlier.priority < cp.priority
              or (earlier.priority = cp.priority and earlier.id > cp.id)
            )
        )
      order by cp.priority asc, cp.id desc
      """)
  List<CompanyProjectEntity> findTopPublicProjectPerCompany(@Param("companyIds") Collection<Long> companyIds);

  @EntityGraph(attributePaths = {"company", "city"})
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
