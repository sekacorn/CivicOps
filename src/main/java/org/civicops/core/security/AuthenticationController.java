package org.civicops.core.security;

import jakarta.validation.Valid;
import java.util.List;
import org.civicops.core.membership.OrganizationMembershipService;
import org.civicops.core.membership.dto.MembershipResponse;
import org.civicops.core.security.dto.LoginRequest;
import org.civicops.core.security.dto.RefreshTokenRequest;
import org.civicops.core.security.dto.TokenResponse;
import org.civicops.core.user.UserService;
import org.civicops.core.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
  private final AuthenticationService authentication;
  private final CurrentUserProvider currentUser;
  private final UserService users;
  private final OrganizationMembershipService memberships;

  public AuthenticationController(
      AuthenticationService authentication,
      CurrentUserProvider currentUser,
      UserService users,
      OrganizationMembershipService memberships) {
    this.authentication = authentication;
    this.currentUser = currentUser;
    this.users = users;
    this.memberships = memberships;
  }

  @PostMapping("/login")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authentication.login(request);
  }

  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return authentication.refresh(request.refreshToken());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody RefreshTokenRequest request) {
    authentication.logout(request.refreshToken());
  }

  @GetMapping("/me")
  public UserResponse me() {
    return users.get(currentUser.currentUserId());
  }

  @GetMapping("/me/memberships")
  public List<MembershipResponse> myMemberships() {
    return memberships.listActiveForUser(currentUser.currentUserId());
  }
}
