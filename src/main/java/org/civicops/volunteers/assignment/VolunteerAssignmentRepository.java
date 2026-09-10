package org.civicops.volunteers.assignment;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VolunteerAssignmentRepository
    extends JpaRepository<VolunteerAssignment, UUID>,
        JpaSpecificationExecutor<VolunteerAssignment> {
  Optional<VolunteerAssignment> findByIdAndOrganizationId(UUID id, UUID orgId);

  Page<VolunteerAssignment> findByOrganizationIdAndShiftId(
      UUID orgId, UUID shiftId, Pageable pageable);

  Page<VolunteerAssignment> findByOrganizationIdAndVolunteerId(
      UUID orgId, UUID volunteerId, Pageable pageable);

  long countByShiftIdAndStatusNot(UUID shiftId, AssignmentStatus status);

  boolean existsByVolunteerIdAndShiftIdAndStatusNot(
      UUID volunteerId, UUID shiftId, AssignmentStatus status);

  @Query(
      "select count(a)>0 from VolunteerAssignment a where a.volunteer.id=:volunteerId and"
          + " a.status<>org.civicops.volunteers.assignment.AssignmentStatus.CANCELLED and"
          + " a.shift.startAt<:endAt and a.shift.endAt>:startAt")
  boolean hasOverlap(
      @Param("volunteerId") UUID volunteerId,
      @Param("startAt") Instant startAt,
      @Param("endAt") Instant endAt);

  @Query(
      "select count(a)>0 from VolunteerAssignment a where a.shift.id=:shiftId and"
          + " a.status<>org.civicops.volunteers.assignment.AssignmentStatus.CANCELLED and exists"
          + " (select b.id from VolunteerAssignment b where b.volunteer.id=a.volunteer.id and"
          + " b.shift.id<>:shiftId and"
          + " b.status<>org.civicops.volunteers.assignment.AssignmentStatus.CANCELLED and"
          + " b.shift.startAt<:endAt and b.shift.endAt>:startAt)")
  boolean shiftUpdateCreatesOverlap(
      @Param("shiftId") UUID shiftId,
      @Param("startAt") Instant startAt,
      @Param("endAt") Instant endAt);
}
