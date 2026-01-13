package com.brandPitara.sfs.builder.repository;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuilderRepository extends JpaRepository<BuilderEntity, Long> {

  Optional<BuilderEntity> findByIdAndDeletedFalse(Long id);

  Page<BuilderEntity> findByDeletedFalse(Pageable pageable);

  Page<BuilderEntity> findByPublishedAndDeletedFalse(boolean published, Pageable pageable);

  Page<BuilderEntity> findByActiveAndDeletedFalse(boolean active, Pageable pageable);

  Page<BuilderEntity> findByPublishedAndActiveAndDeletedFalse(boolean published, boolean active, Pageable pageable);

  Page<BuilderEntity> findByPublishedTrueAndActiveTrueAndDeletedFalse(Pageable pageable);

  List<BuilderEntity> findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc();

  List<BuilderEntity> findTop20ByPublishedTrueAndActiveTrueAndDeletedFalseAndCity_IdOrderByPriorityAscIdDesc(Long cityId);
}
