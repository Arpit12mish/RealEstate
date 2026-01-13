package com.brandPitara.sfs.distributor.repository;

import com.brandPitara.sfs.distributor.entity.DistributorMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistributorMediaRepository extends JpaRepository<DistributorMediaEntity, Long> {

  List<DistributorMediaEntity> findByDistributorIdAndDeletedFalseOrderBySortOrderAsc(Long distributorId);

  Optional<DistributorMediaEntity> findByIdAndDeletedFalse(Long id);

  List<DistributorMediaEntity> findByDistributorIdAndDeletedFalseAndActiveTrueOrderBySortOrderAsc(Long distributorId);
}
