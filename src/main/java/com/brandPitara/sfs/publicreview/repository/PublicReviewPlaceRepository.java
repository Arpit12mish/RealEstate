package com.brandPitara.sfs.publicreview.repository;

import com.brandPitara.sfs.publicreview.entity.PublicReviewPlaceEntity;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PublicReviewPlaceRepository extends JpaRepository<PublicReviewPlaceEntity, Long> {

    Optional<PublicReviewPlaceEntity> findByIdAndTargetTypeAndTargetIdAndDeletedFalse(
        Long id,
        PublicReviewTargetType targetType,
        Long targetId
    );

    /**
     * Locks the place row for the duration of the caller's transaction so
     * concurrent Google-sync attempts for the same place serialize instead of
     * both observing "not yet fetched" and both calling Google / both trying
     * to create the summary row (see PublicReviewServiceImpl#prepareGoogleSync).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PublicReviewPlaceEntity p " +
        "WHERE p.id = :id AND p.targetType = :targetType AND p.targetId = :targetId AND p.deleted = false")
    Optional<PublicReviewPlaceEntity> findByIdAndTargetTypeAndTargetIdAndDeletedFalseForUpdate(
        @Param("id") Long id,
        @Param("targetType") PublicReviewTargetType targetType,
        @Param("targetId") Long targetId
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
