package io.airbyte.server.pro;

import com.example.services.BlotoutAuthentication;
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
        System.out.println("Request path : " + request.getPath());
        if ("v1/health".equalsIgnoreCase(request.getPath())) {
            return chain.proceed(request);  // Continue without any authentication logic
        }

        String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        String originHeader = request.getHeaders().get(HttpHeaders.ORIGIN);
        String teamIdHeader = request.getHeaders().get("Team-Id");

        System.out.println("authorizationHeader : " + authorizationHeader);
        System.out.println("originHeader : " + originHeader);
        System.out.println("teamIdHeader : " + teamIdHeader);

        // If the request is a CORS preflight request (OPTIONS), bypass the authentication
        if (request.getMethod().name().equalsIgnoreCase("OPTIONS")) {
            return chain.proceed(request);
        }

        // Check if the Authorization header is present and starts with "Bearer "
        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer ")) {
            // Extract the token (remove "Bearer " prefix)
            String token = authorizationHeader.substring(7).trim();

            try {
                // Perform standard token validation (for Bearer tokens)
                if (!blotoutAuthentication.validateToken(token)) {
                    return HttpResponse.unauthorized().header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Blotout\"");
                }
            } catch (IOException e) {
                // Handle IOException (e.g., network-related issues, I/O operations)
                LOGGER.error("IOException occurred during validateToken authentication", e);
                return HttpResponse.serverError().body("Internal server error: " + e.getMessage());
            } catch (InterruptedException e) {
                // Handle InterruptedException (e.g., thread interruption, long-running process)
                LOGGER.error("InterruptedException occurred during validateToken authentication", e);
                return HttpResponse.serverError().body("Request interrupted: " + e.getMessage());
            } catch (Exception e) {
                // Catch any other unexpected exceptions
                LOGGER.error("validateToken authentication failed due to unexpected error", e);
                return HttpResponse.serverError().body("Unexpected error occurred");
            }
        }
        // EdgeTag-based authentication (validate token with origin and teamId)
        else if (originHeader != null && teamIdHeader != null) {
            try {
                // Perform EdgeTag validation
                if (!blotoutAuthentication.validateEdgeTagBasedAuthentication(originHeader, authorizationHeader, teamIdHeader)) {
                    return HttpResponse.unauthorized().header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Blotout\"");
                }
            } catch (IOException e) {
                // Handle IOException (e.g., network-related issues, I/O operations)
                LOGGER.error("IOException occurred during EdgeTag-based authentication", e);
                return HttpResponse.serverError().body("Internal server error: " + e.getMessage());
            } catch (InterruptedException e) {
                // Handle InterruptedException (e.g., thread interruption, long-running process)
                LOGGER.error("InterruptedException occurred during EdgeTag-based authentication", e);
                return HttpResponse.serverError().body("Request interrupted: " + e.getMessage());
            } catch (Exception e) {
                // Catch any other unexpected exceptions
                LOGGER.error("EdgeTag-based authentication failed due to unexpected error", e);
                return HttpResponse.serverError().body("Unexpected error occurred");
            }
        } else {
            // If neither authorization header nor EdgeTag-based details are found
            return HttpResponse.unauthorized().header(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Blotout\"");
        }

        // If validation passes, proceed with the request chain
        return chain.proceed(request);
    }
}
