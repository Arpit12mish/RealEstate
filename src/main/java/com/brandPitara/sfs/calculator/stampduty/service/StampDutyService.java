package com.brandPitara.sfs.calculator.stampduty.service;

import com.brandPitara.sfs.calculator.stampduty.dto.*;

import java.util.List;

public interface StampDutyService {

    List<StampDutyCityResponse> getCities();

    List<StampDutyBuyerTypeResponse> getBuyerTypes(String stateName, String cityName);

    List<StampDutyPropertyCategoryResponse> getPropertyCategories(String stateName, String cityName);

    StampDutyCalculateResponse calculate(StampDutyCalculateRequest request);

    StampDutyRuleResponse createRule(StampDutyUpsertRequest request);

    StampDutyRuleResponse updateRule(Long id, StampDutyUpsertRequest request);

    List<StampDutyRuleResponse> getAllRules();
}