package org.civicops.cases.client.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.cases.client.Client;

public record ClientSummaryResponse(
    UUID id,
    String displayName,
    boolean active,
    String externalReferenceNumber,
    Instant createdAt) {
  public static ClientSummaryResponse from(Client c) {
    return new ClientSummaryResponse(
        c.getId(), c.displayName(), c.isActive(), c.getExternalReferenceNumber(), c.getCreatedAt());
  }
}
