package org.civicops.volunteers.volunteer;

import java.util.*;
import java.util.stream.Collectors;
import org.civicops.core.membership.OrganizationMembershipRepository;
import org.civicops.core.organization.*;
import org.civicops.core.user.*;
import org.civicops.shared.exception.*;
import org.civicops.volunteers.volunteer.dto.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VolunteerService {
  private final VolunteerRepository volunteers;
  private final OrganizationService organizations;
  private final UserService users;
  private final OrganizationMembershipRepository memberships;

  public VolunteerService(
      VolunteerRepository v,
      OrganizationService o,
      UserService u,
      OrganizationMembershipRepository m) {
    volunteers = v;
    organizations = o;
    users = u;
    memberships = m;
  }

  @Transactional
  public VolunteerDetailResponse create(UUID orgId, CreateVolunteerRequest r) {
    String email = UserService.normalizeEmail(r.email());
    if (volunteers.existsByOrganizationIdAndEmail(orgId, email))
      throw new ConflictException(
          "DUPLICATE_VOLUNTEER_EMAIL",
          "A volunteer with this email already exists in the organization");
    Organization org = organizations.requireEntity(orgId);
    User user = r.userId() == null ? null : users.requireEntity(r.userId());
    if (user != null && !memberships.existsByOrganizationIdAndUserId(orgId, user.getId()))
      throw new BusinessRuleException(
          "USER_NOT_ORGANIZATION_MEMBER",
          "A linked user must belong to the volunteer's organization");
    if (user != null && volunteers.existsByOrganizationIdAndUserId(orgId, user.getId()))
      throw new ConflictException(
          "DUPLICATE_LINKED_USER", "This user already has a volunteer profile in the organization");
    Set<String> skills =
        r.skills() == null
            ? Set.of()
            : r.skills().stream()
                .map(String::trim)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    Volunteer v =
        new Volunteer(
            org,
            user,
            r.firstName().trim(),
            r.lastName().trim(),
            email,
            clean(r.phone()),
            clean(r.addressLine1()),
            clean(r.addressLine2()),
            clean(r.city()),
            clean(r.state()),
            clean(r.postalCode()),
            upper(r.country()),
            clean(r.emergencyContactName()),
            clean(r.emergencyContactPhone()),
            clean(r.notes()),
            r.status() == null ? VolunteerStatus.APPLICANT : r.status(),
            r.startDate(),
            skills);
    return VolunteerDetailResponse.from(volunteers.save(v));
  }

  @Transactional(readOnly = true)
  public Volunteer require(UUID orgId, UUID id) {
    return volunteers
        .findByIdAndOrganizationId(id, orgId)
        .orElseThrow(() -> new ResourceNotFoundException("Volunteer", id));
  }

  @Transactional(readOnly = true)
  public VolunteerDetailResponse detail(UUID orgId, UUID id) {
    return VolunteerDetailResponse.from(require(orgId, id));
  }

  @Transactional
  public VolunteerDetailResponse changeStatus(UUID orgId, UUID id, VolunteerStatus status) {
    Volunteer v = require(orgId, id);
    v.changeStatus(status);
    return VolunteerDetailResponse.from(v);
  }

  @Transactional
  public VolunteerDetailResponse update(UUID orgId, UUID id, UpdateVolunteerRequest r) {
    Volunteer volunteer = require(orgId, id);
    String email = r.email() == null ? null : UserService.normalizeEmail(r.email());
    if (email != null && volunteers.existsByOrganizationIdAndEmailAndIdNot(orgId, email, id)) {
      throw new ConflictException(
          "DUPLICATE_VOLUNTEER_EMAIL",
          "A volunteer with this email already exists in the organization");
    }
    volunteer.update(
        trimmed(r.firstName()),
        trimmed(r.lastName()),
        email,
        clean(r.phone()),
        clean(r.addressLine1()),
        clean(r.addressLine2()),
        clean(r.city()),
        clean(r.state()),
        clean(r.postalCode()),
        upper(r.country()),
        clean(r.emergencyContactName()),
        clean(r.emergencyContactPhone()),
        clean(r.notes()),
        normalizeSkills(r.skills()));
    return VolunteerDetailResponse.from(volunteer);
  }

  @Transactional(readOnly = true)
  public Page<VolunteerSummaryResponse> list(
      UUID orgId, VolunteerStatus status, String email, String skill, Pageable p) {
    Specification<Volunteer> spec =
        (root, query, cb) -> cb.equal(root.get("organization").get("id"), orgId);
    if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    if (email != null && !email.isBlank()) {
      String normalized = UserService.normalizeEmail(email);
      spec = spec.and((root, query, cb) -> cb.equal(root.get("email"), normalized));
    }
    if (skill != null && !skill.isBlank()) {
      String normalized = skill.trim().toLowerCase(Locale.ROOT);
      spec =
          spec.and(
              (root, query, cb) -> {
                query.distinct(true);
                return cb.equal(root.join("skills"), normalized);
              });
    }
    return volunteers.findAll(spec, p).map(VolunteerSummaryResponse::from);
  }

  private static Set<String> normalizeSkills(Set<String> skills) {
    return skills == null
        ? null
        : skills.stream()
            .map(String::trim)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static String trimmed(String value) {
    return value == null ? null : value.trim();
  }

  private static String clean(String x) {
    return x == null || x.isBlank() ? null : x.trim();
  }

  private static String upper(String x) {
    x = clean(x);
    return x == null ? null : x.toUpperCase(Locale.ROOT);
  }
}
