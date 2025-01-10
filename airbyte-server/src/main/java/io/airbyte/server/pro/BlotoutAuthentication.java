package io.airbyte.server.pro;

import io.micronaut.http.client.DefaultHttpClientConfiguration;
import jakarta.inject.Singleton;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import io.airbyte.server.config.BlotoutConfigs;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

@Singleton
public class BlotoutAuthentication {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlotoutAuthentication.class);

    private final BlotoutConfigs configs;

    public BlotoutAuthentication(BlotoutConfigs blotoutConfigs) {
        this.configs = blotoutConfigs;
    }

    /**
     * Validates the token using the Blotout service (Non-blocking version).
     */
    public Mono<Boolean> validateToken(String token) {
        String blotoutBaseUrl = configs.getBlotoutBaseUrl();
        String blotoutAuthEndpoint = configs.getBlotoutAuthEndpoint();
        LOGGER.info("blotoutBaseUrl : {}", blotoutBaseUrl);
        LOGGER.info("blotoutAuthEndpoint : {}", blotoutAuthEndpoint);
        try {
            // Configure custom HttpClient with timeout
            DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
            configuration.setConnectTimeout(Duration.ofSeconds(10));  // Set connection timeout
            configuration.setReadTimeout(Duration.ofSeconds(120));  // Set read timeout

            // Create a URI from the base URL string
            URI baseUri = URI.create(blotoutBaseUrl);

            // Convert the URI to URL (URI is more flexible and avoids deprecation warning)
            URL baseUrl = baseUri.toURL();
            HttpClient customHttpClient = HttpClient.create(baseUrl, configuration);

            // Build the HttpRequest with headers
            HttpRequest<?> request = HttpRequest.GET(blotoutBaseUrl + blotoutAuthEndpoint)
                    .header("Content-Type", "application/json")
                    .header("token", token);

            // Send the request and process the response
            return Mono.from(customHttpClient.exchange(request, String.class))
                    .doOnTerminate(() -> LOGGER.info("Request completed"))
                    .map(response -> {
                        // Cast response to HttpResponse<String> to access the status code
                        HttpResponse<String> httpResponse = (HttpResponse<String>) response;
                        return httpResponse.getStatus().getCode() == 200;
                    });
        } catch (MalformedURLException e) {
            LOGGER.error("Invalid base URL: {}", blotoutBaseUrl, e);
            return Mono.error(new RuntimeException("Invalid base URL", e));  // You can return an error response here
        }
    }

    /**
     * Validates EdgeTag-based authentication tokens (Non-blocking version).
     */
    public Mono<Boolean> validateEdgeTagBasedAuthentication(String origin, String token, String teamId) {
        String blotoutBaseUrl = configs.getBlotoutBaseUrl();
        String blotoutAuthEndpoint = configs.getBlotoutAuthEndpoint();

        try {
            // Configure custom HttpClient with timeout
            DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
            configuration.setConnectTimeout(Duration.ofSeconds(10));  // Set connection timeout
            configuration.setReadTimeout(Duration.ofSeconds(120));  // Set read timeout

            // Create an HttpClient instance with the configuration
            // Create a URI from the base URL string
            URI baseUri = URI.create(blotoutBaseUrl);

            // Convert the URI to URL (URI is more flexible and avoids deprecation warning)
            URL baseUrl = baseUri.toURL();
            HttpClient customHttpClient = HttpClient.create(baseUrl, configuration);
            // Build the HttpRequest with headers
            HttpRequest<?> request = HttpRequest.GET(blotoutBaseUrl + blotoutAuthEndpoint)
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
            return Mono.from(customHttpClient.exchange(request, String.class))
                    .doOnTerminate(() -> LOGGER.info("Request completed"))
                    .map(response -> {
                        // Cast response to HttpResponse<String> to access the status code
                        HttpResponse<String> httpResponse = (HttpResponse<String>) response;
                        return httpResponse.getStatus().getCode() == 200;
                    });
        } catch (MalformedURLException e) {
            LOGGER.error("Invalid base URL: {}", blotoutBaseUrl, e);
            return Mono.error(new RuntimeException("Invalid base URL", e));  // You can return an error response here
        }
    }
}
