package com.brandPitara.sfs.home.service;

import com.brandPitara.sfs.home.dto.HomeFeedResponse;

public interface HomeFeedService {

  HomeFeedResponse getHome(Long cityId, Long categoryId, Long builderId, Long clientVersion);

  default HomeFeedResponse getHome(Long cityId, Long categoryId, Long clientVersion) {
    return getHome(cityId, categoryId, null, clientVersion);
  }

  default HomeFeedResponse getHome(Long cityId) {
    return getHome(cityId, null, null, null);
  }
}
