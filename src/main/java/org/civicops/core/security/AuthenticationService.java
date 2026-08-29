package org.civicops.core.security;

import org.civicops.core.security.dto.LoginRequest;
import org.civicops.core.security.dto.TokenResponse;
import org.civicops.core.user.User;
import org.civicops.core.user.UserRepository;
import org.civicops.core.user.UserService;
import org.civicops.shared.exception.AuthenticationFailedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final String dummyHash;

    public AuthenticationService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                                 RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.dummyHash = passwordEncoder.encode("civicops-authentication-timing-placeholder");
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = UserService.normalizeEmail(request.email());
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new AuthenticationFailedException();
        }
        boolean passwordValid = user.passwordMatches(request.password(), passwordEncoder);
        if (!user.isActive() || !passwordValid) {
            throw new AuthenticationFailedException();
        }
        return tokensFor(user, refreshTokens.issue(user).value());
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokens.rotate(rawRefreshToken);
        return tokensFor(rotated.user(), rotated.value());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    private TokenResponse tokensFor(User user, String refreshToken) {
        return new TokenResponse(jwtService.issueAccessToken(user), refreshToken, "Bearer",
                jwtService.accessTokenExpiresInSeconds());
    }
}
