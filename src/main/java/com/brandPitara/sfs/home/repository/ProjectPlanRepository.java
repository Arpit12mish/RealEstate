package com.brandPitara.sfs.home.repository;

import com.brandPitara.sfs.home.entity.ProjectPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectPlanRepository extends JpaRepository<ProjectPlanEntity, Long> {

  List<ProjectPlanEntity> findByCategory_IdAndBuilder_IdAndActiveTrueAndDeletedFalseOrderByPriorityAscIdAsc(
      Long categoryId, Long builderId
  );

  List<ProjectPlanEntity> findByCategory_IdAndBuilderIsNullAndActiveTrueAndDeletedFalseOrderByPriorityAscIdAsc(
      Long categoryId
  );
}
