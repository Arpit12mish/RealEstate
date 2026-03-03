package com.brandPitara.sfs.feed.repository;

import com.brandPitara.sfs.feed.entity.FeedSectionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedSectionItemRepository extends JpaRepository<FeedSectionItemEntity, Long> {

  List<FeedSectionItemEntity> findByConfig_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long configId);
}