package com.brandPitara.sfs.dashboard.marketplace.controller;

import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.marketplace.dto.DealerReviewModerationRequest;
import com.brandPitara.sfs.marketplace.dto.DealerReviewModerationResponse;
import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;
import com.brandPitara.sfs.marketplace.service.DealerReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Moderation queue for user-submitted dealer reviews. Approval recomputes the dealer rating. */
@RestController
@RequestMapping("/api/dashboard/dealer-reviews")
@RequiredArgsConstructor
public class DashboardDealerReviewController {

    private final DealerReviewService dealerReviewService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public PageResponse<DealerReviewModerationResponse> queue(
            @RequestParam(defaultValue = "PENDING") BusinessReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return dealerReviewService.moderationQueue(status, page, size);
    }

    @PatchMapping("/{reviewId}/moderation")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public DealerReviewModerationResponse moderate(
            @PathVariable Long reviewId, @Valid @RequestBody DealerReviewModerationRequest request) {
        return dealerReviewService.moderate(reviewId, request);
    }
}
