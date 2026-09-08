package com.brandPitara.sfs.cms.workflow.service.impl;

import com.brandPitara.sfs.cms.content.document.CmsMediaReferenceService;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.security.CmsContentAccessPolicy;
import com.brandPitara.sfs.cms.workflow.domain.*;
import com.brandPitara.sfs.cms.workflow.dto.*;
import com.brandPitara.sfs.cms.workflow.entity.*;
import com.brandPitara.sfs.cms.workflow.repository.*;
import com.brandPitara.sfs.cms.workflow.service.*;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.workflow.domain.ContentTagSnapshot;

import static com.brandPitara.sfs.cms.content.domain.ContentStatus.*;

@Service
@RequiredArgsConstructor
public class ContentWorkflowServiceImpl implements ContentWorkflowService {

    private final ContentPostRepository postRepository;
    private final ContentPostRevisionRepository revisionRepository;
    private final ContentReviewActivityRepository activityRepository;
    private final DashboardUserRepository userRepository;
    private final DashboardCurrentUserService currentUserService;
    private final CmsContentAccessPolicy accessPolicy;
    private final ContentWorkflowTransitionPolicy transitionPolicy;
    private final ContentReviewReadinessValidator readinessValidator;
    private final CmsMediaReferenceService mediaReferenceService;

    @Override
    @Transactional
    public ContentWorkflowResponse submit(
            Long contentId, SubmitReviewRequest request, Authentication authentication
    ) {
        ContentPostEntity post = lockedPost(contentId);
        accessPolicy.assertCanSubmitForReview(authentication, post.getContentOwner().getId());
        checkVersion(post, request.version());
        transitionPolicy.require(post.getStatus(), IN_REVIEW);
        readinessValidator.validate(post);

        DashboardUserEntity actor = actorReference();
        int revisionNumber = revisionRepository.findMaximumRevisionNumber(contentId) + 1;
        ContentPostRevisionEntity revision = revisionRepository.save(snapshot(
                post, actor, revisionNumber, request.version()
        ));
        post.setCurrentReviewRevision(revision);
        post.setApprovedRevision(null);
        post.setCurrentPublishedRevision(null);
        post.setStatus(IN_REVIEW);
        post.setUpdatedBy(actor);
        activity(post, revision, ContentWorkflowAction.SUBMITTED_FOR_REVIEW, null, actor);
        return save(post);
    }

    @Override
    @Transactional
    public ContentWorkflowResponse requestChanges(
            Long contentId, RequestChangesRequest request, Authentication authentication
    ) {
        ContentPostEntity post = lockedPost(contentId);
        accessPolicy.assertCanReview(authentication);
        checkVersion(post, request.version());
        transitionPolicy.require(post.getStatus(), CHANGES_REQUESTED);
        ContentPostRevisionEntity revision = requireReviewRevision(post);
        DashboardUserEntity actor = actorReference();
        post.setStatus(CHANGES_REQUESTED);
        post.setApprovedRevision(null);
        post.setUpdatedBy(actor);
        activity(post, revision, ContentWorkflowAction.CHANGES_REQUESTED,
                normalizeRequiredComment(request.comment()), actor);
        return save(post);
    }

    @Override
    @Transactional
    public ContentWorkflowResponse approve(
            Long contentId, ApproveContentRequest request, Authentication authentication
    ) {
        ContentPostEntity post = lockedPost(contentId);
        accessPolicy.assertCanReview(authentication);
        checkVersion(post, request.version());
        transitionPolicy.require(post.getStatus(), APPROVED);
        ContentPostRevisionEntity revision = requireReviewRevision(post);
        DashboardUserEntity actor = actorReference();
        post.setApprovedRevision(revision);
        post.setStatus(APPROVED);
        post.setUpdatedBy(actor);
        activity(post, revision, ContentWorkflowAction.APPROVED, null, actor);
        return save(post);
    }

    @Override
    @Transactional
    public ContentWorkflowResponse publish(
            Long contentId, PublishContentRequest request, Authentication authentication
    ) {
        ContentPostEntity post = lockedPost(contentId);
        accessPolicy.assertCanPublish(authentication);
        checkVersion(post, request.version());
        transitionPolicy.require(post.getStatus(), PUBLISHED);
        ContentPostRevisionEntity approved = requireApprovedRevision(post);
        try {
            Map<Long, CmsMediaType> additional = new LinkedHashMap<>();
            if (approved.getCoverMediaAssetId() != null) additional.put(approved.getCoverMediaAssetId(), CmsMediaType.IMAGE);
            if (approved.getPublicAuthorProfileMediaAssetId() != null) additional.put(approved.getPublicAuthorProfileMediaAssetId(), CmsMediaType.IMAGE);
            if (additional.isEmpty()) mediaReferenceService.validateAndResolve(approved.getContentDocument());
            else mediaReferenceService.validateAndResolve(approved.getContentDocument(), additional);
        } catch (CmsContentApiException dependencyFailure) {
            throw CmsContentApiException.publishDependencyInvalid();
        }

        DashboardUserEntity actor = actorReference();
        post.setCurrentPublishedRevision(approved);
        post.setPublishedAt(OffsetDateTime.now());
        post.setPublishedBy(actor);
        post.setStatus(PUBLISHED);
        post.setUpdatedBy(actor);
        activity(post, approved, ContentWorkflowAction.PUBLISHED, null, actor);
        return save(post);
    }

