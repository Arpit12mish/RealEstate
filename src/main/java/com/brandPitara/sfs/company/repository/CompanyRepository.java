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

}




