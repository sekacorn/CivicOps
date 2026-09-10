package org.civicops.board.minutes;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardMeetingMinutesRepository extends JpaRepository<BoardMeetingMinutes, UUID> {
  Optional<BoardMeetingMinutes> findByOrganizationIdAndMeetingId(UUID org, UUID meeting);
}
