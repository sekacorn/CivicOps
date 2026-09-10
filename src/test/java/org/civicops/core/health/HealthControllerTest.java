package org.civicops.core.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HealthControllerTest {
  @Test
  void reportsServiceStatus() {
    assertThat(new HealthController().health())
        .containsEntry("status", "UP")
        .containsEntry("service", "civicops-api");
  }
}
