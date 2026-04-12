package com.brandPitara.sfs.calculator.interiorcost.controller.publicapi;

import com.brandPitara.sfs.calculator.interiorcost.dto.InteriorCostCompareRequest;
import com.brandPitara.sfs.calculator.interiorcost.dto.InteriorCostCompareResponse;
import com.brandPitara.sfs.calculator.interiorcost.enums.*;
import com.brandPitara.sfs.calculator.interiorcost.service.InteriorCostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
    public List<InteriorPropertyType> getPropertyTypes() {
        return Arrays.asList(InteriorPropertyType.values());
    }

    @GetMapping("/bhk-types")
    public List<InteriorBhkType> getBhkTypes() {
        return Arrays.asList(InteriorBhkType.values());
    }

    @GetMapping("/package-types")
    public List<InteriorPackageType> getPackageTypes() {
        return Arrays.asList(InteriorPackageType.values());
    }

    @GetMapping("/scope-types")
    public List<InteriorScopeType> getScopeTypes() {
        return Arrays.asList(InteriorScopeType.values());
    }

    @PostMapping("/compare")
    public InteriorCostCompareResponse compare(@Valid @RequestBody InteriorCostCompareRequest request) {
        return interiorCostService.compare(request);
    }
}