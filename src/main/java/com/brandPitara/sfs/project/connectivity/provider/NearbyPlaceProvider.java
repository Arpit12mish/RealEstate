package com.brandPitara.sfs.project.connectivity.provider;

import com.brandPitara.sfs.project.connectivity.provider.dto.NearbyPlaceProviderResult;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;

import java.util.List;

public interface NearbyPlaceProvider {

  List<NearbyPlaceProviderResult> searchNearby(
      Double latitude,
      Double longitude,
      String query,
      ProjectConnectivityCategory category,
      Integer radiusMeters
  );
}
