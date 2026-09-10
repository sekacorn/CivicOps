package org.civicops.facilities.space;

import jakarta.persistence.*;
import org.civicops.core.organization.Organization;
import org.civicops.facilities.facility.Facility;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "facility_space")
public class FacilitySpace extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "facility_id", nullable = false)
  private Facility facility;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 200)
  private String normalizedName;

  @Column(columnDefinition = "TEXT")
  private String description;

  private Integer capacity;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private boolean reservable = true;

  private String locationDetails;

  @Column(columnDefinition = "TEXT")
  private String accessibilityNotes;

  protected FacilitySpace() {}

  public FacilitySpace(
      Facility f,
      String n,
      String normalized,
      String d,
      Integer cap,
      boolean reservable,
      String location,
      String access) {
    organization = f.getOrganization();
    facility = f;
    name = n;
    normalizedName = normalized;
    description = d;
    capacity = cap;
    this.reservable = reservable;
    locationDetails = location;
    accessibilityNotes = access;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Facility getFacility() {
    return facility;
  }

  public String getName() {
    return name;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public String getDescription() {
    return description;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isReservable() {
    return reservable;
  }

  public String getLocationDetails() {
    return locationDetails;
  }

  public String getAccessibilityNotes() {
    return accessibilityNotes;
  }

  public void update(
      String n,
      String normalized,
      String d,
      Integer cap,
      Boolean reservable,
      Boolean active,
      String loc,
      String access) {
    if (n != null) {
      name = n;
      normalizedName = normalized;
    }
    if (d != null) description = d;
    if (cap != null) capacity = cap;
    if (reservable != null) this.reservable = reservable;
    if (active != null) this.active = active;
    if (loc != null) locationDetails = loc;
    if (access != null) accessibilityNotes = access;
  }
}
