package com.brandPitara.sfs.home.service;

import com.brandPitara.sfs.home.dto.FeaturedCarouselCardDto;

import java.util.List;

public interface FeaturedCarouselService {
  List<FeaturedCarouselCardDto> getCarousel(Long cityId, Long categoryId);
}
