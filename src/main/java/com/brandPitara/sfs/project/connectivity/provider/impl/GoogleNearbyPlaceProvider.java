package com.brandPitara.sfs.project.connectivity.provider.impl;

import com.brandPitara.sfs.integration.ExternalProviderException;
import com.brandPitara.sfs.project.connectivity.provider.GooglePlacesProperties;
import com.brandPitara.sfs.project.connectivity.provider.NearbyPlaceProvider;
import com.brandPitara.sfs.project.connectivity.provider.dto.NearbyPlaceProviderResult;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import com.brandPitara.sfs.project.mapper.ProjectConnectivityCategoryMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class GoogleNearbyPlaceProvider implements NearbyPlaceProvider {

  private static final String PROVIDER = "GOOGLE_PLACES";
  private static final String PROVIDER_LABEL = "Google Places";

  private static final String FIELD_MASK = String.join(",",
      "places.id",
      "places.displayName",
      "places.formattedAddress",
      "places.location",
      "places.primaryType",
      "places.rating",
      "places.userRatingCount"
  );

  private final GooglePlacesProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  @Autowired
  public GoogleNearbyPlaceProvider(GooglePlacesProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
        .build();
  }

  @Override
  public List<NearbyPlaceProviderResult> searchNearby(
      Double latitude,
      Double longitude,
      String query,
      ProjectConnectivityCategory category,
      Integer radiusMeters
  ) {
    if (!properties.isEnabled()) {
      throw ExternalProviderException.unavailable(PROVIDER_LABEL, "provider is disabled");
    }
    if (!StringUtils.hasText(properties.getApiKey())) {
      throw ExternalProviderException.unavailable(PROVIDER_LABEL, "configuration is incomplete");
    }
    if (latitude == null || longitude == null) {
      throw new IllegalArgumentException("Project latitude/longitude is required for provider search");
    }

    String cleanedQuery = normalizeQuery(query, category);
    int safeRadius = clampRadius(radiusMeters);

    Map<String, Object> body = buildTextSearchBody(latitude, longitude, cleanedQuery, safeRadius);

    HttpRequest request;
    try {
      request = HttpRequest.newBuilder(URI.create(properties.getTextSearchUrl()))
          .timeout(effectiveResponseTimeout())
          .header("X-Goog-Api-Key", properties.getApiKey())
          .header("X-Goog-FieldMask", FIELD_MASK)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(
              objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
          .build();
    } catch (JsonProcessingException ex) {
      throw ExternalProviderException.upstreamFailure(PROVIDER_LABEL, ex);
    } catch (IllegalArgumentException | NullPointerException ex) {
      throw ExternalProviderException.unavailable(PROVIDER_LABEL, "configuration is invalid");
    }

    GooglePlacesResponse response;
    try {
      HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
        throw ExternalProviderException.upstreamStatus(PROVIDER_LABEL, httpResponse.statusCode());
      }
      response = objectMapper.readValue(httpResponse.body(), GooglePlacesResponse.class);
    } catch (HttpTimeoutException ex) {
      throw ExternalProviderException.timeout(PROVIDER_LABEL, ex);
    } catch (IOException ex) {
      throw ExternalProviderException.upstreamFailure(PROVIDER_LABEL, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw ExternalProviderException.unavailable(PROVIDER_LABEL, "request interrupted");
    }

    if (response == null || response.places() == null) {
      return List.of();
    }

    int maxResults = properties.getMaxResults() > 0 ? properties.getMaxResults() : 20;

    return response.places().stream()
        .filter(Objects::nonNull)
        .map(place -> toProviderResult(place, latitude, longitude, category))
        .filter(result -> StringUtils.hasText(result.getPlaceName()))
        .limit(maxResults)
        .toList();
  }

  private int clampRadius(Integer radiusMeters) {
    int max = properties.getMaxRadiusMeters() > 0 ? properties.getMaxRadiusMeters() : 10000;
    return radiusMeters != null ? Math.min(Math.max(radiusMeters, 100), max) : 5000;
  }

  @Override
  public int getMaxRadiusMeters() {
    return properties.getMaxRadiusMeters() > 0 ? properties.getMaxRadiusMeters() : 10000;
  }

  /** See GooglePlacesClient: the smaller read/request bound limits the full response exchange. */
  private Duration effectiveResponseTimeout() {
    return Duration.ofMillis(Math.min(properties.getReadTimeoutMs(), properties.getRequestTimeoutMs()));
  }

  // Package-private so the shape can be verified in unit tests without invoking Google.
  // places:searchText supports locationBias.circle (soft preference) but NOT locationRestriction.circle
  // (that shape is only valid on places:searchNearby). Hard radius enforcement is done in the service layer.
  Map<String, Object> buildTextSearchBody(Double latitude, Double longitude, String query, int radiusMeters) {
    Map<String, Object> center = Map.of("latitude", latitude, "longitude", longitude);
    Map<String, Object> circle = Map.of("center", center, "radius", (double) radiusMeters);
    Map<String, Object> locationBias = Map.of("circle", circle);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("textQuery", query);
    body.put("locationBias", locationBias);
    body.put("languageCode", properties.getDefaultLanguageCode());
    body.put("regionCode", properties.getDefaultRegionCode());
    return body;
  }

  private NearbyPlaceProviderResult toProviderResult(
      GooglePlace place,
      Double projectLat,
      Double projectLng,
      ProjectConnectivityCategory requestedCategory
  ) {
    String name = place.displayName() != null ? place.displayName().text() : null;
    Double lat = place.location() != null ? place.location().latitude() : null;
    Double lng = place.location() != null ? place.location().longitude() : null;

    Integer distanceMeters = null;
    String distanceLabel = null;
    if (lat != null && lng != null) {
      distanceMeters = haversineMeters(projectLat, projectLng, lat, lng);
      distanceLabel = formatDistance(distanceMeters);
    }

    ProjectConnectivityType type = mapGoogleTypeToConnectivityType(place.primaryType(), requestedCategory);
    ProjectConnectivityCategory category = requestedCategory != null
        ? requestedCategory
        : ProjectConnectivityCategoryMapper.fromType(type);

    return NearbyPlaceProviderResult.builder()
        .placeName(name)
        .placeType(type)
        .category(category)
        .latitude(lat)
        .longitude(lng)
        .distanceMeters(distanceMeters)
        .distanceLabel(distanceLabel)
        .durationSeconds(null)
        .durationLabel(null)
        .address(place.formattedAddress())
        .provider(PROVIDER)
        .externalPlaceId(place.id())
        .rating(place.rating() != null ? BigDecimal.valueOf(place.rating()) : null)
        .userRatingCount(place.userRatingCount())
        .alreadySaved(null)
        .build();
  }

  private String normalizeQuery(String query, ProjectConnectivityCategory category) {
    if (StringUtils.hasText(query)) {
      return query.trim();
    }
    if (category == null) return "nearby places";
    return switch (category) {
      case TRANSIT -> "metro station bus stop";
      case SCHOOLS -> "school";
      case COLLEGES -> "college university";
      case HOSPITALS -> "hospital clinic";
      case PARKS -> "park";
      case RETAIL -> "retail shop market";
      case MALLS -> "shopping mall";
      case GYMS -> "gym fitness center";
      case OFFICES -> "IT park office hub";
      case RESTAURANTS -> "restaurant cafe";
      case BANKS -> "bank ATM";
      case DAILY_NEEDS -> "supermarket grocery store pharmacy";
      case SAFETY -> "police station fire station";
      case LIFESTYLE -> "temple landmark";
      case SEARCH -> "nearby places";
    };
  }

  private ProjectConnectivityType mapGoogleTypeToConnectivityType(
      String primaryType,
      ProjectConnectivityCategory requestedCategory
  ) {
    if (primaryType == null) return fallbackType(requestedCategory);
    return switch (primaryType.toLowerCase(Locale.ROOT)) {
      case "school", "primary_school", "secondary_school" -> ProjectConnectivityType.SCHOOL;
      case "university" -> ProjectConnectivityType.UNIVERSITY;
      case "hospital" -> ProjectConnectivityType.HOSPITAL;
      case "doctor", "clinic" -> ProjectConnectivityType.CLINIC;
      case "pharmacy" -> ProjectConnectivityType.PHARMACY;
      case "subway_station", "transit_station", "light_rail_station" -> ProjectConnectivityType.METRO;
      case "bus_station", "bus_stop" -> ProjectConnectivityType.BUS_STOP;
      case "train_station" -> ProjectConnectivityType.RAILWAY_STATION;
      case "airport" -> ProjectConnectivityType.AIRPORT;
      case "shopping_mall" -> ProjectConnectivityType.MALL;
      case "supermarket", "grocery_or_supermarket" -> ProjectConnectivityType.SUPERMARKET;
      case "grocery_store" -> ProjectConnectivityType.GROCERY_STORE;
      case "convenience_store" -> ProjectConnectivityType.CONVENIENCE_STORE;
      case "restaurant" -> ProjectConnectivityType.RESTAURANT;
      case "cafe" -> ProjectConnectivityType.CAFE;
      case "park" -> ProjectConnectivityType.PARK;
      case "gym" -> ProjectConnectivityType.GYM;
      case "fitness_center" -> ProjectConnectivityType.FITNESS_CENTER;
      case "stadium" -> ProjectConnectivityType.STADIUM;
      case "bank" -> ProjectConnectivityType.BANK;
      case "atm" -> ProjectConnectivityType.ATM;
      case "police", "police_station" -> ProjectConnectivityType.POLICE_STATION;
      case "fire_station" -> ProjectConnectivityType.FIRE_STATION;
      case "hindu_temple" -> ProjectConnectivityType.TEMPLE;
      case "church" -> ProjectConnectivityType.CHURCH;
      case "mosque" -> ProjectConnectivityType.MOSQUE;
      default -> fallbackType(requestedCategory);
    };
  }

  private ProjectConnectivityType fallbackType(ProjectConnectivityCategory category) {
    if (category == null) return ProjectConnectivityType.OTHER;
    return switch (category) {
      case TRANSIT -> ProjectConnectivityType.METRO;
      case SCHOOLS -> ProjectConnectivityType.SCHOOL;
      case COLLEGES -> ProjectConnectivityType.COLLEGE;
      case HOSPITALS -> ProjectConnectivityType.HOSPITAL;
      case PARKS -> ProjectConnectivityType.PARK;
      case RETAIL -> ProjectConnectivityType.RETAIL_SHOP;
      case MALLS -> ProjectConnectivityType.MALL;
      case GYMS -> ProjectConnectivityType.GYM;
      case OFFICES -> ProjectConnectivityType.BUSINESS_HUB;
      case RESTAURANTS -> ProjectConnectivityType.RESTAURANT;
      case BANKS -> ProjectConnectivityType.BANK;
      case DAILY_NEEDS -> ProjectConnectivityType.SUPERMARKET;
      case SAFETY -> ProjectConnectivityType.POLICE_STATION;
      case LIFESTYLE -> ProjectConnectivityType.LANDMARK;
      case SEARCH -> ProjectConnectivityType.OTHER;
    };
  }

  private Integer haversineMeters(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6_371_000;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return (int) Math.round(R * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
  }

  private String formatDistance(Integer meters) {
    if (meters == null) return null;
    if (meters < 1000) return meters + " m";
    return String.format(Locale.US, "%.1f km", meters / 1000.0);
  }

  public record GooglePlacesResponse(List<GooglePlace> places) {}

  public record GooglePlace(
      String id,
      GoogleDisplayName displayName,
      String formattedAddress,
      GoogleLocation location,
      String primaryType,
      Double rating,
      Integer userRatingCount
  ) {}

  public record GoogleDisplayName(String text, String languageCode) {}

  public record GoogleLocation(Double latitude, Double longitude) {}
}
