package org.civicops.facilities.availability;

import java.time.DayOfWeek;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityOperatingHoursRepository
    extends JpaRepository<FacilityOperatingHours, UUID> {
  List<FacilityOperatingHours> findAllByOrganizationIdAndFacilityIdOrderByDayOfWeek(
      UUID org, UUID facility);

  Optional<FacilityOperatingHours> findByOrganizationIdAndFacilityIdAndDayOfWeek(
      UUID org, UUID facility, DayOfWeek day);
}
