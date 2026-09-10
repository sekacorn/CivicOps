package org.civicops.facilities.availability;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.facilities.facility.Facility;
import org.civicops.facilities.space.FacilitySpace;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "facility_blackout")
public class FacilityBlackout extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "facility_id", nullable = false)
  private Facility facility;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "facility_space_id")
  private FacilitySpace facilitySpace;

  @Column(nullable = false)
  private Instant startDateTime, endDateTime;

  @Column(nullable = false, length = 1000)
  private String reason;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false)
  private User createdBy;

  @Column(nullable = false)
  private boolean active = true;

  private Instant cancelledAt;

  protected FacilityBlackout() {}

  public FacilityBlackout(
      Facility f, FacilitySpace s, Instant start, Instant end, String reason, User user) {
    organization = f.getOrganization();
    facility = f;
    facilitySpace = s;
    startDateTime = start;
    endDateTime = end;
    this.reason = reason;
    createdBy = user;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Facility getFacility() {
    return facility;
  }

  public FacilitySpace getFacilitySpace() {
    return facilitySpace;
  }

  public Instant getStartDateTime() {
    return startDateTime;
  }

  public Instant getEndDateTime() {
    return endDateTime;
  }

  public String getReason() {
    return reason;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public void cancel(Instant now) {
    if (!active)
      throw new BusinessRuleException(
          "BLACKOUT_ALREADY_CANCELLED", "Blackout is already cancelled");
    active = false;
    cancelledAt = now;
  }
}
