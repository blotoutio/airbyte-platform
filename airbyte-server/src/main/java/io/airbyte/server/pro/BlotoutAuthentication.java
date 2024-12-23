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

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.net.http.HttpClient; // Corrected import
import java.net.http.HttpRequest; // Corrected import
import java.net.http.HttpResponse.BodyHandlers; // Corrected import

@Singleton
public class BlotoutAuthentication {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlotoutAuthentication.class);

    private final BlotoutConfigs configs;
    private static final HttpClient httpClient = HttpClient.newHttpClient();  // Corrected to HttpClient

    public BlotoutAuthentication(BlotoutConfigs blotoutConfigs) {
        this.configs = blotoutConfigs;
    }

    /**
     * Validates the token using the Blotout service.
     */
    public boolean validateToken(String token) throws IOException, InterruptedException {
        String blotoutBaseUrl = configs.getBlotoutBaseUrl();
        String blotoutAuthEndpoint = configs.getBlotoutAuthEndpoint();
        LOGGER.info("blotoutBaseUrl : " + blotoutBaseUrl);
        LOGGER.info("blotoutAuthEndpoint : " + blotoutAuthEndpoint);

        URI uri = URI.create(blotoutBaseUrl + blotoutAuthEndpoint);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("token", token)
                .build();

        // Send the request using HttpClient
        java.net.http.HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        LOGGER.info("Response: " + response.body());
        return response.statusCode() == 200;
    }

    /**
     * Validates EdgeTag-based authentication tokens.
     */
    public boolean validateEdgeTagBasedAuthentication(String origin, String token, String teamId) throws IOException, InterruptedException {
        String blotoutBaseUrl = configs.getBlotoutBaseUrl();
        String blotoutAuthEndpoint = configs.getBlotoutAuthEndpoint();
        LOGGER.info("blotoutBaseUrl : " + blotoutBaseUrl);
        LOGGER.info("blotoutAuthEndpoint : " + blotoutAuthEndpoint);

        URI uri = URI.create(blotoutBaseUrl + blotoutAuthEndpoint);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("origin", origin)
                .header("token", token);

        if (teamId != null) {
            requestBuilder.header("Team-Id", teamId);
        }

        // Send the request using HttpClient
        java.net.http.HttpResponse<String> response = httpClient.send(requestBuilder.build(), BodyHandlers.ofString());

        LOGGER.info("Response: " + response.body());
        return response.statusCode() == 200;
    }
}
