package org.civicops.donations.donor;

import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.UserService;
import org.civicops.donations.donor.dto.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonorService {
  private final DonorRepository donors;
  private final OrganizationService organizations;

  public DonorService(DonorRepository donors, OrganizationService organizations) {
    this.donors = donors;
    this.organizations = organizations;
  }

  @Transactional
  public DonorDetailResponse create(UUID org, CreateDonorRequest r) {
    validate(r.donorType(), r.firstName(), r.lastName(), r.organizationName(), r.anonymous());
    Donor d =
        new Donor(
            organizations.requireEntity(org),
            r.donorType(),
            clean(r.firstName()),
            clean(r.lastName()),
            clean(r.organizationName()),
            email(r.email()),
            clean(r.phone()),
            clean(r.addressLine1()),
            clean(r.addressLine2()),
            clean(r.city()),
            clean(r.state()),
            clean(r.postalCode()),
            upper(r.country()),
            r.anonymous(),
            r.communicationOptOut(),
            clean(r.notes()));
    return DonorDetailResponse.from(donors.save(d));
  }

  @Transactional(readOnly = true)
  public Donor require(UUID org, UUID id) {
    return donors
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Donor", id));
  }

  @Transactional(readOnly = true)
  public DonorDetailResponse detail(UUID org, UUID id) {
    return DonorDetailResponse.from(require(org, id));
  }

  @Transactional
  public DonorDetailResponse update(UUID org, UUID id, UpdateDonorRequest r) {
    Donor d = require(org, id);
    String first = r.firstName() == null ? d.getFirstName() : r.firstName();
    String last = r.lastName() == null ? d.getLastName() : r.lastName();
    String name = r.organizationName() == null ? d.getOrganizationName() : r.organizationName();
    validate(d.getDonorType(), first, last, name, d.isAnonymous());
    d.update(
        clean(r.firstName()),
        clean(r.lastName()),
        clean(r.organizationName()),
        email(r.email()),
        clean(r.phone()),
        clean(r.addressLine1()),
        clean(r.addressLine2()),
        clean(r.city()),
        clean(r.state()),
        clean(r.postalCode()),
        upper(r.country()),
        r.communicationOptOut(),
        clean(r.notes()));
    return DonorDetailResponse.from(d);
  }

  @Transactional(readOnly = true)
  public Page<DonorSummaryResponse> list(
      UUID org, DonorType type, String email, Boolean anonymous, Pageable p) {
    Specification<Donor> s = (r, q, cb) -> cb.equal(r.get("organization").get("id"), org);
    if (type != null) s = s.and((r, q, cb) -> cb.equal(r.get("donorType"), type));
    if (email != null && !email.isBlank()) {
      String normalized = email(email);
      s = s.and((r, q, cb) -> cb.equal(r.get("email"), normalized));
    }
    if (anonymous != null) s = s.and((r, q, cb) -> cb.equal(r.get("anonymous"), anonymous));
    return donors.findAll(s, p).map(DonorSummaryResponse::from);
  }

  private static void validate(
      DonorType type, String first, String last, String organizationName, boolean anonymous) {
    if (anonymous) return;
    if (type == DonorType.INDIVIDUAL && (blank(first) || blank(last)))
      throw new BusinessRuleException(
          "INDIVIDUAL_DONOR_NAME_REQUIRED", "Individual donors require first and last names");
    if (type != DonorType.INDIVIDUAL && blank(organizationName))
      throw new BusinessRuleException(
          "ORGANIZATION_DONOR_NAME_REQUIRED", "Organizational donors require an organization name");
  }

  private static boolean blank(String x) {
    return x == null || x.isBlank();
  }

  private static String clean(String x) {
    return blank(x) ? null : x.trim();
  }

  private static String email(String x) {
    return blank(x) ? null : UserService.normalizeEmail(x);
  }

  private static String upper(String x) {
    x = clean(x);
    return x == null ? null : x.toUpperCase(Locale.ROOT);
  }
}
