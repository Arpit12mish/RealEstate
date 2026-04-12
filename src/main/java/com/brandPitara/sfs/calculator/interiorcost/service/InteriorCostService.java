package com.brandPitara.sfs.calculator.interiorcost.service;

import com.brandPitara.sfs.calculator.interiorcost.dto.*;

import java.util.List;

public interface InteriorCostService {

    List<String> getCities();

    InteriorCostCompareResponse compare(InteriorCostCompareRequest request);

    InteriorCostRuleResponse createBaseRule(InteriorCostRuleUpsertRequest request);

    InteriorCostAddonRuleResponse createAddonRule(InteriorCostAddonRuleUpsertRequest request);

    List<InteriorCostRuleResponse> getAllBaseRules();

    List<InteriorCostAddonRuleResponse> getAllAddonRules();
}