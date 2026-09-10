package org.civicops.shared.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends CivicOpsException {
  public AuthenticationFailedException() {
    super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid email or password");
  }

  public AuthenticationFailedException(String code, String message) {
    super(HttpStatus.UNAUTHORIZED, code, message);
  }
}
