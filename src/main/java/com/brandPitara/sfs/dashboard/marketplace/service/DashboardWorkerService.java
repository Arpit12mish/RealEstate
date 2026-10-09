package com.brandPitara.sfs.dashboard.marketplace.service;

import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerListItemResponse;
import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerManagementResponse;
import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerProfileUpdateRequest;
import com.brandPitara.sfs.dashboard.marketplace.dto.WorkerVerificationRequest;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.provider.dto.WorkerRatesReplaceRequest;
import com.brandPitara.sfs.provider.dto.WorkerServicesReplaceRequest;

public interface DashboardWorkerService {

    PageResponse<WorkerListItemResponse> list(String query, int page, int size);

    WorkerManagementResponse get(Long workerId);

    WorkerManagementResponse updateProfile(Long workerId, WorkerProfileUpdateRequest request);

    WorkerManagementResponse updateVerification(Long workerId, WorkerVerificationRequest request);

    WorkerManagementResponse replaceServices(Long workerId, WorkerServicesReplaceRequest request);

    WorkerManagementResponse replaceRates(Long workerId, WorkerRatesReplaceRequest request);
}
