package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.BusinessEventRequest;
import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping
    public void create() {
        throw new ResponseStatusException(HttpStatus.GONE,
                "Business creation via this API is disabled. Provider profiles are managed through /api/onboarding.");
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id) {
        throw new ResponseStatusException(HttpStatus.GONE,
                "Business updates via this API are disabled.");
    }

    @GetMapping("/{id}")
    public BusinessResponse getById(@PathVariable Long id) {
        return businessService.getBusiness(id);
    }

    @GetMapping
    public PageResponse<BusinessResponse> list(
            @RequestParam Long cityId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recent") String sort
    ) {
        int pageSize = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, pageSize, resolveSort(sort));
        return businessService.listBusinesses(cityId, categoryId, query, pageable);
    }

    private Sort resolveSort(String sortKey) {
        if (sortKey == null) sortKey = "recent";
        return switch (sortKey) {
            case "ratingDesc" -> Sort.by(Sort.Direction.DESC, "avgRating");
            case "ratingAsc"  -> Sort.by(Sort.Direction.ASC, "avgRating");
            case "nameAsc"    -> Sort.by(Sort.Direction.ASC, "name");
            case "nameDesc"   -> Sort.by(Sort.Direction.DESC, "name");
            default           -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    @PostMapping("/{id}/events")
    public void trackEvent(@PathVariable Long id,
                           @Valid @RequestBody BusinessEventRequest request) {
        businessService.recordBusinessEvent(id, request);
    }
}
