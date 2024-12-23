package io.airbyte.server.config;

import jakarta.inject.Singleton;

@Singleton
public class BlotoutConfigs {

    // Fetch from environment variables
    public String getBlotoutBaseUrl() {
        String blotoutBaseUrl = System.getenv("BLOTOUT_BASE_URL");
        if (blotoutBaseUrl == null) {
            throw new IllegalArgumentException("BLOTOUT_BASE_URL environment variable is not set.");
        }
        return blotoutBaseUrl;
    }

    public String getBlotoutAuthEndpoint() {
        String blotoutAuthEndpoint = System.getenv("BLOTOUT_AUTH_ENDPOINT");
        if (blotoutAuthEndpoint == null) {
            throw new IllegalArgumentException("BLOTOUT_AUTH_ENDPOINT environment variable is not set.");
        }
        return blotoutAuthEndpoint;
    }
}
