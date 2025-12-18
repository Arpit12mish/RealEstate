package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.PromoBannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface PromoBannerRepository extends JpaRepository<PromoBannerEntity, Long> {

    // Active banners for a category, ordered by priority
    List<PromoBannerEntity> findByCategory_IdAndActiveTrueOrderByPriorityAsc(Long categoryId);

    // If you want date-aware banners (optional)
    List<PromoBannerEntity> findByCategory_IdAndActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByPriorityAsc(
            Long categoryId,
            OffsetDateTime now1,
            OffsetDateTime now2
    );
}
