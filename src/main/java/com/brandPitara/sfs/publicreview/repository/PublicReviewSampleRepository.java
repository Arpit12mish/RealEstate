package com.brandPitara.sfs.publicreview.repository;

import com.brandPitara.sfs.publicreview.entity.PublicReviewSampleEntity;
import com.brandPitara.sfs.publicreview.enums.PublicReviewDisplayStatus;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PublicReviewSampleRepository extends JpaRepository<PublicReviewSampleEntity, Long> {

    void deleteByReviewPlaceId(Long reviewPlaceId);

    long countByReviewPlaceId(Long reviewPlaceId);

    List<PublicReviewSampleEntity> findByReviewPlaceIdOrderByRatingAscIdDesc(Long reviewPlaceId);

    List<PublicReviewSampleEntity> findByReviewPlaceIdAndDisplayStatusOrderByRatingAscIdDesc(
        Long reviewPlaceId,
        PublicReviewDisplayStatus displayStatus
    );

    List<PublicReviewSampleEntity> findByTargetTypeAndTargetIdAndDisplayStatusOrderByRatingAscIdDesc(
        PublicReviewTargetType targetType,
        Long targetId,
        PublicReviewDisplayStatus displayStatus
    );
}
