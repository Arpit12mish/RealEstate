package com.brandPitara.sfs.home.repository;

import com.brandPitara.sfs.home.entity.FeaturedCarouselConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface FeaturedCarouselConfigRepository extends JpaRepository<FeaturedCarouselConfigEntity, Long> {

  // city-specific config if present, else fallback to city_id is null
  List<FeaturedCarouselConfigEntity> findByCategoryIdAndActiveTrueAndCityIdOrderByPriorityAscPositionAsc(
      Long categoryId, Long cityId
  );

  List<FeaturedCarouselConfigEntity> findByCategoryIdAndActiveTrueAndCityIdIsNullOrderByPriorityAscPositionAsc(
      Long categoryId
  );
}
