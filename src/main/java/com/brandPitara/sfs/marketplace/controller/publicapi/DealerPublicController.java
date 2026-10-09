package com.brandPitara.sfs.marketplace.controller.publicapi;

import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.marketplace.dto.DealerCardResponse;
import com.brandPitara.sfs.marketplace.dto.DealerDetailResponse;
import com.brandPitara.sfs.marketplace.dto.DealerReviewResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerCardResponse;
import com.brandPitara.sfs.marketplace.service.DealerPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public Marketplace dealer reads. Inactive dealers and worker listings return 404. */
@RestController
@RequestMapping("/api/public/dealers")
@RequiredArgsConstructor
public class DealerPublicController {

    private final DealerPublicService dealerPublicService;

    @GetMapping
    public PageResponse<DealerCardResponse> list(
            /** Omit for all cities (the global home feed has no city). */
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return dealerPublicService.listDealers(cityId, categoryId, page, size);
    }

    @GetMapping("/{dealerId}")
    public DealerDetailResponse get(@PathVariable Long dealerId) {
        return dealerPublicService.getDealer(dealerId);
    }

    @GetMapping("/{dealerId}/similar")
    public PageResponse<DealerCardResponse> similar(
            @PathVariable Long dealerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return dealerPublicService.similarDealers(dealerId, page, size);
    }

    @GetMapping("/{dealerId}/workers")
    public PageResponse<WorkerCardResponse> connectedWorkers(
            @PathVariable Long dealerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return dealerPublicService.connectedWorkers(dealerId, page, size);
    }

    @GetMapping("/{dealerId}/reviews")
    public PageResponse<DealerReviewResponse> reviews(
            @PathVariable Long dealerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return dealerPublicService.approvedReviews(dealerId, page, size);
    }
}
