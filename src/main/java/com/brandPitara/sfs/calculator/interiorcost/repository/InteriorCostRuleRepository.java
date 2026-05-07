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
        select distinct r.propertyType
        from InteriorCostRuleEntity r
        join r.company c
        where r.active = true
          and c.active = true
          and c.published = true
          and c.deleted = false
          and lower(r.cityName) = lower(:cityName)
        order by r.propertyType asc
    """)
    List<InteriorPropertyType> findDistinctPropertyTypesByCity(
            @Param("cityName") String cityName
    );

    @Query("""
        select distinct r.bhkType
        from InteriorCostRuleEntity r
        join r.company c
        where r.active = true
          and c.active = true
          and c.published = true
          and c.deleted = false
          and lower(r.cityName) = lower(:cityName)
          and r.propertyType = :propertyType
        order by r.bhkType asc
    """)
    List<InteriorBhkType> findDistinctBhkTypesByCityAndPropertyType(
            @Param("cityName") String cityName,
            @Param("propertyType") InteriorPropertyType propertyType
    );

    @Query("""
        select distinct r.packageType
        from InteriorCostRuleEntity r
        join r.company c
        where r.active = true
          and c.active = true
          and c.published = true
          and c.deleted = false
          and lower(r.cityName) = lower(:cityName)
          and r.propertyType = :propertyType
          and r.bhkType = :bhkType
        order by r.packageType asc
    """)
    List<InteriorPackageType> findDistinctPackageTypesByCityAndPropertyTypeAndBhkType(
            @Param("cityName") String cityName,
            @Param("propertyType") InteriorPropertyType propertyType,
            @Param("bhkType") InteriorBhkType bhkType
    );

    @Query("""
        select distinct r.scopeType
        from InteriorCostRuleEntity r
        join r.company c
        where r.active = true
          and c.active = true
          and c.published = true
          and c.deleted = false
          and lower(r.cityName) = lower(:cityName)
          and r.propertyType = :propertyType
          and r.bhkType = :bhkType
          and r.packageType = :packageType
        order by r.scopeType asc
    """)
    List<InteriorScopeType> findDistinctScopeTypesByCityAndPropertyTypeAndBhkTypeAndPackageType(
            @Param("cityName") String cityName,
            @Param("propertyType") InteriorPropertyType propertyType,
            @Param("bhkType") InteriorBhkType bhkType,
            @Param("packageType") InteriorPackageType packageType
    );

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
          and r.effectiveFrom <= :asOfDate
          and (r.effectiveTo is null or r.effectiveTo >= :asOfDate)
        order by r.minArea asc, r.maxArea asc, r.id asc
    """)
    List<InteriorCostRuleEntity> findCandidateRulesIgnoringArea(
            @Param("cityName") String cityName,
            @Param("propertyType") InteriorPropertyType propertyType,
            @Param("packageType") InteriorPackageType packageType,
            @Param("scopeType") InteriorScopeType scopeType,
            @Param("bhkType") InteriorBhkType bhkType,
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
      and r.scopeType = :scopeType
      and r.bhkType = :bhkType
      and :normalizedArea between r.minArea and r.maxArea
      and r.effectiveFrom <= :asOfDate
      and (r.effectiveTo is null or r.effectiveTo >= :asOfDate)
    order by r.packageType asc, r.baseRatePerUnit asc, r.id asc
""")
List<InteriorCostRuleEntity> findExactPackageSummaryRules(
        @Param("cityName") String cityName,
        @Param("propertyType") InteriorPropertyType propertyType,
        @Param("scopeType") InteriorScopeType scopeType,
        @Param("bhkType") InteriorBhkType bhkType,
        @Param("normalizedArea") BigDecimal normalizedArea,
        @Param("asOfDate") LocalDate asOfDate
);

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
      and r.scopeType = :scopeType
      and r.bhkType = :bhkType
      and r.effectiveFrom <= :asOfDate
      and (r.effectiveTo is null or r.effectiveTo >= :asOfDate)
    order by r.packageType asc, r.minArea asc, r.maxArea asc, r.id asc
""")
List<InteriorCostRuleEntity> findCandidatePackageSummaryRulesIgnoringArea(
        @Param("cityName") String cityName,
        @Param("propertyType") InteriorPropertyType propertyType,
        @Param("scopeType") InteriorScopeType scopeType,
        @Param("bhkType") InteriorBhkType bhkType,
        @Param("asOfDate") LocalDate asOfDate
);
}