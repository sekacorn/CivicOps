package org.civicops.scholarships;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.scholarships.applicant.*;
import org.civicops.scholarships.applicant.dto.ApplicantSummaryResponse;
import org.civicops.scholarships.application.*;
import org.civicops.scholarships.award.*;
import org.civicops.scholarships.program.*;
import org.civicops.scholarships.review.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class ScholarshipDomainTest {
  Organization organization = mock(Organization.class);
  User user = mock(User.class);
  LocalDate today = LocalDate.of(2030, 1, 10);

  ScholarshipProgram program(Integer awards) {
    return new ScholarshipProgram(
        organization,
        user,
        "Scholars",
        null,
        "2030",
        today,
        today.plusDays(10),
        new BigDecimal("2500"),
        awards,
        "Eligible");
  }

  ScholarshipApplicant applicant() {
    return new ScholarshipApplicant(
        organization,
        null,
        "Ada",
        "Lovelace",
        null,
        "ada@example.org",
        "ada@example.org",
        null,
        null,
        null,
        "High School",
        2030,
        null,
        null);
  }

  ScholarshipApplication application() {
    return new ScholarshipApplication(
        program(1),
        applicant(),
        true,
        null,
        "Statement",
        null,
        new BigDecimal("3.75"),
        null,
        new BigDecimal("2500"));
  }

  @Test
  void programLifecycleReachesAwarded() {
    var p = program(2);
    p.transition(ScholarshipProgramStatus.OPEN);
    p.transition(ScholarshipProgramStatus.CLOSED);
    p.transition(ScholarshipProgramStatus.REVIEWING);
    p.transition(ScholarshipProgramStatus.AWARDED);
    assertThat(p.getStatus()).isEqualTo(ScholarshipProgramStatus.AWARDED);
  }

  @Test
  void terminalProgramCannotReopenOrEdit() {
    var p = program(1);
    p.transition(ScholarshipProgramStatus.CANCELLED);
    assertThatThrownBy(() -> p.transition(ScholarshipProgramStatus.OPEN))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> p.update("Other", null, null, null, null, null, null, null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void invalidProgramDatesAmountAndCountAreRejected() {
    assertThatThrownBy(
            () ->
                new ScholarshipProgram(
                    organization, user, "Bad", null, null, today, today, null, null, null))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                new ScholarshipProgram(
                    organization,
                    user,
                    "Bad",
                    null,
                    null,
                    today,
                    today.plusDays(1),
                    new BigDecimal("-1"),
                    null,
                    null))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> program(0)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void applicationLifecycleSelectsEligibleFinalist() {
    var a = application();
    a.submit(Instant.now());
    a.startReview();
    a.finalist();
    a.select();
    assertThat(a.getStatus()).isEqualTo(ScholarshipApplicationStatus.SELECTED);
  }

  @Test
  void submittedApplicationIsImmutable() {
    var a = application();
    a.submit(Instant.now());
    assertThatThrownBy(() -> a.update(true, null, "Changed", null, null, null, null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void invalidGpaAndMoneyAreRejected() {
    assertThatThrownBy(
            () ->
                new ScholarshipApplication(
                    program(1),
                    applicant(),
                    true,
                    null,
                    "S",
                    null,
                    new BigDecimal("4.01"),
                    null,
                    null))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                new ScholarshipApplication(
                    program(1),
                    applicant(),
                    true,
                    null,
                    "S",
                    null,
                    null,
                    new BigDecimal("-1"),
                    null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void reviewScoreRangeIsEnforced() {
    var assignment = new ScholarshipReviewAssignment(application(), user, user, Instant.now());
    assertThatThrownBy(
            () ->
                new ScholarshipReview(
                    assignment,
                    new BigDecimal("100.01"),
                    ReviewRecommendation.NEUTRAL,
                    null,
                    Instant.now()))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(
            new ScholarshipReview(
                    assignment,
                    new BigDecimal("92.50"),
                    ReviewRecommendation.RECOMMEND,
                    "Good",
                    Instant.now())
                .getScore())
        .isEqualByComparingTo("92.50");
  }

  @Test
  void reviewCannotBeCompletedTwice() {
    var assignment = new ScholarshipReviewAssignment(application(), user, user, Instant.now());
    assignment.complete(Instant.now());
    assertThatThrownBy(() -> assignment.complete(Instant.now()))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void awardLifecycleDisbursesAcceptedOffer() {
    var a = application();
    a.submit(Instant.now());
    a.startReview();
    a.finalist();
    a.select();
    var award = new ScholarshipAward(a, new BigDecimal("2500"), today, null, user);
    award.transition(ScholarshipAwardStatus.ACCEPTED);
    award.transition(ScholarshipAwardStatus.DISBURSED);
    assertThat(award.getStatus()).isEqualTo(ScholarshipAwardStatus.DISBURSED);
  }

  @Test
  void awardRejectsNonPositiveAmountAndInvalidTransition() {
    var a = application();
    assertThatThrownBy(() -> new ScholarshipAward(a, BigDecimal.ZERO, today, null, user))
        .isInstanceOf(BusinessRuleException.class);
    var award = new ScholarshipAward(a, BigDecimal.ONE, today, null, user);
    assertThatThrownBy(() -> award.transition(ScholarshipAwardStatus.DISBURSED))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void applicantSummaryContainsNoPrivateContactData() {
    var summary = ApplicantSummaryResponse.from(applicant());
    assertThat(summary.toString()).doesNotContain("ada@example.org");
  }
}
