package org.civicops.volunteers.opportunity;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.events.event.EventRecord;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "volunteer_opportunity")
public class VolunteerOpportunity extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id")
  private EventRecord event;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(length = 300)
  private String location;

  @Column(nullable = false)
  private Instant startAt;

  @Column(nullable = false)
  private Instant endAt;

  private Integer minimumVolunteers;
  private Integer maximumVolunteers;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private OpportunityStatus status = OpportunityStatus.DRAFT;

  protected VolunteerOpportunity() {}

  public VolunteerOpportunity(
      Organization o,
      String t,
      String d,
      String l,
      Instant s,
      Instant e,
      Integer min,
      Integer max) {
    organization = o;
    title = t;
    description = d;
    location = l;
    startAt = s;
    endAt = e;
    minimumVolunteers = min;
    maximumVolunteers = max;
  }

  public Organization getOrganization() {
    return organization;
  }

  public EventRecord getEvent() {
    return event;
  }

  public void linkEvent(EventRecord event) {
    this.event = event;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getLocation() {
    return location;
  }

  public Instant getStartAt() {
    return startAt;
  }

  public Instant getEndAt() {
    return endAt;
  }

  public Integer getMinimumVolunteers() {
    return minimumVolunteers;
  }

  public Integer getMaximumVolunteers() {
    return maximumVolunteers;
  }

  public OpportunityStatus getStatus() {
    return status;
  }

  public void open() {
    if (status != OpportunityStatus.DRAFT)
      throw new BusinessRuleException(
          "INVALID_OPPORTUNITY_TRANSITION", "Only draft opportunities can be opened");
    status = OpportunityStatus.OPEN;
  }

  public void close() {
    if (status != OpportunityStatus.OPEN && status != OpportunityStatus.FULL)
      throw new BusinessRuleException(
          "INVALID_OPPORTUNITY_TRANSITION", "Only open or full opportunities can be closed");
    status = OpportunityStatus.CLOSED;
  }

  public void cancel() {
    if (status != OpportunityStatus.DRAFT
        && status != OpportunityStatus.OPEN
        && status != OpportunityStatus.FULL)
      throw new BusinessRuleException(
          "INVALID_OPPORTUNITY_TRANSITION",
          "Opportunity cannot be cancelled in its current status");
    status = OpportunityStatus.CANCELLED;
  }

  public void complete() {
    if (status != OpportunityStatus.CLOSED)
      throw new BusinessRuleException(
          "INVALID_OPPORTUNITY_TRANSITION", "Only closed opportunities can be completed");
    status = OpportunityStatus.COMPLETED;
  }

  public void update(
      String title,
      String description,
      String location,
      Instant startAt,
      Instant endAt,
      Integer minimumVolunteers,
      Integer maximumVolunteers) {
    if (status == OpportunityStatus.CLOSED
        || status == OpportunityStatus.CANCELLED
        || status == OpportunityStatus.COMPLETED) {
      throw new BusinessRuleException(
          "TERMINAL_OPPORTUNITY_NOT_EDITABLE", "Terminal opportunities cannot be edited");
    }
    if (title != null) this.title = title;
    if (description != null) this.description = description;
    if (location != null) this.location = location;
    if (startAt != null) this.startAt = startAt;
    if (endAt != null) this.endAt = endAt;
    if (minimumVolunteers != null) this.minimumVolunteers = minimumVolunteers;
    if (maximumVolunteers != null) this.maximumVolunteers = maximumVolunteers;
  }
}
