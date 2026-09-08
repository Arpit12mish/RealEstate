package com.brandPitara.sfs.publicreview.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.repository.CompanyRepository;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.integration.ExternalProviderTransactions;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.publicreview.client.GooglePlaceDetailsResponse;
import com.brandPitara.sfs.publicreview.config.GooglePlacesProperties;
import com.brandPitara.sfs.publicreview.dto.*;
import com.brandPitara.sfs.publicreview.provider.ReviewPlaceProvider;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicReviewServiceImpl implements PublicReviewService {

    private static final String GOOGLE_SOURCE_LABEL = "Google Maps";
    private static final String PUBLIC_DISCLAIMER =
        "Google Maps reviews are public review samples and are not SFS-verified buyer complaints.";

    private static final String KEY_PROJECTS = "PROJECTS";
    private static final String KEY_BUILDERS = "BUILDERS";
    private static final String KEY_COMPANIES = "COMPANIES";

    private final PublicReviewPlaceRepository placeRepository;
    private final PublicReviewSummaryRepository summaryRepository;
    private final PublicReviewSampleRepository sampleRepository;
    private final ProjectReviewRepository projectReviewRepository;

    private final ProjectRepository projectRepository;
    private final BuilderRepository builderRepository;
    private final CompanyRepository companyRepository;
    private final ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;
    private final ReviewPlaceProvider reviewPlaceProvider;
    private final ContentVersionService contentVersionService;
    private final ExternalProviderTransactions externalProviderTransactions;
    private final GooglePlacesProperties googlePlacesProperties;

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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SyncGooglePublicReviewsResponse syncGoogleReviews(
        PublicReviewTargetType targetType,
        Long targetId,
        Long reviewPlaceId
    ) {
        GoogleSyncPreparation preparation = externalProviderTransactions.write(
            () -> prepareGoogleSync(targetType, targetId, reviewPlaceId)
        );
        if (preparation.existingResponse() != null) {
            return preparation.existingResponse();
        }

        GooglePlaceDetailsResponse googleResponse;
        try {
            googleResponse = reviewPlaceProvider.fetchPlaceDetails(preparation.googlePlaceId());
        } catch (RuntimeException ex) {
            markGoogleSyncFailed(reviewPlaceId, targetType, targetId, ex);
            throw ex;
        }

        try {
            return externalProviderTransactions.write(
                () -> persistGoogleSync(targetType, targetId, reviewPlaceId, googleResponse)
            );
        } catch (RuntimeException ex) {
            markGoogleSyncFailed(reviewPlaceId, targetType, targetId, ex);
            throw ex;
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GooglePlaceSearchResponse searchGooglePlaces(Long projectId, String query) {
        GoogleSearchPreparation preparation = externalProviderTransactions.read(() -> {
            ProjectEntity project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
            String resolvedQuery = StringUtils.hasText(query) ? query.trim() : project.getName();
            return new GoogleSearchPreparation(
                project.getId(), resolvedQuery, project.getLatitude(), project.getLongitude());
        });

        List<GooglePlaceSearchResultItem> results = reviewPlaceProvider.searchPlaces(
            preparation.query(),
            preparation.latitude(),
            preparation.longitude()
        );

        return GooglePlaceSearchResponse.builder()
            .projectId(preparation.projectId())
            .query(preparation.query())
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

        // Batched instead of one findByIdAndDeletedFalse per review (N+1):
        // a user viewing dozens of submitted reviews previously issued that
        // many individual project lookups.
        List<Long> projectIds = reviews.stream()
            .map(ProjectReviewEntity::getProjectId)
            .distinct()
            .toList();
        Map<Long, String> projectNamesById = projectRepository.findByIdInAndDeletedFalse(projectIds).stream()
            .collect(Collectors.toMap(ProjectEntity::getId, ProjectEntity::getName));

        return reviews.stream()
            .map(review -> PublicReviewMapper.toMySubmittedReviewResponse(
                review, projectNamesById.get(review.getProjectId())))
            .toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Checks whether this place still needs a Google fetch AND reserves the
     * attempt (fetchStatus -> FETCHING, fetchStartedAt -> now) atomically
     * under the place row's PESSIMISTIC_WRITE lock, before the Google call
     * ever happens. Concurrent admin sync requests for the same place are
     * serialized by that lock: the loser re-reads the winner's committed
     * FETCHING/FETCHED state here and is correctly short-circuited or
     * rejected, instead of both proceeding to call Google and both trying to
     * create the (unique-per-place) summary row.
     * <p>
     * A FETCHING reservation is a lease, not a permanent lock: if the process
     * crashes/restarts after committing FETCHING but before persisting a
     * result (success or failure), fetchStartedAt lets a later sync attempt
     * tell that apart from a genuinely in-flight one and reclaim it once
     * googlePlacesProperties.fetchLeaseSeconds has elapsed - otherwise the
     * row would be stuck rejecting every future sync attempt forever.
     */
    private GoogleSyncPreparation prepareGoogleSync(
        PublicReviewTargetType targetType,
        Long targetId,
        Long reviewPlaceId
    ) {
        validateTargetExistsForAdmin(targetType, targetId);
        PublicReviewPlaceEntity place = placeRepository
            .findByIdAndTargetTypeAndTargetIdAndDeletedFalseForUpdate(reviewPlaceId, targetType, targetId)
            .orElseThrow(() -> new NotFoundException("Review place not found: " + reviewPlaceId));
        PublicReviewSummaryEntity summary = summaryRepository.findByReviewPlaceId(place.getId()).orElse(null);

        if (isGoogleFetchCompleted(place, summary)) {
            return new GoogleSyncPreparation(
                place.getGooglePlaceId(),
                returnExistingGoogleFetch(place, summary, targetType)
            );
        }

        if (place.getFetchStatus() == GoogleReviewFetchStatus.FETCHING && !isReservationStale(place)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Google review sync is already in progress for this place"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        place.setFetchStatus(GoogleReviewFetchStatus.FETCHING);
        place.setFetchStartedAt(now);
        placeRepository.save(place);

        return new GoogleSyncPreparation(place.getGooglePlaceId(), null);
    }

    private boolean isReservationStale(PublicReviewPlaceEntity place) {
        OffsetDateTime startedAt = place.getFetchStartedAt();
        if (startedAt == null) {
            // FETCHING with no recorded start (shouldn't happen going forward,
            // but could for a row already FETCHING before this column
            // existed) - treat as immediately reclaimable rather than a
            // permanent lock with no way out.
            return true;
        }
        Duration lease = Duration.ofSeconds(googlePlacesProperties.getFetchLeaseSeconds());
        return Duration.between(startedAt, OffsetDateTime.now()).compareTo(lease) >= 0;
    }

    private SyncGooglePublicReviewsResponse persistGoogleSync(
        PublicReviewTargetType targetType,
        Long targetId,
        Long reviewPlaceId,
        GooglePlaceDetailsResponse googleResponse
    ) {
        PublicReviewPlaceEntity place = placeRepository
            .findByIdAndTargetTypeAndTargetIdAndDeletedFalse(reviewPlaceId, targetType, targetId)
            .orElseThrow(() -> new NotFoundException("Review place not found: " + reviewPlaceId));
        OffsetDateTime now = OffsetDateTime.now();

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

            samples.add(PublicReviewSampleEntity.builder()
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
                .build());
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

        place.setOneTimeFetched(true);
        place.setFetchStatus(GoogleReviewFetchStatus.FETCHED);
        place.setFetchStartedAt(null);
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
    }

    private void markGoogleSyncFailed(
        Long reviewPlaceId,
        PublicReviewTargetType targetType,
        Long targetId,
        RuntimeException originalFailure
    ) {
        try {
            externalProviderTransactions.write(() -> placeRepository
                .findByIdAndTargetTypeAndTargetIdAndDeletedFalse(reviewPlaceId, targetType, targetId)
                .ifPresent(place -> {
                    place.setFetchStatus(GoogleReviewFetchStatus.FAILED);
                    place.setFetchStartedAt(null);
                    placeRepository.save(place);
                }));
        } catch (RuntimeException statusFailure) {
            originalFailure.addSuppressed(statusFailure);
        }
    }

    private record GoogleSyncPreparation(
        String googlePlaceId,
        SyncGooglePublicReviewsResponse existingResponse
    ) {}

    private record GoogleSearchPreparation(
        Long projectId,
        String query,
        Double latitude,
        Double longitude
    ) {}

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

        if (targetType == PublicReviewTargetType.COMPANY) {
            companyRepository.findByIdAndDeletedFalse(targetId)
                .orElseThrow(() -> new NotFoundException("Company not found: " + targetId));
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

        if (targetType == PublicReviewTargetType.COMPANY) {
            CompanyEntity company = companyRepository.findByIdAndDeletedFalse(targetId)
                .orElseThrow(() -> new NotFoundException("Company not found: " + targetId));

            if (!Boolean.TRUE.equals(company.getPublished())
                || !Boolean.TRUE.equals(company.getActive())
                || Boolean.TRUE.equals(company.getDeleted())) {
                throw new NotFoundException("Company not found: " + targetId);
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
        } else if (targetType == PublicReviewTargetType.COMPANY) {
            contentVersionService.bump(KEY_COMPANIES);
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
