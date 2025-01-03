package io.airbyte.server.pro;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import io.airbyte.server.config.BlotoutConfigs;

@Singleton
public class BlotoutAuthentication {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlotoutAuthentication.class);

    private final BlotoutConfigs configs;

    @Client("${blotout.baseUrl}")  // Micronaut's HttpClient injected via client annotation
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

        // Build the HttpRequest with headers
        HttpRequest request = HttpRequest.GET(blotoutBaseUrl + blotoutAuthEndpoint)
                .header("Content-Type", "application/json")
                .header("token", token);

        // Send the request and process the response
        return Mono.from(httpClient.exchange(request, String.class))
                .doOnTerminate(() -> LOGGER.info("Request completed"))
                .map(response -> {
                    // Cast response to HttpResponse<String> to access the status code
                    HttpResponse<String> httpResponse = (HttpResponse<String>) response;
                    return httpResponse.getStatus().getCode() == 200;
                });
    }

    /**
     * Validates EdgeTag-based authentication tokens (Non-blocking version).
     */
    public Mono<Boolean> validateEdgeTagBasedAuthentication(String origin, String token, String teamId) {
        String blotoutBaseUrl = configs.getBlotoutBaseUrl();
        String blotoutAuthEndpoint = configs.getBlotoutAuthEndpoint();
        LOGGER.info("blotoutBaseUrl : {}", blotoutBaseUrl);
        LOGGER.info("blotoutAuthEndpoint : {}", blotoutAuthEndpoint);

        // Build the HttpRequest with headers
        HttpRequest request = HttpRequest.GET(blotoutBaseUrl + blotoutAuthEndpoint)
                .header("Content-Type", "application/json")
                .header("origin", origin)
                .header("token", token);

        // Conditionally add the "Team-Id" header if it's not null
        if (teamId != null) {
            request = HttpRequest.GET(blotoutBaseUrl + blotoutAuthEndpoint)  // Rebuild request with added header
                    .header("Content-Type", "application/json")
                    .header("origin", origin)
                    .header("token", token)
                    .header("Team-Id", teamId);
        }

        // Send the request and process the response
        return Mono.from(httpClient.exchange(request, String.class))
                .doOnTerminate(() -> LOGGER.info("Request completed"))
                .map(response -> {
                    // Cast response to HttpResponse<String> to access the status code
                    HttpResponse<String> httpResponse = (HttpResponse<String>) response;
                    return httpResponse.getStatus().getCode() == 200;
                });
    }
}
