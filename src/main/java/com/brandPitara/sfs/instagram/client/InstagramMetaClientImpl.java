package com.brandPitara.sfs.instagram.client;

import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class InstagramMetaClientImpl implements InstagramMetaClient {

    private static final String GRAPH_BASE_URL = "https://graph.facebook.com";
    private static final String MEDIA_FIELDS = String.join(",",
        "id",
        "caption",
        "media_type",
        "media_product_type",
        "permalink",
        "thumbnail_url",
        "timestamp",
        "like_count",
        "comments_count"
    );
    private static final int MAX_MEDIA_FETCH = 100;
    private static final int MAX_PAGES = 5;

    private final InstagramMetaProperties properties;
    private final RestClient restClient;

    public InstagramMetaClientImpl(InstagramMetaProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.effectiveRequestTimeoutSeconds());
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
            .baseUrl(GRAPH_BASE_URL)
            .requestFactory(factory)
            .build();
    }

    @Override
    public List<InstagramMetaMedia> fetchMedia() {
        List<InstagramMetaMedia> result = new ArrayList<>();
        String nextUrl = null;
        int page = 0;

        while (page < MAX_PAGES && result.size() < MAX_MEDIA_FETCH) {
            MetaMediaResponse response = fetchMediaPage(nextUrl);
            if (response == null || response.data() == null || response.data().isEmpty()) {
                break;
            }

            response.data().stream()
                .filter(Objects::nonNull)
                .limit(MAX_MEDIA_FETCH - result.size())
                .forEach(result::add);

            nextUrl = response.paging() != null ? response.paging().next() : null;
            if (nextUrl == null || nextUrl.isBlank()) {
                break;
            }
            page++;
        }

        return result;
    }

    @Override
    public InstagramMetaInsights fetchInsights(String mediaId) {
        try {
            return fetchInsights(mediaId, "views,likes,comments,shares,saved");
        } catch (InstagramMetaException ex) {
            log.warn("Instagram insight metrics fallback to views only for mediaId={}", mediaId);
            return fetchInsights(mediaId, "views");
        }
    }

    private MetaMediaResponse fetchMediaPage(String nextUrl) {
        try {
            if (nextUrl != null && !nextUrl.isBlank()) {
                return restClient.get()
                    .uri(URI.create(nextUrl))
                    .retrieve()
                    .body(MetaMediaResponse.class);
            }

            return restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/{version}/{igBusinessAccountId}/media")
                    .queryParam("fields", MEDIA_FIELDS)
                    .queryParam("limit", 25)
                    .queryParam("access_token", properties.getAccessToken())
                    .build(cleanVersion(properties.getGraphApiVersion()), properties.getInstagramBusinessAccountId()))
                .retrieve()
                .body(MetaMediaResponse.class);
        } catch (RestClientException ex) {
            throw new InstagramMetaException("Unable to fetch Instagram media from Meta Graph API", ex);
        }
    }

    private InstagramMetaInsights fetchInsights(String mediaId, String metrics) {
        try {
            MetaInsightsResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/{version}/{mediaId}/insights")
                    .queryParam("metric", metrics)
                    .queryParam("access_token", properties.getAccessToken())
                    .build(cleanVersion(properties.getGraphApiVersion()), mediaId))
                .retrieve()
                .body(MetaInsightsResponse.class);

            return toInsights(response);
        } catch (RestClientException ex) {
            throw new InstagramMetaException("Unable to fetch Instagram media insights", ex);
        }
    }

    private InstagramMetaInsights toInsights(MetaInsightsResponse response) {
        InstagramMetaInsights insights = InstagramMetaInsights.builder().build();
        if (response == null || response.data() == null) {
            return insights;
        }

        for (MetaInsightMetric metric : response.data()) {
            if (metric == null || metric.name() == null) {
                continue;
            }
            long value = extractMetricValue(metric);
            switch (metric.name().toLowerCase(Locale.ROOT)) {
                case "views" -> insights.setViewCount(value);
                case "likes" -> insights.setLikeCount(value);
                case "comments" -> insights.setCommentCount(value);
                case "shares" -> insights.setShareCount(value);
                case "saved" -> insights.setSaveCount(value);
                default -> {
                    // Ignore unknown Meta metrics; API versions may add more.
                }
            }
        }

        return insights;
    }

    private long extractMetricValue(MetaInsightMetric metric) {
        if (metric.values() == null || metric.values().isEmpty()) {
            return 0L;
        }
        Object value = metric.values().get(0).value();
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private String cleanVersion(String version) {
        if (version == null || version.isBlank()) {
            return "v25.0";
        }
        String cleaned = version.trim();
        return cleaned.startsWith("/") ? cleaned.substring(1) : cleaned;
    }

    private record MetaMediaResponse(
        List<InstagramMetaMedia> data,
        MetaPaging paging
    ) {}

    private record MetaPaging(String next) {}

    private record MetaInsightsResponse(List<MetaInsightMetric> data) {}

    private record MetaInsightMetric(
        String name,
        List<MetaInsightValue> values
    ) {}

    private record MetaInsightValue(@JsonProperty("value") Object value) {}
}
