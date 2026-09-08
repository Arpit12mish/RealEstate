package com.brandPitara.sfs.cms.metadata.controller;

import com.brandPitara.sfs.cms.metadata.dto.*;
import com.brandPitara.sfs.cms.metadata.service.CmsMetadataService;
import com.brandPitara.sfs.cms.security.CmsContentAccessPolicy;
import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mandatory security matrix for CmsMetadataController (SFS CMS BACKEND — DASHBOARD
 * INTEGRATION FIX 2, "TAXONOMY LOOKUP READ AUTHORIZATION"): GET (list + by-id) for
 * authors/categories/tags is allowed to any authenticated CMS content-staff member
 * (WRITER/EDITOR/PUBLISHER/ADMIN, via CMS_CONTENT_PREVIEW — see
 * CmsContentAccessPolicy#canReadTaxonomy); POST/PUT remain ADMIN-only, unchanged.
 *
 * Calls the real controller bean from a minimal @EnableMethodSecurity context (same
 * pattern as ContentPostControllerTest) so @PreAuthorize is genuinely enforced by
 * Spring's AOP interceptor, not merely asserted against by hand.
 */
class CmsMetadataControllerSecurityTest {

    private AnnotationConfigApplicationContext context;
    private CmsMetadataController controller;
    private CmsMetadataService service;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(MethodSecurityTestConfig.class);
        controller = context.getBean(CmsMetadataController.class);
        service = context.getBean(CmsMetadataService.class);
        when(service.authors(any(), any(), any())).thenReturn(Page.empty());
        when(service.categories(any(), any(), any())).thenReturn(Page.empty());
        when(service.tags(any(), any(), any())).thenReturn(Page.empty());
        when(service.getAuthor(any())).thenReturn(authorResponse());
        when(service.getCategory(any())).thenReturn(categoryResponse());
        when(service.getTag(any())).thenReturn(tagResponse());
        when(service.createAuthor(any())).thenReturn(authorResponse());
        when(service.updateAuthor(any(), any())).thenReturn(authorResponse());
        when(service.createCategory(any())).thenReturn(categoryResponse());
        when(service.updateCategory(any(), any())).thenReturn(categoryResponse());
        when(service.createTag(any())).thenReturn(tagResponse());
        when(service.updateTag(any(), any())).thenReturn(tagResponse());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void writerEditorPublisherAndAdminCanReadTaxonomyLookups() {
        for (Authentication reader : List.of(
                authentication(11L, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.WRITER.permissions()),
                authentication(21L, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.EDITOR.permissions()),
                authentication(31L, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.PUBLISHER.permissions()),
                authentication(1L, DashboardRole.ADMIN, Set.of())
        )) {
            SecurityContextHolder.getContext().setAuthentication(reader);
            assertThatCode(() -> controller.authors(null, null, 0, 20)).doesNotThrowAnyException();
            assertThatCode(() -> controller.author(1L)).doesNotThrowAnyException();
            assertThatCode(() -> controller.categories(null, null, 0, 20)).doesNotThrowAnyException();
            assertThatCode(() -> controller.category(1L)).doesNotThrowAnyException();
            assertThatCode(() -> controller.tags(null, null, 0, 20)).doesNotThrowAnyException();
            assertThatCode(() -> controller.tag(1L)).doesNotThrowAnyException();
        }
    }

    @Test
    void legacyRolesWithoutCmsPermissionsCannotReadTaxonomyLookups() {
        for (DashboardRole role : Set.of(DashboardRole.DATA_ENTRY, DashboardRole.REVIEWER)) {
            SecurityContextHolder.getContext().setAuthentication(authentication(41L, role, Set.of()));
            assertThatThrownBy(() -> controller.authors(null, null, 0, 20)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.author(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.categories(null, null, 0, 20)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.category(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.tags(null, null, 0, 20)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.tag(1L)).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void unauthenticatedRequestsCannotReadTaxonomyLookups() {
        SecurityContextHolder.clearContext();
        // No Authentication in context at all (not even an unauthenticated one) means the
        // SpEL evaluator can't build its root object — same behavior already established
        // by ContentPostControllerTest#createRequiresCmsCreatePermission for this exact
        // in-JVM method-security setup. The real HTTP path (DashboardAuthenticationEntryPoint)
        // is what turns "no principal" into an actual 401 — verified separately over HTTP.
        assertThatThrownBy(() -> controller.authors(null, null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to evaluate expression");
    }

    @Test
    void onlyAdminCanCreateOrUpdateAuthors() {
        for (Authentication nonAdmin : List.of(
                authentication(11L, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.WRITER.permissions()),
                authentication(21L, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.EDITOR.permissions()),
                authentication(31L, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.PUBLISHER.permissions())
        )) {
            SecurityContextHolder.getContext().setAuthentication(nonAdmin);
            assertThatThrownBy(() -> controller.createAuthor(authorRequest())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.updateAuthor(1L, authorRequest())).isInstanceOf(AccessDeniedException.class);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication(1L, DashboardRole.ADMIN, Set.of()));
        assertThatCode(() -> controller.createAuthor(authorRequest())).doesNotThrowAnyException();
        assertThatCode(() -> controller.updateAuthor(1L, authorRequest())).doesNotThrowAnyException();
    }

    @Test
    void onlyAdminCanCreateOrUpdateCategoriesAndTags() {
        SecurityContextHolder.getContext().setAuthentication(
                authentication(11L, DashboardRole.CONTENT_STAFF, CmsPermissionProfile.WRITER.permissions()));
        assertThatThrownBy(() -> controller.createCategory(categoryRequest())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.updateCategory(1L, categoryRequest())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.createTag(tagRequest())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.updateTag(1L, tagRequest())).isInstanceOf(AccessDeniedException.class);

        SecurityContextHolder.getContext().setAuthentication(authentication(1L, DashboardRole.ADMIN, Set.of()));
        assertThatCode(() -> controller.createCategory(categoryRequest())).doesNotThrowAnyException();
        assertThatCode(() -> controller.updateCategory(1L, categoryRequest())).doesNotThrowAnyException();
        assertThatCode(() -> controller.createTag(tagRequest())).doesNotThrowAnyException();
        assertThatCode(() -> controller.updateTag(1L, tagRequest())).doesNotThrowAnyException();
    }

    private Authentication authentication(
            Long userId,
            DashboardRole role,
            Set<com.brandPitara.sfs.dashboard.common.enums.DashboardPermission> permissions
    ) {
        DashboardUserDetails principal = new DashboardUserDetails(
                new DashboardAuthenticationUserSnapshot(
                        userId, "user" + userId + "@example.com", "User " + userId, role, true, permissions
                )
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private CmsAuthorRequest authorRequest() {
        return new CmsAuthorRequest("Test Author", null, null, null, null, true, null);
    }

    private CmsCategoryRequest categoryRequest() {
        return new CmsCategoryRequest("Test Category", null, null, true, null);
    }

    private CmsTagRequest tagRequest() {
        return new CmsTagRequest("Test Tag", null, true, null);
    }

    private CmsAuthorResponse authorResponse() {
        OffsetDateTime now = OffsetDateTime.now();
        return new CmsAuthorResponse(1L, "Test Author", "test-author", null, null, null, true, now, now, 0L);
    }

    private CmsCategoryResponse categoryResponse() {
        OffsetDateTime now = OffsetDateTime.now();
        return new CmsCategoryResponse(1L, "Test Category", "test-category", null, true, now, now, 0L);
    }

    private CmsTagResponse tagResponse() {
        OffsetDateTime now = OffsetDateTime.now();
        return new CmsTagResponse(1L, "Test Tag", "test-tag", true, now, now, 0L);
    }

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class MethodSecurityTestConfig {
        @Bean
        CmsMetadataService cmsMetadataService() {
            return mock(CmsMetadataService.class);
        }

        @Bean
        DashboardActionAuditService auditService() {
            return mock(DashboardActionAuditService.class);
        }

        @Bean
        CmsContentAccessPolicy cmsContentAccessPolicy() {
            return new CmsContentAccessPolicy();
        }

        @Bean
        CmsMetadataController controller(CmsMetadataService service, DashboardActionAuditService audit) {
            return new CmsMetadataController(service, audit);
        }
    }
}
