package com.brandPitara.sfs.calculator.stampduty.repository;

import com.brandPitara.sfs.calculator.stampduty.entity.StampDutyRuleEntity;
import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyBuyerType;
import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyPropertyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StampDutyRuleRepository extends JpaRepository<StampDutyRuleEntity, Long> {

    @Query("""
        select distinct r.stateName, r.cityName
        from StampDutyRuleEntity r
        where r.active = true
        order by r.stateName asc, r.cityName asc
    """)
    List<Object[]> findDistinctActiveStateAndCity();

    @Query("""
        select distinct r.buyerType
        from StampDutyRuleEntity r
        where r.active = true
          and lower(r.stateName) = lower(:stateName)
          and lower(r.cityName) = lower(:cityName)
        order by r.buyerType asc
    """)
    List<StampDutyBuyerType> findDistinctBuyerTypes(
            @Param("stateName") String stateName,
            @Param("cityName") String cityName
    );

    @Query("""
        select distinct r.propertyCategory
        from StampDutyRuleEntity r
        where r.active = true
          and lower(r.stateName) = lower(:stateName)
          and lower(r.cityName) = lower(:cityName)
        order by r.propertyCategory asc
    """)
    List<StampDutyPropertyCategory> findDistinctPropertyCategories(
            @Param("stateName") String stateName,
            @Param("cityName") String cityName
    );

    @Query("""
        select r
        from StampDutyRuleEntity r
        where r.active = true
          and lower(r.stateName) = lower(:stateName)
          and lower(r.cityName) = lower(:cityName)
          and r.buyerType = :buyerType
          and r.propertyCategory = :propertyCategory
          and r.effectiveFrom <= :asOfDate
          and (r.effectiveTo is null or r.effectiveTo >= :asOfDate)
        order by r.effectiveFrom desc, r.id desc
    """)
    List<StampDutyRuleEntity> findApplicableRules(
            @Param("stateName") String stateName,
            @Param("cityName") String cityName,
            @Param("buyerType") StampDutyBuyerType buyerType,
            @Param("propertyCategory") StampDutyPropertyCategory propertyCategory,
            @Param("asOfDate") LocalDate asOfDate
    );

    List<StampDutyRuleEntity> findAllByOrderByCityNameAscBuyerTypeAscPropertyCategoryAscEffectiveFromDesc();

    @Query("""
        select r
        from StampDutyRuleEntity r
        where r.active = true
          and lower(r.stateName) = lower(:stateName)
          and lower(r.cityName) = lower(:cityName)
          and r.buyerType = :buyerType
          and r.propertyCategory = :propertyCategory
          and (
                (:effectiveTo is null and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
             or (:effectiveTo is not null and r.effectiveFrom <= :effectiveTo and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
          )
        order by r.effectiveFrom desc, r.id desc
    """)
    List<StampDutyRuleEntity> findOverlappingActiveRules(
            @Param("stateName") String stateName,
            @Param("cityName") String cityName,
            @Param("buyerType") StampDutyBuyerType buyerType,
            @Param("propertyCategory") StampDutyPropertyCategory propertyCategory,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo
    );

    @Query("""
        select r
        from StampDutyRuleEntity r
        where r.active = true
          and r.id <> :excludeId
          and lower(r.stateName) = lower(:stateName)
          and lower(r.cityName) = lower(:cityName)
          and r.buyerType = :buyerType
          and r.propertyCategory = :propertyCategory
          and (
                (:effectiveTo is null and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
             or (:effectiveTo is not null and r.effectiveFrom <= :effectiveTo and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
          )
        order by r.effectiveFrom desc, r.id desc
    """)
    List<StampDutyRuleEntity> findOverlappingActiveRulesExcludingId(
            @Param("excludeId") Long excludeId,
            @Param("stateName") String stateName,
            @Param("cityName") String cityName,
            @Param("buyerType") StampDutyBuyerType buyerType,
            @Param("propertyCategory") StampDutyPropertyCategory propertyCategory,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo
    );
}