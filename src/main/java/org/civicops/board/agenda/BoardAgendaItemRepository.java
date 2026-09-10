package org.civicops.board.agenda;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardAgendaItemRepository extends JpaRepository<BoardAgendaItem, UUID> {
  Optional<BoardAgendaItem> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByMeetingIdAndSequenceNumber(UUID meeting, int sequence);

  List<BoardAgendaItem> findAllByOrganizationIdAndMeetingIdOrderBySequenceNumber(
      UUID org, UUID meeting);
}
