/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.inject.Singleton;
import io.micronaut.context.annotation.Value;
import java.util.Objects;

@Singleton
public class BlotoutConfigs {

  @Value("${blotout.baseUrl}")
  private String blotoutBaseUrl;

  @Value("${blotout.authEndpoint}")
  private String blotoutAuthEndpoint;

  private static final Logger LOGGER = LoggerFactory.getLogger(BlotoutConfigs.class);
  // Fetch from environmental variables
  public String getBlotoutBaseUrl() {
      String env_blotoutBaseUrl = System.getenv("BLOTOUT_BASE_URL");
      LOGGER.warn("BLOTOUT_BASE_URL env_blotoutBaseUrl =====>" + env_blotoutBaseUrl);
      if(Objects.nonNull(blotoutBaseUrl) && !blotoutBaseUrl.contains("https")) {
          LOGGER.warn("inside if loop");
          LOGGER.warn("blotoutBaseUrl -> " + blotoutBaseUrl);
          blotoutBaseUrl =  "https://" + blotoutBaseUrl;
      }
      if(blotoutBaseUrl == null) {
          LOGGER.warn("BLOTOUT_BASE_URL env variable not found");
          throw new IllegalArgumentException("BLOTOUT_BASE_URL environment variable is not set.");
      }
      return blotoutBaseUrl;
  }

  public String getBlotoutAuthEndpoint() {
      String env_blotoutAuthEndpoint = System.getenv("BLOTOUT_AUTH_ENDPOINT");
      LOGGER.warn("BLOTOUT_AUTH_ENDPOINT env_blotoutAuthEndpoint =====>" + env_blotoutAuthEndpoint);
      if(blotoutAuthEndpoint == null) {
          LOGGER.warn("BLOTOUT_AUTH_ENDPOINT env variable not found");
          throw new IllegalArgumentException("BLOTOUT_BASE_URL environment variable is not set.");
      }
      return blotoutAuthEndpoint;
  }
}