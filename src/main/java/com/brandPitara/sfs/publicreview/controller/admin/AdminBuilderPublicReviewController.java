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
@RequestMapping("/api/admin/builders/{builderId}/public-reviews")
@RequiredArgsConstructor
public class AdminBuilderPublicReviewController {

    private final PublicReviewService publicReviewService;

    @PostMapping("/places")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public PublicReviewPlaceResponse attachPlace(
        @PathVariable Long builderId,
        @Valid @RequestBody AttachPublicReviewPlaceRequest request
    ) {
        return publicReviewService.attachPlace(PublicReviewTargetType.BUILDER, builderId, request);
    }

    @GetMapping("/places")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public List<PublicReviewPlaceResponse> listPlaces(@PathVariable Long builderId) {
        return publicReviewService.listPlaces(PublicReviewTargetType.BUILDER, builderId);
    }

    @PostMapping("/places/{reviewPlaceId}/sync-google")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public SyncGooglePublicReviewsResponse syncGoogle(
        @PathVariable Long builderId,
        @PathVariable Long reviewPlaceId
    ) {
        return publicReviewService.syncGoogleReviews(PublicReviewTargetType.BUILDER, builderId, reviewPlaceId);
    }

    @GetMapping("/signal")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY','REVIEWER')")
    public PublicReviewSignalResponse signal(@PathVariable Long builderId) {
        return publicReviewService.getAdminSignal(PublicReviewTargetType.BUILDER, builderId);
    }
}
