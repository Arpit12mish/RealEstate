package com.brandPitara.sfs.cms.content.service.impl;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.dto.ContentPostCreateRequest;
import com.brandPitara.sfs.cms.content.dto.ContentPostUpdateRequest;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.content.service.ContentMetadataNormalizer;
import com.brandPitara.sfs.cms.content.slug.ContentSlugServiceImpl;
import com.brandPitara.sfs.cms.security.CmsContentAccessPolicy;
import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentPostServiceImplTest {

    @Mock private ContentPostRepository contentPostRepository;
    @Mock private DashboardUserRepository dashboardUserRepository;
    @Mock private DashboardCurrentUserService currentUserService;

    private ContentPostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ContentPostServiceImpl(
                contentPostRepository,
                dashboardUserRepository,
                currentUserService,
                new CmsContentAccessPolicy(),
                new ContentSlugServiceImpl(contentPostRepository),
                new ContentMetadataNormalizer(),
                new com.brandPitara.sfs.cms.workflow.service.ContentWorkflowTransitionPolicy()
        );
    }

    @Test
    void writerCreatesDraftWithServerDerivedOwnerAndAuditActors() {
        DashboardUserEntity writer = user(11L, DashboardRole.CONTENT_STAFF);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(writer);
        when(dashboardUserRepository.getReferenceById(11L)).thenReturn(writer);
        when(contentPostRepository.existsBySlug("gurgaon-market-guide")).thenReturn(false);
        when(contentPostRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ContentPostEntity post = invocation.getArgument(0);
            post.setId(101L);
            post.setVersion(0L);
            post.setCreatedAt(OffsetDateTime.now());
            post.setUpdatedAt(OffsetDateTime.now());
            return post;
        });

        var response = service.create(createRequest(null), writerAuth(11L));

        assertThat(response.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(response.slug()).isEqualTo("gurgaon-market-guide");
        assertThat(response.contentOwnerDashboardUserId()).isEqualTo(11L);
        assertThat(response.createdByDashboardUserId()).isEqualTo(11L);
        assertThat(response.updatedByDashboardUserId()).isEqualTo(11L);
        assertThat(response.robotsIndex()).isTrue();
        assertThat(response.robotsFollow()).isTrue();
    }

    @Test
    void createPersistsReadingTimeMinutes() {
        DashboardUserEntity writer = user(11L, DashboardRole.CONTENT_STAFF);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(writer);
        when(dashboardUserRepository.getReferenceById(11L)).thenReturn(writer);
        when(contentPostRepository.existsBySlug("gurgaon-market-guide")).thenReturn(false);
        when(contentPostRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ContentPostEntity post = invocation.getArgument(0);
            post.setId(105L);
            post.setVersion(0L);
            post.setCreatedAt(OffsetDateTime.now());
            post.setUpdatedAt(OffsetDateTime.now());
            return post;
        });

        var created = service.create(createRequestWithReadingTime(null, 7), writerAuth(11L));
        assertThat(created.readingTimeMinutes()).isEqualTo(7);
    }

    // Regression for a real production bug: create() used to convert ANY
    // DataIntegrityViolationException on saveAndFlush into a slug conflict,
    // which mislabeled unrelated constraint failures (e.g. the
    // chk_content_post_document_schema_version check constraint being out of
    // sync with ContentDocument.CURRENT_SCHEMA_VERSION) as "slug already
    // exists" - hiding the real cause. Only a violation of the slug's own
    // unique constraint should ever be reported as a slug conflict.
    @Test
    void createRethrowsNonSlugConstraintViolationsUnchanged() {
        DashboardUserEntity writer = user(11L, DashboardRole.CONTENT_STAFF);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(writer);
        when(dashboardUserRepository.getReferenceById(11L)).thenReturn(writer);
        when(contentPostRepository.existsBySlug("gurgaon-market-guide")).thenReturn(false);
        DataIntegrityViolationException schemaVersionViolation = new DataIntegrityViolationException(
                "ERROR: new row for relation \"content_post\" violates check constraint "
                        + "\"chk_content_post_document_schema_version\"");
        when(contentPostRepository.saveAndFlush(any())).thenThrow(schemaVersionViolation);

        assertThatThrownBy(() -> service.create(createRequest(null), writerAuth(11L)))
                .isSameAs(schemaVersionViolation);
    }

    @Test
    void createReportsSlugConflictOnlyForTheSlugUniqueConstraint() {
        DashboardUserEntity writer = user(11L, DashboardRole.CONTENT_STAFF);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(writer);
        when(dashboardUserRepository.getReferenceById(11L)).thenReturn(writer);
        when(contentPostRepository.existsBySlug("gurgaon-market-guide")).thenReturn(false);
        when(contentPostRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"uk_content_post_slug\""));

        assertThatThrownBy(() -> service.create(createRequest(null), writerAuth(11L)))
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo("CONTENT_SLUG_CONFLICT");
    }

    @Test
    void updatePersistsReadingTimeMinutes() {
        DashboardUserEntity writer = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(205L, writer, 1L);
        when(contentPostRepository.findDetailedById(205L)).thenReturn(Optional.of(post));
        when(contentPostRepository.existsBySlugAndIdNot("updated-market-guide", 205L)).thenReturn(false);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(writer);
        when(dashboardUserRepository.getReferenceById(11L)).thenReturn(writer);
        when(contentPostRepository.saveAndFlush(post)).thenAnswer(invocation -> {
            post.setVersion(2L);
            return post;
        });

        var updated = service.update(205L, updateRequestWithReadingTime(1L, 12), writerAuth(11L));

        assertThat(updated.readingTimeMinutes()).isEqualTo(12);
        assertThat(post.getReadingTimeMinutes()).isEqualTo(12);
    }

    @Test
    void editorWithCreateCanCreateButPublisherAndLegacyRoleCannot() {
        DashboardUserEntity editor = user(21L, DashboardRole.CONTENT_STAFF);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(editor);
        when(dashboardUserRepository.getReferenceById(21L)).thenReturn(editor);
        when(contentPostRepository.existsBySlug("gurgaon-market-guide")).thenReturn(false);
        when(contentPostRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ContentPostEntity post = invocation.getArgument(0);
            post.setId(102L);
            post.setCreatedAt(OffsetDateTime.now());
            post.setUpdatedAt(OffsetDateTime.now());
            return post;
        });
        assertThat(service.create(createRequest(null), editorAuth(21L)).status())
                .isEqualTo(ContentStatus.DRAFT);

        assertThatThrownBy(() -> service.create(createRequest(null), publisherAuth(31L)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.create(createRequest(null), legacyAuth(41L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ownerCanEditOwnDraftAndOwnerNeverChanges() {
        DashboardUserEntity owner = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(201L, owner, 4L);
        when(contentPostRepository.findDetailedById(201L)).thenReturn(Optional.of(post));
        when(contentPostRepository.existsBySlugAndIdNot("updated-market-guide", 201L)).thenReturn(false);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(owner);
        when(dashboardUserRepository.getReferenceById(11L)).thenReturn(owner);
        when(contentPostRepository.saveAndFlush(post)).thenAnswer(invocation -> {
            post.setVersion(5L);
            post.setUpdatedAt(OffsetDateTime.now());
            return post;
        });

        var response = service.update(201L, updateRequest(4L), writerAuth(11L));

        assertThat(response.version()).isEqualTo(5L);
        assertThat(response.title()).isEqualTo("Updated Market Guide");
        assertThat(response.contentType()).isEqualTo(ContentType.BLOG);
        assertThat(response.contentOwnerDashboardUserId()).isEqualTo(11L);
        assertThat(response.updatedByDashboardUserId()).isEqualTo(11L);
    }

    @Test
    void otherWriterAndPublisherCannotEditButEditorCanEditAny() {
        DashboardUserEntity owner = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(202L, owner, 2L);
        when(contentPostRepository.findDetailedById(202L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.update(202L, updateRequest(2L), writerAuth(12L)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.update(202L, updateRequest(2L), publisherAuth(31L)))
                .isInstanceOf(AccessDeniedException.class);

        DashboardUserEntity editor = user(21L, DashboardRole.CONTENT_STAFF);
        when(contentPostRepository.existsBySlugAndIdNot("updated-market-guide", 202L)).thenReturn(false);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(editor);
        when(dashboardUserRepository.getReferenceById(21L)).thenReturn(editor);
        when(contentPostRepository.saveAndFlush(post)).thenAnswer(invocation -> {
            post.setVersion(3L);
            return post;
        });

        var response = service.update(202L, updateRequest(2L), editorAuth(21L));
        assertThat(response.contentOwnerDashboardUserId()).isEqualTo(11L);
        assertThat(response.updatedByDashboardUserId()).isEqualTo(21L);
    }

    @Test
    void staleVersionReturnsSpecificConflictWithoutSaving() {
        DashboardUserEntity owner = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(203L, owner, 5L);
        when(contentPostRepository.findDetailedById(203L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.update(203L, updateRequest(4L), writerAuth(11L)))
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo("CONTENT_VERSION_CONFLICT");
        verify(contentPostRepository, never()).saveAndFlush(post);
    }

    @Test
    void changesRequestedContentIsEditableButFirstPublishedSlugIsLocked() {
        DashboardUserEntity owner = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(204L, owner, 7L);
        post.setStatus(ContentStatus.CHANGES_REQUESTED);
        post.setPublishedAt(OffsetDateTime.parse("2026-08-18T10:00:00+05:30"));
        when(contentPostRepository.findDetailedById(204L)).thenReturn(Optional.of(post));
        when(contentPostRepository.existsBySlugAndIdNot("updated-market-guide", 204L)).thenReturn(false);

        assertThatThrownBy(() -> service.update(204L, updateRequest(7L), writerAuth(11L)))
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo("CONTENT_SLUG_LOCKED");
        verify(contentPostRepository, never()).saveAndFlush(post);
    }

    @Test
    void missingContentReturnsSpecificNotFoundCode() {
        when(contentPostRepository.findDetailedById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(999L, writerAuth(11L)))
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo("CONTENT_NOT_FOUND");
    }

    private ContentPostCreateRequest createRequest(String slug) {
        return new ContentPostCreateRequest(
                ContentType.ARTICLE,
                "Gurgaon Market Guide",
                slug,
                "A concise market summary",
                null,
                null,
                null,
                null,
                null
        );
    }

    private ContentPostCreateRequest createRequestWithReadingTime(String slug, Integer readingTimeMinutes) {
        return new ContentPostCreateRequest(
                ContentType.ARTICLE, "Gurgaon Market Guide", slug, "A concise market summary",
                null, null, null, null, null, null, null, Set.of(), null, null, readingTimeMinutes
        );
    }

    private ContentPostUpdateRequest updateRequestWithReadingTime(Long version, Integer readingTimeMinutes) {
        return new ContentPostUpdateRequest(
                version, ContentType.BLOG, "Updated Market Guide", "updated-market-guide", "Updated excerpt",
                "Updated SEO title", "Updated SEO description", "https://squarefootstory.com/updated-market-guide",
                true, true, null, null, Set.of(), null, null, readingTimeMinutes
        );
    }

    private ContentPostUpdateRequest updateRequest(Long version) {
        return new ContentPostUpdateRequest(
                version,
                ContentType.BLOG,
                "Updated Market Guide",
                "updated-market-guide",
                "Updated excerpt",
                "Updated SEO title",
                "Updated SEO description",
                "https://squarefootstory.com/updated-market-guide",
                true,
                true
        );
    }

    private ContentPostEntity post(Long id, DashboardUserEntity owner, Long version) {
        return ContentPostEntity.builder()
                .id(id)
                .contentType(ContentType.ARTICLE)
                .status(ContentStatus.DRAFT)
                .title("Original Market Guide")
                .slug("original-market-guide")
                .contentOwner(owner)
                .createdBy(owner)
                .updatedBy(owner)
                .robotsIndex(true)
                .robotsFollow(true)
                .version(version)
                .build();
    }

    private DashboardUserEntity user(Long id, DashboardRole role) {
        return DashboardUserEntity.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .name("User " + id)
                .passwordHash("unused")
                .role(role)
                .active(true)
                .permissions(new LinkedHashSet<>())
                .build();
    }

    private Authentication writerAuth(Long id) {
        return authentication(id, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.WRITER.permissions());
    }

    private Authentication editorAuth(Long id) {
        return authentication(id, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.EDITOR.permissions());
    }

    private Authentication publisherAuth(Long id) {
        return authentication(id, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.PUBLISHER.permissions());
    }

    private Authentication legacyAuth(Long id) {
        return authentication(id, DashboardRole.REVIEWER, Set.of());
    }

    private Authentication authentication(
            Long id,
            DashboardRole role,
            Set<DashboardPermission> permissions
    ) {
        DashboardUserDetails details = new DashboardUserDetails(new DashboardAuthenticationUserSnapshot(
                id, "user" + id + "@example.com", "User " + id, role, true, permissions
        ));
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
