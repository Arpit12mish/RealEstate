package com.brandPitara.sfs.dashboard.review.controller;

import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.review.dto.FieldReviewIssueResponse;
import com.brandPitara.sfs.dashboard.review.dto.MarkFieldIssueFixedRequest;
import com.brandPitara.sfs.dashboard.review.dto.MarkFieldReviewIssueRequest;
import com.brandPitara.sfs.dashboard.review.dto.ReviewHistoryResponse;
import com.brandPitara.sfs.dashboard.review.service.DashboardFieldReviewIssueService;
import com.brandPitara.sfs.dashboard.review.service.DashboardReviewHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/reviews")
@RequiredArgsConstructor
public class DashboardReviewController {

    private final DashboardFieldReviewIssueService fieldReviewIssueService;
    private final DashboardReviewHistoryService reviewHistoryService;

    /**
     * ADMIN / REVIEWER can mark a specific field as WRONG or RECHECK.
     */
    @PostMapping("/field-issues")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public FieldReviewIssueResponse markFieldIssue(
            @Valid @RequestBody MarkFieldReviewIssueRequest request
    ) {
        return fieldReviewIssueService.markIssue(request);
    }

    /**
     * ADMIN / REVIEWER / DATA_ENTRY can see field issues.
     *
     * activeOnly=true  -> only open WRONG/RECHECK issues
     * activeOnly=false -> full issue history for this entity
     */
    @GetMapping("/field-issues")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<FieldReviewIssueResponse> listFieldIssues(
            @RequestParam ReviewEntityType entityType,
            @RequestParam Long entityId,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return fieldReviewIssueService.listIssues(entityType, entityId, activeOnly);
    }

    /**
     * ADMIN / REVIEWER can delete an issue they marked by mistake.
     * Deletes the record entirely and writes a history entry for traceability.
     */
    @DeleteMapping("/field-issues/{issueId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public void deleteFieldIssue(@PathVariable Long issueId) {
        fieldReviewIssueService.deleteIssue(issueId);
    }

    /**
     * DATA_ENTRY can mark fixed after correcting data.
     * ADMIN can also mark fixed.
     */
    @PatchMapping("/field-issues/{issueId}/fixed")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public FieldReviewIssueResponse markFixed(
            @PathVariable Long issueId,
            @RequestBody(required = false) MarkFieldIssueFixedRequest request
    ) {
        return fieldReviewIssueService.markFixed(issueId, request);
    }

    /**
     * Review history for any dashboard review entity.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<ReviewHistoryResponse> history(
            @RequestParam ReviewEntityType entityType,
            @RequestParam Long entityId
    ) {
        return reviewHistoryService.listHistory(entityType, entityId);
    }
}