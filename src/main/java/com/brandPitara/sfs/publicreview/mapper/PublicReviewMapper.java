package com.brandPitara.sfs.publicreview.mapper;

import com.brandPitara.sfs.publicreview.dto.MySubmittedReviewResponse;
import com.brandPitara.sfs.publicreview.dto.PublicReviewPlaceResponse;
import com.brandPitara.sfs.publicreview.dto.PublicReviewSampleResponse;
import com.brandPitara.sfs.publicreview.dto.SfsReviewResponse;
import com.brandPitara.sfs.publicreview.entity.ProjectReviewEntity;
import com.brandPitara.sfs.publicreview.entity.PublicReviewPlaceEntity;
import com.brandPitara.sfs.publicreview.entity.PublicReviewSampleEntity;

public final class PublicReviewMapper {

    private PublicReviewMapper() {
    }

    public static PublicReviewPlaceResponse toPlaceResponse(PublicReviewPlaceEntity entity) {
        return PublicReviewPlaceResponse.builder()
            .id(entity.getId())
            .targetType(entity.getTargetType())
            .targetId(entity.getTargetId())
            .sourceType(entity.getSourceType())
            .googlePlaceId(entity.getGooglePlaceId())
            .placeName(entity.getPlaceName())
            .formattedAddress(entity.getFormattedAddress())
            .googleMapsUri(entity.getGoogleMapsUri())
            .placeCategory(entity.getPlaceCategory())
            .active(Boolean.TRUE.equals(entity.getActive()))
            .lastSyncedAt(entity.getLastSyncedAt())
            .oneTimeFetched(Boolean.TRUE.equals(entity.getOneTimeFetched()))
            .fetchStatus(entity.getFetchStatus())
            .displayMode(entity.getDisplayMode())
            .displayGoogleReviews(Boolean.TRUE.equals(entity.getDisplayGoogleReviews()))
            .build();
    }

    public static SfsReviewResponse toSfsReviewResponse(ProjectReviewEntity entity) {
        return SfsReviewResponse.builder()
            .id(entity.getId())
            .projectId(entity.getProjectId())
            .reviewerName(entity.getReviewerName())
            .rating(entity.getRating())
            .headline(entity.getHeadline())
            .reviewText(entity.getReviewText())
            .sourceType(entity.getSourceType())
            .verificationStatus(entity.getVerificationStatus())
            .category(entity.getCategory())
            .sentiment(entity.getSentiment())
            .isFeatured(entity.getIsFeatured())
            .displayStatus(entity.getDisplayStatus())
            .internalNote(entity.getInternalNote())
            .reviewedAt(entity.getReviewedAt())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .submittedByUser(entity.getSubmittedByUser())
            .build();
    }

    public static MySubmittedReviewResponse toMySubmittedReviewResponse(
            ProjectReviewEntity entity,
            String projectName
    ) {
        return MySubmittedReviewResponse.builder()
            .id(entity.getId())
            .projectId(entity.getProjectId())
            .projectName(projectName)
            .reviewerName(entity.getReviewerName())
            .rating(entity.getRating())
            .headline(entity.getHeadline())
            .reviewText(entity.getReviewText())
            .verificationStatus(entity.getVerificationStatus())
            .displayStatus(entity.getDisplayStatus())
            .submittedByUser(entity.getSubmittedByUser())
            .createdAt(entity.getCreatedAt())
            .reviewedAt(entity.getReviewedAt())
            .build();
    }

    public static PublicReviewSampleResponse toSampleResponse(PublicReviewSampleEntity entity) {
        return PublicReviewSampleResponse.builder()
            .id(entity.getId())
            .reviewerName(entity.getReviewerName())
            .reviewerProfileUrl(entity.getReviewerProfileUrl())
            .reviewerPhotoUrl(entity.getReviewerPhotoUrl())
            .rating(entity.getRating())
            .reviewText(entity.getReviewText())
            .originalReviewText(entity.getOriginalReviewText())
            .languageCode(entity.getLanguageCode())
            .relativePublishTime(entity.getRelativePublishTime())
            .publishTime(entity.getPublishTime())
            .sentiment(entity.getSentiment())
            .category(entity.getCategory())
            .displayStatus(entity.getDisplayStatus())
            .build();
    }
}
