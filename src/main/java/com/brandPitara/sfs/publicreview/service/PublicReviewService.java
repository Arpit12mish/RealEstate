package com.brandPitara.sfs.publicreview.service;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.publicreview.dto.AttachPublicReviewPlaceRequest;
import com.brandPitara.sfs.publicreview.dto.GooglePlaceSearchResponse;
import com.brandPitara.sfs.publicreview.dto.MySubmittedReviewResponse;
import com.brandPitara.sfs.publicreview.dto.PublicAuthenticatedReviewCreateRequest;
import com.brandPitara.sfs.publicreview.dto.PublicReviewPlaceResponse;
import com.brandPitara.sfs.publicreview.dto.PublicReviewSampleResponse;
import com.brandPitara.sfs.publicreview.dto.PublicReviewSignalResponse;
import com.brandPitara.sfs.publicreview.dto.SfsReviewCreateRequest;
import com.brandPitara.sfs.publicreview.dto.SfsReviewResponse;
import com.brandPitara.sfs.publicreview.dto.SfsReviewUpdateVerificationRequest;
import com.brandPitara.sfs.publicreview.dto.SyncGooglePublicReviewsResponse;
import com.brandPitara.sfs.publicreview.dto.UpdatePublicReviewDisplayStatusRequest;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;

import java.util.List;

public interface PublicReviewService {

    PublicReviewPlaceResponse attachPlace(
            PublicReviewTargetType targetType,
            Long targetId,
            AttachPublicReviewPlaceRequest request
    );

    List<PublicReviewPlaceResponse> listPlaces(
            PublicReviewTargetType targetType,
            Long targetId
    );

    SyncGooglePublicReviewsResponse syncGoogleReviews(
            PublicReviewTargetType targetType,
            Long targetId,
            Long reviewPlaceId
    );

    PublicReviewSignalResponse getAdminSignal(
            PublicReviewTargetType targetType,
            Long targetId
    );

    PublicReviewSignalResponse getPublicSignal(
            PublicReviewTargetType targetType,
            Long targetId
    );

    PublicReviewSampleResponse updateSampleDisplayStatus(
            Long sampleId,
            UpdatePublicReviewDisplayStatusRequest request
    );

    GooglePlaceSearchResponse searchGooglePlaces(Long projectId, String query);

    SfsReviewResponse createSfsReview(Long projectId, SfsReviewCreateRequest request);

    SfsReviewResponse updateSfsReviewVerification(Long reviewId, SfsReviewUpdateVerificationRequest request);

    List<SfsReviewResponse> adminListSfsReviews(Long projectId);

    SfsReviewResponse submitAuthenticatedProjectReview(
            Long projectId,
            PublicAuthenticatedReviewCreateRequest request,
            User currentUser
    );

    List<MySubmittedReviewResponse> listMySubmittedReviews(User currentUser);

    List<SfsReviewResponse> listPublicApprovedReviews(Long projectId);
}
