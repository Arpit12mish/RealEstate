package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.PublicBrandCardResponse;
import com.brandPitara.sfs.brand.dto.PublicBrandCategoryResponse;
import com.brandPitara.sfs.brand.dto.PublicBrandDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BrandPublicService {

  List<PublicBrandCategoryResponse> listPublicCategories();

  Page<PublicBrandCardResponse> listPublicBrands(Long categoryId, String q, Pageable pageable);

  PublicBrandDetailResponse getPublicBrandBySlug(String slug);
}
