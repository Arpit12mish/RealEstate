package com.brandPitara.sfs.dashboard.marketplace.controller;

import com.brandPitara.sfs.dashboard.marketplace.dto.*;
import com.brandPitara.sfs.dashboard.marketplace.service.DashboardDealerService;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.marketplace.enums.BusinessOfferingType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Dealer (Marketplace) management. DATA_ENTRY edits content; only ADMIN/REVIEWER can verify a
 * store's worker recommendation; only ADMIN deletes.
 */
@RestController
@RequestMapping("/api/dashboard/dealers")
@RequiredArgsConstructor
public class DashboardDealerController {

    private final DashboardDealerService dealerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public PageResponse<DealerListItemResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return dealerService.list(q, page, size);
    }

    @GetMapping("/{dealerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DealerManagementResponse get(@PathVariable Long dealerId) {
        return dealerService.get(dealerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DealerManagementResponse create(@Valid @RequestBody DealerCreateRequest request) {
        return dealerService.create(request);
    }

    @PatchMapping("/{dealerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DealerManagementResponse update(@PathVariable Long dealerId, @Valid @RequestBody DealerUpdateRequest request) {
        return dealerService.update(dealerId, request);
    }

    @PutMapping("/{dealerId}/opening-hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DealerManagementResponse replaceOpeningHours(
            @PathVariable Long dealerId, @Valid @RequestBody OpeningHoursReplaceRequest request) {
        return dealerService.replaceOpeningHours(dealerId, request);
    }

    @PutMapping("/{dealerId}/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DealerManagementResponse replaceProducts(
            @PathVariable Long dealerId, @Valid @RequestBody OfferingsReplaceRequest request) {
        return dealerService.replaceOfferings(dealerId, BusinessOfferingType.PRODUCT, request);
    }

    @PutMapping("/{dealerId}/services")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DealerManagementResponse replaceServices(
            @PathVariable Long dealerId, @Valid @RequestBody OfferingsReplaceRequest request) {
        return dealerService.replaceOfferings(dealerId, BusinessOfferingType.SERVICE, request);
    }

    @PostMapping("/{dealerId}/media")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DealerMediaResponse addMedia(@PathVariable Long dealerId, @Valid @RequestBody DealerMediaCreateRequest request) {
        return dealerService.addMedia(dealerId, request);
    }

    @PatchMapping("/{dealerId}/media/{mediaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DealerMediaResponse updateMedia(
            @PathVariable Long dealerId, @PathVariable Long mediaId, @Valid @RequestBody DealerMediaUpdateRequest request) {
        return dealerService.updateMedia(dealerId, mediaId, request);
    }

    @DeleteMapping("/{dealerId}/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMedia(@PathVariable Long dealerId, @PathVariable Long mediaId) {
        dealerService.deleteMedia(dealerId, mediaId);
    }

    @PostMapping("/{dealerId}/workers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DealerWorkerLinkResponse linkWorker(
            @PathVariable Long dealerId, @Valid @RequestBody DealerWorkerLinkCreateRequest request) {
        return dealerService.linkWorker(dealerId, request);
    }

    @PatchMapping("/{dealerId}/workers/{linkId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DealerWorkerLinkResponse updateWorkerLink(
            @PathVariable Long dealerId, @PathVariable Long linkId, @Valid @RequestBody DealerWorkerLinkUpdateRequest request) {
        return dealerService.updateWorkerLink(dealerId, linkId, request);
    }

    @DeleteMapping("/{dealerId}/workers/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteWorkerLink(@PathVariable Long dealerId, @PathVariable Long linkId) {
        dealerService.deleteWorkerLink(dealerId, linkId);
    }

    @PatchMapping("/{dealerId}/workers/{linkId}/recommendation")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public DealerWorkerLinkResponse reviewRecommendation(
            @PathVariable Long dealerId, @PathVariable Long linkId, @Valid @RequestBody RecommendationReviewRequest request) {
        return dealerService.reviewRecommendation(dealerId, linkId, request);
    }
}
