package com.brandPitara.sfs.calculator.interiorcost.service.impl;

import com.brandPitara.sfs.calculator.interiorcost.dto.*;
import com.brandPitara.sfs.calculator.interiorcost.entity.InteriorCostAddonRuleEntity;
import com.brandPitara.sfs.calculator.interiorcost.entity.InteriorCostRuleEntity;
import com.brandPitara.sfs.calculator.interiorcost.enums.*;
import com.brandPitara.sfs.calculator.interiorcost.repository.InteriorCostAddonRuleRepository;
import com.brandPitara.sfs.calculator.interiorcost.repository.InteriorCostRuleRepository;
import com.brandPitara.sfs.calculator.interiorcost.service.InteriorCostService;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InteriorCostServiceImpl implements InteriorCostService {

    private static final BigDecimal SQM_TO_SQFT = new BigDecimal("10.7639");
    private static final BigDecimal SQFT_TO_SQM = new BigDecimal("0.092903");

    private final InteriorCostRuleRepository interiorCostRuleRepository;
    private final InteriorCostAddonRuleRepository interiorCostAddonRuleRepository;
    private final CompanyRepository companyRepository;

    @Override
    public List<String> getCities() {
        return interiorCostRuleRepository.findDistinctActiveCities();
    }

    @Override
    public InteriorCostCompareResponse compare(InteriorCostCompareRequest request) {
        LocalDate asOfDate = request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now();
        String cityName = normalizeText(request.getCityName());

        BigDecimal normalizedArea = normalizeArea(request.getArea(), request.getAreaUnit(), InteriorAreaUnitType.SQFT);

        List<InteriorCostRuleEntity> rules = interiorCostRuleRepository.findApplicableRules(
                cityName,
                request.getPropertyType(),
                request.getPackageType(),
                request.getScopeType(),
                request.getBhkType(),
                normalizedArea,
                asOfDate
        );

        if (rules.isEmpty()) {
            throw new NotFoundException("No interior cost rules found for the given inputs");
        }

        List<InteriorCostCompanyEstimateDto> results = new ArrayList<>();

        for (InteriorCostRuleEntity rule : rules) {
            List<InteriorCostAddonRuleEntity> addonRules =
                    interiorCostAddonRuleRepository.findApplicableRulesForCompany(
                            rule.getCompany().getId(),
                            cityName,
                            rule.getPackageType(),
                            asOfDate
                    );

            Map<InteriorAddonType, BigDecimal> addonPriceMap = addonRules.stream()
                    .collect(Collectors.toMap(
                            InteriorCostAddonRuleEntity::getAddonType,
                            InteriorCostAddonRuleEntity::getUnitPrice,
                            (a, b) -> a
                    ));

            BigDecimal areaInRuleUnit = normalizeArea(request.getArea(), request.getAreaUnit(), rule.getAreaUnit());

            BigDecimal rateBasedAmount = areaInRuleUnit.multiply(rule.getBaseRatePerUnit())
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal baseEstimate = rateBasedAmount.max(rule.getMinimumProjectCost())
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal kitchenAddOn = addonTotal(addonPriceMap.get(InteriorAddonType.KITCHEN), request.getKitchenCount());
            BigDecimal wardrobeAddOn = addonTotal(addonPriceMap.get(InteriorAddonType.WARDROBE), request.getWardrobeCount());
            BigDecimal bathroomAddOn = addonTotal(addonPriceMap.get(InteriorAddonType.BATHROOM), request.getBathroomCount());

            BigDecimal addOnAmount = kitchenAddOn.add(wardrobeAddOn).add(bathroomAddOn)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal subTotal = baseEstimate.add(addOnAmount).setScale(2, RoundingMode.HALF_UP);

            BigDecimal contingencyAmount = percentAmount(subTotal, rule.getContingencyPercent());
            BigDecimal taxableAmount = subTotal.add(contingencyAmount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = percentAmount(taxableAmount, rule.getTaxPercent());

            BigDecimal finalEstimate = taxableAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

            List<String> assumptions = buildAssumptions(rule, request, kitchenAddOn, wardrobeAddOn, bathroomAddOn);

            results.add(
                    InteriorCostCompanyEstimateDto.builder()
                            .companyId(rule.getCompany().getId())
                            .companyName(rule.getCompany().getName())
                            .companyLogoUrl(rule.getCompany().getLogoUrl())
                            .propertyType(request.getPropertyType())
                            .areaUnit(rule.getAreaUnit())
                            .bhkType(request.getBhkType())
                            .packageType(request.getPackageType())
                            .scopeType(request.getScopeType())
                            .baseRatePerUnit(rule.getBaseRatePerUnit())
                            .minimumProjectCost(rule.getMinimumProjectCost())
                            .baseEstimate(baseEstimate)
                            .addOnAmount(addOnAmount)
                            .contingencyAmount(contingencyAmount)
                            .taxAmount(taxAmount)
                            .finalEstimate(finalEstimate)
                            .contingencyPercent(rule.getContingencyPercent())
                            .taxPercent(rule.getTaxPercent())
                            .assumptions(assumptions)
                            .isLowest(false)
                            .build()
            );
        }

        results.sort(Comparator.comparing(InteriorCostCompanyEstimateDto::getFinalEstimate));
        if (!results.isEmpty()) {
            results.get(0).setIsLowest(true);
        }

        return InteriorCostCompareResponse.builder()
                .cityName(cityName)
                .propertyType(request.getPropertyType())
                .area(request.getArea())
                .areaUnit(request.getAreaUnit())
                .bhkType(request.getBhkType())
                .packageType(request.getPackageType())
                .scopeType(request.getScopeType())
                .kitchenCount(safeCount(request.getKitchenCount()))
                .wardrobeCount(safeCount(request.getWardrobeCount()))
                .bathroomCount(safeCount(request.getBathroomCount()))
                .currency("INR")
                .results(results)
                .build();
    }

    @Override
    public InteriorCostRuleResponse createBaseRule(InteriorCostRuleUpsertRequest request) {
        validateAreaRange(request.getMinArea(), request.getMaxArea());
        validateEffectiveDateRange(request.getEffectiveFrom(), request.getEffectiveTo());

        if (Boolean.TRUE.equals(request.getActive())) {
            validateNoOverlappingBaseRule(request);
        }

        CompanyEntity company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new NotFoundException("Company not found with id: " + request.getCompanyId()));

        InteriorCostRuleEntity entity = mapToBaseEntity(new InteriorCostRuleEntity(), company, request);
        InteriorCostRuleEntity saved = interiorCostRuleRepository.save(entity);
        return mapToBaseResponse(saved);
    }

    @Override
    public InteriorCostAddonRuleResponse createAddonRule(InteriorCostAddonRuleUpsertRequest request) {
        validateEffectiveDateRange(request.getEffectiveFrom(), request.getEffectiveTo());

        if (Boolean.TRUE.equals(request.getActive())) {
            validateNoOverlappingAddonRule(request);
        }

        CompanyEntity company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new NotFoundException("Company not found with id: " + request.getCompanyId()));

        InteriorCostAddonRuleEntity entity = mapToAddonEntity(new InteriorCostAddonRuleEntity(), company, request);
        InteriorCostAddonRuleEntity saved = interiorCostAddonRuleRepository.save(entity);
        return mapToAddonResponse(saved);
    }

    @Override
    public List<InteriorCostRuleResponse> getAllBaseRules() {
        return interiorCostRuleRepository.findAllByOrderByCityNameAscIdDesc()
                .stream()
                .map(this::mapToBaseResponse)
                .toList();
    }

    @Override
    public List<InteriorCostAddonRuleResponse> getAllAddonRules() {
        return interiorCostAddonRuleRepository.findAllByOrderByCityNameAscIdDesc()
                .stream()
                .map(this::mapToAddonResponse)
                .toList();
    }

    private void validateNoOverlappingBaseRule(InteriorCostRuleUpsertRequest request) {
        List<InteriorCostRuleEntity> overlaps = interiorCostRuleRepository.findOverlappingActiveRules(
                request.getCompanyId(),
                normalizeText(request.getCityName()),
                request.getPropertyType(),
                request.getPackageType(),
                request.getScopeType(),
                request.getBhkType(),
                request.getEffectiveFrom(),
                request.getEffectiveTo()
        );

        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException("An active overlapping interior cost base rule already exists");
        }
    }

    private void validateNoOverlappingAddonRule(InteriorCostAddonRuleUpsertRequest request) {
        List<InteriorCostAddonRuleEntity> overlaps = interiorCostAddonRuleRepository.findOverlappingActiveRules(
                request.getCompanyId(),
                normalizeText(request.getCityName()),
                request.getPackageType(),
                request.getAddonType(),
                request.getEffectiveFrom(),
                request.getEffectiveTo()
        );

        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException("An active overlapping interior cost addon rule already exists");
        }
    }

    private void validateAreaRange(BigDecimal minArea, BigDecimal maxArea) {
        if (minArea == null || maxArea == null) {
            throw new IllegalArgumentException("Min area and max area are required");
        }
        if (maxArea.compareTo(minArea) < 0) {
            throw new IllegalArgumentException("Max area cannot be less than min area");
        }
    }

    private void validateEffectiveDateRange(LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (effectiveFrom == null) {
            throw new IllegalArgumentException("Effective from date is required");
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Effective to date cannot be before effective from date");
        }
    }

    private BigDecimal normalizeArea(BigDecimal area, InteriorAreaUnitType from, InteriorAreaUnitType to) {
        if (from == to) {
            return area.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal result;
        if (from == InteriorAreaUnitType.SQM && to == InteriorAreaUnitType.SQFT) {
            result = area.multiply(SQM_TO_SQFT);
        } else if (from == InteriorAreaUnitType.SQFT && to == InteriorAreaUnitType.SQM) {
            result = area.multiply(SQFT_TO_SQM);
        } else {
            result = area;
        }

        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal addonTotal(BigDecimal unitPrice, Integer count) {
        BigDecimal safeUnitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        int safeCount = safeCount(count);
        return safeUnitPrice.multiply(BigDecimal.valueOf(safeCount)).setScale(2, RoundingMode.HALF_UP);
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : Math.max(count, 0);
    }

    private BigDecimal percentAmount(BigDecimal base, BigDecimal percent) {
        return base.multiply(percent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private List<String> buildAssumptions(
            InteriorCostRuleEntity rule,
            InteriorCostCompareRequest request,
            BigDecimal kitchenAddOn,
            BigDecimal wardrobeAddOn,
            BigDecimal bathroomAddOn
    ) {
        List<String> assumptions = new ArrayList<>();
        assumptions.add("City: " + normalizeText(request.getCityName()));
        assumptions.add("Package: " + request.getPackageType());
        assumptions.add("Scope: " + request.getScopeType());
        assumptions.add("BHK: " + request.getBhkType());
        assumptions.add("Base rate applied: ₹" + rule.getBaseRatePerUnit().toPlainString() + " per " + rule.getAreaUnit());

        if (safeCount(request.getKitchenCount()) > 0) {
            assumptions.add("Kitchen add-on included: ₹" + kitchenAddOn.toPlainString());
        }
        if (safeCount(request.getWardrobeCount()) > 0) {
            assumptions.add("Wardrobe add-on included: ₹" + wardrobeAddOn.toPlainString());
        }
        if (safeCount(request.getBathroomCount()) > 0) {
            assumptions.add("Bathroom add-on included: ₹" + bathroomAddOn.toPlainString());
        }

        assumptions.add("Contingency applied: " + rule.getContingencyPercent().toPlainString() + "%");
        assumptions.add("Tax applied: " + rule.getTaxPercent().toPlainString() + "%");

        return assumptions;
    }

    private InteriorCostRuleEntity mapToBaseEntity(
            InteriorCostRuleEntity entity,
            CompanyEntity company,
            InteriorCostRuleUpsertRequest request
    ) {
        entity.setCompany(company);
        entity.setCityName(normalizeText(request.getCityName()));
        entity.setPropertyType(request.getPropertyType());
        entity.setAreaUnit(request.getAreaUnit());
        entity.setBhkType(request.getBhkType());
        entity.setPackageType(request.getPackageType());
        entity.setScopeType(request.getScopeType());
        entity.setMinArea(request.getMinArea());
        entity.setMaxArea(request.getMaxArea());
        entity.setBaseRatePerUnit(request.getBaseRatePerUnit());
        entity.setMinimumProjectCost(request.getMinimumProjectCost());
        entity.setContingencyPercent(request.getContingencyPercent());
        entity.setTaxPercent(request.getTaxPercent());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setActive(request.getActive());
        entity.setSourceNote(normalizeText(request.getSourceNote()));
        return entity;
    }

    private InteriorCostAddonRuleEntity mapToAddonEntity(
            InteriorCostAddonRuleEntity entity,
            CompanyEntity company,
            InteriorCostAddonRuleUpsertRequest request
    ) {
        entity.setCompany(company);
        entity.setCityName(normalizeText(request.getCityName()));
        entity.setPackageType(request.getPackageType());
        entity.setAddonType(request.getAddonType());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setActive(request.getActive());
        entity.setSourceNote(normalizeText(request.getSourceNote()));
        return entity;
    }

    private InteriorCostRuleResponse mapToBaseResponse(InteriorCostRuleEntity entity) {
        return InteriorCostRuleResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompany().getId())
                .companyName(entity.getCompany().getName())
                .cityName(entity.getCityName())
                .propertyType(entity.getPropertyType())
                .areaUnit(entity.getAreaUnit())
                .bhkType(entity.getBhkType())
                .packageType(entity.getPackageType())
                .scopeType(entity.getScopeType())
                .minArea(entity.getMinArea())
                .maxArea(entity.getMaxArea())
                .baseRatePerUnit(entity.getBaseRatePerUnit())
                .minimumProjectCost(entity.getMinimumProjectCost())
                .contingencyPercent(entity.getContingencyPercent())
                .taxPercent(entity.getTaxPercent())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .active(entity.getActive())
                .sourceNote(entity.getSourceNote())
                .build();
    }

    private InteriorCostAddonRuleResponse mapToAddonResponse(InteriorCostAddonRuleEntity entity) {
        return InteriorCostAddonRuleResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompany().getId())
                .companyName(entity.getCompany().getName())
                .cityName(entity.getCityName())
                .packageType(entity.getPackageType())
                .addonType(entity.getAddonType())
                .unitPrice(entity.getUnitPrice())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .active(entity.getActive())
                .sourceNote(entity.getSourceNote())
                .build();
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}