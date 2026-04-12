package com.brandPitara.sfs.calculator.interiorcost.repository;

import com.brandPitara.sfs.calculator.interiorcost.entity.InteriorCostAddonRuleEntity;
import com.brandPitara.sfs.calculator.interiorcost.enums.InteriorAddonType;
import com.brandPitara.sfs.calculator.interiorcost.enums.InteriorPackageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InteriorCostAddonRuleRepository extends JpaRepository<InteriorCostAddonRuleEntity, Long> {

    @Query("""
        select r
        from InteriorCostAddonRuleEntity r
        where r.active = true
          and r.company.id = :companyId
          and lower(r.cityName) = lower(:cityName)
          and r.packageType = :packageType
          and r.effectiveFrom <= :asOfDate
          and (r.effectiveTo is null or r.effectiveTo >= :asOfDate)
    """)
    List<InteriorCostAddonRuleEntity> findApplicableRulesForCompany(
            @Param("companyId") Long companyId,
            @Param("cityName") String cityName,
            @Param("packageType") InteriorPackageType packageType,
            @Param("asOfDate") LocalDate asOfDate
    );

    List<InteriorCostAddonRuleEntity> findAllByOrderByCityNameAscIdDesc();

    @Query("""
        select r
        from InteriorCostAddonRuleEntity r
        where r.active = true
          and r.company.id = :companyId
          and lower(r.cityName) = lower(:cityName)
          and r.packageType = :packageType
          and r.addonType = :addonType
          and (
                (:effectiveTo is null and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
             or (:effectiveTo is not null and r.effectiveFrom <= :effectiveTo and (r.effectiveTo is null or r.effectiveTo >= :effectiveFrom))
          )
    """)
    List<InteriorCostAddonRuleEntity> findOverlappingActiveRules(
            @Param("companyId") Long companyId,
            @Param("cityName") String cityName,
            @Param("packageType") InteriorPackageType packageType,
            @Param("addonType") InteriorAddonType addonType,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo
    );
}