package com.brandPitara.sfs.dbsearch.infra;

import com.brandPitara.sfs.company.entity.CompanyEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompanySearchRepository extends Repository<CompanyEntity, Long> {

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
          c.priority desc,
          c.updatedAt desc
        """)
    List<CompanyEntity> searchCompanies(
            @Param("query") String query,
            Pageable pageable
    );
}