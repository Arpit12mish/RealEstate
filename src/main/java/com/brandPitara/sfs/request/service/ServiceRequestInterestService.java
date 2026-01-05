package com.brandPitara.sfs.request.service;

import com.brandPitara.sfs.request.dto.ServiceRequestInterestResponse;

import java.util.List;

public interface ServiceRequestInterestService {

    ServiceRequestInterestResponse expressInterest(Long providerUserId, Long requestId, String message);

    List<ServiceRequestInterestResponse> listInterestsForCustomer(Long customerUserId, Long requestId);

    void closeRequest(Long customerUserId, Long requestId);
}
