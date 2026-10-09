package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.marketplace.dto.DealerCardResponse;
import com.brandPitara.sfs.marketplace.dto.DealerDetailResponse;
import com.brandPitara.sfs.marketplace.dto.DealerReviewResponse;
import com.brandPitara.sfs.marketplace.dto.WorkerCardResponse;

public interface DealerPublicService {

    DealerDetailResponse getDealer(Long dealerId);

    PageResponse<DealerCardResponse> listDealers(Long cityId, Long categoryId, int page, int size);

    PageResponse<DealerCardResponse> similarDealers(Long dealerId, int page, int size);

    PageResponse<WorkerCardResponse> connectedWorkers(Long dealerId, int page, int size);

    PageResponse<DealerReviewResponse> approvedReviews(Long dealerId, int page, int size);
}
