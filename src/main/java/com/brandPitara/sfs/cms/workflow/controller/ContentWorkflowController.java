package com.brandPitara.sfs.cms.workflow.controller;

import com.brandPitara.sfs.cms.workflow.dto.*;
import com.brandPitara.sfs.cms.workflow.service.ContentWorkflowService;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/cms/content/{contentId}")
@RequiredArgsConstructor
public class ContentWorkflowController {

    private final ContentWorkflowService workflowService;
    private final DashboardActionAuditService auditService;

    @PostMapping("/workflow/submit")
    @PreAuthorize("isAuthenticated()")
    public ContentWorkflowResponse submit(
            @PathVariable Long contentId,
            @Valid @RequestBody SubmitReviewRequest request,
            Authentication authentication
    ) {
        return command(contentId, DashboardAuditAction.CONTENT_SUBMITTED_FOR_REVIEW,
                workflowService.submit(contentId, request, authentication));
    }

    @PostMapping("/workflow/request-changes")
    @PreAuthorize("isAuthenticated()")
    public ContentWorkflowResponse requestChanges(
            @PathVariable Long contentId,
            @Valid @RequestBody RequestChangesRequest request,
            Authentication authentication
    ) {
        return command(contentId, DashboardAuditAction.CONTENT_CHANGES_REQUESTED,
                workflowService.requestChanges(contentId, request, authentication));
    }

    @PostMapping("/workflow/approve")
    @PreAuthorize("isAuthenticated()")
    public ContentWorkflowResponse approve(
            @PathVariable Long contentId,
            @Valid @RequestBody ApproveContentRequest request,
            Authentication authentication
    ) {
        return command(contentId, DashboardAuditAction.CONTENT_APPROVED,
                workflowService.approve(contentId, request, authentication));
    }

    @PostMapping("/workflow/publish")
    @PreAuthorize("isAuthenticated()")
    public ContentWorkflowResponse publish(
            @PathVariable Long contentId,
            @Valid @RequestBody PublishContentRequest request,
            Authentication authentication
    ) {
        return command(contentId, DashboardAuditAction.CONTENT_PUBLISHED,
                workflowService.publish(contentId, request, authentication));
    }

    @PostMapping("/workflow/unpublish")
    @PreAuthorize("isAuthenticated()")
    public ContentWorkflowResponse unpublish(
            @PathVariable Long contentId,
            @Valid @RequestBody UnpublishContentRequest request,
            Authentication authentication
    ) {
        return command(contentId, DashboardAuditAction.CONTENT_UNPUBLISHED,
                workflowService.unpublish(contentId, request, authentication));
    }

    @PostMapping("/workflow/archive")
    @PreAuthorize("isAuthenticated()")
    public ContentWorkflowResponse archive(
            @PathVariable Long contentId,
            @Valid @RequestBody ArchiveContentRequest request,
            Authentication authentication
    ) {
        return command(contentId, DashboardAuditAction.CONTENT_ARCHIVED,
                workflowService.archive(contentId, request, authentication));
    }

    @PostMapping("/workflow/reopen")
    @PreAuthorize("isAuthenticated()")
    public ContentWorkflowResponse reopen(
            @PathVariable Long contentId,
            @Valid @RequestBody ReopenContentRequest request,
            Authentication authentication
    ) {
        return command(contentId, DashboardAuditAction.CONTENT_REOPENED,
                workflowService.reopen(contentId, request, authentication));
    }

    @GetMapping("/revisions")
    @PreAuthorize("isAuthenticated()")
    public Page<ContentRevisionListResponse> revisions(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return workflowService.revisions(contentId, PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "revisionNumber")
        ), authentication);
    }

    @GetMapping("/revisions/{revisionId}")
    @PreAuthorize("isAuthenticated()")
    public ContentRevisionDetailResponse revision(
            @PathVariable Long contentId,
            @PathVariable Long revisionId,
            Authentication authentication
    ) {
        return workflowService.revision(contentId, revisionId, authentication);
    }

    @GetMapping("/workflow-history")
    @PreAuthorize("isAuthenticated()")
    public Page<ContentWorkflowActivityResponse> history(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return workflowService.history(contentId, PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        ), authentication);
    }

    private ContentWorkflowResponse command(
            Long contentId,
            DashboardAuditAction action,
            ContentWorkflowResponse response
    ) {
        auditService.record(action, ReviewEntityType.CONTENT_POST, contentId, null);
        return response;
    }
}
