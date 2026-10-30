package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerCardResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerDetailResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerRecommendationResponse;

public interface WorkerPublicService {

    /** Verified workers for the home "Home Services" strip and its "See all" list. */
    PageResponse<WorkerCardResponse> listWorkers(Long cityId, Long categoryId, int page, int size);

    WorkerDetailResponse getWorker(Long workerId);

    PageResponse<WorkerRecommendationResponse> recommendations(Long workerId, int page, int size);
}
