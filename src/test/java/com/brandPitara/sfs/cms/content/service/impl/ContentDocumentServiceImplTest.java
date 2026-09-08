package com.brandPitara.sfs.cms.content.service.impl;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.document.ContentBlock;
import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.document.ContentDocumentValidator;
import com.brandPitara.sfs.cms.content.document.ContentDocumentWordCounter;
import com.brandPitara.sfs.cms.content.document.CmsMediaReferenceService;
import com.brandPitara.sfs.cms.content.document.InlineNode;
import com.brandPitara.sfs.cms.content.dto.ContentDocumentUpdateRequest;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import com.brandPitara.sfs.cms.security.CmsContentAccessPolicy;
import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentDocumentServiceImplTest {

    @Mock private ContentPostRepository contentPostRepository;
    @Mock private DashboardUserRepository dashboardUserRepository;
    @Mock private DashboardCurrentUserService currentUserService;
    @Mock private CmsMediaReferenceService mediaReferenceService;

    private ContentDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new ContentDocumentServiceImpl(
                contentPostRepository,
                dashboardUserRepository,
                currentUserService,
                new CmsContentAccessPolicy(),
                new ContentDocumentValidator(objectMapper),
                new ContentDocumentWordCounter(),
                mediaReferenceService,
                new com.brandPitara.sfs.cms.workflow.service.ContentWorkflowTransitionPolicy()
        );
        lenient().when(mediaReferenceService.validateAndResolve(any())).thenReturn(Map.of());
        lenient().when(mediaReferenceService.createPreviewMap(any())).thenReturn(Map.of());
    }

    @Test
    void writerUpdatesOwnDraftWithSharedAggregateVersionAndDerivedActor() {
        DashboardUserEntity writer = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(101L, writer, ContentStatus.DRAFT, 4L);
        when(contentPostRepository.findDetailedById(101L)).thenReturn(Optional.of(post));
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(writer);
        when(dashboardUserRepository.getReferenceById(11L)).thenReturn(writer);
        when(contentPostRepository.saveAndFlush(post)).thenAnswer(invocation -> {
            post.setVersion(5L);
            post.setUpdatedAt(OffsetDateTime.now());
            return post;
        });

        var response = service.update(101L, request(4L), writerAuth(11L));

        assertThat(response.version()).isEqualTo(5L);
        assertThat(response.document()).isEqualTo(document());
        assertThat(response.wordCount()).isEqualTo(3);
        assertThat(post.getContentDocumentSchemaVersion())
                .isEqualTo((short) ContentDocument.CURRENT_SCHEMA_VERSION);
        assertThat(post.getUpdatedBy().getId()).isEqualTo(11L);
        assertThat(post.getContentOwner().getId()).isEqualTo(11L);
    }

    @Test
    void otherWriterPublisherAndLegacyRoleCannotEditDocument() {
        DashboardUserEntity owner = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(102L, owner, ContentStatus.DRAFT, 2L);
        when(contentPostRepository.findDetailedById(102L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.update(102L, request(2L), writerAuth(12L)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.update(102L, request(2L), publisherAuth(31L)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.update(102L, request(2L), legacyAuth(41L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void editorAndAdminCanEditAnyDraft() {
        DashboardUserEntity owner = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(103L, owner, ContentStatus.DRAFT, 1L);
        DashboardUserEntity editor = user(21L, DashboardRole.CONTENT_STAFF);
        when(contentPostRepository.findDetailedById(103L)).thenReturn(Optional.of(post));
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(editor);
        when(dashboardUserRepository.getReferenceById(21L)).thenReturn(editor);
        when(contentPostRepository.saveAndFlush(post)).thenReturn(post);

        service.update(103L, request(1L), editorAuth(21L));
        assertThat(post.getUpdatedBy().getId()).isEqualTo(21L);

        DashboardUserEntity admin = user(1L, DashboardRole.ADMIN);
        post.setVersion(2L);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(admin);
        when(dashboardUserRepository.getReferenceById(1L)).thenReturn(admin);
        service.update(103L, request(2L), adminAuth(1L));
        assertThat(post.getUpdatedBy().getId()).isEqualTo(1L);
    }

    @Test
    void updateAcceptsSchemaVersion3DocumentWithCalloutAndPersistsCanonicalVersion() {
        DashboardUserEntity writer = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(107L, writer, ContentStatus.DRAFT, 0L);
        when(contentPostRepository.findDetailedById(107L)).thenReturn(Optional.of(post));
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(writer);
        when(dashboardUserRepository.getReferenceById(11L)).thenReturn(writer);
        when(contentPostRepository.saveAndFlush(post)).thenReturn(post);

        ContentDocument v3Callout = new ContentDocument(3, List.of(
                new ContentBlock.Callout(
                        com.brandPitara.sfs.cms.content.document.CalloutVariant.VERDICT,
                        "SFS Verdict",
                        List.of(new InlineNode.Text("Strong pick for this budget.", List.of()))
                )
        ));

        var response = service.update(107L, new ContentDocumentUpdateRequest(0L, v3Callout), writerAuth(11L));

        assertThat(response.document().schemaVersion()).isEqualTo(ContentDocument.CURRENT_SCHEMA_VERSION);
        assertThat(post.getContentDocumentSchemaVersion())
                .isEqualTo((short) ContentDocument.CURRENT_SCHEMA_VERSION);
    }

    @Test
    void staleBodyVersionIncludingAfterMetadataSaveReturnsConflictWithoutWrite() {
        DashboardUserEntity owner = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(104L, owner, ContentStatus.DRAFT, 6L);
        when(contentPostRepository.findDetailedById(104L)).thenReturn(Optional.of(post));

        assertCode(() -> service.update(104L, request(5L), writerAuth(11L)),
                "CONTENT_VERSION_CONFLICT");
        verify(contentPostRepository, never()).saveAndFlush(any());
    }

    @Test
    void nonDraftDocumentIsNotEditable() {
        DashboardUserEntity owner = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(105L, owner, ContentStatus.IN_REVIEW, 1L);
        when(contentPostRepository.findDetailedById(105L)).thenReturn(Optional.of(post));

        assertCode(() -> service.update(105L, request(1L), writerAuth(11L)),
                "CONTENT_NOT_EDITABLE");
    }

    @Test
    void getUsesViewPolicyAndMissingPostReturnsExistingNotFoundContract() {
        DashboardUserEntity owner = user(11L, DashboardRole.CONTENT_STAFF);
        ContentPostEntity post = post(106L, owner, ContentStatus.DRAFT, 0L);
        when(contentPostRepository.findDetailedById(106L)).thenReturn(Optional.of(post));

        assertThat(service.get(106L, writerAuth(11L)).document()).isEqualTo(ContentDocument.empty());
        assertThatThrownBy(() -> service.get(106L, writerAuth(12L)))
                .isInstanceOf(AccessDeniedException.class);

        when(contentPostRepository.findDetailedById(999L)).thenReturn(Optional.empty());
        assertCode(() -> service.get(999L, writerAuth(11L)), "CONTENT_NOT_FOUND");
    }

    private ContentDocumentUpdateRequest request(Long version) {
        return new ContentDocumentUpdateRequest(version, document());
    }

    private ContentDocument document() {
        return new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of(new ContentBlock.Paragraph(List.of(
                new InlineNode.Text("Gurgaon market guide", List.of())
        ))));
    }

    private ContentPostEntity post(
            Long id,
            DashboardUserEntity owner,
            ContentStatus status,
            Long version
    ) {
        return ContentPostEntity.builder()
                .id(id)
                .contentType(ContentType.ARTICLE)
                .status(status)
                .title("Market Guide")
                .slug("market-guide-" + id)
                .contentOwner(owner)
                .createdBy(owner)
                .updatedBy(owner)
                .robotsIndex(true)
                .robotsFollow(true)
                .contentDocument(ContentDocument.empty())
                .contentDocumentSchemaVersion((short) ContentDocument.CURRENT_SCHEMA_VERSION)
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

    private Authentication adminAuth(Long id) {
        return authentication(id, DashboardRole.ADMIN, Set.of());
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

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThatThrownBy(callable)
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo(code);
    }
}
