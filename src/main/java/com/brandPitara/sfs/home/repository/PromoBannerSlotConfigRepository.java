package com.brandPitara.sfs.home.repository;

import com.brandPitara.sfs.home.entity.PromoBannerSlotConfigEntity;
import com.brandPitara.sfs.feed.enums.FeedScreen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromoBannerSlotConfigRepository extends JpaRepository<PromoBannerSlotConfigEntity, Long> {

  List<PromoBannerSlotConfigEntity>
  findByScreenAndHomeCategory_IdAndActiveTrueOrderByPriorityAscIdAsc(FeedScreen screen, Long homeCategoryId);

  List<PromoBannerSlotConfigEntity>
  findByScreenAndHomeCategory_IdAndCity_IdAndActiveTrueOrderByPriorityAscIdAsc(FeedScreen screen, Long homeCategoryId, Long cityId);
}