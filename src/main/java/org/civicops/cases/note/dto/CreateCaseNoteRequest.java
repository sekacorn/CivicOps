package org.civicops.cases.note.dto;

import jakarta.validation.constraints.*;
import org.civicops.cases.note.CaseNoteType;

public record CreateCaseNoteRequest(
    @NotNull CaseNoteType noteType,
    @NotBlank @Size(max = 20000) String content,
    boolean privateNote) {}
