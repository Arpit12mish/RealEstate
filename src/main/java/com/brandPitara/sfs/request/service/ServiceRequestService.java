package com.brandPitara.sfs.request.service;

import com.brandPitara.sfs.request.dto.ServiceRequestCreateRequest;
import com.brandPitara.sfs.request.dto.ServiceRequestResponse;
import com.brandPitara.sfs.request.enums.ServiceRequestStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServiceRequestService {

    ServiceRequestResponse create(Long customerUserId, ServiceRequestCreateRequest request);

    Page<ServiceRequestResponse> myRequests(Long customerUserId, ServiceRequestStatus status, Pageable pageable);

    Page<ServiceRequestResponse> providerFeed(Long providerUserId, ServiceRequestStatus status, Pageable pageable);
}
