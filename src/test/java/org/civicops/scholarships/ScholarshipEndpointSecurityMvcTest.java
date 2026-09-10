package org.civicops.scholarships;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
import org.civicops.scholarships.applicant.*;
import org.civicops.scholarships.applicant.dto.*;
import org.civicops.scholarships.application.*;
import org.civicops.scholarships.application.dto.*;
import org.civicops.scholarships.award.*;
import org.civicops.scholarships.program.*;
import org.civicops.scholarships.reporting.*;
import org.civicops.scholarships.reporting.dto.*;
import org.civicops.scholarships.review.*;
import org.civicops.scholarships.review.dto.*;
import org.civicops.scholarships.security.ScholarshipAccessService;
import org.civicops.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  ScholarshipProgramController.class,
  ScholarshipApplicantController.class,
  ScholarshipProgramApplicationController.class,
  ScholarshipApplicationController.class,
  ScholarshipReviewController.class,
  ScholarshipApplicationAwardController.class,
  ScholarshipAwardController.class,
  ScholarshipReportingController.class
})
@Import({
  CoreSecurityConfiguration.class,
  JwtAuthenticationFilter.class,
  SecurityErrorWriter.class,
  CurrentUserProvider.class,
  GlobalExceptionHandler.class
})
@TestPropertySource(properties = "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef")
class ScholarshipEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean ScholarshipProgramService programs;
  @MockitoBean ScholarshipApplicantService applicants;
  @MockitoBean ScholarshipApplicationService applications;
  @MockitoBean ScholarshipDocumentService documents;
  @MockitoBean ScholarshipReviewService reviews;
  @MockitoBean ScholarshipAwardService awards;
  @MockitoBean ScholarshipReportingService reports;
  @MockitoBean ScholarshipAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedProgramReadIs401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{o}/scholarship-programs", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void managerCanCreateProgram() throws Exception {
    UUID o = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{o}/scholarship-programs", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"name\":\"Scholars\",\"applicationOpenDate\":\"2030-01-01\",\"applicationDeadline\":\"2030-02-01\",\"awardAmount\":2500,\"numberOfAwards\":2}"))
        .andExpect(status().isCreated());
    verify(access).requireScholarshipManagement(o);
  }

  @Test
  void viewerCanReadProgramSummary() throws Exception {
    UUID o = UUID.randomUUID();
    when(programs.list(eq(o), any(), any(), any(), any(), any())).thenReturn(Page.empty());
    mvc.perform(
            get("/api/v1/organizations/{o}/scholarship-programs", o).with(authentication(auth())))
        .andExpect(status().isOk());
    verify(access).requireProgramRead(o);
  }

  @Test
  void applicantSummaryOmitsPrivateFields() throws Exception {
    UUID o = UUID.randomUUID();
    when(applicants.list(eq(o), any(), any(), any(), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new ApplicantSummaryResponse(
                        UUID.randomUUID(), "Ada Lovelace", "Central High", 2030))));
    mvc.perform(
            get("/api/v1/organizations/{o}/scholarship-applicants", o).with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].displayName").value("Ada Lovelace"))
        .andExpect(jsonPath("$.content[0].email").doesNotExist())
        .andExpect(jsonPath("$.content[0].phone").doesNotExist())
        .andExpect(jsonPath("$.content[0].dateOfBirth").doesNotExist())
        .andExpect(jsonPath("$.content[0].address").doesNotExist())
        .andExpect(jsonPath("$.content[0].notes").doesNotExist());
  }

  @Test
  void viewerCannotReadApplicantPrivateDetail() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("PII denied")).when(access).requireApplicantDetailAccess(o);
    mvc.perform(
            get("/api/v1/organizations/{o}/scholarship-applicants/{id}", o, UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void managerApplicantDetailContainsAuthorizedContact() throws Exception {
    UUID o = UUID.randomUUID(), id = UUID.randomUUID();
    when(applicants.detail(o, id))
        .thenReturn(
            new ApplicantDetailResponse(
                id,
                o,
                null,
                "Ada",
                "Lovelace",
                null,
                "private@example.org",
                "555-0100",
                LocalDate.of(2006, 1, 1),
                "Private address",
                "Central High",
                2030,
                null,
                "Private note",
                Instant.now(),
                Instant.now(),
                0));
    mvc.perform(
            get("/api/v1/organizations/{o}/scholarship-applicants/{id}", o, id)
                .with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("private@example.org"))
        .andExpect(jsonPath("$.notes").value("Private note"));
  }

  @Test
  void unassignedReviewerCannotReadReviewApplication() throws Exception {
    UUID o = UUID.randomUUID(), id = UUID.randomUUID();
    ScholarshipReviewAssignment assignment = mock(ScholarshipReviewAssignment.class);
    when(reviews.require(o, id)).thenReturn(assignment);
    doThrow(new AccessDeniedException("unassigned"))
        .when(access)
        .requireOwnAssignment(o, assignment);
    mvc.perform(
            get("/api/v1/organizations/{o}/scholarship-review-assignments/{id}/application", o, id)
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void assignedReviewerReceivesPrivacySafeTransactionalResponse() throws Exception {
    UUID o = UUID.randomUUID(),
        id = UUID.randomUUID(),
        application = UUID.randomUUID(),
        program = UUID.randomUUID(),
        applicant = UUID.randomUUID();
    ScholarshipReviewAssignment assignment = mock(ScholarshipReviewAssignment.class);
    when(reviews.require(o, id)).thenReturn(assignment);
    when(reviews.application(o, id))
        .thenReturn(
            new ReviewerApplicationResponse(
                application,
                program,
                "Community Scholars",
                applicant,
                "Ada Lovelace",
                "Central High",
                2030,
                ScholarshipApplicationStatus.UNDER_REVIEW,
                true,
                "Service statement",
                "Need statement",
                new BigDecimal("3.75"),
                new BigDecimal("2500.00"),
                Instant.now()));
    mvc.perform(
            get("/api/v1/organizations/{o}/scholarship-review-assignments/{id}/application", o, id)
                .with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.programName").value("Community Scholars"))
        .andExpect(jsonPath("$.email").doesNotExist())
        .andExpect(jsonPath("$.phone").doesNotExist())
        .andExpect(jsonPath("$.dateOfBirth").doesNotExist())
        .andExpect(jsonPath("$.address").doesNotExist())
        .andExpect(jsonPath("$.householdIncome").doesNotExist())
        .andExpect(jsonPath("$.notes").doesNotExist());
    verify(reviews).application(o, id);
  }

  @Test
  void reviewerCannotCreateAward() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("manager only")).when(access).requireScholarshipManagement(o);
    mvc.perform(
            post(
                    "/api/v1/organizations/{o}/scholarship-applications/{id}/award",
                    o,
                    UUID.randomUUID())
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"amount\":2500,\"awardDate\":\"2030-04-01\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void programManagerCanRetrieveAggregateReport() throws Exception {
    UUID o = UUID.randomUUID();
    when(reports.summary(o))
        .thenReturn(new ScholarshipSummaryResponse(o, 1, 3, 2, 1, 1, 1, new BigDecimal("2500.00")));
    mvc.perform(
            get("/api/v1/organizations/{o}/scholarship-reports/summary", o)
                .with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalAwarded").value(2500.00));
    verify(access).requireScholarshipReporting(o);
  }

  @Test
  void crossOrganizationReadReturns403() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("cross org")).when(access).requireProgramRead(o);
    mvc.perform(
            get("/api/v1/organizations/{o}/scholarship-programs", o).with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void unsupportedAndSensitiveSortsReturn400() throws Exception {
    UUID o = UUID.randomUUID();
    mvc.perform(
            get("/api/v1/organizations/{o}/scholarship-programs?sort=eligibilityDescription", o)
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/organizations/{o}/scholarship-applicants?sort=email", o)
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "scholarship@example.org"), null, List.of());
  }
}
