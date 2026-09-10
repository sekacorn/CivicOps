package org.civicops.shared.finance;

import java.math.*;

public final class Money {
  private Money() {}

  public static BigDecimal amount(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
    return denominator.signum() == 0
        ? BigDecimal.ZERO.setScale(2)
        : numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
  }
}
