package com.brandPitara.sfs.cms.media.controller;

import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.media.dto.*;
import com.brandPitara.sfs.cms.media.service.CmsMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/cms/media")
@RequiredArgsConstructor
public class CmsMediaController {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id", "createdAt", "createdAt", "updatedAt", "updatedAt", "filename", "originalFilename"
    );
    private final CmsMediaService mediaService;

    @PostMapping("/uploads")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@cmsMediaAccessPolicy.canUpload(authentication)")
    public CmsMediaUploadResponse createUpload(
            @Valid @RequestBody CmsMediaUploadRequest request, Authentication authentication
    ) {
        return mediaService.createUpload(request, authentication);
    }

    @PostMapping("/{mediaId}/complete")
    @PreAuthorize("@cmsMediaAccessPolicy.canUpload(authentication)")
    public CmsMediaAssetResponse complete(@PathVariable Long mediaId, Authentication authentication) {
        return mediaService.complete(mediaId, authentication);
    }

    @GetMapping
    @PreAuthorize("@cmsMediaAccessPolicy.canRead(authentication)")
    public Page<CmsMediaAssetResponse> list(
            @RequestParam(required = false) CmsMediaType mediaType,
            @RequestParam(required = false) CmsMediaStatus status,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        String property = SORT_FIELDS.get(sortBy);
        if (property == null) throw new IllegalArgumentException("Unsupported media sort field: " + sortBy);
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
        }
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 50),
                Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id")));
        return mediaService.list(mediaType, status, createdBy, search, pageable);
    }

    @GetMapping("/{mediaId}")
    @PreAuthorize("@cmsMediaAccessPolicy.canRead(authentication)")
    public CmsMediaAssetResponse get(@PathVariable Long mediaId) {
        return mediaService.get(mediaId);
    }
}
