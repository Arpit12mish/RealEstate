package com.brandPitara.sfs.dashboard.category.service.impl;

import com.brandPitara.sfs.dashboard.category.dto.DashboardCategoryUpsertRequest;
import com.brandPitara.sfs.dashboard.category.service.DashboardCategoryService;
import com.brandPitara.sfs.dto.CategoryResponse;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardCategoryServiceImpl implements DashboardCategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse create(DashboardCategoryUpsertRequest request) {
        if (categoryRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already in use: " + request.getSlug());
        }

        CategoryEntity parent = resolveParent(request.getParentId());

        CategoryEntity entity = CategoryEntity.builder()
                .name(request.getName().trim())
                .slug(request.getSlug().trim())
                .parent(parent)
                .priority(request.getPriority() != null ? request.getPriority() : 100)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return toResponse(categoryRepository.save(entity));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long categoryId, DashboardCategoryUpsertRequest request) {
        CategoryEntity entity = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));

        categoryRepository.findBySlug(request.getSlug())
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(c -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already in use: " + request.getSlug()); });

        entity.setName(request.getName().trim());
        entity.setSlug(request.getSlug().trim());
        if (request.getParentId() != null) entity.setParent(resolveParent(request.getParentId()));
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getActive() != null)   entity.setActive(request.getActive());

        return toResponse(categoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("Category not found: " + categoryId);
        }
        categoryRepository.deleteById(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse get(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findByActiveTrueOrderByPriorityAscIdAsc()
                .stream().map(this::toResponse).toList();
    }

    private CategoryEntity resolveParent(Long parentId) {
        if (parentId == null) return null;
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Parent category not found: " + parentId));
    }

    private CategoryResponse toResponse(CategoryEntity e) {
        return CategoryResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .slug(e.getSlug())
                .parentId(e.getParent() != null ? e.getParent().getId() : null)
                .priority(e.getPriority())
                .active(e.getActive())
                .build();
    }
}
