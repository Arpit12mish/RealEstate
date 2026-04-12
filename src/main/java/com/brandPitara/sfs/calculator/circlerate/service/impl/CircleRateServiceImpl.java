package com.brandPitara.sfs.calculator.circlerate.service.impl;

import com.brandPitara.sfs.calculator.circlerate.dto.*;
import com.brandPitara.sfs.calculator.circlerate.entity.CircleRateRuleEntity;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRatePropertyType;
import com.brandPitara.sfs.calculator.circlerate.repository.CircleRateRuleRepository;
import com.brandPitara.sfs.calculator.circlerate.service.CircleRateService;
import com.brandPitara.sfs.calculator.circlerate.strategy.CircleRateFormulaStrategy;
import com.brandPitara.sfs.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CircleRateServiceImpl implements CircleRateService {

    private final CircleRateRuleRepository circleRateRuleRepository;
    private final List<CircleRateFormulaStrategy> formulaStrategies;

    @Override
    public List<CircleRateCityResponse> getCities() {
        return circleRateRuleRepository.findDistinctActiveStateAndCity()
                .stream()
                .map(row -> CircleRateCityResponse.builder()
                        .stateName((String) row[0])
                        .cityName((String) row[1])
                        .build())
                .toList();
    }

    @Override
    public List<CircleRateLocalityResponse> getLocalities(String stateName, String cityName) {
        return circleRateRuleRepository.findDistinctLocalities(
                        normalizeText(stateName),
                        normalizeText(cityName)
                )
                .stream()
                .map(locality -> CircleRateLocalityResponse.builder()
                        .localityName(locality)
                        .build())
                .toList();
    }

    @Override
    public List<CircleRatePropertyTypeResponse> getPropertyTypes(
            String stateName,
            String cityName,
            String localityName
    ) {
        return circleRateRuleRepository.findDistinctPropertyTypes(
                        normalizeText(stateName),
                        normalizeText(cityName),
                        normalizeText(localityName)
                )
                .stream()
                .map(propertyType -> CircleRatePropertyTypeResponse.builder()
                        .propertyType(propertyType)
                        .label(toDisplayLabel(propertyType))
                        .build())
                .toList();
    }

    @Override
    public CircleRateCalculateResponse calculate(CircleRateCalculateRequest request) {
        LocalDate asOfDate = request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now();

        String stateName = normalizeText(request.getStateName());
        String cityName = normalizeText(request.getCityName());
        String localityName = normalizeText(request.getLocalityName());

        List<CircleRateRuleEntity> rules = circleRateRuleRepository.findApplicableRules(
                stateName,
                cityName,
                localityName,
                request.getPropertyType(),
                asOfDate
        );

        CircleRateRuleEntity rule = rules.stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No circle rate rule found for the given inputs"));

        Map<?, CircleRateFormulaStrategy> strategyMap = formulaStrategies.stream()
                .collect(Collectors.toMap(CircleRateFormulaStrategy::getSupportedFormulaType, s -> s));

        CircleRateFormulaStrategy strategy = strategyMap.get(rule.getFormulaType());
        if (strategy == null) {
            throw new IllegalStateException("No strategy found for formula type: " + rule.getFormulaType());
        }

        request.setStateName(stateName);
        request.setCityName(cityName);
        request.setLocalityName(localityName);

        return strategy.calculate(rule, request);
    }

    @Override
    public CircleRateRuleResponse createRule(CircleRateUpsertRequest request) {
        validateEffectiveDateRange(request.getEffectiveFrom(), request.getEffectiveTo());

        String stateName = normalizeText(request.getStateName());
        String cityName = normalizeText(request.getCityName());
        String localityName = normalizeText(request.getLocalityName());

        if (Boolean.TRUE.equals(request.getActive())) {
            validateNoOverlappingActiveRule(
                    stateName,
                    cityName,
                    localityName,
                    request.getPropertyType(),
                    request.getEffectiveFrom(),
                    request.getEffectiveTo()
            );
        }

        CircleRateRuleEntity entity = mapToEntity(new CircleRateRuleEntity(), request);
        CircleRateRuleEntity saved = circleRateRuleRepository.save(entity);
        return mapToResponse(saved);
    }

    @Override
    public CircleRateRuleResponse updateRule(Long id, CircleRateUpsertRequest request) {
        validateEffectiveDateRange(request.getEffectiveFrom(), request.getEffectiveTo());

        CircleRateRuleEntity entity = circleRateRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Circle rate rule not found with id: " + id));

        String stateName = normalizeText(request.getStateName());
        String cityName = normalizeText(request.getCityName());
        String localityName = normalizeText(request.getLocalityName());

        if (Boolean.TRUE.equals(request.getActive())) {
            validateNoOverlappingActiveRuleExcludingId(
                    id,
                    stateName,
                    cityName,
                    localityName,
                    request.getPropertyType(),
                    request.getEffectiveFrom(),
                    request.getEffectiveTo()
            );
        }

        entity = mapToEntity(entity, request);
        CircleRateRuleEntity saved = circleRateRuleRepository.save(entity);
        return mapToResponse(saved);
    }

    @Override
    public List<CircleRateRuleResponse> getAllRules() {
        return circleRateRuleRepository.findAllByOrderByCityNameAscLocalityNameAscPropertyTypeAscEffectiveFromDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateNoOverlappingActiveRule(
            String stateName,
            String cityName,
            String localityName,
            CircleRatePropertyType propertyType,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        List<CircleRateRuleEntity> overlaps = circleRateRuleRepository.findOverlappingActiveRules(
                stateName,
                cityName,
                localityName,
                propertyType,
                effectiveFrom,
                effectiveTo
        );

        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException(buildOverlapMessage(
                    stateName, cityName, localityName, propertyType
            ));
        }
    }

    private void validateNoOverlappingActiveRuleExcludingId(
            Long excludeId,
            String stateName,
            String cityName,
            String localityName,
            CircleRatePropertyType propertyType,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        List<CircleRateRuleEntity> overlaps = circleRateRuleRepository.findOverlappingActiveRulesExcludingId(
                excludeId,
                stateName,
                cityName,
                localityName,
                propertyType,
                effectiveFrom,
                effectiveTo
        );

        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException(buildOverlapMessage(
                    stateName, cityName, localityName, propertyType
            ));
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

    private String buildOverlapMessage(
            String stateName,
            String cityName,
            String localityName,
            CircleRatePropertyType propertyType
    ) {
        return "An active overlapping circle rate rule already exists for "
                + stateName + " / " + cityName + " / " + localityName + " / " + propertyType;
    }

    private CircleRateRuleEntity mapToEntity(CircleRateRuleEntity entity, CircleRateUpsertRequest request) {
        entity.setStateName(normalizeText(request.getStateName()));
        entity.setCityName(normalizeText(request.getCityName()));
        entity.setLocalityName(normalizeText(request.getLocalityName()));
        entity.setPropertyType(request.getPropertyType());
        entity.setUnitType(request.getUnitType());
        entity.setFormulaType(request.getFormulaType());
        entity.setRatePerUnit(request.getRatePerUnit());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setActive(request.getActive());
        entity.setSourceNote(normalizeText(request.getSourceNote()));
        return entity;
    }

    private CircleRateRuleResponse mapToResponse(CircleRateRuleEntity entity) {
        return CircleRateRuleResponse.builder()
                .id(entity.getId())
                .stateName(entity.getStateName())
                .cityName(entity.getCityName())
                .localityName(entity.getLocalityName())
                .propertyType(entity.getPropertyType())
                .unitType(entity.getUnitType())
                .formulaType(entity.getFormulaType())
                .ratePerUnit(entity.getRatePerUnit())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .active(entity.getActive())
                .sourceNote(entity.getSourceNote())
                .build();
    }

    private String toDisplayLabel(CircleRatePropertyType propertyType) {
        return switch (propertyType) {
            case PLOT -> "Plot";
            case APARTMENT -> "Apartment";
            case BUILDER_FLOOR -> "Builder Floor";
            case HOUSE -> "House";
            case COMMERCIAL -> "Commercial";
            case SHOP -> "Shop";
            case OFFICE -> "Office";
        };
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}