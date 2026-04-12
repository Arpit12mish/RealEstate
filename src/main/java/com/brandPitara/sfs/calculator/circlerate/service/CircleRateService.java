package com.brandPitara.sfs.calculator.circlerate.service;

import com.brandPitara.sfs.calculator.circlerate.dto.*;

import java.util.List;

public interface CircleRateService {

    List<CircleRateCityResponse> getCities();

    List<CircleRateLocalityResponse> getLocalities(String stateName, String cityName);

    List<CircleRatePropertyTypeResponse> getPropertyTypes(String stateName, String cityName, String localityName);

    CircleRateCalculateResponse calculate(CircleRateCalculateRequest request);

    CircleRateRuleResponse createRule(CircleRateUpsertRequest request);

    CircleRateRuleResponse updateRule(Long id, CircleRateUpsertRequest request);

    List<CircleRateRuleResponse> getAllRules();
}