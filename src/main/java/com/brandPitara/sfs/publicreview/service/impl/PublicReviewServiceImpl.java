package com.brandPitara.sfs.publicreview.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.publicreview.client.GooglePlaceDetailsResponse;
import com.brandPitara.sfs.publicreview.client.GooglePlacesClient;
import com.brandPitara.sfs.publicreview.dto.*;
import com.brandPitara.sfs.publicreview.entity.ProjectReviewEntity;
import com.brandPitara.sfs.publicreview.entity.PublicReviewPlaceEntity;
import com.brandPitara.sfs.publicreview.entity.PublicReviewSampleEntity;
import com.brandPitara.sfs.publicreview.entity.PublicReviewSummaryEntity;
import com.brandPitara.sfs.publicreview.enums.*;
import com.brandPitara.sfs.publicreview.mapper.PublicReviewMapper;
import com.brandPitara.sfs.publicreview.repository.ProjectReviewRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewPlaceRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewSampleRepository;
import com.brandPitara.sfs.publicreview.repository.PublicReviewSummaryRepository;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicReviewServiceImpl implements PublicReviewService {

    private static final String GOOGLE_SOURCE_LABEL = "Google Maps";
    private static final String PUBLIC_DISCLAIMER =
        "Google Maps reviews are public review samples and are not SFS-verified buyer complaints.";

    private static final String KEY_PROJECTS = "PROJECTS";
    private static final String KEY_BUILDERS = "BUILDERS";

    private final PublicReviewPlaceRepository placeRepository;
    private final PublicReviewSummaryRepository summaryRepository;
    private final PublicReviewSampleRepository sampleRepository;
    private final ProjectReviewRepository projectReviewRepository;

    private final ProjectRepository projectRepository;
    private final BuilderRepository builderRepository;
    private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;
    private final GooglePlacesClient googlePlacesClient;
    private final ContentVersionService contentVersionService;

    // =========================================================================
    // Existing methods (unchanged public contract)
    // =========================================================================

    @Override
    @Transactional
    public PublicReviewPlaceResponse attachPlace(
        PublicReviewTargetType targetType,
        Long targetId,
        AttachPublicReviewPlaceRequest request
    ) {
        validateTargetExistsForAdmin(targetType, targetId);

        String cleanedPlaceId = cleanRequired(request.getGooglePlaceId(), "googlePlaceId");
        PublicReviewPlaceCategory category = request.getPlaceCategory() != null
            ? request.getPlaceCategory() : PublicReviewPlaceCategory.OTHER;
        boolean active = request.getActive() != null ? request.getActive() : true;

        // Lookup includes soft-deleted rows so we can reactivate instead of hitting the unique constraint.
        PublicReviewPlaceEntity entity = placeRepository
            .findByTargetTypeAndTargetIdAndGooglePlaceId(targetType, targetId, cleanedPlaceId)
            .orElseGet(() -> PublicReviewPlaceEntity.builder()
                .targetType(targetType)
                .targetId(targetId)
                .sourceType(PublicReviewSourceType.GOOGLE_PLACES)
                .googlePlaceId(cleanedPlaceId)
                .build());

        entity.setDeleted(false);
        entity.setPlaceCategory(category);
        entity.setActive(active);

        try {
            PublicReviewPlaceEntity saved = placeRepository.save(entity);
            bumpContentVersion(targetType);
            return toPlaceResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // Race condition: two simultaneous attach requests both found no row and tried to INSERT.
            // The first succeeded; recover by returning the row that now exists.
            return placeRepository
                .findByTargetTypeAndTargetIdAndGooglePlaceId(targetType, targetId, cleanedPlaceId)
                .map(existing -> {
                    existing.setDeleted(false);
                    existing.setPlaceCategory(category);
                    existing.setActive(active);
                    PublicReviewPlaceEntity recovered = placeRepository.save(existing);
                    bumpContentVersion(targetType);
                    return toPlaceResponse(recovered);
                })
                .orElseThrow(() -> ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicReviewPlaceResponse> listPlaces(PublicReviewTargetType targetType, Long targetId) {
        validateTargetExistsForAdmin(targetType, targetId);

        return placeRepository.findByTargetTypeAndTargetIdAndDeletedFalseOrderByIdDesc(targetType, targetId)
            .stream()
            .map(this::toPlaceResponse)
            .toList();
    }

    @Override
    @Transactional
    public SyncGooglePublicReviewsResponse syncGoogleReviews(
        PublicReviewTargetType targetType,
        Long targetId,
        Long reviewPlaceId
    ) {
        validateTargetExistsForAdmin(targetType, targetId);

        PublicReviewPlaceEntity place = placeRepository
            .findByIdAndTargetTypeAndTargetIdAndDeletedFalse(reviewPlaceId, targetType, targetId)
            .orElseThrow(() -> new NotFoundException("Review place not found: " + reviewPlaceId));

        PublicReviewSummaryEntity existingSummary = summaryRepository.findByReviewPlaceId(place.getId()).orElse(null);
        if (isGoogleFetchCompleted(place, existingSummary)) {
            return returnExistingGoogleFetch(place, existingSummary, targetType);
        }

        OffsetDateTime now = OffsetDateTime.now();

        try {
            GooglePlaceDetailsResponse googleResponse = googlePlacesClient.fetchPlaceDetails(place.getGooglePlaceId());

            place.setPlaceName(extractText(googleResponse.getDisplayName()));
            place.setFormattedAddress(clean(googleResponse.getFormattedAddress()));
            place.setGoogleMapsUri(clean(googleResponse.getGoogleMapsUri()));
            place.setLastSyncedAt(now);

            List<GooglePlaceDetailsResponse.GoogleReview> googleReviews =
                googleResponse.getReviews() != null ? googleResponse.getReviews() : List.of();

            List<PublicReviewSampleEntity> samples = new ArrayList<>();

            int positive = 0;
            int negative = 0;
            int neutral = 0;
            int mixed = 0;

            sampleRepository.deleteByReviewPlaceId(place.getId());

            for (GooglePlaceDetailsResponse.GoogleReview googleReview : googleReviews) {
                PublicReviewSentiment sentiment = classifySentiment(googleReview.getRating());

                if (sentiment == PublicReviewSentiment.POSITIVE) positive++;
                else if (sentiment == PublicReviewSentiment.NEGATIVE) negative++;
                else if (sentiment == PublicReviewSentiment.NEUTRAL) neutral++;
                else mixed++;

                PublicReviewSampleEntity sample = PublicReviewSampleEntity.builder()
                    .reviewPlace(place)
                    .targetType(targetType)
                    .targetId(targetId)
                    .sourceType(PublicReviewSourceType.GOOGLE_PLACES)
                    .reviewerName(extractReviewerName(googleReview))
                    .reviewerProfileUrl(extractReviewerProfileUrl(googleReview))
                    .reviewerPhotoUrl(extractReviewerPhotoUrl(googleReview))
                    .rating(googleReview.getRating())
                    .reviewText(extractText(googleReview.getText()))
                    .originalReviewText(extractText(googleReview.getOriginalText()))
                    .languageCode(extractLanguageCode(googleReview))
                    .relativePublishTime(clean(googleReview.getRelativePublishTimeDescription()))
                    .publishTime(googleReview.getPublishTime())
                    .sentiment(sentiment)
                    .category(classifyCategory(extractText(googleReview.getText()), googleReview.getRating()))
                    .displayStatus(PublicReviewDisplayStatus.INTERNAL_ONLY)
                    .fetchedAt(now)
                    .build();

                samples.add(sample);
            }

            sampleRepository.saveAll(samples);

            PublicReviewSummaryEntity summary = summaryRepository.findByReviewPlaceId(place.getId())
                .orElseGet(() -> PublicReviewSummaryEntity.builder()
                    .reviewPlace(place)
                    .targetType(targetType)
                    .targetId(targetId)
                    .sourceType(PublicReviewSourceType.GOOGLE_PLACES)
                    .build());

            summary.setRating(googleResponse.getRating());
            summary.setUserRatingCount(googleResponse.getUserRatingCount());
            summary.setPositiveSampleCount(positive);
            summary.setNegativeSampleCount(negative);
            summary.setNeutralSampleCount(neutral);
            summary.setMixedSampleCount(mixed);
            summary.setSourceLabel(GOOGLE_SOURCE_LABEL);
            summary.setDisclaimer(PUBLIC_DISCLAIMER);
            summary.setLastSyncedAt(now);
            summaryRepository.save(summary);

            // Mark as successfully fetched
            place.setOneTimeFetched(true);
            place.setFetchStatus(GoogleReviewFetchStatus.FETCHED);
            place.setDisplayMode(GoogleReviewDisplayMode.RATING_AND_REVIEWS);
            place.setDisplayGoogleReviews(true);
            placeRepository.save(place);

            bumpContentVersion(targetType);

            return SyncGooglePublicReviewsResponse.builder()
                .reviewPlaceId(place.getId())
                .googlePlaceId(place.getGooglePlaceId())
                .placeName(place.getPlaceName())
                .rating(googleResponse.getRating())
                .userRatingCount(googleResponse.getUserRatingCount())
                .fetchedReviewSampleCount(samples.size())
                .syncedAt(now)
                .build();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            place.setFetchStatus(GoogleReviewFetchStatus.FAILED);
            placeRepository.save(place);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PublicReviewSignalResponse getAdminSignal(PublicReviewTargetType targetType, Long targetId) {
        validateTargetExistsForAdmin(targetType, targetId);
        return buildSignal(targetType, targetId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicReviewSignalResponse getPublicSignal(PublicReviewTargetType targetType, Long targetId) {
        validateTargetVisibleForPublic(targetType, targetId);
        return buildSignal(targetType, targetId, true);
    }

    @Override
    @Transactional
    public PublicReviewSampleResponse updateSampleDisplayStatus(
        Long sampleId,
        UpdatePublicReviewDisplayStatusRequest request
    ) {
        PublicReviewSampleEntity sample = sampleRepository.findById(sampleId)
            .orElseThrow(() -> new NotFoundException("Review sample not found: " + sampleId));

        sample.setDisplayStatus(request.getDisplayStatus());
        PublicReviewSampleEntity saved = sampleRepository.save(sample);

        bumpContentVersion(saved.getTargetType());

        return PublicReviewMapper.toSampleResponse(saved);
    }

    // =========================================================================
    // New methods — Google Place Search (preview only, never stored)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public GooglePlaceSearchResponse searchGooglePlaces(Long projectId, String query) {
        ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        String resolvedQuery = StringUtils.hasText(query) ? query.trim() : project.getName();

        List<GooglePlaceSearchResultItem> results = googlePlacesClient.searchPlaces(
            resolvedQuery,
            project.getLatitude(),
            project.getLongitude()
        );

        return GooglePlaceSearchResponse.builder()
            .projectId(projectId)
            .query(resolvedQuery)
            .results(results)
            .build();
    }

    // =========================================================================
    // New methods — SFS Native Reviews
    // =========================================================================

    @Override
    @Transactional
    public SfsReviewResponse createSfsReview(Long projectId, SfsReviewCreateRequest request) {
        projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        SfsReviewSourceType sourceType = request.getSourceType() != null
            ? request.getSourceType()
            : SfsReviewSourceType.USER_SUBMITTED;

        SfsReviewVerificationStatus verificationStatus = request.getVerificationStatus() != null
            ? request.getVerificationStatus()
            : SfsReviewVerificationStatus.PENDING;

        // Dashboard-created reviews with VERIFIED status are immediately display-eligible
        String displayStatus = (verificationStatus == SfsReviewVerificationStatus.VERIFIED)
            ? "APPROVED_PUBLIC"
            : "INTERNAL_ONLY";

        ProjectReviewEntity entity = ProjectReviewEntity.builder()
            .projectId(projectId)
            .reviewerName(clean(request.getReviewerName()))
            .rating(request.getRating())
            .headline(clean(request.getHeadline()))
            .reviewText(clean(request.getReviewText()))
            .sourceType(sourceType)
            .verificationStatus(verificationStatus)
            .displayStatus(displayStatus)
            .build();

        ProjectReviewEntity saved = projectReviewRepository.save(entity);
        contentVersionService.bump(KEY_PROJECTS);

        return PublicReviewMapper.toSfsReviewResponse(saved);
    }

    @Override
    @Transactional
    public SfsReviewResponse updateSfsReviewVerification(Long reviewId, SfsReviewUpdateVerificationRequest request) {
        ProjectReviewEntity review = projectReviewRepository.findById(reviewId)
            .orElseThrow(() -> new NotFoundException("SFS review not found: " + reviewId));

        if (Boolean.TRUE.equals(review.getDeleted())) {
            throw new NotFoundException("SFS review not found: " + reviewId);
        }

        review.setVerificationStatus(request.getVerificationStatus());

        if (StringUtils.hasText(request.getInternalNote())) {
            review.setInternalNote(request.getInternalNote().trim());
        }

        review.setReviewedAt(OffsetDateTime.now());

        // Auto-promote or demote display status based on new verification status
        if (request.getVerificationStatus() == SfsReviewVerificationStatus.VERIFIED) {
            review.setDisplayStatus("APPROVED_PUBLIC");
        } else if (request.getVerificationStatus() == SfsReviewVerificationStatus.REJECTED) {
            review.setDisplayStatus("REJECTED");
        }

        ProjectReviewEntity saved = projectReviewRepository.save(review);
        contentVersionService.bump(KEY_PROJECTS);

        return PublicReviewMapper.toSfsReviewResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SfsReviewResponse> adminListSfsReviews(Long projectId) {
        projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        return projectReviewRepository
            .findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(projectId)
            .stream()
            .map(PublicReviewMapper::toSfsReviewResponse)
            .toList();
    }

    // =========================================================================
    // New methods — Authenticated mobile review submission
    // =========================================================================

    @Override
    @Transactional
    public SfsReviewResponse submitAuthenticatedProjectReview(
        Long projectId,
        PublicAuthenticatedReviewCreateRequest request,
        User currentUser
    ) {
        projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        // One active review per user per project — prevents duplicate spam
        if (projectReviewRepository.existsByUserIdAndProjectIdAndSubmittedByUserTrueAndDeletedFalse(
                currentUser.getId(), projectId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "You have already submitted a review for this project."
            );
        }

        ProjectReviewEntity entity = ProjectReviewEntity.builder()
            .projectId(projectId)
            .userId(currentUser.getId())
            .userPhoneHash(hashPhone(currentUser.getPhoneNumber()))
            .reviewerName(clean(request.getReviewerName()))
            .rating(request.getRating())
            .headline(clean(request.getHeadline()))
            .reviewText(clean(request.getReviewText()))
            .sourceType(SfsReviewSourceType.USER_SUBMITTED)
            .verificationStatus(SfsReviewVerificationStatus.PENDING)
            .displayStatus("INTERNAL_ONLY")
            .isFeatured(false)
            .submittedByUser(true)
            .deleted(false)
            .build();

        ProjectReviewEntity saved = projectReviewRepository.save(entity);
        contentVersionService.bump(KEY_PROJECTS);

        SfsReviewResponse response = PublicReviewMapper.toSfsReviewResponse(saved);
        response.setMessage("Review submitted successfully and is pending moderation.");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SfsReviewResponse> listPublicApprovedReviews(Long projectId) {
        validateTargetVisibleForPublic(PublicReviewTargetType.PROJECT, projectId);

        return projectReviewRepository
            .findByProjectIdAndDisplayStatusAndDeletedFalse(projectId, "APPROVED_PUBLIC")
            .stream()
            .map(PublicReviewMapper::toSfsReviewResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MySubmittedReviewResponse> listMySubmittedReviews(User currentUser) {
        List<ProjectReviewEntity> reviews = projectReviewRepository
            .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(currentUser.getId());

        return reviews.stream()
            .map(review -> {
                String projectName = projectRepository.findByIdAndDeletedFalse(review.getProjectId())
                    .map(ProjectEntity::getName)
                    .orElse(null);
                return PublicReviewMapper.toMySubmittedReviewResponse(review, projectName);
            })
            .toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private PublicReviewSignalResponse buildSignal(
        PublicReviewTargetType targetType,
        Long targetId,
        boolean publicOnly
    ) {
        List<PublicReviewPlaceEntity> places = publicOnly
            ? placeRepository.findByTargetTypeAndTargetIdAndActiveTrueAndDeletedFalseOrderByIdDesc(targetType, targetId)
            : placeRepository.findByTargetTypeAndTargetIdAndDeletedFalseOrderByIdDesc(targetType, targetId);

        if (places.isEmpty()) {
            return PublicReviewSignalResponse.builder()
                .targetType(targetType)
                .targetId(targetId)
                .sourceType(PublicReviewSourceType.GOOGLE_PLACES)
                .rating(null)
                .userRatingCount(0)
                .positiveSampleCount(0)
                .negativeSampleCount(0)
                .neutralSampleCount(0)
                .mixedSampleCount(0)
                .sourceLabel(GOOGLE_SOURCE_LABEL)
                .disclaimer(PUBLIC_DISCLAIMER)
                .places(List.of())
                .samples(List.of())
                .build();
        }

        List<PublicReviewPlaceResponse> placeResponses = places.stream()
            .map(this::toPlaceResponse)
            .toList();

        BigDecimal bestRating = null;
        Integer totalRatingCount = 0;
        Integer positive = 0;
        Integer negative = 0;
        Integer neutral = 0;
        Integer mixed = 0;
        OffsetDateTime latestSync = null;

        List<PublicReviewSampleResponse> sampleResponses = new ArrayList<>();

        for (PublicReviewPlaceEntity place : places) {
            PublicReviewSummaryEntity summary = summaryRepository.findByReviewPlaceId(place.getId()).orElse(null);

            if (summary != null) {
                if (summary.getRating() != null && bestRating == null) {
                    bestRating = summary.getRating();
                }

                if (summary.getUserRatingCount() != null) {
                    totalRatingCount += summary.getUserRatingCount();
                }

                positive += safeInt(summary.getPositiveSampleCount());
                negative += safeInt(summary.getNegativeSampleCount());
                neutral += safeInt(summary.getNeutralSampleCount());
                mixed += safeInt(summary.getMixedSampleCount());

                if (summary.getLastSyncedAt() != null &&
                    (latestSync == null || summary.getLastSyncedAt().isAfter(latestSync))) {
                    latestSync = summary.getLastSyncedAt();
                }
            }

            List<PublicReviewSampleEntity> samples = publicOnly
                ? sampleRepository.findByReviewPlaceIdAndDisplayStatusOrderByRatingAscIdDesc(
                    place.getId(),
                    PublicReviewDisplayStatus.APPROVED_PUBLIC
                )
                : sampleRepository.findByReviewPlaceIdOrderByRatingAscIdDesc(place.getId());

            sampleResponses.addAll(samples.stream().map(PublicReviewMapper::toSampleResponse).toList());
        }

        return PublicReviewSignalResponse.builder()
            .targetType(targetType)
            .targetId(targetId)
            .sourceType(PublicReviewSourceType.GOOGLE_PLACES)
            .rating(bestRating)
            .userRatingCount(totalRatingCount)
            .positiveSampleCount(positive)
            .negativeSampleCount(negative)
            .neutralSampleCount(neutral)
            .mixedSampleCount(mixed)
            .sourceLabel(GOOGLE_SOURCE_LABEL)
            .disclaimer(PUBLIC_DISCLAIMER)
            .lastSyncedAt(latestSync)
            .places(placeResponses)
            .samples(sampleResponses)
            .build();
    }

    private PublicReviewPlaceResponse toPlaceResponse(PublicReviewPlaceEntity place) {
        PublicReviewSummaryEntity summary = summaryRepository.findByReviewPlaceId(place.getId()).orElse(null);
        PublicReviewPlaceResponse response = PublicReviewMapper.toPlaceResponse(place);

        boolean completed = isGoogleFetchCompleted(place, summary);
        response.setOneTimeFetched(completed);
        response.setFetchStatus(completed ? GoogleReviewFetchStatus.FETCHED : place.getFetchStatus());

        if (summary != null) {
            response.setRating(summary.getRating());
            response.setUserRatingCount(summary.getUserRatingCount());
        }

        return response;
    }

    private boolean isGoogleFetchCompleted(
        PublicReviewPlaceEntity place,
        PublicReviewSummaryEntity summary
    ) {
        return Boolean.TRUE.equals(place.getOneTimeFetched())
            || place.getFetchStatus() == GoogleReviewFetchStatus.FETCHED
            || summary != null;
    }

    private SyncGooglePublicReviewsResponse returnExistingGoogleFetch(
        PublicReviewPlaceEntity place,
        PublicReviewSummaryEntity summary,
        PublicReviewTargetType targetType
    ) {
        boolean repaired = false;

        if (!Boolean.TRUE.equals(place.getOneTimeFetched())) {
            place.setOneTimeFetched(true);
            repaired = true;
        }
        if (place.getFetchStatus() != GoogleReviewFetchStatus.FETCHED) {
            place.setFetchStatus(GoogleReviewFetchStatus.FETCHED);
            repaired = true;
        }
        if (place.getLastSyncedAt() == null && summary != null && summary.getLastSyncedAt() != null) {
            place.setLastSyncedAt(summary.getLastSyncedAt());
            repaired = true;
        }

        if (repaired) {
            placeRepository.save(place);
            bumpContentVersion(targetType);
        }

        OffsetDateTime syncedAt = summary != null ? summary.getLastSyncedAt() : place.getLastSyncedAt();
        long sampleCount = sampleRepository.countByReviewPlaceId(place.getId());

        return SyncGooglePublicReviewsResponse.builder()
            .reviewPlaceId(place.getId())
            .googlePlaceId(place.getGooglePlaceId())
            .placeName(place.getPlaceName())
            .rating(summary != null ? summary.getRating() : null)
            .userRatingCount(summary != null ? summary.getUserRatingCount() : null)
            .fetchedReviewSampleCount(Math.toIntExact(sampleCount))
            .syncedAt(syncedAt)
            .build();
    }

    private void validateTargetExistsForAdmin(PublicReviewTargetType targetType, Long targetId) {
        if (targetType == PublicReviewTargetType.PROJECT) {
            projectRepository.findByIdAndDeletedFalse(targetId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + targetId));
            return;
        }

        if (targetType == PublicReviewTargetType.BUILDER) {
            builderRepository.findByIdAndDeletedFalse(targetId)
                .orElseThrow(() -> new NotFoundException("Builder not found: " + targetId));
            return;
        }

        throw new IllegalArgumentException("Unsupported target type: " + targetType);
    }

    private void validateTargetVisibleForPublic(PublicReviewTargetType targetType, Long targetId) {
        if (targetType == PublicReviewTargetType.PROJECT) {
            ProjectEntity project = projectRepository.findByIdAndDeletedFalse(targetId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + targetId));

            projectPublicVisibilityPolicy.assertPubliclyVisible(project, targetId);
            return;
        }

        if (targetType == PublicReviewTargetType.BUILDER) {
            BuilderEntity builder = builderRepository.findByIdAndDeletedFalse(targetId)
                .orElseThrow(() -> new NotFoundException("Builder not found: " + targetId));

            if (!Boolean.TRUE.equals(builder.getPublished())
                || !Boolean.TRUE.equals(builder.getActive())
                || Boolean.TRUE.equals(builder.getDeleted())) {
                throw new NotFoundException("Builder not found: " + targetId);
            }
            return;
        }

        throw new IllegalArgumentException("Unsupported target type: " + targetType);
    }

    private PublicReviewSentiment classifySentiment(Integer rating) {
        if (rating == null) return PublicReviewSentiment.NEUTRAL;
        if (rating <= 2) return PublicReviewSentiment.NEGATIVE;
        if (rating == 3) return PublicReviewSentiment.MIXED;
        return PublicReviewSentiment.POSITIVE;
    }

    private String classifyCategory(String text, Integer rating) {
        String value = text == null ? "" : text.toLowerCase();

        if (containsAny(value, "delay", "possession", "handover", "late")) {
            return PublicReviewCategory.POSSESSION_DELAY.name();
        }
        if (containsAny(value, "maintenance", "society maintenance", "repair")) {
            return PublicReviewCategory.MAINTENANCE.name();
        }
        if (containsAny(value, "sales", "crm", "staff", "response", "communication")) {
            return PublicReviewCategory.CRM_RESPONSE.name();
        }
        if (containsAny(value, "quality", "construction", "seepage", "crack", "defect")) {
            return PublicReviewCategory.CONSTRUCTION_QUALITY.name();
        }
        if (containsAny(value, "location", "connectivity", "metro", "road", "golf course")) {
            return PublicReviewCategory.LOCATION.name();
        }
        if (containsAny(value, "amenities", "clubhouse", "pool", "gym", "spa", "garden")) {
            return PublicReviewCategory.AMENITIES.name();
        }
        if (containsAny(value, "security", "safe", "privacy")) {
            return PublicReviewCategory.SECURITY.name();
        }
        if (containsAny(value, "luxury", "premium", "world-class", "elegance", "sophistication")) {
            return PublicReviewCategory.LUXURY_LIFESTYLE.name();
        }
        if (rating != null && rating >= 4) return PublicReviewCategory.GENERAL_POSITIVE.name();
        if (rating != null && rating <= 2) return PublicReviewCategory.GENERAL_NEGATIVE.name();
        return PublicReviewCategory.GENERAL.name();
    }

    private boolean containsAny(String source, String... words) {
        if (source == null) return false;
        for (String word : words) {
            if (source.contains(word)) return true;
        }
        return false;
    }

    private String extractReviewerName(GooglePlaceDetailsResponse.GoogleReview review) {
        return review.getAuthorAttribution() != null ? clean(review.getAuthorAttribution().getDisplayName()) : null;
    }

    private String extractReviewerProfileUrl(GooglePlaceDetailsResponse.GoogleReview review) {
        return review.getAuthorAttribution() != null ? clean(review.getAuthorAttribution().getUri()) : null;
    }

    private String extractReviewerPhotoUrl(GooglePlaceDetailsResponse.GoogleReview review) {
        return review.getAuthorAttribution() != null ? clean(review.getAuthorAttribution().getPhotoUri()) : null;
    }

    private String extractLanguageCode(GooglePlaceDetailsResponse.GoogleReview review) {
        if (review.getText() != null && StringUtils.hasText(review.getText().getLanguageCode())) {
            return review.getText().getLanguageCode();
        }
        if (review.getOriginalText() != null && StringUtils.hasText(review.getOriginalText().getLanguageCode())) {
            return review.getOriginalText().getLanguageCode();
        }
        return null;
    }

    private String extractText(GooglePlaceDetailsResponse.LocalizedText localizedText) {
        return localizedText != null ? clean(localizedText.getText()) : null;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim();
    }

    private String cleanRequired(String value, String fieldName) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return cleaned;
    }

    private void bumpContentVersion(PublicReviewTargetType targetType) {
        if (targetType == PublicReviewTargetType.PROJECT) {
            contentVersionService.bump(KEY_PROJECTS);
        } else if (targetType == PublicReviewTargetType.BUILDER) {
            contentVersionService.bump(KEY_BUILDERS);
        }
    }

    private String hashPhone(String phoneNumber) {
        if (phoneNumber == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(phoneNumber.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
