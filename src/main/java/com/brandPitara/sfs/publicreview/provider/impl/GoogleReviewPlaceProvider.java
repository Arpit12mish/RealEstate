package com.brandPitara.sfs.publicreview.provider.impl;

import com.brandPitara.sfs.publicreview.client.GooglePlaceDetailsResponse;
import com.brandPitara.sfs.publicreview.client.GooglePlacesClient;
import com.brandPitara.sfs.publicreview.dto.GooglePlaceSearchResultItem;
import com.brandPitara.sfs.publicreview.provider.ReviewPlaceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Thin adapter over GooglePlacesClient (the existing HTTP client, unchanged)
 * so business code depends on the ReviewPlaceProvider port instead.
 */
@Component
@RequiredArgsConstructor
public class GoogleReviewPlaceProvider implements ReviewPlaceProvider {

    private final GooglePlacesClient googlePlacesClient;

    @Override
    public GooglePlaceDetailsResponse fetchPlaceDetails(String externalPlaceId) {
        return googlePlacesClient.fetchPlaceDetails(externalPlaceId);
    }

    @Override
    public List<GooglePlaceSearchResultItem> searchPlaces(String textQuery, Double latitude, Double longitude) {
        return googlePlacesClient.searchPlaces(textQuery, latitude, longitude);
    }
}
