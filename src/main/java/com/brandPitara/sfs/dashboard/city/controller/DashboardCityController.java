package com.brandPitara.sfs.dashboard.city.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.city.dto.DashboardCityUpsertRequest;
import com.brandPitara.sfs.dashboard.city.service.DashboardCityService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dto.CityResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/cities")
@RequiredArgsConstructor
public class DashboardCityController {

    private final DashboardCityService cityService;
    private final DashboardActionAuditService auditService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CityResponse create(@Valid @RequestBody DashboardCityUpsertRequest request) {
        CityResponse response = cityService.create(request);
        auditService.record(DashboardAuditAction.CITY_CREATED, ReviewEntityType.CITY, response.getId(), null);
        return response;
    }

    @PutMapping("/{cityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CityResponse update(
            @PathVariable Long cityId,
            @Valid @RequestBody DashboardCityUpsertRequest request
    ) {
        CityResponse response = cityService.update(cityId, request);
        auditService.record(DashboardAuditAction.CITY_UPDATED, ReviewEntityType.CITY, cityId, null);
        return response;
    }

    @DeleteMapping("/{cityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long cityId) {
        cityService.delete(cityId);
        auditService.record(DashboardAuditAction.CITY_DELETED, ReviewEntityType.CITY, cityId, null);
    }

    @GetMapping("/{cityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public CityResponse get(@PathVariable Long cityId) {
        return cityService.get(cityId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<CityResponse> list(@RequestParam(required = false) String query) {
        return cityService.list(query);
    }
}
