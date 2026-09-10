package org.civicops.grants;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.*;
import java.math.BigDecimal;
import java.util.Arrays;
import org.civicops.grants.expense.dto.CreateGrantExpenseRequest;
import org.civicops.grants.grant.dto.*;
import org.junit.jupiter.api.*;

class GrantRequestValidationTest {
  static Validator validator;

  @BeforeAll
  static void setup() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void grantRequiresNameGrantorAndNonnegativeAward() {
    var request =
        new CreateGrantRequest(
            "",
            "",
            null,
            null,
            new BigDecimal("-1.00"),
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null);
    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("grantName", "grantorName", "awardAmount");
  }

  @Test
  void moneyRejectsMoreThanTwoFractionalDigits() {
    var request =
        new CreateGrantRequest(
            "Grant",
            "Grantor",
            null,
            null,
            new BigDecimal("1.001"),
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null);
    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("awardAmount");
  }

  @Test
  void expenseRejectsZeroAndMissingFields() {
    var request = new CreateGrantExpenseRequest(BigDecimal.ZERO, null, null, "", null, null, null);
    assertThat(validator.validate(request))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("amount", "expenseDate", "category", "description");
  }

  @Test
  void summaryDoesNotExposeNotesOrContactInformation() {
    var names =
        Arrays.stream(GrantSummaryResponse.class.getRecordComponents())
            .map(c -> c.getName())
            .toList();
    assertThat(names)
        .doesNotContain(
            "notes",
            "primaryContactName",
            "primaryContactEmail",
            "restrictionDescription",
            "description");
  }
}
