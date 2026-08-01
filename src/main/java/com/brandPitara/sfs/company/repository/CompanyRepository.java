package com.brandPitara.sfs.company.repository;

import com.brandPitara.sfs.company.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {

  Optional<CompanyEntity> findByIdAndDeletedFalse(Long id);

  List<CompanyEntity> findByIdInAndActiveTrueAndPublishedTrueAndDeletedFalse(List<Long> ids);

  Optional<CompanyEntity> findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(Long id);

  Optional<CompanyEntity> findBySlugAndActiveTrueAndPublishedTrueAndDeletedFalse(String slug);

  Page<CompanyEntity> findByActiveTrueAndPublishedTrueAndDeletedFalse(Pageable pageable);

  Page<CompanyEntity> findByCompanyTypeAndActiveTrueAndPublishedTrueAndDeletedFalse(String companyType, Pageable pageable);

  List<CompanyEntity> findByIdInAndActiveTrueAndPublishedTrueAndDeletedFalse(Collection<Long> ids);

  Page<CompanyEntity> findByCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
      Collection<String> companyTypes,
      Pageable pageable
  );


  @Query("""
      select c
      from CompanyEntity c
      where c.active = true
        and c.published = true
        and c.deleted = false
        and (
              lower(c.name) like lower(concat(:query, '%'))
           or lower(c.name) like lower(concat('%', :query, '%'))
           or lower(c.companyType) like lower(concat(:query, '%'))
           or lower(c.companyType) like lower(concat('%', :query, '%'))
        )
      order by
        case
          when lower(c.name) = lower(:query) then 1
          when lower(c.name) like lower(concat(:query, '%')) then 2
          when lower(c.companyType) = lower(:query) then 3
          when lower(c.companyType) like lower(concat(:query, '%')) then 4
          when lower(c.name) like lower(concat('%', :query, '%')) then 5
          else 6
        end,
        c.priority asc,
        c.id desc
      """)
  List<CompanyEntity> searchForSuggestions(
      @Param("query") String query,
      Pageable pageable
  );

  @Query("""
      select c
      from CompanyEntity c
      where c.active = true
        and c.published = true
        and c.deleted = false
        and (
              lower(c.name) like lower(concat(:query, '%'))
           or lower(c.name) like lower(concat('%', :query, '%'))
           or lower(c.companyType) like lower(concat(:query, '%'))
           or lower(c.companyType) like lower(concat('%', :query, '%'))
        )
      order by
        case
          when lower(c.name) = lower(:query) then 1
          when lower(c.name) like lower(concat(:query, '%')) then 2
          when lower(c.companyType) = lower(:query) then 3
          when lower(c.companyType) like lower(concat(:query, '%')) then 4
          when lower(c.name) like lower(concat('%', :query, '%')) then 5
          else 6
        end,
        c.priority asc,
        c.id desc
      """)
  Page<CompanyEntity> searchForResults(
      @Param("query") String query,
      Pageable pageable
  );

  // ---------- dashboard admin read/write paths (Company Profile CMS) ----------

  Optional<CompanyEntity> findBySlug(String slug);

  Optional<CompanyEntity> findBySlugAndIdNot(String slug, Long id);

  // Never pass a null/blank q into lower(:q)-based query - Postgres can't infer a type for
  // a null bound only inside lower(...)/concat(...) (see BrandPublicServiceImpl). The service
  // layer picks this variant only when q has text, and searchForDashboard otherwise.
  @Query(value = """
      select c
      from CompanyEntity c
        left join fetch c.city ci
      where c.deleted = false
        and (:companyType is null or c.companyType = :companyType)
        and (:cityId is null or ci.id = :cityId)
        and (:active is null or c.active = :active)
        and (:published is null or c.published = :published)
        and lower(c.name) like lower(concat('%', :q, '%'))
      order by c.priority asc, c.id desc
      """,
      countQuery = """
      select count(c)
      from CompanyEntity c
      where c.deleted = false
        and (:companyType is null or c.companyType = :companyType)
        and (:cityId is null or c.city.id = :cityId)
        and (:active is null or c.active = :active)
        and (:published is null or c.published = :published)
        and lower(c.name) like lower(concat('%', :q, '%'))
      """)
  Page<CompanyEntity> searchForDashboardByName(
      @Param("q") String q,
      @Param("companyType") String companyType,
      @Param("cityId") Long cityId,
      @Param("active") Boolean active,
      @Param("published") Boolean published,
      Pageable pageable
  );

  @Query(value = """
      select c
      from CompanyEntity c
        left join fetch c.city ci
      where c.deleted = false
        and (:companyType is null or c.companyType = :companyType)
        and (:cityId is null or ci.id = :cityId)
        and (:active is null or c.active = :active)
        and (:published is null or c.published = :published)
      order by c.priority asc, c.id desc
      """,
      countQuery = """
      select count(c)
      from CompanyEntity c
      where c.deleted = false
        and (:companyType is null or c.companyType = :companyType)
        and (:cityId is null or c.city.id = :cityId)
        and (:active is null or c.active = :active)
        and (:published is null or c.published = :published)
      """)
  Page<CompanyEntity> searchForDashboard(
      @Param("companyType") String companyType,
      @Param("cityId") Long cityId,
      @Param("active") Boolean active,
      @Param("published") Boolean published,
      Pageable pageable
  );
}




