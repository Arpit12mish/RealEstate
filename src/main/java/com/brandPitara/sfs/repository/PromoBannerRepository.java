package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.PromoBannerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PromoBannerRepository extends JpaRepository<PromoBannerEntity, Long>,
        JpaSpecificationExecutor<PromoBannerEntity> {

    List<PromoBannerEntity> findByCategory_IdAndActiveTrueAndDeletedFalseOrderByPriorityAsc(Long categoryId);

    List<PromoBannerEntity> findByCategory_IdAndActiveTrueAndDeletedFalseAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByPriorityAsc(
            Long categoryId,
            OffsetDateTime now1,
            OffsetDateTime now2
    );

    // ✅ FIXED (ActiveTrue)
    List<PromoBannerEntity> findByCategory_IdAndSlotKeyAndActiveTrueAndDeletedFalseOrderByPriorityAsc(
            Long categoryId,
            String slotKey
    );

    List<PromoBannerEntity> findByCategory_IdAndSlotKeyAndActiveTrueAndDeletedFalseOrderByPriorityAscIdAsc(
            Long categoryId,
            String slotKey,
            Pageable pageable
    );

    Optional<PromoBannerEntity> findByIdAndDeletedFalse(Long id);

    boolean existsByIdAndDeletedFalse(Long id);
}
