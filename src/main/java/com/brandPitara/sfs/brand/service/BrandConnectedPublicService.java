package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.PublicConnectedBrandsSectionResponse;

public interface BrandConnectedPublicService {

  PublicConnectedBrandsSectionResponse getProjectConnectedBrands(Long projectId, int limit);

  PublicConnectedBrandsSectionResponse getBuilderConnectedBrands(Long builderId, int limit);
}
