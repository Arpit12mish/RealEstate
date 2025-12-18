package com.brandPitara.sfs.service;

import com.brandPitara.sfs.dto.BusinessCreateRequest;
import com.brandPitara.sfs.dto.BusinessEventRequest;
import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface BusinessService {

    BusinessResponse createBusiness(BusinessCreateRequest request);

    BusinessResponse updateBusiness(Long id, BusinessCreateRequest request);

    BusinessResponse getBusiness(Long id);

    PageResponse<BusinessResponse> listBusinesses(Long cityId,
                                              Long categoryId,
                                              String query,
                                              Pageable pageable);

    
    void recordBusinessEvent(Long businessId, BusinessEventRequest request);
}
