package org.civicops.volunteers;

import static org.assertj.core.api.Assertions.*;

import jakarta.validation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.civicops.volunteers.hours.dto.CreateHourEntryRequest;
import org.civicops.volunteers.support.VolunteerPageables;
import org.civicops.volunteers.volunteer.dto.CreateVolunteerRequest;
import org.civicops.volunteers.volunteer.dto.VolunteerSummaryResponse;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.PageRequest;

class VolunteerRequestValidationTest {
  private Validator validator;

  @BeforeEach
  void setup() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void rejectsMalformedVolunteerEmail() {
    var r =
        new CreateVolunteerRequest(
            null,
            "Ada",
            "Lovelace",
            "not-email",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Set.of());
    assertThat(validator.validate(r))
        .extracting(ConstraintViolation::getPropertyPath)
        .extracting(Object::toString)
        .contains("email");
  }

  @Test
  void rejectsMoreThanTwentyFourHours() {
    var r =
        new CreateHourEntryRequest(
            java.util.UUID.randomUUID(), null, LocalDate.now(), new BigDecimal("24.01"), null);
    assertThat(validator.validate(r)).isNotEmpty();
  }

  @Test
  void volunteerSummaryDoesNotExposePrivateFields() {
    Set<String> fields =
        java.util.Arrays.stream(VolunteerSummaryResponse.class.getRecordComponents())
            .map(java.lang.reflect.RecordComponent::getName)
            .collect(java.util.stream.Collectors.toSet());
    assertThat(fields)
        .doesNotContain(
            "notes",
            "emergencyContactName",
            "emergencyContactPhone",
            "addressLine1",
            "addressLine2",
            "email",
            "phone");
  }

  @Test
  void rejectsUndocumentedSortProperties() {
    assertThatThrownBy(
            () ->
                VolunteerPageables.allow(
                    PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("notes")),
                    Set.of("lastName", "createdAt")))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }
}
