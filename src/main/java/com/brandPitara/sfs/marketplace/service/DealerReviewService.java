package com.brandPitara.sfs.marketplace.service;

import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.marketplace.dto.DealerReviewCreateRequest;
import com.brandPitara.sfs.marketplace.dto.DealerReviewModerationRequest;
import com.brandPitara.sfs.marketplace.dto.DealerReviewModerationResponse;
import com.brandPitara.sfs.marketplace.dto.DealerReviewResponse;
import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;

import java.util.Optional;

public interface DealerReviewService {

    DealerReviewResponse submit(Long dealerId, DealerReviewCreateRequest request, User currentUser);

    /** The caller's own review of a dealer (any moderation state), so the app can show "pending". */
    Optional<DealerReviewResponse> findMine(Long dealerId, User currentUser);

    PageResponse<DealerReviewModerationResponse> moderationQueue(BusinessReviewStatus status, int page, int size);

    DealerReviewModerationResponse moderate(Long reviewId, DealerReviewModerationRequest request);
}
