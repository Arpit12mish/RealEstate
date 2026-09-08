package com.brandPitara.sfs.cms.content.controller;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.dto.ContentPostCreateRequest;
import com.brandPitara.sfs.cms.content.dto.ContentPostDetailResponse;
import com.brandPitara.sfs.cms.content.service.ContentPostService;
import com.brandPitara.sfs.cms.security.CmsContentAccessPolicy;
import com.brandPitara.sfs.cms.security.CmsPermissionProfile;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.DashboardPermission;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.common.exception.DashboardExceptionHandler;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContentPostControllerTest {

    private AnnotationConfigApplicationContext context;
    private ContentPostController securedController;
    private ContentPostService securedService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(MethodSecurityTestConfig.class);
        securedController = context.getBean(ContentPostController.class);
        securedService = context.getBean(ContentPostService.class);
        when(securedService.create(any(), any())).thenReturn(response(101L));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void createRequiresCmsCreatePermission() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> securedController.create(createRequest(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to evaluate expression");

        assertCreateDenied(authentication(31L, DashboardRole.CONTENT_STAFF,
                CmsPermissionProfile.PUBLISHER.permissions()));
        assertCreateDenied(authentication(41L, DashboardRole.REVIEWER, Set.of()));
        assertCreateDenied(authentication(42L, DashboardRole.DATA_ENTRY, Set.of()));

        assertCreateAllowed(authentication(11L, DashboardRole.CONTENT_STAFF,
                CmsPermissionProfile.WRITER.permissions()));
        assertCreateAllowed(authentication(21L, DashboardRole.CONTENT_STAFF,
                CmsPermissionProfile.EDITOR.permissions()));
        assertCreateAllowed(authentication(1L, DashboardRole.ADMIN, Set.of()));
    }

    @Test
    void createRejectsWorkflowAndOwnershipFieldsFromJson() throws Exception {
        ContentPostService service = mock(ContentPostService.class);
        MockMvc mockMvc = standaloneMvc(service, mock(DashboardActionAuditService.class));

        mockMvc.perform(post("/api/dashboard/cms/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson().replace("\n}", ",\n  \"status\": \"PUBLISHED\"\n}")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/dashboard/cms/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson().replace("\n}", ",\n  \"contentOwnerDashboardUserId\": 999\n}")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void readingTimeMinutesOutOfBoundsIsRejectedAndInBoundsIsAccepted() throws Exception {
        ContentPostService service = mock(ContentPostService.class);
        when(service.create(any(), any())).thenReturn(response(401L));
        MockMvc mockMvc = standaloneMvc(service, mock(DashboardActionAuditService.class));

        mockMvc.perform(post("/api/dashboard/cms/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson().replace("\n}", ",\n  \"readingTimeMinutes\": 0\n}")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/dashboard/cms/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson().replace("\n}", ",\n  \"readingTimeMinutes\": 181\n}")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);

        mockMvc.perform(post("/api/dashboard/cms/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson().replace("\n}", ",\n  \"readingTimeMinutes\": 7\n}")))
                .andExpect(status().isCreated());
        verify(service).create(argThat(request -> Integer.valueOf(7).equals(request.readingTimeMinutes())), any());
    }

    @Test
    void listIsBoundedUsesAllowlistedSortAndPassesFilters() throws Exception {
        ContentPostService service = mock(ContentPostService.class);
        when(service.list(any(), any(), any(), any(), any(), any())).thenAnswer(invocation ->
                new PageImpl<>(java.util.List.of(), invocation.getArgument(4), 0)
        );
        MockMvc mockMvc = standaloneMvc(service, mock(DashboardActionAuditService.class));

        mockMvc.perform(get("/api/dashboard/cms/content")
                        .param("contentType", "BLOG")
                        .param("status", "DRAFT")
                        .param("ownerId", "11")
                        .param("search", "gurgaon")
                        .param("page", "-8")
                        .param("size", "5000")
                        .param("sortBy", "title")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk());

        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(
                org.mockito.ArgumentMatchers.eq(ContentType.BLOG),
                org.mockito.ArgumentMatchers.eq(ContentStatus.DRAFT),
                org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.eq("gurgaon"),
                pageableCaptor.capture(),
                isNull()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("title").isAscending()).isTrue();

        mockMvc.perform(get("/api/dashboard/cms/content").param("sortBy", "contentOwner.passwordHash"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAuditContainsOnlyActionEntityAndIdentifier() {
        ContentPostService service = mock(ContentPostService.class);
        DashboardActionAuditService audit = mock(DashboardActionAuditService.class);
        when(service.create(any(), any())).thenReturn(response(301L));
        ContentPostController controller = new ContentPostController(service, audit);

        controller.create(createRequest(), null);

        verify(audit).record(
                DashboardAuditAction.CONTENT_CREATED,
                ReviewEntityType.CONTENT_POST,
                301L,
                null
        );
    }

    private void assertCreateDenied(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        assertThatThrownBy(() -> securedController.create(createRequest(), authentication))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void assertCreateAllowed(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        assertThatCode(() -> securedController.create(createRequest(), authentication))
                .doesNotThrowAnyException();
    }

    private ContentPostCreateRequest createRequest() {
        return new ContentPostCreateRequest(
                ContentType.ARTICLE,
                "Gurgaon Market Guide",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private ContentPostDetailResponse response(Long id) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ContentPostDetailResponse(
                id, ContentType.ARTICLE, ContentStatus.DRAFT,
                "Gurgaon Market Guide", "gurgaon-market-guide", null,
                11L, "Writer", 11L, "Writer", 11L, "Writer",
                null, null, null, true, true,
                null, null, null, null, null,
                now, now, 0L
        );
    }

    private Authentication authentication(
            Long userId,
            DashboardRole role,
            Set<DashboardPermission> permissions
    ) {
        DashboardUserDetails principal = new DashboardUserDetails(
                new DashboardAuthenticationUserSnapshot(
                        userId,
                        "user" + userId + "@example.com",
                        "User " + userId,
                        role,
                        true,
                        permissions
                )
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private MockMvc standaloneMvc(
            ContentPostService service,
            DashboardActionAuditService auditService
    ) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new ContentPostController(service, auditService))
                .setControllerAdvice(new DashboardExceptionHandler(new LogSanitizer()))
                .setValidator(validator)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                        new ObjectMapper().findAndRegisterModules()
                ))
                .build();
    }

    private String createJson() {
        return """
                {
                  "contentType": "ARTICLE",
                  "title": "Gurgaon Market Guide"
                }
                """;
    }

    @Configuration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class MethodSecurityTestConfig {
        @Bean
        ContentPostService contentPostService() {
            return mock(ContentPostService.class);
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
        ContentPostController controller(
                ContentPostService contentPostService,
                DashboardActionAuditService auditService
        ) {
            return new ContentPostController(contentPostService, auditService);
        }
    }
}
