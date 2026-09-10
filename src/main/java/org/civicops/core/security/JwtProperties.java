package org.civicops.core.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "civicops.security.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
  public JwtProperties {
    if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("CIVICOPS_JWT_SECRET must contain at least 32 bytes");
    }
    if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
      throw new IllegalStateException("Access-token TTL must be positive");
    }
    if (refreshTokenTtl == null || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
      throw new IllegalStateException("Refresh-token TTL must be positive");
    }
  }
}
