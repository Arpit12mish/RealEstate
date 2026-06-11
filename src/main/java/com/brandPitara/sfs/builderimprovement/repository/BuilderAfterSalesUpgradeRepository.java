package com.brandPitara.sfs.builderimprovement.repository;

import com.brandPitara.sfs.builderimprovement.entity.BuilderAfterSalesUpgradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuilderAfterSalesUpgradeRepository extends JpaRepository<BuilderAfterSalesUpgradeEntity, Long> {

    Optional<BuilderAfterSalesUpgradeEntity> findByIdAndProfile_IdAndDeletedFalse(
            Long id,
            Long profileId
    );

    List<BuilderAfterSalesUpgradeEntity> findByProfile_IdAndDeletedFalseOrderByDisplayOrderAscIdAsc(
            Long profileId
    );

    List<BuilderAfterSalesUpgradeEntity>
    findByProfile_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(
            Long profileId
    );
}