package com.brandPitara.sfs.calculator.stampduty.controller.publicapi;

import com.brandPitara.sfs.calculator.stampduty.dto.*;
import com.brandPitara.sfs.calculator.stampduty.service.StampDutyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/stamp-duty")
@RequiredArgsConstructor
public class StampDutyPublicController {

    private final StampDutyService stampDutyService;

    @GetMapping("/cities")
    public List<StampDutyCityResponse> getCities() {
        return stampDutyService.getCities();
    }

    @GetMapping("/buyer-types")
    public List<StampDutyBuyerTypeResponse> getBuyerTypes(
            @RequestParam String stateName,
            @RequestParam String cityName
    ) {
        return stampDutyService.getBuyerTypes(stateName, cityName);
    }

    @GetMapping("/property-categories")
    public List<StampDutyPropertyCategoryResponse> getPropertyCategories(
            @RequestParam String stateName,
            @RequestParam String cityName
    ) {
        return stampDutyService.getPropertyCategories(stateName, cityName);
    }

    @PostMapping("/calculate")
    public StampDutyCalculateResponse calculate(@Valid @RequestBody StampDutyCalculateRequest request) {
        return stampDutyService.calculate(request);
    }
}