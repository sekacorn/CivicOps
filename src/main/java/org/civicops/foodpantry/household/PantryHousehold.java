package org.civicops.foodpantry.household;

import jakarta.persistence.*;
import java.util.Locale;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "pantry_household")
public class PantryHousehold extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  private String externalReferenceNumber;
  private String householdName;
  private String primaryContactFirstName;
  private String primaryContactLastName;
  private String email;
  private String normalizedEmail;
  private String phone;

  @Column(columnDefinition = "TEXT")
  private String address;

  private int householdSize;

  @Column(nullable = false)
  private boolean active = true;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected PantryHousehold() {}

  public PantryHousehold(
      Organization o,
      String ref,
      String name,
      String first,
      String last,
      String email,
      String phone,
      String address,
      int size,
      String notes) {
    organization = o;
    update(ref, name, first, last, email, phone, address, size, notes);
  }

  public void update(
      String ref,
      String name,
      String first,
      String last,
      String email,
      String phone,
      String address,
      Integer size,
      String notes) {
    if (ref != null) externalReferenceNumber = clean(ref);
    if (name != null) householdName = clean(name);
    if (first != null) primaryContactFirstName = clean(first);
    if (last != null) primaryContactLastName = clean(last);
    if (email != null) {
      this.email = clean(email);
      normalizedEmail = normalizeEmail(email);
    }
    if (phone != null) this.phone = clean(phone);
    if (address != null) this.address = clean(address);
    if (size != null) {
      if (size <= 0)
        throw new BusinessRuleException(
            "INVALID_HOUSEHOLD_SIZE", "Household size must be positive");
      householdSize = size;
    }
    if (notes != null) this.notes = clean(notes);
    if (householdSize <= 0)
      throw new BusinessRuleException("INVALID_HOUSEHOLD_SIZE", "Household size must be positive");
  }

  public void deactivate() {
    active = false;
  }

  public void activate() {
    active = true;
  }

  public String displayName() {
    String contact =
        ((primaryContactFirstName == null ? "" : primaryContactFirstName)
                + " "
                + (primaryContactLastName == null ? "" : primaryContactLastName))
            .trim();
    return householdName != null
        ? householdName
        : contact.isBlank() ? "Household" : contact + " Household";
  }

  public static String normalizeEmail(String v) {
    String c = clean(v);
    return c == null ? null : c.toLowerCase(Locale.ROOT);
  }

  private static String clean(String v) {
    return v == null || v.isBlank() ? null : v.trim();
  }

  public Organization getOrganization() {
    return organization;
  }

  public String getExternalReferenceNumber() {
    return externalReferenceNumber;
  }

  public String getHouseholdName() {
    return householdName;
  }

  public String getPrimaryContactFirstName() {
    return primaryContactFirstName;
  }

  public String getPrimaryContactLastName() {
    return primaryContactLastName;
  }

  public String getEmail() {
    return email;
  }

  public String getNormalizedEmail() {
    return normalizedEmail;
  }

  public String getPhone() {
    return phone;
  }

  public String getAddress() {
    return address;
  }

  public int getHouseholdSize() {
    return householdSize;
  }

  public boolean isActive() {
    return active;
  }

  public String getNotes() {
    return notes;
  }
}
