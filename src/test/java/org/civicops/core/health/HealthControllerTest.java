package org.civicops.core.health;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {
    @Test
    void reportsServiceStatus() {
        assertThat(new HealthController().health())
                .containsEntry("status", "UP")
                .containsEntry("service", "civicops-api");
    }
}
