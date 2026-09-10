package org.civicops.core.security;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.civicops.core.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final JwtProperties properties;
  private final Clock clock;

  public JwtService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties properties, Clock clock) {
    this.encoder = encoder;
    this.decoder = decoder;
    this.properties = properties;
    this.clock = clock;
  }

  public String issueAccessToken(User user) {
    Instant issuedAt = Instant.now(clock);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("civicops-api")
            .subject(user.getId().toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(properties.accessTokenTtl()))
            .id(UUID.randomUUID().toString())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public Jwt decode(String token) {
    return decoder.decode(token);
  }

  public long accessTokenExpiresInSeconds() {
    return properties.accessTokenTtl().toSeconds();
  }
}
