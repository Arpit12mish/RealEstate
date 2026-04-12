package com.brandPitara.sfs.calculator.circlerate.controller.publicapi;

import com.brandPitara.sfs.calculator.circlerate.dto.*;
import com.brandPitara.sfs.calculator.circlerate.service.CircleRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/circle-rates")
@RequiredArgsConstructor
public class CircleRatePublicController {

    private final CircleRateService circleRateService;

    @GetMapping("/cities")
    public List<CircleRateCityResponse> getCities() {
        return circleRateService.getCities();
    }

    @GetMapping("/localities")
    public List<CircleRateLocalityResponse> getLocalities(
            @RequestParam String stateName,
            @RequestParam String cityName
    ) {
        return circleRateService.getLocalities(stateName, cityName);
    }

    @GetMapping("/property-types")
    public List<CircleRatePropertyTypeResponse> getPropertyTypes(
            @RequestParam String stateName,
            @RequestParam String cityName,
            @RequestParam String localityName
    ) {
        return circleRateService.getPropertyTypes(stateName, cityName, localityName);
    }

    @PostMapping("/calculate")
    public CircleRateCalculateResponse calculate(@Valid @RequestBody CircleRateCalculateRequest request) {
        return circleRateService.calculate(request);
    }
}