package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    Optional<CategoryEntity> findBySlug(String slug);

    // existing (kept for internal use if needed)
    List<CategoryEntity> findByParentIsNullOrderByPriorityAsc();
    List<CategoryEntity> findByParentIdOrderByPriorityAsc(Long parentId);

    // new: only active root categories
    List<CategoryEntity> findByParentIsNullAndActiveTrueOrderByPriorityAsc();

    // new: only active children by parent
    List<CategoryEntity> findByParentIdAndActiveTrueOrderByPriorityAsc(Long parentId);
}
