package com.brandPitara.sfs.marketplace.repository;

import com.brandPitara.sfs.marketplace.entity.BusinessReviewEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BusinessReviewRepository extends JpaRepository<BusinessReviewEntity, Long> {

    Page<BusinessReviewEntity> findByBusinessIdAndModerationStatusAndDeletedFalse(
            Long businessId, BusinessReviewStatus status, Pageable pageable);

    Page<BusinessReviewEntity> findByModerationStatusAndDeletedFalse(BusinessReviewStatus status, Pageable pageable);

    boolean existsByBusinessIdAndUserIdAndDeletedFalse(Long businessId, Long userId);

    Optional<BusinessReviewEntity> findByIdAndDeletedFalse(Long id);

    List<BusinessReviewEntity> findByUserIdAndDeletedFalseOrderByCreatedAtDescIdDesc(Long userId);

    /** [count, avg] of APPROVED, non-deleted reviews. */
    @Query("""
            select count(r), avg(r.rating) from BusinessReviewEntity r
            where r.businessId = :businessId
              and r.moderationStatus = com.brandPitara.sfs.marketplace.enums.BusinessReviewStatus.APPROVED
              and r.deleted = false
            """)
    List<Object[]> approvedAggregate(@Param("businessId") Long businessId);
}
