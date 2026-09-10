package org.civicops.foodpantry.pantry;

import jakarta.persistence.*;
import java.time.ZoneId;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "food_pantry_location")
public class FoodPantryLocation extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  private String addressLine1;
  private String addressLine2;
  private String city;
  private String state;
  private String postalCode;

  @Column(length = 2)
  private String country;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false, length = 100)
  private String timezone;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected FoodPantryLocation() {}

  public FoodPantryLocation(
      Organization organization,
      String name,
      String description,
      String addressLine1,
      String addressLine2,
      String city,
      String state,
      String postalCode,
      String country,
      String timezone,
      String notes) {
    this.organization = organization;
    update(
        name,
        description,
        addressLine1,
        addressLine2,
        city,
        state,
        postalCode,
        country,
        timezone,
        notes);
  }

  public void update(
      String name,
      String description,
      String addressLine1,
      String addressLine2,
      String city,
      String state,
      String postalCode,
      String country,
      String timezone,
      String notes) {
    if (name != null) this.name = required(name, "Pantry name");
    if (description != null) this.description = clean(description);
    if (addressLine1 != null) this.addressLine1 = clean(addressLine1);
    if (addressLine2 != null) this.addressLine2 = clean(addressLine2);
    if (city != null) this.city = clean(city);
    if (state != null) this.state = clean(state);
    if (postalCode != null) this.postalCode = clean(postalCode);
    if (country != null)
      this.country = clean(country) == null ? null : country.trim().toUpperCase();
    if (timezone != null) {
      try {
        ZoneId.of(timezone);
      } catch (Exception e) {
        throw new BusinessRuleException(
            "INVALID_TIMEZONE", "Timezone must be a valid IANA timezone");
      }
      this.timezone = timezone;
    }
    if (notes != null) this.notes = clean(notes);
    if (this.timezone == null)
      throw new BusinessRuleException("INVALID_TIMEZONE", "Timezone is required");
  }

  public void deactivate() {
    active = false;
  }

  public void activate() {
    active = true;
  }

  private static String required(String v, String label) {
    if (v == null || v.isBlank())
      throw new BusinessRuleException("INVALID_PANTRY_LOCATION", label + " is required");
    return v.trim();
  }

  private static String clean(String v) {
    return v == null || v.isBlank() ? null : v.trim();
  }

  public Organization getOrganization() {
    return organization;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getAddressLine1() {
    return addressLine1;
  }

  public String getAddressLine2() {
    return addressLine2;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getCountry() {
    return country;
  }

  public boolean isActive() {
    return active;
  }

  public String getTimezone() {
    return timezone;
  }

  public String getNotes() {
    return notes;
  }
}
