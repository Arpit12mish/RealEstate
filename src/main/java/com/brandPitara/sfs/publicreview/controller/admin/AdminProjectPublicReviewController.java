package com.brandPitara.sfs.publicreview.controller.admin;

import com.brandPitara.sfs.publicreview.dto.*;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/public-reviews")
@RequiredArgsConstructor
public class AdminProjectPublicReviewController {

    private final PublicReviewService publicReviewService;

    @PostMapping("/places")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public PublicReviewPlaceResponse attachPlace(
        @PathVariable Long projectId,
        @Valid @RequestBody AttachPublicReviewPlaceRequest request
    ) {
        return publicReviewService.attachPlace(PublicReviewTargetType.PROJECT, projectId, request);
    }

    @GetMapping("/places")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public List<PublicReviewPlaceResponse> listPlaces(@PathVariable Long projectId) {
        return publicReviewService.listPlaces(PublicReviewTargetType.PROJECT, projectId);
    }

    @PostMapping("/places/{reviewPlaceId}/sync-google")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public SyncGooglePublicReviewsResponse syncGoogle(
        @PathVariable Long projectId,
        @PathVariable Long reviewPlaceId
    ) {
        return publicReviewService.syncGoogleReviews(PublicReviewTargetType.PROJECT, projectId, reviewPlaceId);
    }

    @GetMapping("/signal")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY','REVIEWER')")
    public PublicReviewSignalResponse signal(@PathVariable Long projectId) {
        return publicReviewService.getAdminSignal(PublicReviewTargetType.PROJECT, projectId);
    }

    /**
     * Preview-only Google Places text search — results are never auto-saved.
     * API key stays on the server; frontend receives only place metadata.
     */
    @GetMapping("/places/google-search")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public GooglePlaceSearchResponse googleSearch(
        @PathVariable Long projectId,
        @RequestParam String query
    ) {
        return publicReviewService.searchGooglePlaces(projectId, query);
    }
}
