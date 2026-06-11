package com.brandPitara.sfs.publicreview.client;

import com.brandPitara.sfs.publicreview.config.GooglePlacesProperties;
import com.brandPitara.sfs.publicreview.dto.GooglePlaceSearchResultItem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    private static final int SEARCH_MAX_RESULTS = 5;

    private final GooglePlacesProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GooglePlaceDetailsResponse fetchPlaceDetails(String googlePlaceId) {
        assertApiKeyPresent();

        if (!StringUtils.hasText(googlePlaceId)) {
            throw new IllegalArgumentException("googlePlaceId is required");
        }

        String encodedPlaceId = URLEncoder.encode(googlePlaceId.trim(), StandardCharsets.UTF_8);
        String baseUrl = normalizeBaseUrl(properties.getBaseUrl());

        URI uri = URI.create(baseUrl + "/places/" + encodedPlaceId);

        HttpRequest request = HttpRequest.newBuilder(uri)
            .GET()
            .header("Content-Type", "application/json")
            .header("X-Goog-Api-Key", properties.getApiKey())
            .header("X-Goog-FieldMask", properties.getFieldMask())
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertSuccess(response);
            return objectMapper.readValue(response.body(), GooglePlaceDetailsResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Google Places API response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Google Places API request interrupted", e);
        }
    }

    /**
     * Text Search (POST) — preview only, not stored. lat/lng bias is optional.
     * API key is never included in logs or responses.
     */
    public List<GooglePlaceSearchResultItem> searchPlaces(
        String textQuery,
        Double latitude,
        Double longitude
    ) {
        assertApiKeyPresent();

        if (!StringUtils.hasText(textQuery)) {
            throw new IllegalArgumentException("textQuery is required");
        }

        String body = buildSearchRequestBody(textQuery.trim(), latitude, longitude);

        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getSearchTextUrl()))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .header("Content-Type", "application/json")
            .header("X-Goog-Api-Key", properties.getApiKey())
            .header("X-Goog-FieldMask",
                "places.id,places.displayName,places.formattedAddress,places.googleMapsUri,places.rating,places.userRatingCount")
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertSuccess(response);
            return parseSearchResponse(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Google Places Text Search response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Google Places Text Search request interrupted", e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildSearchRequestBody(String textQuery, Double latitude, Double longitude) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"textQuery\":\"").append(escape(textQuery)).append("\"");
        sb.append(",\"maxResultCount\":").append(SEARCH_MAX_RESULTS);

        if (latitude != null && longitude != null) {
            sb.append(",\"locationBias\":{");
            sb.append("\"circle\":{");
            sb.append("\"center\":{\"latitude\":").append(latitude).append(",\"longitude\":").append(longitude).append("}");
            sb.append(",\"radius\":5000.0");
            sb.append("}}");
        }

        sb.append("}");
        return sb.toString();
    }

    private List<GooglePlaceSearchResultItem> parseSearchResponse(String body) throws IOException {
        TextSearchApiResponse apiResponse = objectMapper.readValue(body, TextSearchApiResponse.class);
        if (apiResponse.getPlaces() == null) return List.of();

        List<GooglePlaceSearchResultItem> results = new ArrayList<>();
        for (TextSearchApiResponse.SearchPlace p : apiResponse.getPlaces()) {
            results.add(GooglePlaceSearchResultItem.builder()
                .placeId(p.getId())
                .displayName(p.getDisplayName() != null ? p.getDisplayName().getText() : null)
                .formattedAddress(p.getFormattedAddress())
                .googleMapsUri(p.getGoogleMapsUri())
                .rating(p.getRating())
                .userRatingCount(p.getUserRatingCount())
                .build());
        }
        return results;
    }

    private void assertApiKeyPresent() {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("Google Places API key is missing. Set GOOGLE_PLACES_API_KEY.");
        }
    }

    private void assertSuccess(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                "Google Places API failed. status=" + response.statusCode()
            );
        }
    }

    private String normalizeBaseUrl(String url) {
        return (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // -------------------------------------------------------------------------
    // Internal response shapes (Text Search API)
    // -------------------------------------------------------------------------

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TextSearchApiResponse {
        private List<SearchPlace> places;

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class SearchPlace {
            private String id;
            private LocalizedText displayName;
            private String formattedAddress;
            private String googleMapsUri;
            private BigDecimal rating;
            private Integer userRatingCount;
        }

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class LocalizedText {
            private String text;
            private String languageCode;
        }
    }
}
