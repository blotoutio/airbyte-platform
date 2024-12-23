package io.airbyte.server.pro;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpFilter;
import io.micronaut.http.filter.FilterChain;
import io.micronaut.http.HttpRequest;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Filter("/**")  // Apply filter to all endpoints
@Singleton
public class AuthenticationFilter implements HttpFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final BlotoutAuthentication blotoutAuthentication;

    public AuthenticationFilter(BlotoutAuthentication blotoutAuthentication) {
        this.blotoutAuthentication = blotoutAuthentication;
    }

    @Override
    public Mono<HttpResponse<?>> doFilter(HttpRequest<?> request, FilterChain chain) {
        // Skip health check path (if needed)
        if ("v1/health".equalsIgnoreCase(request.getPath())) {
            return Mono.from(chain.proceed(request));  // Continue without any authentication logic
        }

        String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        String originHeader = request.getHeaders().get(HttpHeaders.ORIGIN);
        String teamIdHeader = request.getHeaders().get("Team-Id");

        // If the request is a CORS preflight request (OPTIONS), bypass the authentication
        if (request.getMethod().name().equalsIgnoreCase("OPTIONS")) {
            return Mono.from(chain.proceed(request));  // Continue without any authentication logic
        }

        // Check if the Authorization header is present and starts with "Bearer "
        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer ")) {
            // Extract the token (remove "Bearer " prefix)
            String token = authorizationHeader.substring(7).trim();

            try {
                // Perform standard token validation (for Bearer tokens)
                if (!blotoutAuthentication.validateToken(token)) {
                    return Mono.just(HttpResponse.unauthorized().header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Blotout\""));
                }
            } catch (IOException e) {
                LOGGER.error("IOException occurred during validateToken authentication", e);
                return Mono.just(HttpResponse.serverError().body("Internal server error: " + e.getMessage()));
            } catch (InterruptedException e) {
                LOGGER.error("InterruptedException occurred during validateToken authentication", e);
                return Mono.just(HttpResponse.serverError().body("Request interrupted: " + e.getMessage()));
            } catch (Exception e) {
                LOGGER.error("validateToken authentication failed due to unexpected error", e);
                return Mono.just(HttpResponse.serverError().body("Unexpected error occurred"));
            }
        }
        // EdgeTag-based authentication (validate token with origin and teamId)
        else if (originHeader != null && teamIdHeader != null) {
            try {
                // Perform EdgeTag validation
                if (!blotoutAuthentication.validateEdgeTagBasedAuthentication(originHeader, authorizationHeader, teamIdHeader)) {
                    return Mono.just(HttpResponse.unauthorized().header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Blotout\""));
                }
            } catch (IOException e) {
                LOGGER.error("IOException occurred during EdgeTag-based authentication", e);
                return Mono.just(HttpResponse.serverError().body("Internal server error: " + e.getMessage()));
            } catch (InterruptedException e) {
                LOGGER.error("InterruptedException occurred during EdgeTag-based authentication", e);
                return Mono.just(HttpResponse.serverError().body("Request interrupted: " + e.getMessage()));
            } catch (Exception e) {
                LOGGER.error("EdgeTag-based authentication failed due to unexpected error", e);
                return Mono.just(HttpResponse.serverError().body("Unexpected error occurred"));
            }
        } else {
            return Mono.just(HttpResponse.unauthorized().header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Blotout\""));
        }

        // If validation passes, proceed with the request chain
        return Mono.from(chain.proceed(request));  // Correct the return type
    }
}