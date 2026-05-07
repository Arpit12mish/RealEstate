package com.brandPitara.sfs.location.controller;

import com.brandPitara.sfs.location.dto.LocationResolveRequest;
import com.brandPitara.sfs.location.dto.LocationResolveResponse;
import com.brandPitara.sfs.location.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/resolve")
    public LocationResolveResponse resolve(@Valid @RequestBody LocationResolveRequest request) {
        return locationService.resolve(request);
    }
}