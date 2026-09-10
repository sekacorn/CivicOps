package org.civicops.board.agenda;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.board.security.BoardAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Board Agendas")
public class BoardAgendaController {
  private final BoardAgendaService agenda;
  private final BoardAccessService access;

  public BoardAgendaController(BoardAgendaService a, BoardAccessService access) {
    agenda = a;
    this.access = access;
  }

  @PostMapping("/board-meetings/{meetingId}/agenda-items")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardAgendaDtos.Response create(
      @PathVariable UUID organizationId,
      @PathVariable UUID meetingId,
      @Valid @RequestBody BoardAgendaDtos.Create r) {
    access.requireBoardManagement(organizationId);
    return agenda.create(organizationId, meetingId, r);
  }

  @GetMapping("/board-meetings/{meetingId}/agenda-items")
  public List<BoardAgendaDtos.Response> list(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    access.requireBoardRead(organizationId);
    return agenda.list(organizationId, meetingId);
  }

  @PatchMapping("/board-agenda-items/{itemId}")
  public BoardAgendaDtos.Response update(
      @PathVariable UUID organizationId,
      @PathVariable UUID itemId,
      @Valid @RequestBody BoardAgendaDtos.Update r) {
    access.requireBoardManagement(organizationId);
    return agenda.update(organizationId, itemId, r);
  }

  @PostMapping("/board-agenda-items/{itemId}/start")
  public BoardAgendaDtos.Response start(
      @PathVariable UUID organizationId, @PathVariable UUID itemId) {
    return transition(organizationId, itemId, AgendaItemStatus.IN_PROGRESS);
  }

  @PostMapping("/board-agenda-items/{itemId}/complete")
  public BoardAgendaDtos.Response complete(
      @PathVariable UUID organizationId, @PathVariable UUID itemId) {
    return transition(organizationId, itemId, AgendaItemStatus.COMPLETED);
  }

  @PostMapping("/board-agenda-items/{itemId}/skip")
  public BoardAgendaDtos.Response skip(
      @PathVariable UUID organizationId, @PathVariable UUID itemId) {
    return transition(organizationId, itemId, AgendaItemStatus.SKIPPED);
  }

  private BoardAgendaDtos.Response transition(UUID org, UUID id, AgendaItemStatus s) {
    access.requireBoardManagement(org);
    return agenda.transition(org, id, s);
  }
}
