package org.civicops.core.security.dto;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
