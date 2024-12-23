package io.airbyte.server.pro;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpFilter;
import io.micronaut.http.filter.FilterChain;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
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
        // Ensure chain.proceed(request) returns a Mono<HttpResponse<?>>, and apply CORS headers
        return Mono.from(chain.proceed(request))  // Wrap Publisher in Mono to use map
                .map(response -> {
                    // Add CORS headers to the response
                    CORS_HEADERS.forEach(response.headers()::add);
                    return response;
                });
    }
}
