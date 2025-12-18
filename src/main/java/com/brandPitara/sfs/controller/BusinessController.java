package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.BusinessCreateRequest;
import com.brandPitara.sfs.dto.BusinessEventRequest;
import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping
    public BusinessResponse create(@Valid @RequestBody BusinessCreateRequest request) {
        return businessService.createBusiness(request);
    }

    @PutMapping("/{id}")
    public BusinessResponse update(@PathVariable Long id,
                                   @Valid @RequestBody BusinessCreateRequest request) {
        return businessService.updateBusiness(id, request);
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
        int pageSize = Math.min(size, 50); // hard cap

        Pageable pageable = PageRequest.of(page, pageSize, resolveSort(sort));

        return businessService.listBusinesses(cityId, categoryId, query, pageable);
    }

    /**
     * Maps sort key from API to Spring Sort.
     * Allowed values:
     *  - ratingDesc (default for rating)
     *  - ratingAsc
     *  - nameAsc
     *  - nameDesc
     *  - recent (default overall)
     */
    private Sort resolveSort(String sortKey) {
        if (sortKey == null) {
            sortKey = "recent";
        }

        return switch (sortKey) {
            case "ratingDesc" -> Sort.by(Sort.Direction.DESC, "avgRating");
            case "ratingAsc"  -> Sort.by(Sort.Direction.ASC, "avgRating");
            case "nameAsc"    -> Sort.by(Sort.Direction.ASC, "name");
            case "nameDesc"   -> Sort.by(Sort.Direction.DESC, "name");
            case "recent"     -> Sort.by(Sort.Direction.DESC, "createdAt");
            default           -> Sort.by(Sort.Direction.DESC, "createdAt"); // safe fallback
        };
    }

    @PostMapping("/{id}/events")
    public void trackEvent(@PathVariable Long id,
                           @Valid @RequestBody BusinessEventRequest request) {
        businessService.recordBusinessEvent(id, request);
    }

}
