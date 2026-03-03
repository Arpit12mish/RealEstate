package com.brandPitara.sfs.home.repository;

import com.brandPitara.sfs.home.entity.HomeSectionItemEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeSectionItemRepository extends JpaRepository<HomeSectionItemEntity, Long> {

  List<HomeSectionItemEntity> findByHomeCategory_IdAndSectionTypeAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(
      Long categoryId,
      HomeSectionType sectionType
  );
  List<HomeSectionItemEntity> findByConfig_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long configId);
  List<HomeSectionItemEntity> findByHomeCategory_IdAndSectionTypeAndGroupKeyAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(
      Long homeCategoryId,
      HomeSectionType sectionType,
      String groupKey
  );
}
