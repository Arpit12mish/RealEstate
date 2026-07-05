package com.brandPitara.sfs.publicreview.provider;

import com.brandPitara.sfs.publicreview.client.GooglePlaceDetailsResponse;
import com.brandPitara.sfs.publicreview.dto.GooglePlaceSearchResultItem;

import java.util.List;

/**
 * App-owned port for fetching/searching third-party review places. Mirrors
 * the existing NearbyPlaceProvider/GoogleNearbyPlaceProvider pattern in
 * com.brandPitara.sfs.project.connectivity.provider - PublicReviewServiceImpl
 * depends on this, not on GooglePlacesClient directly.
 */
public interface ReviewPlaceProvider {

    GooglePlaceDetailsResponse fetchPlaceDetails(String externalPlaceId);

    List<GooglePlaceSearchResultItem> searchPlaces(String textQuery, Double latitude, Double longitude);
}
