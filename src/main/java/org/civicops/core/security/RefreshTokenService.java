package org.civicops.core.security;

import org.civicops.core.user.User;
import org.civicops.shared.exception.AuthenticationFailedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository tokens;
    private final JwtProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository tokens, JwtProperties properties, Clock clock) {
        this.tokens = tokens;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issue(User user) {
        return create(user);
    }

    @Transactional
    public RotatedRefreshToken rotate(String rawToken) {
        Instant now = Instant.now(clock);
        RefreshToken current = tokens.findByHashForUpdate(hash(rawToken))
                .orElseThrow(() -> invalid("INVALID_REFRESH_TOKEN"));
        if (!current.isUsableAt(now) || !current.getUser().isActive()) {
            throw invalid(current.getRevokedAt() != null ? "REVOKED_REFRESH_TOKEN" : "EXPIRED_REFRESH_TOKEN");
        }
        IssuedRefreshToken replacement = create(current.getUser());
        current.revoke(now, replacement.entity().getId());
        tokens.save(current);
        return new RotatedRefreshToken(current.getUser(), replacement.value());
    }

    @Transactional
    public void revoke(String rawToken) {
        Instant now = Instant.now(clock);
        tokens.findByHashForUpdate(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.revoke(now, null);
                tokens.save(token);
            }
        });
    }

    private IssuedRefreshToken create(User user) {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        Instant now = Instant.now(clock);
        RefreshToken entity = new RefreshToken(user, hash(raw), now.plus(properties.refreshTokenTtl()), now);
        return new IssuedRefreshToken(raw, tokens.saveAndFlush(entity));
    }

    static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private AuthenticationFailedException invalid(String code) {
        return new AuthenticationFailedException(code, "The refresh token is invalid, expired, or revoked");
    }

    public record IssuedRefreshToken(String value, RefreshToken entity) {}
    public record RotatedRefreshToken(User user, String value) {}
}
