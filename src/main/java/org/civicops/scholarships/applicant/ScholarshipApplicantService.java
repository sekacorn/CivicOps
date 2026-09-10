package org.civicops.scholarships.applicant;

import java.util.*;
import org.civicops.core.membership.OrganizationMembershipRepository;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.*;
import org.civicops.scholarships.applicant.dto.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScholarshipApplicantService {
  private final ScholarshipApplicantRepository applicants;
  private final OrganizationService organizations;
  private final UserService users;
  private final OrganizationMembershipRepository memberships;

  public ScholarshipApplicantService(
      ScholarshipApplicantRepository a,
      OrganizationService o,
      UserService u,
      OrganizationMembershipRepository m) {
    applicants = a;
    organizations = o;
    users = u;
    memberships = m;
  }

  @Transactional
  public ApplicantDetailResponse create(UUID org, CreateScholarshipApplicantRequest r) {
    String normalized = UserService.normalizeEmail(r.email());
    if (applicants.existsByOrganizationIdAndNormalizedEmail(org, normalized))
      throw new ConflictException(
          "DUPLICATE_APPLICANT_EMAIL", "Applicant email already exists in this organization");
    User user = member(org, r.userId());
    return ApplicantDetailResponse.from(
        applicants.save(
            new ScholarshipApplicant(
                organizations.requireEntity(org),
                user,
                r.firstName().trim(),
                r.lastName().trim(),
                clean(r.preferredName()),
                r.email().trim(),
                normalized,
                clean(r.phone()),
                r.dateOfBirth(),
                clean(r.address()),
                clean(r.schoolName()),
                r.graduationYear(),
                clean(r.studentId()),
                clean(r.notes()))));
  }

  @Transactional
  public ApplicantDetailResponse update(UUID org, UUID id, UpdateScholarshipApplicantRequest r) {
    ScholarshipApplicant a = require(org, id);
    String normalized = r.email() == null ? null : UserService.normalizeEmail(r.email());
    if (normalized != null
        && !normalized.equals(a.getNormalizedEmail())
        && applicants.existsByOrganizationIdAndNormalizedEmail(org, normalized))
      throw new ConflictException(
          "DUPLICATE_APPLICANT_EMAIL", "Applicant email already exists in this organization");
    a.update(
        clean(r.firstName()),
        clean(r.lastName()),
        clean(r.preferredName()),
        r.email() == null ? null : r.email().trim(),
        normalized,
        clean(r.phone()),
        r.dateOfBirth(),
        clean(r.address()),
        clean(r.schoolName()),
        r.graduationYear(),
        clean(r.studentId()),
        clean(r.notes()));
    return ApplicantDetailResponse.from(a);
  }

  @Transactional(readOnly = true)
  public ScholarshipApplicant require(UUID org, UUID id) {
    return applicants
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Scholarship applicant", id));
  }

  @Transactional(readOnly = true)
  public ApplicantDetailResponse detail(UUID org, UUID id) {
    return ApplicantDetailResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<ApplicantSummaryResponse> list(
      UUID org, String school, Integer year, String email, Pageable pageable) {
    Specification<ScholarshipApplicant> s =
        (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (school != null)
      s =
          s.and(
              (r, q, c) ->
                  c.equal(c.lower(r.get("schoolName")), school.trim().toLowerCase(Locale.ROOT)));
    if (year != null) s = s.and((r, q, c) -> c.equal(r.get("graduationYear"), year));
    if (email != null) {
      String normalized = UserService.normalizeEmail(email);
      s = s.and((r, q, c) -> c.equal(r.get("normalizedEmail"), normalized));
    }
    return applicants.findAll(s, pageable).map(ApplicantSummaryResponse::from);
  }

  private User member(UUID org, UUID id) {
    if (id == null) return null;
    User u = users.requireEntity(id);
    if (!u.isActive() || memberships.findByOrganizationIdAndUserIdAndActiveTrue(org, id).isEmpty())
      throw new BusinessRuleException(
          "INVALID_APPLICANT_USER", "Linked applicant user must be an active organization member");
    return u;
  }

  private static String clean(String x) {
    return x == null || x.isBlank() ? null : x.trim();
  }
}
