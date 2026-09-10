package org.civicops.volunteers.shift;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.volunteers.opportunity.VolunteerOpportunity;

@Entity
@Table(name = "volunteer_shift")
public class VolunteerShift extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "opportunity_id")
  private VolunteerOpportunity opportunity;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false)
  private Instant startAt;

  @Column(nullable = false)
  private Instant endAt;

  @Column(nullable = false)
  private int capacity;

  protected VolunteerShift() {}

  public VolunteerShift(
      Organization o, VolunteerOpportunity p, String t, Instant s, Instant e, int c) {
    organization = o;
    opportunity = p;
    title = t;
    startAt = s;
    endAt = e;
    capacity = c;
  }

  public Organization getOrganization() {
    return organization;
  }

  public VolunteerOpportunity getOpportunity() {
    return opportunity;
  }

  public String getTitle() {
    return title;
  }

  public Instant getStartAt() {
    return startAt;
  }

  public Instant getEndAt() {
    return endAt;
  }

  public int getCapacity() {
    return capacity;
  }

  public void update(String title, Instant startAt, Instant endAt, Integer capacity) {
    if (title != null) this.title = title;
    if (startAt != null) this.startAt = startAt;
    if (endAt != null) this.endAt = endAt;
    if (capacity != null) this.capacity = capacity;
  }
}
