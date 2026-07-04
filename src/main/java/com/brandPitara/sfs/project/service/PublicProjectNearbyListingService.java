package com.brandPitara.sfs.project.service;

import com.brandPitara.sfs.project.dto.ProjectNearbyListingCardDto;

import java.util.List;

public interface PublicProjectNearbyListingService {
  List<ProjectNearbyListingCardDto> listNearby(Double latitude, Double longitude, Long cityId, int limit);
}
