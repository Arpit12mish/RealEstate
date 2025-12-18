package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.search.BusinessSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class BusinessSearchController {

    private final BusinessSearchService searchService;

    @GetMapping("/businesses")
    public List<BusinessResponse> searchBusinesses(
            @RequestParam Long cityId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return searchService.search(cityId, categoryId, q, lat, lon, page, size);
    }
}
