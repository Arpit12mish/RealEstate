package com.brandPitara.sfs.service;

import com.brandPitara.sfs.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

    /**
     * Root categories (no parent), typically the main blocks like
     * Real Estate, Home Décor & Interior, etc.
     */
    List<CategoryResponse> getRootCategories();

    /**
     * Subcategories for a given parent id.
     */
    List<CategoryResponse> getChildren(Long parentId);
}
