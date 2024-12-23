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

@Singleton
public class BlotoutAuthentication {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlotoutAuthentication.class);

    private final BlotoutConfigs configs;
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public BlotoutAuthentication(BlotoutConfigs blotoutConfigs) {
        this.configs = blotoutConfigs;
    }

    /**
     * Validates the token using the Blotout service.
     */
    public boolean validateToken(String token) throws IOException, InterruptedException {
        String blotoutBaseUrl = configs.getBlotoutBaseUrl();
        String blotoutAuthEndpoint = configs.getBlotoutAuthEndpoint();
        System.out.println("blotoutBaseUrl : " + blotoutBaseUrl);
        System.out.println("blotoutAuthEndpoint : " + blotoutAuthEndpoint);

        final var request = HttpRequest
                .newBuilder(URI.create(blotoutBaseUrl + blotoutAuthEndpoint))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json") // connect type
                .header("token", token) // validate token
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        LOGGER.info("Response: " + response.body());
        return response.statusCode() == 200;
    }

    /**
     * Validates EdgeTag-based authentication tokens.
     */
    public boolean validateEdgeTagBasedAuthentication(String origin, String token, String teamId) throws IOException, InterruptedException {
        String blotoutBaseUrl = configs.getBlotoutBaseUrl();
        String blotoutAuthEndpoint = configs.getBlotoutAuthEndpoint();
        System.out.println("blotoutBaseUrl : " + blotoutBaseUrl);
        System.out.println("blotoutAuthEndpoint : " + blotoutAuthEndpoint);

        HttpResponse<String> response;
        if (teamId != null) {
            final var request = HttpRequest
                    .newBuilder(URI.create(blotoutBaseUrl + blotoutAuthEndpoint))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("origin", origin)
                    .header("token", token)
                    .header("Team-Id", teamId)
                    .build();
            response = httpClient.send(request, BodyHandlers.ofString());
        } else {
            final var request = HttpRequest
                    .newBuilder(URI.create(blotoutBaseUrl + blotoutAuthEndpoint))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("origin", origin)
                    .header("token", token)
                    .build();
            response = httpClient.send(request, BodyHandlers.ofString());
        }

        LOGGER.info("Response: " + response.body());
        return response.statusCode() == 200;
    }
}
