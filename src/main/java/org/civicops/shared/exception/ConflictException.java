package org.civicops.shared.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends CivicOpsException {
  public ConflictException(String code, String message) {
    super(HttpStatus.CONFLICT, code, message);
  }
}
