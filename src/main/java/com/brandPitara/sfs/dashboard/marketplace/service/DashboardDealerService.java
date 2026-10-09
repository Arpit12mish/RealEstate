package com.brandPitara.sfs.dashboard.marketplace.service;

import com.brandPitara.sfs.dashboard.marketplace.dto.*;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.marketplace.enums.BusinessOfferingType;

public interface DashboardDealerService {

    PageResponse<DealerListItemResponse> list(String query, int page, int size);

    DealerManagementResponse get(Long dealerId);

    DealerManagementResponse create(DealerCreateRequest request);

    DealerManagementResponse update(Long dealerId, DealerUpdateRequest request);

    DealerManagementResponse replaceOpeningHours(Long dealerId, OpeningHoursReplaceRequest request);

    DealerManagementResponse replaceOfferings(Long dealerId, BusinessOfferingType type, OfferingsReplaceRequest request);

    DealerMediaResponse addMedia(Long dealerId, DealerMediaCreateRequest request);

    DealerMediaResponse updateMedia(Long dealerId, Long mediaId, DealerMediaUpdateRequest request);

    void deleteMedia(Long dealerId, Long mediaId);

    DealerWorkerLinkResponse linkWorker(Long dealerId, DealerWorkerLinkCreateRequest request);

    DealerWorkerLinkResponse updateWorkerLink(Long dealerId, Long linkId, DealerWorkerLinkUpdateRequest request);

    void deleteWorkerLink(Long dealerId, Long linkId);

    DealerWorkerLinkResponse reviewRecommendation(Long dealerId, Long linkId, RecommendationReviewRequest request);
}
