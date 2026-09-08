package com.brandPitara.sfs.cms.workflow.service.impl;

import com.brandPitara.sfs.cms.content.domain.*;
import com.brandPitara.sfs.cms.content.document.*;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.security.*;
import com.brandPitara.sfs.cms.workflow.domain.ContentWorkflowAction;
import com.brandPitara.sfs.cms.workflow.dto.*;
import com.brandPitara.sfs.cms.workflow.entity.*;
import com.brandPitara.sfs.cms.workflow.repository.*;
import com.brandPitara.sfs.cms.workflow.service.*;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.*;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import com.brandPitara.sfs.cms.author.entity.CmsPublicAuthorEntity;
import com.brandPitara.sfs.cms.taxonomy.entity.CmsContentCategoryEntity;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentWorkflowServiceImplTest {

    @Mock private ContentPostRepository postRepository;
    @Mock private ContentPostRevisionRepository revisionRepository;
    @Mock private ContentReviewActivityRepository activityRepository;
    @Mock private DashboardUserRepository userRepository;
    @Mock private DashboardCurrentUserService currentUserService;
    @Mock private ContentReviewReadinessValidator readinessValidator;
    @Mock private CmsMediaReferenceService mediaReferenceService;

    private ContentWorkflowServiceImpl service;
    private DashboardUserEntity writer;

    @BeforeEach
    void setUp() {
        service = new ContentWorkflowServiceImpl(
                postRepository, revisionRepository, activityRepository, userRepository,
                currentUserService, new CmsContentAccessPolicy(),
                new ContentWorkflowTransitionPolicy(), readinessValidator, mediaReferenceService
        );
        writer = user(11L);
        lenient().when(currentUserService.getCurrentUserOrThrow()).thenReturn(writer);
        lenient().when(userRepository.getReferenceById(anyLong())).thenReturn(writer);
        lenient().when(revisionRepository.save(any())).thenAnswer(invocation -> {
            ContentPostRevisionEntity revision = invocation.getArgument(0);
            ReflectionTestUtils.setField(revision, "id", 1000L + revision.getRevisionNumber());
            return revision;
        });
        lenient().when(postRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ContentPostEntity post = invocation.getArgument(0);
            post.setVersion(post.getVersion() + 1);
            post.setUpdatedAt(OffsetDateTime.now());
            return post;
        });
    }

    @Test
    void writerSubmitsOwnDraftAndCreatesExactImmutableSnapshot() {
        ContentPostEntity post = post(ContentStatus.DRAFT, 4L);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));
        when(revisionRepository.findMaximumRevisionNumber(50L)).thenReturn(1);

        ContentWorkflowResponse response = service.submit(50L, new SubmitReviewRequest(4L), writerAuth(11L));

        ArgumentCaptor<ContentPostRevisionEntity> revision = ArgumentCaptor.forClass(ContentPostRevisionEntity.class);
        verify(revisionRepository).save(revision.capture());
        assertThat(revision.getValue().getRevisionNumber()).isEqualTo(2);
        assertThat(revision.getValue().getTitle()).isEqualTo(post.getTitle());
        assertThat(revision.getValue().getSlug()).isEqualTo(post.getSlug());
        assertThat(revision.getValue().getContentDocument()).isEqualTo(post.getContentDocument());
        assertThat(revision.getValue().getCreatedFromPostVersion()).isEqualTo(4L);
        assertThat(revision.getValue().getPublicAuthorName()).isEqualTo("Author");
        assertThat(revision.getValue().getCategorySlug()).isEqualTo("guides");
        assertThat(revision.getValue().getCoverMediaAssetId()).isEqualTo(90L);
        assertThat(revision.getValue().getCoverAltText()).isEqualTo("Article cover");
        assertThat(response.status()).isEqualTo(ContentStatus.IN_REVIEW);
        assertThat(response.currentReviewRevisionId()).isEqualTo(1002L);
        assertThat(response.approvedRevisionId()).isNull();
        verify(readinessValidator).validate(post);
        verify(activityRepository).save(argThat(activity ->
                activity.getAction() == ContentWorkflowAction.SUBMITTED_FOR_REVIEW
                        && activity.getRevision().getId().equals(1002L)));
    }

    @Test
    void submitSnapshotsReadingTimeMinutesAndOlderRevisionKeepsItsOwnValueAfterPostIsEdited() {
        ContentPostEntity post = post(ContentStatus.DRAFT, 4L);
        post.setReadingTimeMinutes(5);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));
        when(revisionRepository.findMaximumRevisionNumber(50L)).thenReturn(0);

        service.submit(50L, new SubmitReviewRequest(4L), writerAuth(11L));

        ArgumentCaptor<ContentPostRevisionEntity> firstRevision = ArgumentCaptor.forClass(ContentPostRevisionEntity.class);
        verify(revisionRepository).save(firstRevision.capture());
        assertThat(firstRevision.getValue().getReadingTimeMinutes()).isEqualTo(5);

        // The editor later changes reading time on the mutable post; the already-created revision
        // must keep showing its own original value forever (D5 Step 39 style immutability).
        post.setReadingTimeMinutes(8);
        assertThat(firstRevision.getValue().getReadingTimeMinutes()).isEqualTo(5);
    }

    @Test
    void submitSnapshotsASchemaVersion3DocumentExactlyAsStored() {
        ContentPostEntity post = post(ContentStatus.DRAFT, 4L);
        post.setContentDocument(new ContentDocument(3, List.of(new ContentBlock.Callout(
                CalloutVariant.VERDICT, "SFS Verdict",
                List.of(new InlineNode.Text("Strong pick for this budget.", List.of()))
        ))));
        post.setContentDocumentSchemaVersion((short) 3);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));
        when(revisionRepository.findMaximumRevisionNumber(50L)).thenReturn(1);

        service.submit(50L, new SubmitReviewRequest(4L), writerAuth(11L));

        ArgumentCaptor<ContentPostRevisionEntity> revision = ArgumentCaptor.forClass(ContentPostRevisionEntity.class);
        verify(revisionRepository).save(revision.capture());
        assertThat(revision.getValue().getContentDocument()).isEqualTo(post.getContentDocument());
        assertThat(revision.getValue().getContentDocument().schemaVersion()).isEqualTo(3);
        assertThat(revision.getValue().getContentDocumentSchemaVersion()).isEqualTo((short) 3);
    }

    @Test
    void otherWriterAndPublisherCannotSubmitAndStaleVersionCannotSnapshot() {
        ContentPostEntity post = post(ContentStatus.DRAFT, 4L);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.submit(50L, new SubmitReviewRequest(4L), writerAuth(12L)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.submit(50L, new SubmitReviewRequest(4L), publisherAuth(31L)))
                .isInstanceOf(AccessDeniedException.class);
        assertCode(() -> service.submit(50L, new SubmitReviewRequest(3L), writerAuth(11L)),
                "CONTENT_VERSION_CONFLICT");
        verify(revisionRepository, never()).save(any());
    }

    @Test
    void editorRequestsChangesWithPersistedBoundedCommentAndWriterCannot() {
        ContentPostRevisionEntity revision = revision(1, 1001L);
        ContentPostEntity post = post(ContentStatus.IN_REVIEW, 5L);
        post.setCurrentReviewRevision(revision);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));

        var response = service.requestChanges(50L,
                new RequestChangesRequest(5L, "  Rewrite pricing.\r\nAdd sources.  "), editorAuth(21L));

        assertThat(response.status()).isEqualTo(ContentStatus.CHANGES_REQUESTED);
        verify(activityRepository).save(argThat(activity ->
                "Rewrite pricing.\nAdd sources.".equals(activity.getComment())
                        && activity.getRevision() == revision));

        post.setStatus(ContentStatus.IN_REVIEW);
        post.setVersion(6L);
        assertThatThrownBy(() -> service.requestChanges(50L,
                new RequestChangesRequest(6L, "No"), writerAuth(11L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resubmissionCreatesNewRevisionWithoutMutatingRejectedRevision() {
        ContentPostRevisionEntity oldRevision = revision(1, 1001L);
        ContentPostEntity post = post(ContentStatus.CHANGES_REQUESTED, 8L);
        post.setCurrentReviewRevision(oldRevision);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));
        when(revisionRepository.findMaximumRevisionNumber(50L)).thenReturn(1);

        service.submit(50L, new SubmitReviewRequest(8L), writerAuth(11L));

        assertThat(post.getCurrentReviewRevision().getId()).isEqualTo(1002L);
        assertThat(oldRevision.getRevisionNumber()).isEqualTo(1);
        verify(revisionRepository, never()).save(oldRevision);
    }

    @Test
    void editorApprovesExactCurrentRevisionAndWriterCannotApprove() {
        ContentPostRevisionEntity revision = revision(3, 1003L);
        ContentPostEntity post = post(ContentStatus.IN_REVIEW, 9L);
        post.setCurrentReviewRevision(revision);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));

        var response = service.approve(50L, new ApproveContentRequest(9L), editorAuth(21L));
        assertThat(response.status()).isEqualTo(ContentStatus.APPROVED);
        assertThat(response.approvedRevisionId()).isEqualTo(1003L);

        post.setStatus(ContentStatus.IN_REVIEW);
        post.setApprovedRevision(null);
        post.setVersion(10L);
        assertThatThrownBy(() -> service.approve(50L, new ApproveContentRequest(10L), writerAuth(11L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void publisherPublishesApprovedSnapshotNotMutablePostAndDependenciesAreRechecked() {
        ContentPostRevisionEntity approved = revision(2, 1002L);
        ContentPostEntity post = post(ContentStatus.APPROVED, 10L);
        post.setTitle("Mutable changed title");
        post.setCurrentReviewRevision(approved);
        post.setApprovedRevision(approved);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));
        when(mediaReferenceService.validateAndResolve(approved.getContentDocument())).thenReturn(Map.of());

        var response = service.publish(50L, new PublishContentRequest(10L), publisherAuth(31L));

        assertThat(response.status()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(response.currentPublishedRevisionId()).isEqualTo(1002L);
        assertThat(response.publishedAt()).isNotNull();
        assertThat(approved.getTitle()).isEqualTo("Revision title");
        verify(mediaReferenceService).validateAndResolve(approved.getContentDocument());
    }

    @Test
    void publishRejectsInvalidDependencyAndNonPublishers() {
        ContentPostRevisionEntity approved = revision(2, 1002L);
        ContentPostEntity post = post(ContentStatus.APPROVED, 10L);
        post.setCurrentReviewRevision(approved);
        post.setApprovedRevision(approved);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));
        when(mediaReferenceService.validateAndResolve(any()))
                .thenThrow(CmsContentApiException.mediaNotReady(44L));

        assertCode(() -> service.publish(50L, new PublishContentRequest(10L), publisherAuth(31L)),
                "CONTENT_PUBLISH_DEPENDENCY_INVALID");
        assertThatThrownBy(() -> service.publish(50L, new PublishContentRequest(10L), editorAuth(21L)))
                .isInstanceOf(AccessDeniedException.class);
        verify(postRepository, never()).saveAndFlush(any());
    }

    @Test
    void unpublishClearsOnlyCurrentPointerAndRepublishRestoresSameApprovedRevision() {
        ContentPostRevisionEntity revision = revision(2, 1002L);
        ContentPostEntity post = post(ContentStatus.PUBLISHED, 11L);
        post.setCurrentReviewRevision(revision);
        post.setApprovedRevision(revision);
        post.setCurrentPublishedRevision(revision);
        post.setPublishedAt(OffsetDateTime.parse("2026-08-18T10:00:00+05:30"));
        post.setPublishedBy(writer);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));

        var unpublished = service.unpublish(50L, new UnpublishContentRequest(11L), publisherAuth(31L));
        assertThat(unpublished.status()).isEqualTo(ContentStatus.UNPUBLISHED);
        assertThat(unpublished.currentPublishedRevisionId()).isNull();
        assertThat(unpublished.approvedRevisionId()).isEqualTo(1002L);
        assertThat(unpublished.publishedAt()).isNotNull();

        when(mediaReferenceService.validateAndResolve(any())).thenReturn(Map.of());
        var republished = service.publish(50L, new PublishContentRequest(12L), publisherAuth(31L));
        assertThat(republished.currentPublishedRevisionId()).isEqualTo(1002L);
    }

    @Test
    void publisherReopensUnpublishedToDraftClearingReviewPointersButKeepingPublishHistory() {
        ContentPostRevisionEntity revision = revision(1, 1001L);
        ContentPostEntity post = post(ContentStatus.UNPUBLISHED, 9L);
        post.setCurrentReviewRevision(revision);
        post.setApprovedRevision(revision);
        post.setCurrentPublishedRevision(null); // already cleared by an earlier unpublish()
        OffsetDateTime publishedAt = OffsetDateTime.parse("2026-08-18T10:00:00+05:30");
        post.setPublishedAt(publishedAt);
        post.setPublishedBy(writer);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));

        var response = service.reopen(50L, new ReopenContentRequest(9L), publisherAuth(31L));

        assertThat(response.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(response.currentReviewRevisionId()).isNull();
        assertThat(response.approvedRevisionId()).isNull();
        assertThat(response.currentPublishedRevisionId()).isNull();
        // Historical "this was published" facts survive the reopen - not a status-gated field.
        assertThat(post.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(post.getPublishedBy()).isEqualTo(writer);
        verify(activityRepository).save(argThat(activity ->
                activity.getAction() == ContentWorkflowAction.REOPENED && activity.getRevision() == revision));
        // No revision is created or touched merely by reopening (D7 Step 8/10).
        verify(revisionRepository, never()).save(any());
        verifyNoInteractions(readinessValidator);
    }

    @Test
    void reopenRejectsFromEveryStatusExceptUnpublishedWithoutMutatingOrTouchingRevisions() {
        for (ContentStatus ineligible : java.util.EnumSet.complementOf(java.util.EnumSet.of(ContentStatus.UNPUBLISHED))) {
            ContentPostEntity post = post(ineligible, 5L);
            when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));

            assertCode(() -> service.reopen(50L, new ReopenContentRequest(5L), publisherAuth(31L)),
                    "CONTENT_WORKFLOW_INVALID_TRANSITION");
        }
        verify(postRepository, never()).saveAndFlush(any());
        verify(revisionRepository, never()).save(any());
        verify(activityRepository, never()).save(any());
    }

    @Test
    void reopenRejectsStaleVersionWithoutMutating() {
        ContentPostEntity post = post(ContentStatus.UNPUBLISHED, 9L);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));

        assertCode(() -> service.reopen(50L, new ReopenContentRequest(8L), publisherAuth(31L)),
                "CONTENT_VERSION_CONFLICT");
        verify(postRepository, never()).saveAndFlush(any());
    }

    @Test
    void reopenRequiresUnpublishPermissionNotJustEditPermission() {
        ContentPostEntity post = post(ContentStatus.UNPUBLISHED, 9L);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.reopen(50L, new ReopenContentRequest(9L), writerAuth(11L)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.reopen(50L, new ReopenContentRequest(9L), editorAuth(21L)))
                .isInstanceOf(AccessDeniedException.class);
        verify(postRepository, never()).saveAndFlush(any());
    }

    @Test
    void publishedContentMustBeUnpublishedBeforeArchive() {
        ContentPostEntity post = post(ContentStatus.PUBLISHED, 11L);
        when(postRepository.findDetailedByIdForUpdate(50L)).thenReturn(Optional.of(post));
        assertCode(() -> service.archive(50L, new ArchiveContentRequest(11L), publisherAuth(31L)),
                "CONTENT_WORKFLOW_INVALID_TRANSITION");
    }

    @Test
    void revisionListIsMetadataOnlyAndDetailBulkResolvesMediaOnce() {
        ContentPostEntity post = post(ContentStatus.IN_REVIEW, 5L);
        ContentPostRevisionEntity revision = revision(1, 1001L);
        when(postRepository.findDetailedById(50L)).thenReturn(Optional.of(post));
        var pageable = PageRequest.of(0, 20);
        ContentRevisionListResponse summary = new ContentRevisionListResponse(
                1001L, 1, ContentType.ARTICLE, "Revision title", "revision-title",
                4L, com.brandPitara.sfs.cms.workflow.domain.ContentRevisionReason.REVIEW_SUBMISSION,
                11L, "User 11", OffsetDateTime.now()
        );
        when(revisionRepository.findListByPostId(50L, pageable))
                .thenReturn(new PageImpl<>(List.of(summary), pageable, 1));

        assertThat(service.revisions(50L, pageable, writerAuth(11L)).getContent())
                .containsExactly(summary);

        when(revisionRepository.findDetailed(50L, 1001L)).thenReturn(Optional.of(revision));
        when(mediaReferenceService.validateAndResolve(revision.getContentDocument())).thenReturn(Map.of());
        when(mediaReferenceService.createPreviewMap(Map.of())).thenReturn(Map.of());

        ContentRevisionDetailResponse detail = service.revision(50L, 1001L, writerAuth(11L));

        assertThat(detail.document()).isEqualTo(revision.getContentDocument());
        assertThat(detail.readingTimeMinutes()).isEqualTo(6);
        verify(mediaReferenceService, times(1)).validateAndResolve(revision.getContentDocument());
        verify(revisionRepository).findListByPostId(50L, pageable);
        verify(revisionRepository).findDetailed(50L, 1001L);
    }

    private ContentPostEntity post(ContentStatus status, long version) {
        return ContentPostEntity.builder()
                .id(50L).contentType(ContentType.ARTICLE).status(status)
                .title("Mutable title").slug("mutable-title").excerpt("Summary")
                .publicAuthor(CmsPublicAuthorEntity.builder().id(3L).displayName("Author").slug("author").active(true).build())
                .category(CmsContentCategoryEntity.builder().id(4L).name("Guides").slug("guides").active(true).build())
                .coverMediaAsset(CmsMediaAssetEntity.builder().id(90L).mediaType(CmsMediaType.IMAGE)
                        .status(CmsMediaStatus.READY).build()).coverAltText("Article cover")
                .contentOwner(writer).createdBy(writer).updatedBy(writer)
                .robotsIndex(true).robotsFollow(true)
                .contentDocument(new ContentDocument(2, List.of(new ContentBlock.Paragraph(List.of(
                        new InlineNode.Text("Meaningful article", List.of())
                )))))
                .contentDocumentSchemaVersion((short) 2).version(version).build();
    }

    private ContentPostRevisionEntity revision(int number, long id) {
        return ContentPostRevisionEntity.builder()
                .id(id).contentPost(post(ContentStatus.DRAFT, 1L)).revisionNumber(number)
                .contentType(ContentType.ARTICLE).title("Revision title").slug("revision-title")
                .readingTimeMinutes(6)
                .robotsIndex(true).robotsFollow(true)
                .contentDocument(ContentDocument.empty())
                .contentDocumentSchemaVersion((short) ContentDocument.CURRENT_SCHEMA_VERSION)
                .createdFromPostVersion(1L).createdBy(writer)
                .revisionReason(com.brandPitara.sfs.cms.workflow.domain.ContentRevisionReason.REVIEW_SUBMISSION)
                .build();
    }

    private DashboardUserEntity user(Long id) {
        return DashboardUserEntity.builder().id(id).email("user" + id + "@example.com")
                .name("User " + id).passwordHash("unused").role(DashboardRole.CONTENT_STAFF)
                .active(true).permissions(new LinkedHashSet<>()).build();
    }

    private Authentication writerAuth(Long id) {
        return authentication(id, CmsPermissionProfile.WRITER.permissions());
    }

    private Authentication editorAuth(Long id) {
        return authentication(id, CmsPermissionProfile.EDITOR.permissions());
    }

    private Authentication publisherAuth(Long id) {
        return authentication(id, CmsPermissionProfile.PUBLISHER.permissions());
    }

    private Authentication authentication(Long id, Set<DashboardPermission> permissions) {
        DashboardUserDetails details = new DashboardUserDetails(new DashboardAuthenticationUserSnapshot(
                id, "user" + id + "@example.com", "User " + id,
                DashboardRole.CONTENT_STAFF, true, permissions
        ));
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThatThrownBy(callable).isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode()).isEqualTo(code);
    }
}
