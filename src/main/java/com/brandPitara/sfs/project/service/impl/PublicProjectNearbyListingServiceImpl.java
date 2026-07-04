package com.brandPitara.sfs.project.service.impl;

import com.brandPitara.sfs.project.dto.ProjectNearbyListingCardDto;
import com.brandPitara.sfs.project.mapper.ProjectNearbyListingMapper;
import com.brandPitara.sfs.project.repository.ProjectNearbyListingProjection;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.project.service.PublicProjectNearbyListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicProjectNearbyListingServiceImpl implements PublicProjectNearbyListingService {

  private static final int DEFAULT_LIMIT = 5;
  private static final int MAX_LIMIT = 10;

  private final ProjectRepository projectRepository;
  private final ProjectFavoriteService projectFavoriteService;

  @Override
  public List<ProjectNearbyListingCardDto> listNearby(Double latitude, Double longitude, Long cityId, int limit) {
    int safeLimit = normalizeLimit(limit);
    boolean hasCoordinates = isValidCoordinate(latitude, longitude);

    List<ProjectNearbyListingProjection> rows;
    if (hasCoordinates) {
      rows = projectRepository.findNearbyPublicProjectCards(latitude, longitude, safeLimit);
    } else if (cityId != null && cityId > 0) {
      rows = projectRepository.findPublicProjectCardsByCity(cityId, safeLimit);
      if (rows.isEmpty()) {
        rows = projectRepository.findFallbackPublicProjectCards(safeLimit);
      }
    } else {
      rows = projectRepository.findFallbackPublicProjectCards(safeLimit);
    }

    List<ProjectNearbyListingCardDto> cards = rows.stream()
        .map(ProjectNearbyListingMapper::toCard)
        .filter(Objects::nonNull)
        .toList();

    projectFavoriteService.enrichNearbyListingCards(cards);
    return cards;
  }

  private int normalizeLimit(int limit) {
    if (limit <= 0) {
      return DEFAULT_LIMIT;
    }
    return Math.min(limit, MAX_LIMIT);
  }

  private boolean isValidCoordinate(Double latitude, Double longitude) {
    return latitude != null
        && longitude != null
        && latitude >= -90.0
        && latitude <= 90.0
        && longitude >= -180.0
        && longitude <= 180.0;
  }
}
