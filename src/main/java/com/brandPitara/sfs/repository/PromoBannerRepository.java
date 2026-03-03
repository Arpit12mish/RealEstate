package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.PromoBannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface PromoBannerRepository extends JpaRepository<PromoBannerEntity, Long> {

    List<PromoBannerEntity> findByCategory_IdAndActiveTrueOrderByPriorityAsc(Long categoryId);

    List<PromoBannerEntity> findByCategory_IdAndActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByPriorityAsc(
            Long categoryId,
            OffsetDateTime now1,
            OffsetDateTime now2
    );

    // ✅ FIXED (ActiveTrue)
    List<PromoBannerEntity> findByCategory_IdAndSlotKeyAndActiveTrueOrderByPriorityAsc(
            Long categoryId,
            String slotKey
    );
}