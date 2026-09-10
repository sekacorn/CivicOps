package org.civicops.donations;

import static org.assertj.core.api.Assertions.*;

import jakarta.validation.*;
import java.math.BigDecimal;
import java.util.Arrays;
import org.civicops.donations.campaign.dto.CreateCampaignRequest;
import org.civicops.donations.donation.*;
import org.civicops.donations.donation.dto.CreateDonationRequest;
import org.civicops.donations.donor.dto.DonorSummaryResponse;
import org.junit.jupiter.api.*;

class DonationValidationPrivacyTest {
  static Validator validator;

  @BeforeAll
  static void setup() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void donationRejectsZero() {
    var r =
        new CreateDonationRequest(
            null,
            true,
            BigDecimal.ZERO,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThat(validator.validate(r))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("amount", "donationDate", "paymentMethod");
  }

  @Test
  void donationRejectsNegative() {
    var r =
        new CreateDonationRequest(
            null,
            true,
            new BigDecimal("-1"),
            java.time.LocalDate.now(),
            DonationPaymentMethod.CASH,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThat(validator.validate(r))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("amount");
  }

  @Test
  void moneyRejectsMoreThanTwoDecimals() {
    var r =
        new CreateDonationRequest(
            null,
            true,
            new BigDecimal("1.001"),
            java.time.LocalDate.now(),
            DonationPaymentMethod.CASH,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThat(validator.validate(r))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("amount");
  }

  @Test
  void campaignRejectsNegativeGoalAndMissingName() {
    var r = new CreateCampaignRequest("", null, new BigDecimal("-1"), null, null);
    assertThat(validator.validate(r))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("name", "goalAmount");
  }

  @Test
  void donorSummaryOmitsPii() {
    var names =
        Arrays.stream(DonorSummaryResponse.class.getRecordComponents())
            .map(c -> c.getName())
            .toList();
    assertThat(names)
        .doesNotContain(
            "email",
            "phone",
            "addressLine1",
            "addressLine2",
            "city",
            "state",
            "postalCode",
            "notes");
  }
}
