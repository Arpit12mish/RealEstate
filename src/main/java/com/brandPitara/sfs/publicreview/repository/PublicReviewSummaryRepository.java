package com.brandPitara.sfs.publicreview.repository;

import com.brandPitara.sfs.publicreview.entity.PublicReviewSummaryEntity;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicReviewSummaryRepository extends JpaRepository<PublicReviewSummaryEntity, Long> {

    Optional<PublicReviewSummaryEntity> findByReviewPlaceId(Long reviewPlaceId);

    List<PublicReviewSummaryEntity> findByTargetTypeAndTargetIdOrderByLastSyncedAtDesc(
        PublicReviewTargetType targetType,
        Long targetId
    );
}
