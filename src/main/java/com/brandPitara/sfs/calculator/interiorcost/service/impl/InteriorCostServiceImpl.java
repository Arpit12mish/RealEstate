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
    public List<InteriorPropertyType> getPropertyTypes(String cityName) {
        return interiorCostRuleRepository.findDistinctPropertyTypesByCity(normalizeText(cityName));
    }

    @Override
    public List<InteriorBhkType> getBhkTypes(String cityName, InteriorPropertyType propertyType) {
        return interiorCostRuleRepository.findDistinctBhkTypesByCityAndPropertyType(
                normalizeText(cityName),
                propertyType
        );
    }

    @Override
    public List<InteriorPackageType> getPackageTypes(
            String cityName,
            InteriorPropertyType propertyType,
            InteriorBhkType bhkType
    ) {
        return interiorCostRuleRepository.findDistinctPackageTypesByCityAndPropertyTypeAndBhkType(
                normalizeText(cityName),
                propertyType,
                bhkType
        );
    }

    @Override
    public List<InteriorScopeType> getScopeTypes(
            String cityName,
            InteriorPropertyType propertyType,
            InteriorBhkType bhkType,
            InteriorPackageType packageType
    ) {
        return interiorCostRuleRepository.findDistinctScopeTypesByCityAndPropertyTypeAndBhkTypeAndPackageType(
                normalizeText(cityName),
                propertyType,
                bhkType,
                packageType
        );
    }

    @Override
    public InteriorCostCompareResponse compare(InteriorCostCompareRequest request) {
        LocalDate asOfDate = request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now();
        String cityName = normalizeText(request.getCityName());

        BigDecimal normalizedArea = normalizeArea(
                request.getArea(),
                request.getAreaUnit(),
                InteriorAreaUnitType.SQFT
        );

        List<InteriorCostRuleEntity> exactRules = interiorCostRuleRepository.findApplicableRules(
                cityName,
                request.getPropertyType(),
                request.getPackageType(),
                request.getScopeType(),
                request.getBhkType(),
                normalizedArea,
                asOfDate
        );

        Map<Long, InteriorCostRuleEntity> selectedRuleByCompany = new LinkedHashMap<>();
        Map<Long, String> matchTypeByCompany = new HashMap<>();

        if (!exactRules.isEmpty()) {
            for (InteriorCostRuleEntity rule : exactRules) {
                selectedRuleByCompany.put(rule.getCompany().getId(), rule);
                matchTypeByCompany.put(rule.getCompany().getId(), "EXACT");
            }
        } else {
            List<InteriorCostRuleEntity> candidates = interiorCostRuleRepository.findCandidateRulesIgnoringArea(
                    cityName,
                    request.getPropertyType(),
                    request.getPackageType(),
                    request.getScopeType(),
                    request.getBhkType(),
                    asOfDate
            );

            if (candidates.isEmpty()) {
                throw new NotFoundException("No interior cost rules found for the given inputs");
            }

            Map<Long, List<InteriorCostRuleEntity>> byCompany = candidates.stream()
                    .collect(Collectors.groupingBy(rule -> rule.getCompany().getId(), LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<Long, List<InteriorCostRuleEntity>> entry : byCompany.entrySet()) {
                InteriorCostRuleEntity best = chooseNearestRule(entry.getValue(), normalizedArea);
                selectedRuleByCompany.put(entry.getKey(), best);
                matchTypeByCompany.put(entry.getKey(), "APPROXIMATE");
            }
        }

        List<InteriorCostCompanyEstimateDto> results = new ArrayList<>();

        for (InteriorCostRuleEntity rule : selectedRuleByCompany.values()) {
            String matchType = matchTypeByCompany.get(rule.getCompany().getId());

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

            List<String> assumptions = buildAssumptions(rule, request, kitchenAddOn, wardrobeAddOn, bathroomAddOn, matchType);

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
                            .matchedMinArea(rule.getMinArea())
                            .matchedMaxArea(rule.getMaxArea())
                            .matchType(matchType)
                            .approximate("APPROXIMATE".equals(matchType))
                            .assumptions(assumptions)
                            .isLowest(false)
                            .build()
            );
        }

        results.sort(Comparator.comparing(InteriorCostCompanyEstimateDto::getFinalEstimate));
        if (!results.isEmpty()) {
            results.get(0).setIsLowest(true);
        }

        boolean hasApproximate = results.stream().anyMatch(item -> Boolean.TRUE.equals(item.getApproximate()));

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
                .message(hasApproximate
                        ? "Some estimates are approximate based on the nearest configured pricing band."
                        : "Exact estimates found for the selected combination.")
                .results(results)
                .build();
    }

    private InteriorCostRuleEntity chooseNearestRule(List<InteriorCostRuleEntity> rules, BigDecimal inputArea) {
        return rules.stream()
                .min(Comparator
                        .comparing((InteriorCostRuleEntity r) -> areaDistance(inputArea, r.getMinArea(), r.getMaxArea()))
                        .thenComparing(r -> bandWidth(r.getMinArea(), r.getMaxArea()))
                        .thenComparing(InteriorCostRuleEntity::getEffectiveFrom, Comparator.reverseOrder())
                        .thenComparing(InteriorCostRuleEntity::getId))
                .orElseThrow(() -> new NotFoundException("No nearest rule found"));
    }

    private BigDecimal areaDistance(BigDecimal inputArea, BigDecimal minArea, BigDecimal maxArea) {
        if (inputArea.compareTo(minArea) < 0) {
            return minArea.subtract(inputArea);
        }
        if (inputArea.compareTo(maxArea) > 0) {
            return inputArea.subtract(maxArea);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal bandWidth(BigDecimal minArea, BigDecimal maxArea) {
        return maxArea.subtract(minArea).abs();
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
            BigDecimal bathroomAddOn,
            String matchType
    ) {
        List<String> assumptions = new ArrayList<>();
        assumptions.add("City: " + normalizeText(request.getCityName()));
        assumptions.add("Package: " + request.getPackageType());
        assumptions.add("Scope: " + request.getScopeType());
        assumptions.add("BHK: " + request.getBhkType());
        assumptions.add("Pricing band used: " + rule.getMinArea().toPlainString() + " - " + rule.getMaxArea().toPlainString() + " " + rule.getAreaUnit());
        assumptions.add("Match type: " + matchType);
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

    @Override
public InteriorPackageSummaryResponse getPackageSummary(
        String cityName,
        InteriorPropertyType propertyType,
        InteriorBhkType bhkType,
        InteriorScopeType scopeType,
        BigDecimal area,
        InteriorAreaUnitType areaUnit
) {
    String normalizedCity = normalizeText(cityName);
    LocalDate asOfDate = LocalDate.now();

    BigDecimal normalizedArea = normalizeArea(area, areaUnit, InteriorAreaUnitType.SQFT);

    List<InteriorCostRuleEntity> exactRules = interiorCostRuleRepository.findExactPackageSummaryRules(
            normalizedCity,
            propertyType,
            scopeType,
            bhkType,
            normalizedArea,
            asOfDate
    );

    Map<InteriorPackageType, List<InteriorCostRuleEntity>> rulesByPackage = new LinkedHashMap<>();
    Map<InteriorPackageType, String> matchTypeByPackage = new HashMap<>();

    if (!exactRules.isEmpty()) {
        rulesByPackage = exactRules.stream()
                .collect(Collectors.groupingBy(
                        InteriorCostRuleEntity::getPackageType,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (InteriorPackageType packageType : rulesByPackage.keySet()) {
            matchTypeByPackage.put(packageType, "EXACT");
        }
    } else {
        List<InteriorCostRuleEntity> candidates = interiorCostRuleRepository.findCandidatePackageSummaryRulesIgnoringArea(
                normalizedCity,
                propertyType,
                scopeType,
                bhkType,
                asOfDate
        );

        if (candidates.isEmpty()) {
            throw new NotFoundException("No package pricing summary found for the given inputs");
        }

        Map<InteriorPackageType, List<InteriorCostRuleEntity>> candidateByPackage = candidates.stream()
                .collect(Collectors.groupingBy(
                        InteriorCostRuleEntity::getPackageType,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (Map.Entry<InteriorPackageType, List<InteriorCostRuleEntity>> entry : candidateByPackage.entrySet()) {
            InteriorCostRuleEntity nearest = chooseNearestRule(entry.getValue(), normalizedArea);
            rulesByPackage.put(entry.getKey(), List.of(nearest));
            matchTypeByPackage.put(entry.getKey(), "APPROXIMATE");
        }
    }

    List<InteriorPackageSummaryItemDto> packageItems = new ArrayList<>();

    for (Map.Entry<InteriorPackageType, List<InteriorCostRuleEntity>> entry : rulesByPackage.entrySet()) {
        InteriorPackageType packageType = entry.getKey();
        List<InteriorCostRuleEntity> packageRules = entry.getValue();

        BigDecimal minRate = packageRules.stream()
                .map(InteriorCostRuleEntity::getBaseRatePerUnit)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxRate = packageRules.stream()
                .map(InteriorCostRuleEntity::getBaseRatePerUnit)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        InteriorCostRuleEntity sampleRule = packageRules.get(0);
        String matchType = matchTypeByPackage.get(packageType);

        packageItems.add(
                InteriorPackageSummaryItemDto.builder()
                        .packageType(packageType)
                        .minRatePerUnit(minRate.setScale(2, RoundingMode.HALF_UP))
                        .maxRatePerUnit(maxRate.setScale(2, RoundingMode.HALF_UP))
                        .areaUnit(sampleRule.getAreaUnit().name())
                        .currency("INR")
                        .matchType(matchType)
                        .matchedMinArea(sampleRule.getMinArea())
                        .matchedMaxArea(sampleRule.getMaxArea())
                        .displayLabel(buildPackageDisplayLabel(packageType, minRate, maxRate))
                        .build()
        );
    }

    packageItems.sort(Comparator.comparing(InteriorPackageSummaryItemDto::getPackageType));

    boolean hasApproximate = packageItems.stream()
            .anyMatch(item -> "APPROXIMATE".equals(item.getMatchType()));

    return InteriorPackageSummaryResponse.builder()
            .cityName(normalizedCity)
            .propertyType(propertyType)
            .bhkType(bhkType)
            .scopeType(scopeType)
            .area(area)
            .areaUnit(areaUnit)
            .currency("INR")
            .message(hasApproximate
                    ? "Some package summaries are based on the nearest configured pricing band."
                    : "Exact package pricing summary found for the selected combination.")
            .packages(packageItems)
            .build();
}

private String buildPackageDisplayLabel(
        InteriorPackageType packageType,
        BigDecimal minRate,
        BigDecimal maxRate
) {
    String label = switch (packageType) {
        case BASIC -> "Basic";
        case STANDARD -> "Standard";
        case PREMIUM -> "Premium";
        case LUXURY -> "Luxury";
    };

    return label + " — ₹" + minRate.setScale(0, RoundingMode.HALF_UP).toPlainString()
            + " to ₹" + maxRate.setScale(0, RoundingMode.HALF_UP).toPlainString()
            + " / sq.ft";
}

@Override
public InteriorCostCompareResponse compareCustom(InteriorCostCustomCompareRequest request) {
    LocalDate asOfDate = request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now();
    String cityName = normalizeText(request.getCityName());

    BigDecimal normalizedArea = normalizeArea(
            request.getArea(),
            request.getAreaUnit(),
            InteriorAreaUnitType.SQFT
    );

    List<InteriorCostRuleEntity> exactRules = interiorCostRuleRepository.findApplicableRules(
            cityName,
            request.getPropertyType(),
            request.getPackageType(),
            request.getScopeType(),
            request.getBhkType(),
            normalizedArea,
            asOfDate
    );

    Map<Long, InteriorCostRuleEntity> selectedRuleByCompany = new LinkedHashMap<>();
    Map<Long, String> matchTypeByCompany = new HashMap<>();

    if (!exactRules.isEmpty()) {
        for (InteriorCostRuleEntity rule : exactRules) {
            selectedRuleByCompany.put(rule.getCompany().getId(), rule);
            matchTypeByCompany.put(rule.getCompany().getId(), "EXACT");
        }
    } else {
        List<InteriorCostRuleEntity> candidates = interiorCostRuleRepository.findCandidateRulesIgnoringArea(
                cityName,
                request.getPropertyType(),
                request.getPackageType(),
                request.getScopeType(),
                request.getBhkType(),
                asOfDate
        );

        if (candidates.isEmpty()) {
            throw new NotFoundException("No interior cost rules found for the given custom inputs");
        }

        Map<Long, List<InteriorCostRuleEntity>> byCompany = candidates.stream()
                .collect(Collectors.groupingBy(
                        rule -> rule.getCompany().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (Map.Entry<Long, List<InteriorCostRuleEntity>> entry : byCompany.entrySet()) {
            InteriorCostRuleEntity best = chooseNearestRule(entry.getValue(), normalizedArea);
            selectedRuleByCompany.put(entry.getKey(), best);
            matchTypeByCompany.put(entry.getKey(), "APPROXIMATE");
        }
    }

    List<InteriorCostCompanyEstimateDto> results = new ArrayList<>();

    for (InteriorCostRuleEntity rule : selectedRuleByCompany.values()) {
        String matchType = matchTypeByCompany.get(rule.getCompany().getId());

        BigDecimal areaInRuleUnit = normalizeArea(
                request.getArea(),
                request.getAreaUnit(),
                rule.getAreaUnit()
        );

        BigDecimal rateBasedAmount = areaInRuleUnit.multiply(request.getCustomRatePerUnit())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal baseEstimate = rateBasedAmount.max(rule.getMinimumProjectCost())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal kitchenAddOn = addonTotal(request.getCustomKitchenCost(), request.getKitchenCount());
        BigDecimal wardrobeAddOn = addonTotal(request.getCustomWardrobeCost(), request.getWardrobeCount());
        BigDecimal bathroomAddOn = addonTotal(request.getCustomBathroomCost(), request.getBathroomCount());

        BigDecimal addOnAmount = kitchenAddOn.add(wardrobeAddOn).add(bathroomAddOn)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal subTotal = baseEstimate.add(addOnAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal contingencyAmount = percentAmount(subTotal, request.getCustomContingencyPercent());
        BigDecimal taxableAmount = subTotal.add(contingencyAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = percentAmount(taxableAmount, request.getCustomTaxPercent());

        BigDecimal finalEstimate = taxableAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        List<String> assumptions = buildCustomAssumptions(
                rule,
                request,
                kitchenAddOn,
                wardrobeAddOn,
                bathroomAddOn,
                matchType
        );

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
                        .baseRatePerUnit(request.getCustomRatePerUnit())
                        .minimumProjectCost(rule.getMinimumProjectCost())
                        .baseEstimate(baseEstimate)
                        .addOnAmount(addOnAmount)
                        .contingencyAmount(contingencyAmount)
                        .taxAmount(taxAmount)
                        .finalEstimate(finalEstimate)
                        .contingencyPercent(request.getCustomContingencyPercent())
                        .taxPercent(request.getCustomTaxPercent())
                        .matchedMinArea(rule.getMinArea())
                        .matchedMaxArea(rule.getMaxArea())
                        .matchType(matchType)
                        .approximate("APPROXIMATE".equals(matchType))
                        .assumptions(assumptions)
                        .isLowest(false)
                        .build()
        );
    }

    results.sort(Comparator.comparing(InteriorCostCompanyEstimateDto::getFinalEstimate));
    if (!results.isEmpty()) {
        results.get(0).setIsLowest(true);
    }

    boolean hasApproximate = results.stream().anyMatch(item -> Boolean.TRUE.equals(item.getApproximate()));

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
            .message(hasApproximate
                    ? "Custom comparison generated. Some estimates are approximate based on the nearest configured pricing band."
                    : "Custom comparison generated successfully.")
            .results(results)
            .build();
}

private List<String> buildCustomAssumptions(
        InteriorCostRuleEntity rule,
        InteriorCostCustomCompareRequest request,
        BigDecimal kitchenAddOn,
        BigDecimal wardrobeAddOn,
        BigDecimal bathroomAddOn,
        String matchType
) {
    List<String> assumptions = new ArrayList<>();
    assumptions.add("City: " + normalizeText(request.getCityName()));
    assumptions.add("Package base selected: " + request.getPackageType());
    assumptions.add("Scope: " + request.getScopeType());
    assumptions.add("BHK: " + request.getBhkType());
    assumptions.add("Pricing band used: " + rule.getMinArea().toPlainString() + " - " + rule.getMaxArea().toPlainString() + " " + rule.getAreaUnit());
    assumptions.add("Match type: " + matchType);
    assumptions.add("Custom base rate applied: ₹" + request.getCustomRatePerUnit().toPlainString() + " per " + rule.getAreaUnit());
    assumptions.add("Custom kitchen add-on: ₹" + request.getCustomKitchenCost().toPlainString());
    assumptions.add("Custom wardrobe add-on: ₹" + request.getCustomWardrobeCost().toPlainString());
    assumptions.add("Custom bathroom add-on: ₹" + request.getCustomBathroomCost().toPlainString());
    assumptions.add("Custom contingency applied: " + request.getCustomContingencyPercent().toPlainString() + "%");
    assumptions.add("Custom tax applied: " + request.getCustomTaxPercent().toPlainString() + "%");
    return assumptions;
}
}