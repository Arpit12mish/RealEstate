package com.brandPitara.sfs.dashboard.category.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.category.dto.DashboardCategoryUpsertRequest;
import com.brandPitara.sfs.dashboard.category.service.DashboardCategoryService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dto.CategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/categories")
@RequiredArgsConstructor
public class DashboardCategoryController {

    private final DashboardCategoryService categoryService;
    private final DashboardActionAuditService auditService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse create(@Valid @RequestBody DashboardCategoryUpsertRequest request) {
        CategoryResponse response = categoryService.create(request);
        auditService.record(DashboardAuditAction.CATEGORY_CREATED, ReviewEntityType.CATEGORY, response.getId(), null);
        return response;
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(
            @PathVariable Long categoryId,
            @Valid @RequestBody DashboardCategoryUpsertRequest request
    ) {
        CategoryResponse response = categoryService.update(categoryId, request);
        auditService.record(DashboardAuditAction.CATEGORY_UPDATED, ReviewEntityType.CATEGORY, categoryId, null);
        return response;
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        auditService.record(DashboardAuditAction.CATEGORY_DELETED, ReviewEntityType.CATEGORY, categoryId, null);
    }

    @GetMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public CategoryResponse get(@PathVariable Long categoryId) {
        return categoryService.get(categoryId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<CategoryResponse> list() {
        return categoryService.list();
    }
}
