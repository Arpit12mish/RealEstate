package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

  Optional<ProjectEntity> findByIdAndDeletedFalse(Long id);

  Page<ProjectEntity> findByDeletedFalse(Pageable pageable);

  Page<ProjectEntity> findByBuilderIdAndDeletedFalse(Long builderId, Pageable pageable);

  Page<ProjectEntity> findByBuilderIdAndPublishedTrueAndActiveTrueAndDeletedFalse(Long builderId, Pageable pageable);

  Page<ProjectEntity> findByPublishedTrueAndActiveTrueAndDeletedFalse(Pageable pageable);

  @EntityGraph(attributePaths = {"builder"}) // ✅ avoids lazy loading builder N+1
  List<ProjectEntity> findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalse(Collection<Long> ids);

}