    @Override
    @Transactional
    public ContentWorkflowResponse unpublish(
            Long contentId, UnpublishContentRequest request, Authentication authentication
    ) {
        ContentPostEntity post = lockedPost(contentId);
        accessPolicy.assertCanUnpublish(authentication);
        checkVersion(post, request.version());
        transitionPolicy.require(post.getStatus(), UNPUBLISHED);
        ContentPostRevisionEntity published = requirePublishedRevision(post);
        DashboardUserEntity actor = actorReference();
        post.setCurrentPublishedRevision(null);
        post.setStatus(UNPUBLISHED);
        post.setUpdatedBy(actor);
        activity(post, published, ContentWorkflowAction.UNPUBLISHED, null, actor);
        return save(post);
    }

    @Override
    @Transactional
    public ContentWorkflowResponse archive(
            Long contentId, ArchiveContentRequest request, Authentication authentication
    ) {
        ContentPostEntity post = lockedPost(contentId);
        accessPolicy.assertCanArchive(authentication);
        checkVersion(post, request.version());
        transitionPolicy.require(post.getStatus(), ARCHIVED);
        DashboardUserEntity actor = actorReference();
        ContentPostRevisionEntity revision = post.getApprovedRevision() != null
                ? post.getApprovedRevision() : post.getCurrentReviewRevision();
        post.setStatus(ARCHIVED);
        post.setUpdatedBy(actor);
        activity(post, revision, ContentWorkflowAction.ARCHIVED, null, actor);
        return save(post);
    }

