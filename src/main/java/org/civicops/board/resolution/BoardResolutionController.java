package org.civicops.board.resolution;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.civicops.board.security.BoardAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Board Resolutions")
public class BoardResolutionController {
  private final BoardResolutionService resolutions;
  private final BoardAccessService access;

  public BoardResolutionController(BoardResolutionService r, BoardAccessService a) {
    resolutions = r;
    access = a;
  }

  @PostMapping("/board-motions/{motionId}/resolution")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardResolutionDtos.Response create(
      @PathVariable UUID organizationId,
      @PathVariable UUID motionId,
      @Valid @RequestBody BoardResolutionDtos.Create r) {
    access.requireBoardManagement(organizationId);
    return resolutions.create(organizationId, motionId, access.userId(), r);
  }

  @GetMapping("/board-resolutions")
  public Page<BoardResolutionDtos.Response> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) ResolutionStatus status,
      @RequestParam(required = false) LocalDate adoptedFrom,
      @RequestParam(required = false) LocalDate adoptedTo,
      @PageableDefault(size = 20, sort = "adoptedDate") Pageable p) {
    access.requireBoardRead(organizationId);
    return resolutions.list(
        organizationId,
        status,
        adoptedFrom,
        adoptedTo,
        SafePageables.allow(p, Set.of("adoptedDate", "resolutionNumber", "createdAt")));
  }

  @GetMapping("/board-resolutions/{resolutionId}")
  public BoardResolutionDtos.Response detail(
      @PathVariable UUID organizationId, @PathVariable UUID resolutionId) {
    access.requireBoardRead(organizationId);
    return resolutions.detail(organizationId, resolutionId);
  }

  @PostMapping("/board-resolutions/{resolutionId}/rescind")
  public BoardResolutionDtos.Response rescind(
      @PathVariable UUID organizationId, @PathVariable UUID resolutionId) {
    access.requireBoardManagement(organizationId);
    return resolutions.rescind(organizationId, resolutionId);
  }
}
