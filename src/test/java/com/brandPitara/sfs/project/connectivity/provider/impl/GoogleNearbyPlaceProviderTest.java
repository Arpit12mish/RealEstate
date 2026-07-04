package com.brandPitara.sfs.project.connectivity.provider.impl;

import com.brandPitara.sfs.project.connectivity.provider.GooglePlacesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GoogleNearbyPlaceProvider request body construction.
 *
 * The Google Places Text Search endpoint (places:searchText) supports
 * locationBias.circle for a soft circle preference but does NOT support
 * locationRestriction.circle (that shape is only valid on places:searchNearby).
 * Using locationRestriction.circle causes HTTP 400 INVALID_ARGUMENT from Google.
 *
 * Hard radius enforcement is done in ProjectConnectivityServiceImpl via
 * distanceMeters <= requestedRadius after the provider returns results.
 */
class GoogleNearbyPlaceProviderTest {

  private GoogleNearbyPlaceProvider provider;

  @BeforeEach
  void setUp() {
    GooglePlacesProperties props = new GooglePlacesProperties();
    props.setEnabled(true);
    props.setApiKey("test-key-for-unit-tests");
    props.setTextSearchUrl("https://places.googleapis.com/v1/places:searchText");
    props.setDefaultLanguageCode("en");
    props.setDefaultRegionCode("IN");
    props.setTimeoutMs(5000);
    props.setMaxResults(20);
    props.setMaxRadiusMeters(10000);
    provider = new GoogleNearbyPlaceProvider(props);
  }

  @Test
  void requestBodyUsesLocationBiasNotLocationRestriction() {
    Map<String, Object> body = provider.buildTextSearchBody(28.4595, 77.0266, "metro station", 3000);

    // locationBias is valid for places:searchText; locationRestriction.circle is NOT.
    assertThat(body).containsKey("locationBias");
    assertThat(body).doesNotContainKey("locationRestriction");
  }

  @Test
  void requestBodyCircleHasCorrectCenterAndRadius() {
    Map<String, Object> body = provider.buildTextSearchBody(28.4595, 77.0266, "metro station", 3000);

    @SuppressWarnings("unchecked")
    Map<String, Object> locationBias = (Map<String, Object>) body.get("locationBias");
    @SuppressWarnings("unchecked")
    Map<String, Object> circle = (Map<String, Object>) locationBias.get("circle");
    @SuppressWarnings("unchecked")
    Map<String, Object> center = (Map<String, Object>) circle.get("center");

    assertThat(center.get("latitude")).isEqualTo(28.4595);
    assertThat(center.get("longitude")).isEqualTo(77.0266);
    assertThat(circle.get("radius")).isEqualTo(3000.0);
  }

  @Test
  void requestBodyIncludesQueryAndLocaleFields() {
    Map<String, Object> body = provider.buildTextSearchBody(28.4595, 77.0266, "metro station", 3000);

    assertThat(body.get("textQuery")).isEqualTo("metro station");
    assertThat(body.get("languageCode")).isEqualTo("en");
    assertThat(body.get("regionCode")).isEqualTo("IN");
  }

  @Test
  void maxRadiusMetersComesFromProperties() {
    assertThat(provider.getMaxRadiusMeters()).isEqualTo(10000);
  }
}
