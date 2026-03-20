package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectPriceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectPriceHistoryRepository extends JpaRepository<ProjectPriceHistoryEntity, Long> {
    List<ProjectPriceHistoryEntity> findByProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);
}