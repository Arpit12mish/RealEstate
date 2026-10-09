package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerDetailResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerRecommendationResponse;

public interface WorkerPublicService {

    WorkerDetailResponse getWorker(Long workerId);

    PageResponse<WorkerRecommendationResponse> recommendations(Long workerId, int page, int size);
}
