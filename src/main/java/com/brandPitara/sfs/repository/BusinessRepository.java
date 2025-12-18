package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.BusinessEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<BusinessEntity, Long> {

    // city + category (no search text)
    @EntityGraph(attributePaths = {"city", "category"})
    Page<BusinessEntity> findByCity_IdAndCategory_IdAndActiveTrue(
            Long cityId, Long categoryId, Pageable pageable);

    // city only (no search text)
    @EntityGraph(attributePaths = {"city", "category"})
    Page<BusinessEntity> findByCity_IdAndActiveTrue(
            Long cityId, Pageable pageable);

    // city + category + name search
    @EntityGraph(attributePaths = {"city", "category"})
    Page<BusinessEntity> findByCity_IdAndCategory_IdAndActiveTrueAndNameContainingIgnoreCase(
            Long cityId, Long categoryId, String name, Pageable pageable);

    // city + name search
    @EntityGraph(attributePaths = {"city", "category"})
    Page<BusinessEntity> findByCity_IdAndActiveTrueAndNameContainingIgnoreCase(
            Long cityId, String name, Pageable pageable);
}
