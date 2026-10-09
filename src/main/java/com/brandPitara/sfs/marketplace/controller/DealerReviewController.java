package com.brandPitara.sfs.marketplace.controller;

import com.brandPitara.sfs.marketplace.dto.DealerReviewCreateRequest;
import com.brandPitara.sfs.marketplace.dto.DealerReviewResponse;
import com.brandPitara.sfs.marketplace.service.DealerReviewService;
import com.brandPitara.sfs.security.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Signed-in (non-guest) users review a dealer; submissions stay private until moderated. */
@RestController
@RequiredArgsConstructor
public class DealerReviewController {

    private final DealerReviewService dealerReviewService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/dealers/{dealerId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("!hasRole('GUEST')")
    public DealerReviewResponse submit(
            @PathVariable Long dealerId,
            @Valid @RequestBody DealerReviewCreateRequest request
    ) {
        return dealerReviewService.submit(dealerId, request, currentUserService.requireUser());
    }

    @GetMapping("/api/dealers/{dealerId}/reviews/me")
    @PreAuthorize("!hasRole('GUEST')")
    public ResponseEntity<DealerReviewResponse> mine(@PathVariable Long dealerId) {
        return dealerReviewService.findMine(dealerId, currentUserService.requireUser())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