    /**
     * UNPUBLISHED -> DRAFT (D7): reopens content that was previously approved/published/
     * unpublished so it can be edited again. Reuses the CMS_CONTENT_UNPUBLISH permission —
     * the symmetric counterpart of the action that got the content into UNPUBLISHED in the
     * first place, not a plain editing permission. Clears the review/approval pointers back
     * to their pre-submission state (mirroring a never-yet-submitted DRAFT exactly) since
     * neither an active review nor an approval exists once this content is back in draft;
     * the next submit() overwrites them fresh regardless. currentPublishedRevisionId is
     * already null from unpublish(). publishedAt/publishedByDashboardUserId are left as-is —
     * historical "this was once published" facts that no status-specific CHECK constraint
     * or read path requires clearing for DRAFT. Creates no revision and mutates no existing
     * revision row; ContentPostRevisionEntity has no setters and is DB-trigger-immutable.
     */
    @Override
    @Transactional
    public ContentWorkflowResponse reopen(
            Long contentId, ReopenContentRequest request, Authentication authentication
    ) {
        ContentPostEntity post = lockedPost(contentId);
        accessPolicy.assertCanUnpublish(authentication);
        checkVersion(post, request.version());
        transitionPolicy.require(post.getStatus(), DRAFT);
        ContentPostRevisionEntity lastRevision = post.getApprovedRevision() != null
                ? post.getApprovedRevision() : post.getCurrentReviewRevision();
        DashboardUserEntity actor = actorReference();
        post.setApprovedRevision(null);
        post.setCurrentReviewRevision(null);
        post.setStatus(DRAFT);
        post.setUpdatedBy(actor);
        activity(post, lastRevision, ContentWorkflowAction.REOPENED, null, actor);
        return save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentRevisionListResponse> revisions(
            Long contentId, Pageable pageable, Authentication authentication
    ) {
        ContentPostEntity post = readablePost(contentId, authentication);
        return revisionRepository.findListByPostId(post.getId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentRevisionDetailResponse revision(
            Long contentId, Long revisionId, Authentication authentication
    ) {
        readablePost(contentId, authentication);
        ContentPostRevisionEntity revision = revisionRepository.findDetailed(contentId, revisionId)
                .orElseThrow(() -> CmsContentApiException.revisionNotFound(revisionId));
        Map<Long, CmsMediaType> additional = new LinkedHashMap<>();
        if (revision.getCoverMediaAssetId() != null) additional.put(revision.getCoverMediaAssetId(), CmsMediaType.IMAGE);
        if (revision.getPublicAuthorProfileMediaAssetId() != null) additional.put(revision.getPublicAuthorProfileMediaAssetId(), CmsMediaType.IMAGE);
        Map<Long, com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity> media = additional.isEmpty()
                ? mediaReferenceService.validateAndResolve(revision.getContentDocument())
                : mediaReferenceService.validateAndResolve(revision.getContentDocument(), additional);
        return ContentRevisionDetailResponse.from(
                revision, mediaReferenceService.createPreviewMap(media)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentWorkflowActivityResponse> history(
            Long contentId, Pageable pageable, Authentication authentication
    ) {
        ContentPostEntity post = readablePost(contentId, authentication);
        return activityRepository.findHistoryByPostId(post.getId(), pageable);
    }

    private ContentPostRevisionEntity snapshot(
            ContentPostEntity post,
            DashboardUserEntity actor,
            int revisionNumber,
            long sourceVersion
    ) {
        return ContentPostRevisionEntity.builder()
                .contentPost(post)
                .revisionNumber(revisionNumber)
                .contentType(post.getContentType())
                .title(post.getTitle())
                .slug(post.getSlug())
                .excerpt(post.getExcerpt())
                .readingTimeMinutes(post.getReadingTimeMinutes())
                .publicAuthorId(post.getPublicAuthor() == null ? null : post.getPublicAuthor().getId())
                .publicAuthorName(post.getPublicAuthor() == null ? null : post.getPublicAuthor().getDisplayName())
                .publicAuthorSlug(post.getPublicAuthor() == null ? null : post.getPublicAuthor().getSlug())
                .publicAuthorDesignation(post.getPublicAuthor() == null ? null : post.getPublicAuthor().getDesignation())
                .publicAuthorProfileMediaAssetId(post.getPublicAuthor() == null || post.getPublicAuthor().getProfileMediaAsset() == null
                        ? null : post.getPublicAuthor().getProfileMediaAsset().getId())
                .categoryId(post.getCategory() == null ? null : post.getCategory().getId())
                .categoryName(post.getCategory() == null ? null : post.getCategory().getName())
                .categorySlug(post.getCategory() == null ? null : post.getCategory().getSlug())
                .tagSnapshots(post.getTags().stream().sorted(java.util.Comparator.comparing(tag -> tag.getId()))
                        .map(tag -> new ContentTagSnapshot(tag.getId(), tag.getName(), tag.getSlug())).toList())
                .coverMediaAssetId(post.getCoverMediaAsset() == null ? null : post.getCoverMediaAsset().getId())
                .coverAltText(post.getCoverAltText())
                .seoTitle(post.getSeoTitle())
                .seoDescription(post.getSeoDescription())
                .canonicalUrl(post.getCanonicalUrl())
                .robotsIndex(post.getRobotsIndex())
                .robotsFollow(post.getRobotsFollow())
                .contentDocument(post.getContentDocument())
                .contentDocumentSchemaVersion(post.getContentDocumentSchemaVersion())
                .createdFromPostVersion(sourceVersion)
                .createdBy(actor)
                .revisionReason(ContentRevisionReason.REVIEW_SUBMISSION)
                .build();
    }

    private void activity(
            ContentPostEntity post,
            ContentPostRevisionEntity revision,
            ContentWorkflowAction action,
            String comment,
            DashboardUserEntity actor
    ) {
        activityRepository.save(ContentReviewActivityEntity.builder()
                .contentPost(post)
                .revision(revision)
                .action(action)
                .comment(comment)
                .actor(actor)
                .build());
    }

    private ContentWorkflowResponse save(ContentPostEntity post) {
        try {
            return ContentWorkflowResponse.from(postRepository.saveAndFlush(post));
        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException exception) {
            throw CmsContentApiException.versionConflict();
        } catch (DataIntegrityViolationException exception) {
            throw CmsContentApiException.workflowConflict();
        }
    }

    private ContentPostEntity lockedPost(Long contentId) {
        return postRepository.findDetailedByIdForUpdate(contentId)
                .orElseThrow(() -> CmsContentApiException.notFound(contentId));
    }

    private ContentPostEntity readablePost(Long contentId, Authentication authentication) {
        ContentPostEntity post = postRepository.findDetailedById(contentId)
                .orElseThrow(() -> CmsContentApiException.notFound(contentId));
        accessPolicy.assertCanView(authentication, post.getContentOwner().getId());
        return post;
    }

    private void checkVersion(ContentPostEntity post, Long expected) {
        if (!Objects.equals(post.getVersion(), expected)) throw CmsContentApiException.versionConflict();
    }

    private DashboardUserEntity actorReference() {
        DashboardUserEntity actor = currentUserService.getCurrentUserOrThrow();
        return userRepository.getReferenceById(actor.getId());
    }

    private ContentPostRevisionEntity requireReviewRevision(ContentPostEntity post) {
        if (post.getCurrentReviewRevision() == null) throw CmsContentApiException.reviewRevisionMissing();
        return post.getCurrentReviewRevision();
    }

    private ContentPostRevisionEntity requireApprovedRevision(ContentPostEntity post) {
        if (post.getApprovedRevision() == null) throw CmsContentApiException.approvedRevisionMissing();
        return post.getApprovedRevision();
    }

    private ContentPostRevisionEntity requirePublishedRevision(ContentPostEntity post) {
        if (post.getCurrentPublishedRevision() == null) throw CmsContentApiException.publishedRevisionMissing();
        return post.getCurrentPublishedRevision();
    }

    private String normalizeRequiredComment(String value) {
        if (value == null) throw CmsContentApiException.reviewCommentRequired();
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isEmpty()) throw CmsContentApiException.reviewCommentRequired();
        if (normalized.length() > 4000 || normalized.codePoints()
                .anyMatch(character -> Character.isISOControl(character)
                        && character != '\n' && character != '\t')) {
            throw CmsContentApiException.reviewCommentInvalid();
        }
        return normalized;
    }
}
