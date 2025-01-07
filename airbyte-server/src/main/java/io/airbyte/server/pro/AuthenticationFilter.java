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
import io.airbyte.server.enums.EdgeTagClient;
import reactor.core.publisher.Mono;

@Filter("/**")  // Apply filter to all endpoints
@Singleton
public class AuthenticationFilter implements HttpFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final BlotoutAuthentication blotoutAuthentication;

    private static final String AUTHENTICATION_SCHEME = "Bearer";
    private static final String TEAM_ID_HEADER = "Team-Id";
    private static final String ORIGIN_HEADER = "origin";
    private static final String TOKEN_HEADER = "token";

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
        String originHeader = request.getHeaders().get(ORIGIN_HEADER);
        String teamIdHeader = request.getHeaders().get(TEAM_ID_HEADER);
        String tokenHeader = request.getHeaders().get(TOKEN_HEADER);

        // If the request is a CORS preflight request (OPTIONS), bypass the authentication
        if (request.getMethod().name().equalsIgnoreCase("OPTIONS")) {
            return Mono.from(chain.proceed(request));  // Continue without any authentication logic
        }

        if (isEdgeTagBasedAuthentication(originHeader)) {
            LOGGER.warn("authorizationHeader -> " + authorizationHeader);
            LOGGER.warn("originHeader -> " + originHeader);
            LOGGER.warn("teamIdHeader -> " + teamIdHeader);
            // Use Mono.defer to ensure this operation is handled asynchronously
            return Mono.defer(() -> blotoutAuthentication.validateEdgeTagBasedAuthentication(originHeader, authorizationHeader, teamIdHeader)
                    .flatMap(valid -> {
                        if (valid) {
                            // If valid, continue with the request chain
                            return Mono.from(chain.proceed(request));  // Ensure it's wrapped in Mono
                        } else {
                            // Return unauthorized response if invalid token
                            return Mono.just(HttpResponse.unauthorized()
                                    .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Blotout\""));
                        }
                    })
                    .onErrorResume(e -> {
                        LOGGER.error("Authentication failed: {}", e.getMessage(), e);
                        // Return server error if validation throws an exception
                        return Mono.just(HttpResponse.serverError().body("Authentication error: " + e.getMessage()));
                    })
            );
        } else if (isTokenBasedAuthentication(tokenHeader)) {
            LOGGER.warn("authorizationHeader -> " + authorizationHeader);
            LOGGER.warn("originHeader -> " + originHeader);
            LOGGER.warn("teamIdHeader -> " + teamIdHeader);
            LOGGER.warn("tokenHeader -> " + tokenHeader);
            // Extract the token (remove "Bearer " prefix)
            return Mono.defer(() -> blotoutAuthentication.validateToken(tokenHeader)
                    .flatMap(valid -> {
                        if (valid) {
                            // If valid, continue with the request chain
                            return Mono.from(chain.proceed(request));  // Ensure it's wrapped in Mono
                        } else {
                            // Return unauthorized response if invalid token
                            return Mono.just(HttpResponse.unauthorized()
                                    .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Blotout\""));
                        }
                    })
                    .onErrorResume(e -> {
                        LOGGER.error("Authentication failed: {}", e.getMessage(), e);
                        // Return server error if validation throws an exception
                        return Mono.just(HttpResponse.serverError().body("Authentication error: " + e.getMessage()));
                    })
            );
        } else {
            LOGGER.debug(" return from last else part ");
            return Mono.from(chain.proceed(request));  // Continue without any authentication logic
        }
    }

    private boolean isEdgeTagBasedAuthentication(String originHeader) {
        return originHeader != null &&
                EdgeTagClient.getEdgeTagOrigins().contains(originHeader);
    }

    private boolean isTokenBasedAuthentication(String tokenHeader) {
        // Check if the Token header is present
        return Objects.nonNull(tokenHeader);
    }
}
