package org.civicops.cases.note;

import java.util.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.note.dto.*;
import org.civicops.core.user.UserService;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseNoteService {
  private final CaseNoteRepository notes;
  private final CaseRecordService cases;
  private final UserService users;

  public CaseNoteService(CaseNoteRepository n, CaseRecordService c, UserService u) {
    notes = n;
    cases = c;
    users = u;
  }

  @Transactional
  public CaseNoteResponse create(UUID org, UUID caseId, UUID author, CreateCaseNoteRequest r) {
    CaseRecord c = cases.require(org, caseId);
    if (c.isTerminal())
      throw new BusinessRuleException(
          "TERMINAL_CASE_HISTORY_ONLY", "Notes cannot be added to a closed or cancelled case");
    return CaseNoteResponse.from(
        notes.save(
            new CaseNote(
                c.getOrganization(),
                c,
                users.requireEntity(author),
                r.noteType(),
                r.content().trim(),
                r.privateNote())));
  }

  @Transactional(readOnly = true)
  public Page<CaseNoteResponse> list(UUID org, UUID caseId, Pageable p) {
    cases.require(org, caseId);
    return notes.findAllByOrganizationIdAndCaseRecordId(org, caseId, p).map(CaseNoteResponse::from);
  }

  @Transactional(readOnly = true)
  public CaseNoteResponse detail(UUID org, UUID caseId, UUID note) {
    return CaseNoteResponse.from(
        notes
            .findByIdAndOrganizationIdAndCaseRecordId(note, org, caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case note", note)));
  }
}
