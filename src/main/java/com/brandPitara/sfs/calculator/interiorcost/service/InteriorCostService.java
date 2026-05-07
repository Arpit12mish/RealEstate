package com.brandPitara.sfs.calculator.interiorcost.service;

import com.brandPitara.sfs.calculator.interiorcost.dto.*;
import com.brandPitara.sfs.calculator.interiorcost.enums.*;

import java.util.List;

public interface InteriorCostService {

    List<String> getCities();

    List<InteriorPropertyType> getPropertyTypes(String cityName);

    List<InteriorBhkType> getBhkTypes(String cityName, InteriorPropertyType propertyType);

    List<InteriorPackageType> getPackageTypes(String cityName, InteriorPropertyType propertyType, InteriorBhkType bhkType);

    List<InteriorScopeType> getScopeTypes(
            String cityName,
            InteriorPropertyType propertyType,
            InteriorBhkType bhkType,
            InteriorPackageType packageType
    );

    InteriorCostCompareResponse compare(InteriorCostCompareRequest request);

    InteriorCostRuleResponse createBaseRule(InteriorCostRuleUpsertRequest request);

    InteriorCostAddonRuleResponse createAddonRule(InteriorCostAddonRuleUpsertRequest request);

    List<InteriorCostRuleResponse> getAllBaseRules();

    List<InteriorCostAddonRuleResponse> getAllAddonRules();

    InteriorPackageSummaryResponse getPackageSummary(
            String cityName,
            InteriorPropertyType propertyType,
            InteriorBhkType bhkType,
            InteriorScopeType scopeType,
            java.math.BigDecimal area,
            InteriorAreaUnitType areaUnit
    );

    InteriorCostCompareResponse compareCustom(InteriorCostCustomCompareRequest request);
}

