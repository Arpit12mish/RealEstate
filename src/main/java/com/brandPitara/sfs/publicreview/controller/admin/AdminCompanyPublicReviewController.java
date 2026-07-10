package com.brandPitara.sfs.publicreview.controller.admin;

import com.brandPitara.sfs.publicreview.dto.*;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Mirrors AdminBuilderPublicReviewController exactly - same generic PublicReviewService,
// just PublicReviewTargetType.COMPANY instead of BUILDER (see PublicReviewServiceImpl's
// validateTargetExistsForAdmin/validateTargetVisibleForPublic COMPANY branches, Phase 4.8B).
@RestController
@RequestMapping("/api/admin/companies/{companyId}/public-reviews")
@RequiredArgsConstructor
public class AdminCompanyPublicReviewController {

    private final PublicReviewService publicReviewService;

    @PostMapping("/places")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public PublicReviewPlaceResponse attachPlace(
        @PathVariable Long companyId,
        @Valid @RequestBody AttachPublicReviewPlaceRequest request
    ) {
        return publicReviewService.attachPlace(PublicReviewTargetType.COMPANY, companyId, request);
    }

    @GetMapping("/places")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public List<PublicReviewPlaceResponse> listPlaces(@PathVariable Long companyId) {
        return publicReviewService.listPlaces(PublicReviewTargetType.COMPANY, companyId);
    }

    @PostMapping("/places/{reviewPlaceId}/sync-google")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public SyncGooglePublicReviewsResponse syncGoogle(
        @PathVariable Long companyId,
        @PathVariable Long reviewPlaceId
    ) {
        try {
            return publicReviewService.syncGoogleReviews(PublicReviewTargetType.COMPANY, companyId, reviewPlaceId);
        } catch (IllegalStateException e) {
            // The Google Places client throws IllegalStateException for any non-2xx
            // upstream response (bad place ID, quota, outage) - map it to a clean
            // 502 here instead of letting it fall through to a generic 500.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google reviews fetch failed: " + e.getMessage());
        }
    }

    @GetMapping("/signal")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY','REVIEWER')")
    public PublicReviewSignalResponse signal(@PathVariable Long companyId) {
        return publicReviewService.getAdminSignal(PublicReviewTargetType.COMPANY, companyId);
    }
}
