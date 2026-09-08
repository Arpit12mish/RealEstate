package com.brandPitara.sfs.publicreview.client;

import com.brandPitara.sfs.integration.ExternalProviderException;
import com.brandPitara.sfs.publicreview.config.GooglePlacesProperties;
import com.brandPitara.sfs.publicreview.dto.GooglePlaceSearchResultItem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class GooglePlacesClient {

    private static final int SEARCH_MAX_RESULTS = 5;
    private static final String PROVIDER = "Google Places";

    private final GooglePlacesProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    public GooglePlacesClient(GooglePlacesProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
            .build();
    }

    public GooglePlaceDetailsResponse fetchPlaceDetails(String googlePlaceId) {
        assertApiKeyPresent();

        if (!StringUtils.hasText(googlePlaceId)) {
            throw new IllegalArgumentException("googlePlaceId is required");
        }

        String encodedPlaceId = URLEncoder.encode(googlePlaceId.trim(), StandardCharsets.UTF_8);
        String baseUrl = normalizeBaseUrl(properties.getBaseUrl());

        HttpRequest request;
        try {
            URI uri = URI.create(baseUrl + "/places/" + encodedPlaceId);
            request = HttpRequest.newBuilder(uri)
                .timeout(effectiveResponseTimeout())
                .GET()
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", properties.getApiKey())
                .header("X-Goog-FieldMask", properties.getFieldMask())
                .build();
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw ExternalProviderException.unavailable(PROVIDER, "configuration is invalid");
        }

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertSuccess(response);
            return objectMapper.readValue(response.body(), GooglePlaceDetailsResponse.class);
        } catch (HttpTimeoutException e) {
            throw ExternalProviderException.timeout(PROVIDER, e);
        } catch (IOException e) {
            throw ExternalProviderException.upstreamFailure(PROVIDER, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ExternalProviderException.unavailable(PROVIDER, "request interrupted");
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

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(properties.getSearchTextUrl()))
                .timeout(effectiveResponseTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", properties.getApiKey())
                .header("X-Goog-FieldMask",
                    "places.id,places.displayName,places.formattedAddress,places.googleMapsUri,places.rating,places.userRatingCount")
                .build();
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw ExternalProviderException.unavailable(PROVIDER, "configuration is invalid");
        }

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertSuccess(response);
            return parseSearchResponse(response.body());
        } catch (HttpTimeoutException e) {
            throw ExternalProviderException.timeout(PROVIDER, e);
        } catch (IOException e) {
            throw ExternalProviderException.upstreamFailure(PROVIDER, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ExternalProviderException.unavailable(PROVIDER, "request interrupted");
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
            throw ExternalProviderException.unavailable(PROVIDER, "configuration is incomplete");
        }
    }

    private void assertSuccess(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw ExternalProviderException.upstreamStatus(PROVIDER, response.statusCode());
        }
    }

    /**
     * java.net.http exposes one full-response timeout rather than a separate socket-read timeout.
     * The smaller configured read/request bound is therefore applied to the complete exchange.
     */
    private Duration effectiveResponseTimeout() {
        return Duration.ofMillis(Math.min(properties.getReadTimeoutMs(), properties.getRequestTimeoutMs()));
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
