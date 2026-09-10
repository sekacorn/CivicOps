package org.civicops.facilities.availability;

import jakarta.persistence.*;
import java.time.*;
import org.civicops.core.organization.Organization;
import org.civicops.facilities.facility.Facility;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "facility_operating_hours")
public class FacilityOperatingHours extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "facility_id", nullable = false)
  private Facility facility;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private DayOfWeek dayOfWeek;

  private LocalTime openTime, closeTime;

  @Column(nullable = false)
  private boolean closed;

  protected FacilityOperatingHours() {}

  public FacilityOperatingHours(Facility f, DayOfWeek d, LocalTime o, LocalTime c, boolean closed) {
    organization = f.getOrganization();
    facility = f;
    dayOfWeek = d;
    set(o, c, closed);
  }

  public void set(LocalTime o, LocalTime c, boolean closed) {
    this.closed = closed;
    openTime = closed ? null : o;
    closeTime = closed ? null : c;
  }

  public Facility getFacility() {
    return facility;
  }

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  public LocalTime getOpenTime() {
    return openTime;
  }

  public LocalTime getCloseTime() {
    return closeTime;
  }

  public boolean isClosed() {
    return closed;
  }
}
