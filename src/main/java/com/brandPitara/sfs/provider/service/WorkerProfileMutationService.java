package com.brandPitara.sfs.provider.service;

import com.brandPitara.sfs.provider.dto.WorkerAvailabilityUpdateRequest;
import com.brandPitara.sfs.provider.dto.WorkerOfferingsResponse;
import com.brandPitara.sfs.provider.dto.WorkerRatesReplaceRequest;
import com.brandPitara.sfs.provider.dto.WorkerServicesReplaceRequest;

/** Worker marketplace fields, shared by worker self-service and dashboard management. */
public interface WorkerProfileMutationService {

    Long requireWorkerIdForUser(Long userId);

    WorkerOfferingsResponse get(Long workerId);

    WorkerOfferingsResponse replaceServices(Long workerId, WorkerServicesReplaceRequest request);

    WorkerOfferingsResponse replaceRates(Long workerId, WorkerRatesReplaceRequest request);

    WorkerOfferingsResponse updateAvailability(Long workerId, WorkerAvailabilityUpdateRequest request);
}
