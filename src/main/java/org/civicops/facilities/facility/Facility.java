package org.civicops.facilities.facility;

import jakarta.persistence.*;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "facility")
public class Facility extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private FacilityType facilityType;

  private String addressLine1, addressLine2, city, state, postalCode, country;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false, length = 80)
  private String timezone;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected Facility() {}

  public Facility(
      Organization o,
      String n,
      String d,
      FacilityType t,
      String a1,
      String a2,
      String c,
      String s,
      String p,
      String country,
      String zone,
      String notes) {
    organization = o;
    name = n;
    description = d;
    facilityType = t;
    addressLine1 = a1;
    addressLine2 = a2;
    city = c;
    state = s;
    postalCode = p;
    this.country = country;
    timezone = zone;
    this.notes = notes;
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

  public FacilityType getFacilityType() {
    return facilityType;
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

  public void update(
      String n,
      String d,
      FacilityType t,
      String a1,
      String a2,
      String c,
      String s,
      String p,
      String country,
      String zone,
      String notes,
      Boolean active) {
    if (n != null) name = n;
    if (d != null) description = d;
    if (t != null) facilityType = t;
    if (a1 != null) addressLine1 = a1;
    if (a2 != null) addressLine2 = a2;
    if (c != null) city = c;
    if (s != null) state = s;
    if (p != null) postalCode = p;
    if (country != null) this.country = country;
    if (zone != null) timezone = zone;
    if (notes != null) this.notes = notes;
    if (active != null) this.active = active;
  }
}
