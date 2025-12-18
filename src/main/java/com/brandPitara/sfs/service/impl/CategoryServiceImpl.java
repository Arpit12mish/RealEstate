package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.CategoryResponse;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getRootCategories() {
        List<CategoryEntity> entities =
                categoryRepository.findByParentIsNullAndActiveTrueOrderByPriorityAsc();

        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> getChildren(Long parentId) {
        // Optional: validate parent exists & active
        categoryRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + parentId));

        List<CategoryEntity> entities =
                categoryRepository.findByParentIdAndActiveTrueOrderByPriorityAsc(parentId);

        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(CategoryEntity e) {
        Long parentId = (e.getParent() != null ? e.getParent().getId() : null);
        return CategoryResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .slug(e.getSlug())
                .parentId(parentId)
                .priority(e.getPriority())
                .active(e.getActive())
                .build();
    }
}
