package com.brandPitara.sfs.home.service.impl;

import com.brandPitara.sfs.home.dto.FeaturedCarouselCardDto;
import com.brandPitara.sfs.home.repository.FeaturedCarouselConfigRepository;
import com.brandPitara.sfs.home.service.FeaturedCarouselService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeaturedCarouselServiceImpl implements FeaturedCarouselService {

  private final FeaturedCarouselConfigRepository repo;

  @Override
  public List<FeaturedCarouselCardDto> getCarousel(Long cityId, Long categoryId) {
    if (categoryId == null) return List.of();

    // 1) Try city-specific config
    List<?> rows = (cityId == null)
        ? List.of()
        : repo.findByCategoryIdAndActiveTrueAndCityIdOrderByPriorityAscPositionAsc(categoryId, cityId);

    // 2) Fallback to global config (city_id is null)
    if (rows.isEmpty()) {
      rows = repo.findByCategoryIdAndActiveTrueAndCityIdIsNullOrderByPriorityAscPositionAsc(categoryId);
    }

    var list = rows.stream()
        .map(r -> (com.brandPitara.sfs.home.entity.FeaturedCarouselConfigEntity) r)
        .sorted(Comparator.comparingInt(x -> x.getPosition() == null ? 99 : x.getPosition()))
        .map(x -> FeaturedCarouselCardDto.builder()
            .variant(x.getVariant())
            .position(x.getPosition())
            .title(x.getTitle())
            .subtitle(x.getSubtitle())
            .imageUrl(x.getImageUrl())
            .logoUrl(x.getLogoUrl())
            .entityType(x.getEntityType())
            .entityId(x.getEntityId())
            .targetUrl(x.getTargetUrl())
            .build()
        )
        .toList();

    // ✅ Must be usable by BrandCarousel UI: need all 3 variants
    boolean hasTall = list.stream().anyMatch(x -> "TALL".equalsIgnoreCase(x.getVariant()));
    boolean hasTop = list.stream().anyMatch(x -> "SMALL_TOP".equalsIgnoreCase(x.getVariant()));
    boolean hasBottom = list.stream().anyMatch(x -> "SMALL_BOTTOM".equalsIgnoreCase(x.getVariant()));

    if (!(hasTall && hasTop && hasBottom)) return List.of();

    return list;
  }
}
