/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.inject.Singleton;
import java.util.Objects;

@Singleton
public class BlotoutConfigs {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlotoutConfigs.class);
  // Fetch from environmental variables
  public String getBlotoutBaseUrl() {
      String blotoutBaseUrl = System.getenv("BLOTOUT_BASE_URL");
      LOGGER.debug("BLOTOUT_BASE_URL =====> " + blotoutBaseUrl);
      if(Objects.isNull(blotoutBaseUrl)) {
          LOGGER.warn("BLOTOUT_BASE_URL env variable not found");
          throw new IllegalArgumentException("BLOTOUT_BASE_URL environment variable is not set.");
      }
      return blotoutBaseUrl;
  }

  public String getBlotoutAuthEndpoint() {
      String blotoutAuthEndpoint = System.getenv("BLOTOUT_AUTH_ENDPOINT");
      LOGGER.debug("BLOTOUT_AUTH_ENDPOINT =====> " + blotoutAuthEndpoint);
      if(Objects.isNull(blotoutAuthEndpoint)) {
          LOGGER.warn("BLOTOUT_AUTH_ENDPOINT env variable not found");
          throw new IllegalArgumentException("BLOTOUT_BASE_URL environment variable is not set.");
      }
      return blotoutAuthEndpoint;
  }
}