package com.brandPitara.sfs.marketplace.controller.publicapi;

import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerCardResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerDetailResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerRecommendationResponse;
import com.brandPitara.sfs.marketplace.service.WorkerPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public Allied Services worker reads. Only VERIFIED workers are visible; others return 404. */
@RestController
@RequestMapping("/api/public/workers")
@RequiredArgsConstructor
public class WorkerPublicController {

    private final WorkerPublicService workerPublicService;

    @GetMapping
    public PageResponse<WorkerCardResponse> list(
            /** Omit for all cities (the global home feed has no city). */
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return workerPublicService.listWorkers(cityId, categoryId, page, size);
    }

    @GetMapping("/{workerId}")
    public WorkerDetailResponse get(@PathVariable Long workerId) {
        return workerPublicService.getWorker(workerId);
    }

    @GetMapping("/{workerId}/recommendations")
    public PageResponse<WorkerRecommendationResponse> recommendations(
            @PathVariable Long workerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return workerPublicService.recommendations(workerId, page, size);
    }
}
