package org.civicops.core.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.civicops.core.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void validAccessTokenContainsMinimalIdentityClaims() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtService service = service(SECRET, Clock.fixed(now, ZoneOffset.UTC));
        User user = mock(User.class);
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);

        var decoded = service.decode(service.issueAccessToken(user));

        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getExpiresAt()).isEqualTo(now.plusSeconds(900));
        assertThat(decoded.getClaims()).containsKeys("sub", "iat", "exp", "jti", "iss");
        assertThat(decoded.getClaims()).doesNotContainKeys("password", "memberships", "roles");
    }

    @Test
    void invalidSignatureIsRejected() {
        JwtService issuer = service(SECRET, Clock.systemUTC());
        JwtService verifier = service("abcdef0123456789abcdef0123456789", Clock.systemUTC());
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> verifier.decode(issuer.issueAccessToken(user))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        SecretKey key = key(SECRET);
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
        Instant past = Instant.now().minusSeconds(120);
        var claims = JwtClaimsSet.builder().subject(UUID.randomUUID().toString())
                .issuedAt(past.minusSeconds(60)).expiresAt(past).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThatThrownBy(() -> decoder(key).decode(token)).isInstanceOf(JwtValidationException.class);
    }

    private JwtService service(String secret, Clock clock) {
        SecretKey key = key(secret);
        JwtProperties properties = new JwtProperties(secret, Duration.ofMinutes(15), Duration.ofDays(30));
        return new JwtService(new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key)), decoder(key), properties, clock);
    }

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static NimbusJwtDecoder decoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
