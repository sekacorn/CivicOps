package org.civicops.volunteers.assignment;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.volunteers.shift.VolunteerShift;
import org.civicops.volunteers.volunteer.Volunteer;

@Entity
@Table(name = "volunteer_assignment")
public class VolunteerAssignment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "volunteer_id")
  private Volunteer volunteer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shift_id")
  private VolunteerShift shift;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AssignmentStatus status = AssignmentStatus.REGISTERED;

  private Instant checkedInAt;
  private Instant checkedOutAt;

  protected VolunteerAssignment() {}

  public VolunteerAssignment(Organization o, Volunteer v, VolunteerShift s) {
    organization = o;
    volunteer = v;
    shift = s;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Volunteer getVolunteer() {
    return volunteer;
  }

  public VolunteerShift getShift() {
    return shift;
  }

  public AssignmentStatus getStatus() {
    return status;
  }

  public Instant getCheckedInAt() {
    return checkedInAt;
  }

  public Instant getCheckedOutAt() {
    return checkedOutAt;
  }

  public void cancel() {
    if (status == AssignmentStatus.ATTENDED || status == AssignmentStatus.CANCELLED)
      throw new BusinessRuleException(
          "INVALID_ASSIGNMENT_TRANSITION", "Assignment cannot be cancelled in its current status");
    status = AssignmentStatus.CANCELLED;
  }

  public void checkIn(Instant now) {
    if (checkedInAt != null
        || (status != AssignmentStatus.REGISTERED && status != AssignmentStatus.CONFIRMED))
      throw new BusinessRuleException("INVALID_CHECK_IN", "Assignment cannot be checked in");
    checkedInAt = now;
    status = AssignmentStatus.CONFIRMED;
  }

  public void checkOut(Instant now) {
    if (checkedInAt == null || checkedOutAt != null)
      throw new BusinessRuleException(
          "INVALID_CHECK_OUT", "Assignment must be checked in and not already checked out");
    checkedOutAt = now;
    status = AssignmentStatus.ATTENDED;
  }
}
