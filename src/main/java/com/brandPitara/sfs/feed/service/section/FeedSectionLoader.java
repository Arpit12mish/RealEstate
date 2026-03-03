package com.brandPitara.sfs.feed.service.section;

import com.brandPitara.sfs.feed.entity.FeedSectionConfigEntity;
import com.brandPitara.sfs.home.dto.HomeSectionDto;

public interface FeedSectionLoader {

  /**
   * Must match FeedSectionConfigEntity.sectionType from DB
   * Example: "BUILDER_ABOUT", "BUILDER_PROJECTS"
   */
  String supports();

  HomeSectionDto<?> load(FeedSectionConfigEntity cfg, FeedContext ctx);
}