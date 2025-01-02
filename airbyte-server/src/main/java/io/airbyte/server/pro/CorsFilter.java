package io.airbyte.server.pro;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpFilter;
import io.micronaut.http.filter.FilterChain;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpHeaders;  // Ensure MutableHttpHeaders is imported
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.Map;

@Filter("/**")  // Apply filter to all endpoints
@Singleton
public class CorsFilter implements HttpFilter {

    private static final Map<String, String> CORS_HEADERS = Map.of(
            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*",
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Origin, Content-Type, Accept, Content-Encoding, Authorization",
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD",
            HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"
    );

    @Override
    public Mono<HttpResponse<?>> doFilter(HttpRequest<?> request, FilterChain chain) {
        // Ensure the response is a Mono<HttpResponse<?>> and use reactive map to modify headers
        System.out.println("inside CORSFilter...");
        return Mono.from(chain.proceed(request))  // Convert the Publisher to Mono
                .map(response -> {
                    // Ensure that headers are mutable before modifying them
                    MutableHttpHeaders headers = (MutableHttpHeaders) response.getHeaders();
                    // Add CORS headers to the response inside the Mono flow
                    CORS_HEADERS.forEach((key, value) -> headers.add(key, value));  // Use add() here
                    return response;
                });
    }
}
