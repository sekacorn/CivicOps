package org.civicops.cases.note.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.cases.note.*;

public record CaseNoteResponse(
    UUID id,
    UUID caseId,
    UUID authorUserId,
    String authorDisplayName,
    CaseNoteType noteType,
    String content,
    boolean privateNote,
    Instant createdAt) {
  public static CaseNoteResponse from(CaseNote n) {
    return new CaseNoteResponse(
        n.getId(),
        n.getCaseRecord().getId(),
        n.getAuthor().getId(),
        (n.getAuthor().getFirstName() + " " + n.getAuthor().getLastName()).trim(),
        n.getNoteType(),
        n.getContent(),
        n.isPrivateNote(),
        n.getCreatedAt());
  }
}
