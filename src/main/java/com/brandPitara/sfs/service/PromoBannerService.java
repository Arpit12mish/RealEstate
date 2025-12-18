package com.brandPitara.sfs.service;

import com.brandPitara.sfs.dto.PromoBannerResponse;

import java.util.List;

public interface PromoBannerService {
    List<PromoBannerResponse> getBannersForCategory(Long categoryId);
}
