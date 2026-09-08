package com.brandPitara.sfs.cms.workflow.controller;

import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.workflow.domain.ContentRevisionReason;
import com.brandPitara.sfs.cms.workflow.dto.ContentRevisionDetailResponse;
import com.brandPitara.sfs.cms.workflow.dto.ContentWorkflowResponse;
import com.brandPitara.sfs.cms.workflow.service.ContentWorkflowService;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.common.exception.DashboardExceptionHandler;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContentWorkflowControllerTest {

    @Test
    void commandEndpointsExposeOnlyExplicitVersionedCommandsAndAuditIdentifiers() throws Exception {
        ContentWorkflowService service = mock(ContentWorkflowService.class);
        DashboardActionAuditService audit = mock(DashboardActionAuditService.class);
        when(service.submit(eq(50L), any(), isNull())).thenReturn(response(ContentStatus.IN_REVIEW));
        MockMvc mvc = mvc(service, audit);

        mvc.perform(post("/api/dashboard/cms/content/50/workflow/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.currentReviewRevisionId").value(1001));

        verify(audit).record(
                DashboardAuditAction.CONTENT_SUBMITTED_FOR_REVIEW,
                ReviewEntityType.CONTENT_POST,
                50L,
                null
        );
    }

    @Test
    void requestChangesRequiresBoundedPlainTextCommentBeforeServiceExecution() throws Exception {
        ContentWorkflowService service = mock(ContentWorkflowService.class);
        MockMvc mvc = mvc(service, mock(DashboardActionAuditService.class));

        mvc.perform(post("/api/dashboard/cms/content/50/workflow/request-changes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":4,\"comment\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void reopenEndpointExistsIsVersionedAndAudited() throws Exception {
        ContentWorkflowService service = mock(ContentWorkflowService.class);
        DashboardActionAuditService audit = mock(DashboardActionAuditService.class);
        when(service.reopen(eq(15L), any(), isNull())).thenReturn(response(ContentStatus.DRAFT));
        MockMvc mvc = mvc(service, audit);

        mvc.perform(post("/api/dashboard/cms/content/15/workflow/reopen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(audit).record(
                DashboardAuditAction.CONTENT_REOPENED,
                ReviewEntityType.CONTENT_POST,
                15L,
                null
        );
    }

    @Test
    void revisionDetailExposesReadingTimeMinutesAsAFlatFieldNoMetadataWrapper() throws Exception {
        ContentWorkflowService service = mock(ContentWorkflowService.class);
        when(service.revision(eq(50L), eq(1001L), isNull())).thenReturn(revisionDetail(6));
        MockMvc mvc = mvc(service, mock(DashboardActionAuditService.class));

        mvc.perform(get("/api/dashboard/cms/content/50/revisions/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readingTimeMinutes").value(6))
                .andExpect(jsonPath("$.metadata").doesNotExist());
    }

    @Test
    void revisionDetailReturnsNullReadingTimeForALegacyRevisionWithoutOne() throws Exception {
        ContentWorkflowService service = mock(ContentWorkflowService.class);
        when(service.revision(eq(50L), eq(1002L), isNull())).thenReturn(revisionDetail(null));
        MockMvc mvc = mvc(service, mock(DashboardActionAuditService.class));

        mvc.perform(get("/api/dashboard/cms/content/50/revisions/1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readingTimeMinutes").doesNotExist());
    }

    @Test
    void historyEndpointsClampPageSizeAndRemainMetadataOnly() throws Exception {
        ContentWorkflowService service = mock(ContentWorkflowService.class);
        when(service.revisions(eq(50L), any(), isNull()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));
        when(service.history(eq(50L), any(), isNull()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));
        MockMvc mvc = mvc(service, mock(DashboardActionAuditService.class));

        mvc.perform(get("/api/dashboard/cms/content/50/revisions?size=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/dashboard/cms/content/50/workflow-history?size=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(service).revisions(eq(50L), argThat(page -> page.getPageSize() == 50), isNull());
        verify(service).history(eq(50L), argThat(page -> page.getPageSize() == 50), isNull());
    }

    private MockMvc mvc(ContentWorkflowService service, DashboardActionAuditService audit) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new ContentWorkflowController(service, audit))
                .setControllerAdvice(new DashboardExceptionHandler(new LogSanitizer()))
                .setValidator(validator)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                        new ObjectMapper().findAndRegisterModules()
                ))
                .build();
    }

    private ContentWorkflowResponse response(ContentStatus status) {
        return new ContentWorkflowResponse(50L, status, 5L, 1001L, null, null, null, null);
    }

    private ContentRevisionDetailResponse revisionDetail(Integer readingTimeMinutes) {
        return new ContentRevisionDetailResponse(
                1001L, 50L, 1, ContentType.ARTICLE, "Revision title", "revision-title", "Excerpt",
                readingTimeMinutes,
                null, null, null, null, null, // publicAuthorId/Name/Slug/Designation/ProfileMediaAssetId
                null, null, null, // categoryId/Name/Slug
                List.of(), // tags
                null, null, // coverMediaAssetId/coverAltText
                null, null, null, // seoTitle/seoDescription/canonicalUrl
                true, true, // robotsIndex/robotsFollow
                new ContentDocument(ContentDocument.CURRENT_SCHEMA_VERSION, List.of()),
                (short) ContentDocument.CURRENT_SCHEMA_VERSION, 1L, ContentRevisionReason.REVIEW_SUBMISSION,
                11L, "Writer", java.time.OffsetDateTime.now(), java.util.Map.of()
        );
    }
}
