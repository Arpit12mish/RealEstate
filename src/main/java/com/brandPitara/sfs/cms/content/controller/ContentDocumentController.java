package com.brandPitara.sfs.cms.content.controller;

import com.brandPitara.sfs.cms.content.dto.ContentDocumentResponse;
import com.brandPitara.sfs.cms.content.dto.ContentDocumentUpdateRequest;
import com.brandPitara.sfs.cms.content.service.ContentDocumentService;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/cms/content/{contentId}/document")
@RequiredArgsConstructor
public class ContentDocumentController {

    private final ContentDocumentService contentDocumentService;
    private final DashboardActionAuditService auditService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ContentDocumentResponse get(
            @PathVariable Long contentId,
            Authentication authentication
    ) {
        return contentDocumentService.get(contentId, authentication);
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ContentDocumentResponse update(
            @PathVariable Long contentId,
            @Valid @RequestBody ContentDocumentUpdateRequest request,
            Authentication authentication
    ) {
        ContentDocumentResponse response = contentDocumentService.update(
                contentId, request, authentication
        );
        auditService.record(
                DashboardAuditAction.CONTENT_DOCUMENT_UPDATED,
                ReviewEntityType.CONTENT_POST,
                contentId,
                null
        );
        return response;
    }
}
