package com.brandPitara.sfs.integration;

import com.brandPitara.sfs.project.connectivity.provider.impl.GoogleNearbyPlaceProvider;
import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.publicreview.client.GooglePlacesClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleProviderTimeoutTest {

    private HttpServer server;
    private ExecutorService serverExecutor;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
        if (serverExecutor != null) serverExecutor.shutdownNow();
    }

    @Test
    void publicReviewClientMapsDelayedResponseToGatewayTimeout() {
        server.createContext("/v1/places/place-1", exchange -> delayedJson(exchange, 400, "{}"));
        GooglePlacesClient client = new GooglePlacesClient(publicReviewProperties(), new ObjectMapper());

        assertThatThrownBy(() -> client.fetchPlaceDetails("place-1"))
            .isInstanceOf(ExternalProviderException.class)
            .satisfies(ex -> assertThat(((ExternalProviderException) ex).getStatusCode())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }

    @Test
    void connectivityClientMapsDelayedResponseToGatewayTimeout() {
        server.createContext("/nearby", exchange -> delayedJson(exchange, 400, "{\"places\":[]}"));
        var properties = connectivityProperties();
        GoogleNearbyPlaceProvider provider = new GoogleNearbyPlaceProvider(properties, new ObjectMapper());

        assertThatThrownBy(() -> provider.searchNearby(
            28.4595, 77.0266, "metro", ProjectConnectivityCategory.TRANSIT, 3000))
            .isInstanceOf(ExternalProviderException.class)
            .satisfies(ex -> assertThat(((ExternalProviderException) ex).getStatusCode())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }

    @Test
    void nonSuccessfulGoogleResponseMapsToBadGatewayWithoutBodyLeakage() {
        server.createContext("/v1/places/place-2", exchange ->
            writeJson(exchange, 429, "{\"error\":\"secret upstream body\"}"));
        GooglePlacesClient client = new GooglePlacesClient(publicReviewProperties(), new ObjectMapper());

        assertThatThrownBy(() -> client.fetchPlaceDetails("place-2"))
            .isInstanceOf(ExternalProviderException.class)
            .satisfies(ex -> {
                ExternalProviderException providerException = (ExternalProviderException) ex;
                assertThat(providerException.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                assertThat(providerException.getReason()).doesNotContain("secret upstream body");
            });
    }

    @Test
    void missingProviderConfigurationMapsToServiceUnavailable() {
        var properties = publicReviewProperties();
        properties.setApiKey(" ");
        GooglePlacesClient client = new GooglePlacesClient(properties, new ObjectMapper());

        assertThatThrownBy(() -> client.fetchPlaceDetails("place-3"))
            .isInstanceOf(ExternalProviderException.class)
            .satisfies(ex -> assertThat(((ExternalProviderException) ex).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    private com.brandPitara.sfs.publicreview.config.GooglePlacesProperties publicReviewProperties() {
        var properties = new com.brandPitara.sfs.publicreview.config.GooglePlacesProperties();
        properties.setApiKey("unit-test-key");
        properties.setBaseUrl(baseUrl + "/v1");
        properties.setSearchTextUrl(baseUrl + "/search");
        properties.setConnectTimeoutMs(200);
        properties.setReadTimeoutMs(100);
        properties.setRequestTimeoutMs(150);
        return properties;
    }

    private com.brandPitara.sfs.project.connectivity.provider.GooglePlacesProperties connectivityProperties() {
        var properties = new com.brandPitara.sfs.project.connectivity.provider.GooglePlacesProperties();
        properties.setEnabled(true);
        properties.setApiKey("unit-test-key");
        properties.setTextSearchUrl(baseUrl + "/nearby");
        properties.setConnectTimeoutMs(200);
        properties.setReadTimeoutMs(100);
        properties.setRequestTimeoutMs(150);
        return properties;
    }

    private void delayedJson(HttpExchange exchange, long delayMillis, String body) throws IOException {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        writeJson(exchange, 200, body);
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
