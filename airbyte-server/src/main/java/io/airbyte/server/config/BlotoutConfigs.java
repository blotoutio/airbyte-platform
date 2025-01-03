/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.config;

import jakarta.inject.Singleton;

@Singleton
public class BlotoutConfigs {
  // Fetch from environmental variables
  public String getBlotoutBaseUrl() {
      String blotoutBaseUrl = System.getenv("BLOTOUT_BASE_URL");
      if(blotoutBaseUrl == null) {
          System.out.println("BLOTOUT_BASE_URL env variable not found");
          throw new IllegalArgumentException("BLOTOUT_BASE_URL environment variable is not set.");
      }
      return blotoutBaseUrl;
  }

  public String getBlotoutAuthEndpoint() {
      String blotoutAuthEndpoint = System.getenv("BLOTOUT_AUTH_ENDPOINT");
      if(blotoutAuthEndpoint == null) {
          System.out.println("BLOTOUT_AUTH_ENDPOINT env variable not found");
          throw new IllegalArgumentException("BLOTOUT_BASE_URL environment variable is not set.");
      }
      return blotoutAuthEndpoint;
  }
}