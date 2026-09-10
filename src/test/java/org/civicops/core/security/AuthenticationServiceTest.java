package org.civicops.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.civicops.core.security.dto.LoginRequest;
import org.civicops.core.user.User;
import org.civicops.core.user.UserRepository;
import org.civicops.shared.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
  @Mock UserRepository users;
  @Mock PasswordEncoder encoder;
  @Mock JwtService jwt;
  @Mock RefreshTokenService refreshTokens;
  private AuthenticationService service;

  @BeforeEach
  void setUp() {
    when(encoder.encode("civicops-authentication-timing-placeholder")).thenReturn("dummy-hash");
    service = new AuthenticationService(users, encoder, jwt, refreshTokens);
  }

  @Test
  void validCredentialsReturnTokenPair() {
    User user = mock(User.class);
    RefreshToken refreshEntity = mock(RefreshToken.class);
    when(users.findByEmail("jane@example.org")).thenReturn(Optional.of(user));
    when(user.isActive()).thenReturn(true);
    when(user.passwordMatches("correct-password", encoder)).thenReturn(true);
    when(refreshTokens.issue(user))
        .thenReturn(new RefreshTokenService.IssuedRefreshToken("refresh-value", refreshEntity));
    when(jwt.issueAccessToken(user)).thenReturn("access-value");
    when(jwt.accessTokenExpiresInSeconds()).thenReturn(900L);

    var response = service.login(new LoginRequest(" JANE@EXAMPLE.ORG ", "correct-password"));

    assertThat(response.accessToken()).isEqualTo("access-value");
    assertThat(response.refreshToken()).isEqualTo("refresh-value");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(900);
  }

  @Test
  void wrongPasswordIsRejectedGenerically() {
    User user = mock(User.class);
    when(users.findByEmail("jane@example.org")).thenReturn(Optional.of(user));
    when(user.isActive()).thenReturn(true);
    when(user.passwordMatches("wrong-password", encoder)).thenReturn(false);

    assertGenericFailure(
        () -> service.login(new LoginRequest("jane@example.org", "wrong-password")));
  }

  @Test
  void unknownUserIsRejectedWithoutDisclosure() {
    when(users.findByEmail("missing@example.org")).thenReturn(Optional.empty());
    assertGenericFailure(
        () -> service.login(new LoginRequest("missing@example.org", "wrong-password")));
  }

  @Test
  void inactiveUserIsRejectedGenerically() {
    User user = mock(User.class);
    when(users.findByEmail("jane@example.org")).thenReturn(Optional.of(user));
    when(user.isActive()).thenReturn(false);
    when(user.passwordMatches("correct-password", encoder)).thenReturn(true);
    assertGenericFailure(
        () -> service.login(new LoginRequest("jane@example.org", "correct-password")));
  }

  @Test
  void validRefreshProducesNewTokenPair() {
    User user = mock(User.class);
    when(refreshTokens.rotate("old-refresh"))
        .thenReturn(new RefreshTokenService.RotatedRefreshToken(user, "new-refresh"));
    when(jwt.issueAccessToken(user)).thenReturn("new-access");
    when(jwt.accessTokenExpiresInSeconds()).thenReturn(900L);

    var result = service.refresh("old-refresh");

    assertThat(result.accessToken()).isEqualTo("new-access");
    assertThat(result.refreshToken()).isEqualTo("new-refresh");
  }

  @Test
  void logoutDelegatesRefreshTokenRevocation() {
    service.logout("refresh-to-revoke");
    org.mockito.Mockito.verify(refreshTokens).revoke("refresh-to-revoke");
  }

  private void assertGenericFailure(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
    assertThatThrownBy(action)
        .isInstanceOf(AuthenticationFailedException.class)
        .hasMessage("Invalid email or password");
  }
}
