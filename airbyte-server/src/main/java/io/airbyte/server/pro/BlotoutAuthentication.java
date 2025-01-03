package io.airbyte.server.pro;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.airbyte.server.config.BlotoutConfigs;
import reactor.core.publisher.Mono;

@Singleton
public class BlotoutAuthentication {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlotoutAuthentication.class);

    private final BlotoutConfigs configs;

    @Client("${blotout.baseUrl}")  // Use Micronaut's HttpClient injected via client annotation
    private final HttpClient httpClient;

    public BlotoutAuthentication(BlotoutConfigs blotoutConfigs, HttpClient httpClient) {
        this.configs = blotoutConfigs;
        this.httpClient = httpClient;
    }

    /**
     * Validates the token using the Blotout service (Non-blocking version).
     */
    public Mono<Boolean> validateToken(String token) {
        String blotoutBaseUrl = configs.getBlotoutBaseUrl();
        String blotoutAuthEndpoint = configs.getBlotoutAuthEndpoint();
        LOGGER.info("blotoutBaseUrl : {}", blotoutBaseUrl);
        LOGGER.info("blotoutAuthEndpoint : {}", blotoutAuthEndpoint);

        HttpRequest request = HttpRequest.GET(blotoutBaseUrl + blotoutAuthEndpoint)
                .header("Content-Type", "application/json")
                .header("token", token);

        return Mono.from(httpClient.exchange(request, String.class))  // Convert Publisher to Mono
                .doOnTerminate(() -> LOGGER.info("Request completed"))
                .map(response -> response.status().getCode() == 200);
    }

    /**
     * Validates EdgeTag-based authentication tokens (Non-blocking version).
     */
    public Mono<Boolean> validateEdgeTagBasedAuthentication(String origin, String token, String teamId) {
        String blotoutBaseUrl = configs.getBlotoutBaseUrl();
        String blotoutAuthEndpoint = configs.getBlotoutAuthEndpoint();
        LOGGER.info("blotoutBaseUrl : {}", blotoutBaseUrl);
        LOGGER.info("blotoutAuthEndpoint : {}", blotoutAuthEndpoint);

        // Create HttpRequest directly, no builder needed
        HttpRequest request = HttpRequest.GET(blotoutBaseUrl + blotoutAuthEndpoint)
                .header("Content-Type", "application/json")
                .header("origin", origin)
                .header("token", token);

        if (teamId != null) {
            request.header("Team-Id", teamId);
        }

        return Mono.from(httpClient.exchange(request, String.class))  // Convert Publisher to Mono
                .doOnTerminate(() -> LOGGER.info("Request completed"))
                .map(response -> response.status().getCode() == 200);
    }
}
