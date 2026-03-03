package com.brandPitara.sfs.home.service.section;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;

public interface HomeSectionLoader {
  HomeSectionType supports();
  HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx);
}
