package com.brandPitara.sfs.calculator.interiorcost.controller.publicapi;

import com.brandPitara.sfs.calculator.interiorcost.dto.InteriorCostCompareRequest;
import com.brandPitara.sfs.calculator.interiorcost.dto.InteriorCostCompareResponse;
import com.brandPitara.sfs.calculator.interiorcost.dto.InteriorCostCustomCompareRequest;
import com.brandPitara.sfs.calculator.interiorcost.dto.InteriorPackageSummaryResponse;
import com.brandPitara.sfs.calculator.interiorcost.enums.*;
import com.brandPitara.sfs.calculator.interiorcost.service.InteriorCostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/interior-cost")
@RequiredArgsConstructor
public class InteriorCostPublicController {

    private final InteriorCostService interiorCostService;

    @GetMapping("/cities")
    public List<String> getCities() {
        return interiorCostService.getCities();
    }

    @GetMapping("/property-types")
    public List<InteriorPropertyType> getPropertyTypes(
            @RequestParam String cityName
    ) {
        return interiorCostService.getPropertyTypes(cityName);
    }

    @GetMapping("/bhk-types")
    public List<InteriorBhkType> getBhkTypes(
            @RequestParam String cityName,
            @RequestParam InteriorPropertyType propertyType
    ) {
        return interiorCostService.getBhkTypes(cityName, propertyType);
    }

    @GetMapping("/package-types")
    public List<InteriorPackageType> getPackageTypes(
            @RequestParam String cityName,
            @RequestParam InteriorPropertyType propertyType,
            @RequestParam InteriorBhkType bhkType
    ) {
        return interiorCostService.getPackageTypes(cityName, propertyType, bhkType);
    }

    @GetMapping("/scope-types")
    public List<InteriorScopeType> getScopeTypes(
            @RequestParam String cityName,
            @RequestParam InteriorPropertyType propertyType,
            @RequestParam InteriorBhkType bhkType,
            @RequestParam InteriorPackageType packageType
    ) {
        return interiorCostService.getScopeTypes(cityName, propertyType, bhkType, packageType);
    }

    @PostMapping("/compare")
    public InteriorCostCompareResponse compare(@Valid @RequestBody InteriorCostCompareRequest request) {
        return interiorCostService.compare(request);
    }

    @GetMapping("/package-summary")
    public InteriorPackageSummaryResponse getPackageSummary(
            @RequestParam String cityName,
            @RequestParam InteriorPropertyType propertyType,
            @RequestParam InteriorBhkType bhkType,
            @RequestParam InteriorScopeType scopeType,
            @RequestParam java.math.BigDecimal area,
            @RequestParam InteriorAreaUnitType areaUnit
    ) {
        return interiorCostService.getPackageSummary(
                cityName,
                propertyType,
                bhkType,
                scopeType,
                area,
                areaUnit
        );
    }

    @PostMapping("/compare-custom")
    public InteriorCostCompareResponse compareCustom(
            @Valid @RequestBody InteriorCostCustomCompareRequest request
    ) {
        return interiorCostService.compareCustom(request);
    }
}