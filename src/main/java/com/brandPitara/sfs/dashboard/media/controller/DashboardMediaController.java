package com.brandPitara.sfs.dashboard.media.controller;

import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadRequest;
import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadResponse;
import com.brandPitara.sfs.dashboard.media.service.DashboardMediaPresignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/media")
@RequiredArgsConstructor
public class DashboardMediaController {

    private final DashboardMediaPresignService presignService;

    @PostMapping("/presign-upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DashboardPresignUploadResponse presignUpload(
            @Valid @RequestBody DashboardPresignUploadRequest request
    ) {
        return presignService.createPresignedUpload(request);
    }
}
