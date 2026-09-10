package org.civicops.board.minutes;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.civicops.board.security.BoardAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/board-meetings/{meetingId}/minutes")
@Tag(name = "Board Meeting Minutes")
public class BoardMinutesController {
  private final BoardMinutesService minutes;
  private final BoardAccessService access;

  public BoardMinutesController(BoardMinutesService m, BoardAccessService a) {
    minutes = m;
    access = a;
  }

  @PutMapping
  public BoardMinutesDtos.Response put(
      @PathVariable UUID organizationId,
      @PathVariable UUID meetingId,
      @Valid @RequestBody BoardMinutesDtos.Put r) {
    access.requireMinutesManagement(organizationId);
    return minutes.put(organizationId, meetingId, access.userId(), r.content());
  }

  @GetMapping
  public BoardMinutesDtos.Response get(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    access.requireBoardRead(organizationId);
    return minutes.get(organizationId, meetingId, access.canManage(organizationId));
  }

  @PostMapping("/submit")
  public BoardMinutesDtos.Response submit(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    access.requireMinutesManagement(organizationId);
    return minutes.submit(organizationId, meetingId);
  }

  @PostMapping("/approve")
  public BoardMinutesDtos.Response approve(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    access.requireMinutesManagement(organizationId);
    return minutes.approve(organizationId, meetingId, access.userId());
  }
}
