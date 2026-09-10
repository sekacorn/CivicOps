package org.civicops.cases.client;

import java.util.*;
import org.civicops.cases.client.dto.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.UserService;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {
  private final ClientRepository clients;
  private final OrganizationService organizations;

  public ClientService(ClientRepository c, OrganizationService o) {
    clients = c;
    organizations = o;
  }

  @Transactional
  public ClientDetailResponse create(UUID org, CreateClientRequest r) {
    String ref = clean(r.externalReferenceNumber());
    if (ref != null && clients.existsByOrganizationIdAndExternalReferenceNumber(org, ref))
      throw new ConflictException(
          "DUPLICATE_CLIENT_REFERENCE", "External reference already exists in this organization");
    Client c =
        new Client(
            organizations.requireEntity(org),
            ref,
            required(r.firstName()),
            required(r.lastName()),
            clean(r.preferredName()),
            r.dateOfBirth(),
            email(r.email()),
            clean(r.phone()),
            clean(r.addressLine1()),
            clean(r.addressLine2()),
            clean(r.city()),
            clean(r.state()),
            clean(r.postalCode()),
            upper(r.country()),
            clean(r.preferredContactMethod()));
    return ClientDetailResponse.from(clients.save(c));
  }

  @Transactional(readOnly = true)
  public Client require(UUID org, UUID id) {
    return clients
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Client", id));
  }

  @Transactional(readOnly = true)
  public ClientDetailResponse detail(UUID org, UUID id) {
    return ClientDetailResponse.from(require(org, id));
  }

  @Transactional
  public ClientDetailResponse update(UUID org, UUID id, UpdateClientRequest r) {
    Client c = require(org, id);
    String ref = clean(r.externalReferenceNumber());
    if (ref != null
        && !ref.equals(c.getExternalReferenceNumber())
        && clients.existsByOrganizationIdAndExternalReferenceNumber(org, ref))
      throw new ConflictException(
          "DUPLICATE_CLIENT_REFERENCE", "External reference already exists in this organization");
    c.update(
        ref,
        clean(r.firstName()),
        clean(r.lastName()),
        clean(r.preferredName()),
        r.dateOfBirth(),
        email(r.email()),
        clean(r.phone()),
        clean(r.addressLine1()),
        clean(r.addressLine2()),
        clean(r.city()),
        clean(r.state()),
        clean(r.postalCode()),
        upper(r.country()),
        clean(r.preferredContactMethod()),
        r.active());
    return ClientDetailResponse.from(c);
  }

  @Transactional(readOnly = true)
  public Page<ClientSummaryResponse> list(
      UUID org, Boolean active, String email, String ref, String name, Pageable p) {
    Specification<Client> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (active != null) s = s.and((r, q, c) -> c.equal(r.get("active"), active));
    if (email != null && !email.isBlank()) {
      String e = email(email);
      s = s.and((r, q, c) -> c.equal(r.get("email"), e));
    }
    if (ref != null && !ref.isBlank())
      s = s.and((r, q, c) -> c.equal(r.get("externalReferenceNumber"), ref.trim()));
    if (name != null && !name.isBlank()) {
      String n = "%" + name.trim().toLowerCase(Locale.ROOT) + "%";
      s =
          s.and(
              (r, q, c) ->
                  c.or(
                      c.like(c.lower(r.get("firstName")), n),
                      c.like(c.lower(r.get("lastName")), n),
                      c.like(c.lower(r.get("preferredName")), n)));
    }
    return clients.findAll(s, p).map(ClientSummaryResponse::from);
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  private static String required(String s) {
    return s.trim();
  }

  private static String email(String s) {
    return clean(s) == null ? null : UserService.normalizeEmail(s);
  }

  private static String upper(String s) {
    String x = clean(s);
    return x == null ? null : x.toUpperCase(Locale.ROOT);
  }
}
