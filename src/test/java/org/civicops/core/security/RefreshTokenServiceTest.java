package org.civicops.core.security;

import org.civicops.core.user.User;
import org.civicops.shared.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock RefreshTokenRepository tokens;
    private RefreshTokenService service;
    private final Instant now = Instant.parse("2026-08-29T12:00:00Z");

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(tokens,
                new JwtProperties("0123456789abcdef0123456789abcdef", Duration.ofMinutes(15), Duration.ofDays(30)),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void validRefreshTokenRotatesAndCannotBeReused() {
        User user = activeUser();
        when(tokens.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RefreshToken current = new RefreshToken(user, RefreshTokenService.hash("old-token"), now.plusSeconds(60), now.minusSeconds(60));
        when(tokens.findByHashForUpdate(RefreshTokenService.hash("old-token"))).thenReturn(Optional.of(current));

        var rotated = service.rotate("old-token");

        assertThat(rotated.user()).isSameAs(user);
        assertThat(rotated.value()).isNotBlank().isNotEqualTo("old-token");
        assertThat(current.getRevokedAt()).isEqualTo(now);
        assertThatThrownBy(() -> service.rotate("old-token")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void expiredRefreshTokenIsRejected() {
        User user = mock(User.class);
        RefreshToken expired = new RefreshToken(user, "hash", now.minusSeconds(1), now.minusSeconds(60));
        when(tokens.findByHashForUpdate(RefreshTokenService.hash("expired"))).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.rotate("expired")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void revokedRefreshTokenIsRejected() {
        User user = mock(User.class);
        RefreshToken revoked = new RefreshToken(user, "hash", now.plusSeconds(60), now.minusSeconds(60));
        revoked.revoke(now.minusSeconds(1), null);
        when(tokens.findByHashForUpdate(RefreshTokenService.hash("revoked"))).thenReturn(Optional.of(revoked));
        assertThatThrownBy(() -> service.rotate("revoked")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void logoutRevokesRefreshToken() {
        RefreshToken token = new RefreshToken(mock(User.class), "hash", now.plusSeconds(60), now.minusSeconds(60));
        when(tokens.findByHashForUpdate(RefreshTokenService.hash("logout-token"))).thenReturn(Optional.of(token));
        service.revoke("logout-token");
        assertThat(token.getRevokedAt()).isEqualTo(now);
        verify(tokens).save(token);
    }

    private User activeUser() {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        return user;
    }
}
