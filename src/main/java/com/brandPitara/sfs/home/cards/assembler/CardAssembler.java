package com.brandPitara.sfs.home.cards.assembler;

import com.brandPitara.sfs.home.cards.dto.FeedCardDto;
import com.brandPitara.sfs.home.entity.HomeSectionItemEntity;
import com.brandPitara.sfs.home.enums.HomeSectionItemType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CardAssembler {

  HomeSectionItemType supports();

  // batch fetch to keep API fast (1 query per type)
  Map<Long, Object> prefetchByIds(Set<Long> ids);

  // convert 1 row into 1 card
  FeedCardDto toCard(HomeSectionItemEntity row, Object entity, String variant);
}