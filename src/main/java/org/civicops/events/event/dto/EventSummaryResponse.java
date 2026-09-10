package org.civicops.events.event.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.events.event.*;

public record EventSummaryResponse(
    UUID id,
    String name,
    EventType eventType,
    Instant startDateTime,
    Instant endDateTime,
    Integer capacity,
    EventStatus status) {
  public static EventSummaryResponse from(EventRecord e) {
    return new EventSummaryResponse(
        e.getId(),
        e.getName(),
        e.getEventType(),
        e.getStartDateTime(),
        e.getEndDateTime(),
        e.getCapacity(),
        e.getStatus());
  }
}
