package org.civicops.events.registration;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EventRegistrationRepository
    extends JpaRepository<EventRegistration, UUID>, JpaSpecificationExecutor<EventRegistration> {
  Optional<EventRegistration> findByIdAndOrganizationId(UUID id, UUID organizationId);

  @Query(
      "select count(r) from EventRegistration r where r.event.id=:eventId and r.status in (org.civicops.events.registration.RegistrationStatus.REGISTERED,org.civicops.events.registration.RegistrationStatus.ATTENDED)")
  long countSeated(@Param("eventId") UUID eventId);

  @Query(
      "select r from EventRegistration r where r.event.id=:eventId and r.status=org.civicops.events.registration.RegistrationStatus.WAITLISTED order by r.registrationDate asc, r.id asc")
  List<EventRegistration> waitlisted(@Param("eventId") UUID eventId);

  boolean existsByEventIdAndRegisteredUserIdAndStatusIn(
      UUID eventId, UUID userId, Collection<RegistrationStatus> statuses);

  boolean existsByEventIdAndAttendeeEmailIgnoreCaseAndStatusIn(
      UUID eventId, String email, Collection<RegistrationStatus> statuses);

  long countByOrganizationIdAndStatus(UUID orgId, RegistrationStatus status);

  long countByEventIdAndStatus(UUID eventId, RegistrationStatus status);
}
