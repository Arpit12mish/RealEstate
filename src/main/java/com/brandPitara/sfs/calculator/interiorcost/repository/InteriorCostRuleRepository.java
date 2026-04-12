package com.brandPitara.sfs.calculator.interiorcost.repository;

import com.brandPitara.sfs.calculator.interiorcost.entity.InteriorCostRuleEntity;
import com.brandPitara.sfs.calculator.interiorcost.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InteriorCostRuleRepository extends JpaRepository<InteriorCostRuleEntity, Long> {

    @Query("""
        select distinct r.cityName
        from InteriorCostRuleEntity r
        join r.company c
        where r.active = true
          and c.active = true
          and c.published = true
          and c.deleted = false
        order by r.cityName asc
    """)
    List<String> findDistinctActiveCities();

    @Query("""
        select r
        from InteriorCostRuleEntity r
        join fetch r.company c
        where r.active = true
          and c.active = true
          and c.published = true
          and c.deleted = false
          and lower(r.cityName) = lower(:cityName)
          and r.propertyType = :propertyType
          and r.packageType = :packageType
          and r.scopeType = :scopeType
          and r.bhkType = :bhkType
          and :normalizedArea between r.minArea and r.maxArea
          and r.effectiveFrom <= :asOfDate
          and (r.effectiveTo is null or r.effectiveTo >= :asOfDate)
        order by r.baseRatePerUnit asc, r.id asc
    """)
    List<InteriorCostRuleEntity> findApplicableRules(
            @Param("cityName") String cityName,
            @Param("propertyType") InteriorPropertyType propertyType,
            @Param("packageType") InteriorPackageType packageType,
            @Param("scopeType") InteriorScopeType scopeType,
            @Param("bhkType") InteriorBhkType bhkType,
            @Param("normalizedArea") BigDecimal normalizedArea,
            @Param("asOfDate") LocalDate asOfDate
    );

    List<InteriorCostRuleEntity> findAllByOrderByCityNameAscIdDesc();

    @Query("""
        select r
        from InteriorCostRuleEntity r
        where r.active = true
          and r.company.id = :companyId
          and lower(r.cityName) = lower(:cityName)
          and r.propertyType = :propertyType
          and r.packageType = :packageType
          and r.scopeType = :scopeType
          and r.bhkType = :bhkType
          and (
                (:effectiveTo is null and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
             or (:effectiveTo is not null and r.effectiveFrom <= :effectiveTo and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
          )
    """)
    List<InteriorCostRuleEntity> findOverlappingActiveRules(
            @Param("companyId") Long companyId,
            @Param("cityName") String cityName,
            @Param("propertyType") InteriorPropertyType propertyType,
            @Param("packageType") InteriorPackageType packageType,
            @Param("scopeType") InteriorScopeType scopeType,
            @Param("bhkType") InteriorBhkType bhkType,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo
    );
}