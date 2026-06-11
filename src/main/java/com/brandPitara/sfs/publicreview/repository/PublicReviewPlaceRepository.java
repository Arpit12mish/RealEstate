package com.brandPitara.sfs.publicreview.repository;

import com.brandPitara.sfs.publicreview.entity.PublicReviewPlaceEntity;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicReviewPlaceRepository extends JpaRepository<PublicReviewPlaceEntity, Long> {

    Optional<PublicReviewPlaceEntity> findByIdAndTargetTypeAndTargetIdAndDeletedFalse(
        Long id,
        PublicReviewTargetType targetType,
        Long targetId
    );

    Optional<PublicReviewPlaceEntity> findByTargetTypeAndTargetIdAndGooglePlaceIdAndDeletedFalse(
        PublicReviewTargetType targetType,
        Long targetId,
        String googlePlaceId
    );

    Optional<PublicReviewPlaceEntity> findByTargetTypeAndTargetIdAndGooglePlaceId(
        PublicReviewTargetType targetType,
        Long targetId,
        String googlePlaceId
    );

    List<PublicReviewPlaceEntity> findByTargetTypeAndTargetIdAndDeletedFalseOrderByIdDesc(
        PublicReviewTargetType targetType,
        Long targetId
    );

    List<PublicReviewPlaceEntity> findByTargetTypeAndTargetIdAndActiveTrueAndDeletedFalseOrderByIdDesc(
        PublicReviewTargetType targetType,
        Long targetId
    );
}
