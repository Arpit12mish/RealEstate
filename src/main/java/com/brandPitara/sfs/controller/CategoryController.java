package com.brandPitara.sfs.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.brandPitara.sfs.dto.CategoryResponse;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.service.CategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryRepository categoryRepo;

    @GetMapping("/blocks")
    public List<CategoryEntity> getBlocks() {
        return categoryRepo.findByParentIsNullOrderByPriorityAsc();
    }

    @GetMapping("/blocks/{blockId}/children")
    public List<CategoryEntity> getChildren(@PathVariable Long blockId) {
        return categoryRepo.findByParentIdOrderByPriorityAsc(blockId);
    }

        /**
     * Root categories = blocks like "Real Estate Agents", "Home Décor & Interior" etc.
     *
     * GET /api/categories/root -> show the 12 blocks you designed
     */
    @GetMapping("/root")
    public List<CategoryResponse> rootCategories() {
        return categoryService.getRootCategories();
    }

    /**
     * Children for a given parent category.
     *
     * GET /api/categories/{parentId}/children -> show sub-categories inside a block
     */
    @GetMapping("/{parentId}/children")
    public List<CategoryResponse> children(@PathVariable Long parentId) {
        return categoryService.getChildren(parentId);
    }

}


// Hit in Postman:

// GET http://localhost:8080/api/categories/blocks

// GET http://localhost:8080/api/categories/blocks/1/children (replace 1 with actual id)
