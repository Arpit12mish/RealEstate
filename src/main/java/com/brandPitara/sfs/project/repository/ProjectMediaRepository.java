package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMediaRepository extends JpaRepository<ProjectMediaEntity, Long> {

  List<ProjectMediaEntity> findByProjectIdAndDeletedFalseOrderBySortOrderAscIdDesc(Long projectId);

  List<ProjectMediaEntity> findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(Long projectId);

  @Query("""
    select m
    from ProjectMediaEntity m
    where m.project.id = :projectId
      and m.mediaType = com.brandPitara.sfs.project.enums.ProjectMediaType.IMAGE
      and m.active = true
      and m.deleted = false
    order by m.sortOrder asc, m.id asc
  """)
  List<ProjectMediaEntity> findPublicImagesByProjectId(@Param("projectId") Long projectId);

  Optional<ProjectMediaEntity> findByIdAndProjectIdAndDeletedFalse(Long id, Long projectId);

  @Query("""
    select m
    from ProjectMediaEntity m
    where m.deleted = false
      and m.active = true
      and m.project.id in :projectIds
    order by m.project.id asc, m.sortOrder asc, m.id asc
  """)
  List<ProjectMediaEntity> findActiveByProjectIds(@Param("projectIds") List<Long> projectIds);

}
