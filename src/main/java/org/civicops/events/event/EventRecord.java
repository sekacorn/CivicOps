package org.civicops.events.event;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.donations.campaign.DonationCampaign;
import org.civicops.grants.grant.Grant;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "event_record")
public class EventRecord extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private EventType eventType;

  @Column(length = 300)
  private String location;

  @Column(nullable = false)
  private Instant startDateTime;

  @Column(nullable = false)
  private Instant endDateTime;

  private Integer capacity;

  @Column(nullable = false)
  private boolean registrationRequired;

  private Instant registrationDeadline;

  @Column(nullable = false)
  private boolean waitlistEnabled;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private EventStatus status = EventStatus.DRAFT;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "linked_grant_id")
  private Grant linkedGrant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "linked_campaign_id")
  private DonationCampaign linkedDonationCampaign;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdBy;

  protected EventRecord() {}

  public EventRecord(
      Organization org,
      User creator,
      String name,
      String description,
      EventType type,
      String location,
      Instant start,
      Instant end,
      Integer capacity,
      boolean registrationRequired,
      Instant deadline,
      boolean waitlist,
      Grant grant,
      DonationCampaign campaign) {
    this.organization = org;
    this.createdBy = creator;
    this.name = name;
    this.description = description;
    this.eventType = type;
    this.location = location;
    this.startDateTime = start;
    this.endDateTime = end;
    this.capacity = capacity;
    this.registrationRequired = registrationRequired;
    this.registrationDeadline = deadline;
    this.waitlistEnabled = waitlist;
    this.linkedGrant = grant;
    this.linkedDonationCampaign = campaign;
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

  public EventType getEventType() {
    return eventType;
  }

  public String getLocation() {
    return location;
  }

  public Instant getStartDateTime() {
    return startDateTime;
  }

  public Instant getEndDateTime() {
    return endDateTime;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public boolean isRegistrationRequired() {
    return registrationRequired;
  }

  public Instant getRegistrationDeadline() {
    return registrationDeadline;
  }

  public boolean isWaitlistEnabled() {
    return waitlistEnabled;
  }

  public EventStatus getStatus() {
    return status;
  }

  public Grant getLinkedGrant() {
    return linkedGrant;
  }

  public DonationCampaign getLinkedDonationCampaign() {
    return linkedDonationCampaign;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public boolean acceptsRegistrations(Instant now) {
    return status == EventStatus.REGISTRATION_OPEN
        && registrationRequired
        && (registrationDeadline == null || !now.isAfter(registrationDeadline));
  }

  public void transition(EventStatus target) {
    boolean allowed =
        switch (status) {
          case DRAFT -> target == EventStatus.PUBLISHED || target == EventStatus.CANCELLED;
          case PUBLISHED ->
              target == EventStatus.REGISTRATION_OPEN || target == EventStatus.CANCELLED;
          case REGISTRATION_OPEN ->
              target == EventStatus.REGISTRATION_CLOSED || target == EventStatus.CANCELLED;
          case REGISTRATION_CLOSED ->
              target == EventStatus.COMPLETED || target == EventStatus.CANCELLED;
          default -> false;
        };
    if (!allowed)
      throw new BusinessRuleException(
          "INVALID_EVENT_TRANSITION", "Event cannot transition from " + status + " to " + target);
    status = target;
  }

  public void update(
      String name,
      String description,
      EventType type,
      String location,
      Instant start,
      Instant end,
      Integer capacity,
      Boolean registrationRequired,
      Instant deadline,
      Boolean waitlist,
      Grant grant,
      DonationCampaign campaign) {
    if (status == EventStatus.COMPLETED || status == EventStatus.CANCELLED)
      throw new BusinessRuleException(
          "TERMINAL_EVENT_NOT_EDITABLE", "Completed or cancelled events cannot be edited");
    if (name != null) this.name = name;
    if (description != null) this.description = description;
    if (type != null) this.eventType = type;
    if (location != null) this.location = location;
    if (start != null) this.startDateTime = start;
    if (end != null) this.endDateTime = end;
    if (capacity != null) this.capacity = capacity;
    if (registrationRequired != null) this.registrationRequired = registrationRequired;
    if (deadline != null) this.registrationDeadline = deadline;
    if (waitlist != null) this.waitlistEnabled = waitlist;
    this.linkedGrant = grant;
    this.linkedDonationCampaign = campaign;
  }
}
