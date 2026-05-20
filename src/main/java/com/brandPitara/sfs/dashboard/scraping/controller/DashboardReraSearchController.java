package com.brandPitara.sfs.dashboard.scraping.controller;

import com.brandPitara.sfs.dashboard.scraping.dto.ReraNumberSearchRequest;
import com.brandPitara.sfs.dashboard.scraping.dto.ReraNumberSearchResponse;
import com.brandPitara.sfs.dashboard.scraping.service.ReraNumberSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/scraping/rera")
@RequiredArgsConstructor
public class DashboardReraSearchController {

    private final ReraNumberSearchService reraNumberSearchService;

    @PostMapping("/search-by-number")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReraNumberSearchResponse> searchByReraNumber(
            @Valid @RequestBody ReraNumberSearchRequest request
    ) {
        return ResponseEntity.ok(reraNumberSearchService.searchByReraNumber(request));
    }
}
