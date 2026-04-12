package com.brandPitara.sfs.calculator.stampduty.service.impl;

import com.brandPitara.sfs.calculator.stampduty.dto.*;
import com.brandPitara.sfs.calculator.stampduty.entity.StampDutyRuleEntity;
import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyBuyerType;
import com.brandPitara.sfs.calculator.stampduty.enums.StampDutyPropertyCategory;
import com.brandPitara.sfs.calculator.stampduty.repository.StampDutyRuleRepository;
import com.brandPitara.sfs.calculator.stampduty.service.StampDutyService;
import com.brandPitara.sfs.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StampDutyServiceImpl implements StampDutyService {

    private final StampDutyRuleRepository stampDutyRuleRepository;

    @Override
    public List<StampDutyCityResponse> getCities() {
        return stampDutyRuleRepository.findDistinctActiveStateAndCity()
                .stream()
                .map(row -> StampDutyCityResponse.builder()
                        .stateName((String) row[0])
                        .cityName((String) row[1])
                        .build())
                .toList();
    }

    @Override
    public List<StampDutyBuyerTypeResponse> getBuyerTypes(String stateName, String cityName) {
        return stampDutyRuleRepository.findDistinctBuyerTypes(
                        normalizeText(stateName),
                        normalizeText(cityName)
                )
                .stream()
                .map(buyerType -> StampDutyBuyerTypeResponse.builder()
                        .buyerType(buyerType)
                        .label(toBuyerTypeLabel(buyerType))
                        .build())
                .toList();
    }

    @Override
    public List<StampDutyPropertyCategoryResponse> getPropertyCategories(String stateName, String cityName) {
        return stampDutyRuleRepository.findDistinctPropertyCategories(
                        normalizeText(stateName),
                        normalizeText(cityName)
                )
                .stream()
                .map(propertyCategory -> StampDutyPropertyCategoryResponse.builder()
                        .propertyCategory(propertyCategory)
                        .label(toPropertyCategoryLabel(propertyCategory))
                        .build())
                .toList();
    }

    @Override
    public StampDutyCalculateResponse calculate(StampDutyCalculateRequest request) {
        LocalDate asOfDate = request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now();

        String stateName = normalizeText(request.getStateName());
        String cityName = normalizeText(request.getCityName());

        List<StampDutyRuleEntity> rules = stampDutyRuleRepository.findApplicableRules(
                stateName,
                cityName,
                request.getBuyerType(),
                request.getPropertyCategory(),
                asOfDate
        );

        StampDutyRuleEntity rule = rules.stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No stamp duty rule found for the given inputs"));

        BigDecimal propertyValue = request.getPropertyValue().setScale(2, RoundingMode.HALF_UP);

        BigDecimal stampDutyAmount = percentageAmount(propertyValue, rule.getStampDutyPercent());
        BigDecimal registrationAmount = percentageAmount(propertyValue, rule.getRegistrationPercent());
        BigDecimal localBodyTaxAmount = percentageAmount(propertyValue, rule.getLocalBodyTaxPercent());
        BigDecimal totalCharges = stampDutyAmount.add(registrationAmount).add(localBodyTaxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return StampDutyCalculateResponse.builder()
                .stateName(stateName)
                .cityName(cityName)
                .buyerType(request.getBuyerType())
                .propertyCategory(request.getPropertyCategory())
                .propertyValue(propertyValue)
                .stampDutyPercent(rule.getStampDutyPercent())
                .registrationPercent(rule.getRegistrationPercent())
                .localBodyTaxPercent(rule.getLocalBodyTaxPercent())
                .stampDutyAmount(stampDutyAmount)
                .registrationAmount(registrationAmount)
                .localBodyTaxAmount(localBodyTaxAmount)
                .totalCharges(totalCharges)
                .currency("INR")
                .displayMessage("Estimated total stamp duty and registration charges are ₹" + totalCharges.toPlainString())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .build();
    }

    @Override
    public StampDutyRuleResponse createRule(StampDutyUpsertRequest request) {
        validateEffectiveDateRange(request.getEffectiveFrom(), request.getEffectiveTo());
        validatePercentages(request);

        String stateName = normalizeText(request.getStateName());
        String cityName = normalizeText(request.getCityName());

        if (Boolean.TRUE.equals(request.getActive())) {
            validateNoOverlappingActiveRule(
                    stateName,
                    cityName,
                    request.getBuyerType(),
                    request.getPropertyCategory(),
                    request.getEffectiveFrom(),
                    request.getEffectiveTo()
            );
        }

        StampDutyRuleEntity entity = mapToEntity(new StampDutyRuleEntity(), request);
        StampDutyRuleEntity saved = stampDutyRuleRepository.save(entity);
        return mapToResponse(saved);
    }

    @Override
    public StampDutyRuleResponse updateRule(Long id, StampDutyUpsertRequest request) {
        validateEffectiveDateRange(request.getEffectiveFrom(), request.getEffectiveTo());
        validatePercentages(request);

        StampDutyRuleEntity entity = stampDutyRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stamp duty rule not found with id: " + id));

        String stateName = normalizeText(request.getStateName());
        String cityName = normalizeText(request.getCityName());

        if (Boolean.TRUE.equals(request.getActive())) {
            validateNoOverlappingActiveRuleExcludingId(
                    id,
                    stateName,
                    cityName,
                    request.getBuyerType(),
                    request.getPropertyCategory(),
                    request.getEffectiveFrom(),
                    request.getEffectiveTo()
            );
        }

        entity = mapToEntity(entity, request);
        StampDutyRuleEntity saved = stampDutyRuleRepository.save(entity);
        return mapToResponse(saved);
    }

    @Override
    public List<StampDutyRuleResponse> getAllRules() {
        return stampDutyRuleRepository.findAllByOrderByCityNameAscBuyerTypeAscPropertyCategoryAscEffectiveFromDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private BigDecimal percentageAmount(BigDecimal base, BigDecimal percent) {
        return base.multiply(percent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private void validatePercentages(StampDutyUpsertRequest request) {
        if (request.getStampDutyPercent().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Stamp duty percent cannot be greater than 100");
        }
        if (request.getRegistrationPercent().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Registration percent cannot be greater than 100");
        }
        if (request.getLocalBodyTaxPercent().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Local body tax percent cannot be greater than 100");
        }
    }

    private void validateNoOverlappingActiveRule(
            String stateName,
            String cityName,
            StampDutyBuyerType buyerType,
            StampDutyPropertyCategory propertyCategory,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        List<StampDutyRuleEntity> overlaps = stampDutyRuleRepository.findOverlappingActiveRules(
                stateName,
                cityName,
                buyerType,
                propertyCategory,
                effectiveFrom,
                effectiveTo
        );

        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException(buildOverlapMessage(
                    stateName, cityName, buyerType, propertyCategory
            ));
        }
    }

    private void validateNoOverlappingActiveRuleExcludingId(
            Long excludeId,
            String stateName,
            String cityName,
            StampDutyBuyerType buyerType,
            StampDutyPropertyCategory propertyCategory,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        List<StampDutyRuleEntity> overlaps = stampDutyRuleRepository.findOverlappingActiveRulesExcludingId(
                excludeId,
                stateName,
                cityName,
                buyerType,
                propertyCategory,
                effectiveFrom,
                effectiveTo
        );

        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException(buildOverlapMessage(
                    stateName, cityName, buyerType, propertyCategory
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
            StampDutyBuyerType buyerType,
            StampDutyPropertyCategory propertyCategory
    ) {
        return "An active overlapping stamp duty rule already exists for "
                + stateName + " / " + cityName + " / " + buyerType + " / " + propertyCategory;
    }

    private StampDutyRuleEntity mapToEntity(StampDutyRuleEntity entity, StampDutyUpsertRequest request) {
        entity.setStateName(normalizeText(request.getStateName()));
        entity.setCityName(normalizeText(request.getCityName()));
        entity.setBuyerType(request.getBuyerType());
        entity.setPropertyCategory(request.getPropertyCategory());
        entity.setStampDutyPercent(request.getStampDutyPercent());
        entity.setRegistrationPercent(request.getRegistrationPercent());
        entity.setLocalBodyTaxPercent(request.getLocalBodyTaxPercent());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());
        entity.setActive(request.getActive());
        entity.setSourceNote(normalizeText(request.getSourceNote()));
        return entity;
    }

    private StampDutyRuleResponse mapToResponse(StampDutyRuleEntity entity) {
        return StampDutyRuleResponse.builder()
                .id(entity.getId())
                .stateName(entity.getStateName())
                .cityName(entity.getCityName())
                .buyerType(entity.getBuyerType())
                .propertyCategory(entity.getPropertyCategory())
                .stampDutyPercent(entity.getStampDutyPercent())
                .registrationPercent(entity.getRegistrationPercent())
                .localBodyTaxPercent(entity.getLocalBodyTaxPercent())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .active(entity.getActive())
                .sourceNote(entity.getSourceNote())
                .build();
    }

    private String toBuyerTypeLabel(StampDutyBuyerType buyerType) {
        return switch (buyerType) {
            case MALE -> "Male";
            case FEMALE -> "Female";
            case JOINT -> "Joint";
        };
    }

    private String toPropertyCategoryLabel(StampDutyPropertyCategory propertyCategory) {
        return switch (propertyCategory) {
            case RESIDENTIAL -> "Residential";
            case COMMERCIAL -> "Commercial";
        };
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}