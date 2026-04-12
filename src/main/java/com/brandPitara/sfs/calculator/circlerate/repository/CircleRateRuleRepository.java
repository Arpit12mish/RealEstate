package com.brandPitara.sfs.calculator.circlerate.repository;

import com.brandPitara.sfs.calculator.circlerate.entity.CircleRateRuleEntity;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRatePropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CircleRateRuleRepository extends JpaRepository<CircleRateRuleEntity, Long> {

    @Query("""
        select distinct r.stateName, r.cityName
        from CircleRateRuleEntity r
        where r.active = true
        order by r.stateName asc, r.cityName asc
    """)
    List<Object[]> findDistinctActiveStateAndCity();

    @Query("""
        select distinct r.localityName
        from CircleRateRuleEntity r
        where r.active = true
          and lower(r.stateName) = lower(:stateName)
          and lower(r.cityName) = lower(:cityName)
        order by r.localityName asc
    """)
    List<String> findDistinctLocalities(
            @Param("stateName") String stateName,
            @Param("cityName") String cityName
    );

    @Query("""
        select distinct r.propertyType
        from CircleRateRuleEntity r
        where r.active = true
          and lower(r.stateName) = lower(:stateName)
          and lower(r.cityName) = lower(:cityName)
          and lower(r.localityName) = lower(:localityName)
        order by r.propertyType asc
    """)
    List<CircleRatePropertyType> findDistinctPropertyTypes(
            @Param("stateName") String stateName,
            @Param("cityName") String cityName,
            @Param("localityName") String localityName
    );

    @Query("""
        select r
        from CircleRateRuleEntity r
        where r.active = true
          and lower(r.stateName) = lower(:stateName)
          and lower(r.cityName) = lower(:cityName)
          and lower(r.localityName) = lower(:localityName)
          and r.propertyType = :propertyType
          and r.effectiveFrom <= :asOfDate
          and (r.effectiveTo is null or r.effectiveTo >= :asOfDate)
        order by r.effectiveFrom desc, r.id desc
    """)
    List<CircleRateRuleEntity> findApplicableRules(
            @Param("stateName") String stateName,
            @Param("cityName") String cityName,
            @Param("localityName") String localityName,
            @Param("propertyType") CircleRatePropertyType propertyType,
            @Param("asOfDate") LocalDate asOfDate
    );

    Optional<CircleRateRuleEntity> findByIdAndActiveTrue(Long id);

    List<CircleRateRuleEntity> findAllByOrderByCityNameAscLocalityNameAscPropertyTypeAscEffectiveFromDesc();

    @Query("""
        select r
        from CircleRateRuleEntity r
        where r.active = true
          and lower(r.stateName) = lower(:stateName)
          and lower(r.cityName) = lower(:cityName)
          and lower(r.localityName) = lower(:localityName)
          and r.propertyType = :propertyType
          and (
                (:effectiveTo is null and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
             or (:effectiveTo is not null and r.effectiveFrom <= :effectiveTo and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
          )
        order by r.effectiveFrom desc, r.id desc
    """)
    List<CircleRateRuleEntity> findOverlappingActiveRules(
            @Param("stateName") String stateName,
            @Param("cityName") String cityName,
            @Param("localityName") String localityName,
            @Param("propertyType") CircleRatePropertyType propertyType,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo
    );

    @Query("""
        select r
        from CircleRateRuleEntity r
        where r.active = true
          and r.id <> :excludeId
          and lower(r.stateName) = lower(:stateName)
          and lower(r.cityName) = lower(:cityName)
          and lower(r.localityName) = lower(:localityName)
          and r.propertyType = :propertyType
          and (
                (:effectiveTo is null and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
             or (:effectiveTo is not null and r.effectiveFrom <= :effectiveTo and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
          )
        order by r.effectiveFrom desc, r.id desc
    """)
    List<CircleRateRuleEntity> findOverlappingActiveRulesExcludingId(
            @Param("excludeId") Long excludeId,
            @Param("stateName") String stateName,
            @Param("cityName") String cityName,
            @Param("localityName") String localityName,
            @Param("propertyType") CircleRatePropertyType propertyType,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo
    );
}