package com.brandPitara.sfs.search;

import com.brandPitara.sfs.dto.BusinessResponse;

import java.util.List;

public interface BusinessSearchService {

    void indexBusiness(com.brandPitara.sfs.entity.BusinessEntity entity);

    void deleteBusiness(Long businessId);

    List<BusinessResponse> search(
            Long cityId,
            Long categoryId,
            String text,
            Double userLat,
            Double userLon,
            int page,
            int size
    );
}
