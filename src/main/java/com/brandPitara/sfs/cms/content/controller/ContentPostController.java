package com.brandPitara.sfs.cms.content.controller;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.content.dto.ContentPostCreateRequest;
import com.brandPitara.sfs.cms.content.dto.ContentPostDetailResponse;
import com.brandPitara.sfs.cms.content.dto.ContentPostListResponse;
import com.brandPitara.sfs.cms.content.dto.ContentPostUpdateRequest;
import com.brandPitara.sfs.cms.content.service.ContentPostService;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/cms/content")
@RequiredArgsConstructor
public class ContentPostController {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "title", "title"
    );

    private final ContentPostService contentPostService;
    private final DashboardActionAuditService auditService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@cmsContentAccessPolicy.canCreate(authentication)")
    public ContentPostDetailResponse create(
            @Valid @RequestBody ContentPostCreateRequest request,
            Authentication authentication
    ) {
        ContentPostDetailResponse response = contentPostService.create(request, authentication);
        auditService.record(
                DashboardAuditAction.CONTENT_CREATED,
                ReviewEntityType.CONTENT_POST,
                response.id(),
                null
        );
        return response;
    }

    @GetMapping("/{contentId}")
    @PreAuthorize("isAuthenticated()")
    public ContentPostDetailResponse get(
            @PathVariable Long contentId,
            Authentication authentication
    ) {
        return contentPostService.get(contentId, authentication);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<ContentPostListResponse> list(
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Authentication authentication
    ) {
        String property = SORT_FIELDS.get(sortBy);
        if (property == null) {
            throw new IllegalArgumentException("Unsupported content sort field: " + sortBy);
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
        }
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"))
        );
        return contentPostService.list(
                contentType, status, ownerId, search, pageable, authentication
        );
    }

    @PutMapping("/{contentId}")
    @PreAuthorize("isAuthenticated()")
    public ContentPostDetailResponse update(
            @PathVariable Long contentId,
            @Valid @RequestBody ContentPostUpdateRequest request,
            Authentication authentication
    ) {
        ContentPostDetailResponse response = contentPostService.update(contentId, request, authentication);
        auditService.record(
                DashboardAuditAction.CONTENT_UPDATED,
                ReviewEntityType.CONTENT_POST,
                contentId,
                null
        );
        return response;
    }
}
