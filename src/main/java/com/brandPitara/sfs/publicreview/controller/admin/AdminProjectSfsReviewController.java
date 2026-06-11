package com.brandPitara.sfs.publicreview.controller.admin;

import com.brandPitara.sfs.publicreview.dto.SfsReviewCreateRequest;
import com.brandPitara.sfs.publicreview.dto.SfsReviewResponse;
import com.brandPitara.sfs.publicreview.dto.SfsReviewUpdateVerificationRequest;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminProjectSfsReviewController {

    private final PublicReviewService publicReviewService;

    @PostMapping("/api/admin/projects/{projectId}/sfs-reviews")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY')")
    public SfsReviewResponse create(
        @PathVariable Long projectId,
        @Valid @RequestBody SfsReviewCreateRequest request
    ) {
        return publicReviewService.createSfsReview(projectId, request);
    }

    @GetMapping("/api/admin/projects/{projectId}/sfs-reviews")
    @PreAuthorize("hasAnyRole('ADMIN','DATA_ENTRY','REVIEWER')")
    public List<SfsReviewResponse> list(@PathVariable Long projectId) {
        return publicReviewService.adminListSfsReviews(projectId);
    }

    @PatchMapping("/api/admin/sfs-reviews/{reviewId}/verification")
    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public SfsReviewResponse updateVerification(
        @PathVariable Long reviewId,
        @Valid @RequestBody SfsReviewUpdateVerificationRequest request
    ) {
        return publicReviewService.updateSfsReviewVerification(reviewId, request);
    }
}
