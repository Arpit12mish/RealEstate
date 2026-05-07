package com.brandPitara.sfs.dashboard.media.service;

import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadRequest;
import com.brandPitara.sfs.dashboard.media.dto.DashboardPresignUploadResponse;

public interface DashboardMediaPresignService {

    DashboardPresignUploadResponse createPresignedUpload(DashboardPresignUploadRequest request);
}
