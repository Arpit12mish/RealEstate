package com.brandPitara.sfs.dashboard.category.service;

import com.brandPitara.sfs.dashboard.category.dto.DashboardCategoryUpsertRequest;
import com.brandPitara.sfs.dto.CategoryResponse;

import java.util.List;

public interface DashboardCategoryService {

    CategoryResponse create(DashboardCategoryUpsertRequest request);

    CategoryResponse update(Long categoryId, DashboardCategoryUpsertRequest request);

    void delete(Long categoryId);

    CategoryResponse get(Long categoryId);

    List<CategoryResponse> list();
}
