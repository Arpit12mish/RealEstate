package com.brandPitara.sfs.home.repository;

import com.brandPitara.sfs.home.entity.ProjectAnalyticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectAnalyticsRepository extends JpaRepository<ProjectAnalyticsEntity, Long> {

  List<ProjectAnalyticsEntity> findByCategory_IdAndBuilder_IdAndActiveTrueAndDeletedFalseOrderByPriorityAscIdAsc(
      Long categoryId, Long builderId
  );

  List<ProjectAnalyticsEntity> findByCategory_IdAndBuilderIsNullAndActiveTrueAndDeletedFalseOrderByPriorityAscIdAsc(
      Long categoryId
  );
}
