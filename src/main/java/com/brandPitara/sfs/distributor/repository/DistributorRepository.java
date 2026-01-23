package com.brandPitara.sfs.distributor.repository;

import com.brandPitara.sfs.distributor.entity.DistributorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistributorRepository extends JpaRepository<DistributorEntity, Long> {

  Optional<DistributorEntity> findByIdAndDeletedFalse(Long id);

  Page<DistributorEntity> findByDeletedFalse(Pageable pageable);

  Page<DistributorEntity> findByCityIdAndActiveTrueAndDeletedFalse(Long cityId, Pageable pageable);
  
}
