package com.brandPitara.sfs.home.repository;

import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeSectionConfigRepository extends JpaRepository<HomeSectionConfigEntity, Long> {
  List<HomeSectionConfigEntity> findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(Long categoryId);
}
