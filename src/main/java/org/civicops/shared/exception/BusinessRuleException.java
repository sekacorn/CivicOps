package org.civicops.shared.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends CivicOpsException {
  public BusinessRuleException(String code, String message) {
    super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
  }
}
