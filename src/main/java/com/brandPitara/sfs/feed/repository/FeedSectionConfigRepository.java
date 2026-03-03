package com.brandPitara.sfs.feed.repository;

import com.brandPitara.sfs.feed.entity.FeedSectionConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedSectionConfigRepository extends JpaRepository<FeedSectionConfigEntity, Long> {

  List<FeedSectionConfigEntity> findByScreenAndEnabledTrueOrderBySortOrderAscIdAsc(String screen);

  List<FeedSectionConfigEntity> findByScreenAndEntityIdAndEnabledTrueOrderBySortOrderAscIdAsc(String screen, Long entityId);

  List<FeedSectionConfigEntity> findByScreenAndCategoryIdAndEnabledTrueOrderBySortOrderAscIdAsc(String screen, Long categoryId);
}