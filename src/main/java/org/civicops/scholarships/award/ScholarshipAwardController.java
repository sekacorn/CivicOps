package org.civicops.scholarships.award;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.*;
import org.civicops.scholarships.award.dto.*;
import org.civicops.scholarships.security.ScholarshipAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/scholarship-awards")
@Tag(name = "Scholarship Awards")
public class ScholarshipAwardController {
  private final ScholarshipAwardService awards;
  private final ScholarshipAccessService access;

  public ScholarshipAwardController(ScholarshipAwardService a, ScholarshipAccessService x) {
    awards = a;
    access = x;
  }

  @GetMapping
  public Page<ScholarshipAwardResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) UUID programId,
      @RequestParam(required = false) ScholarshipAwardStatus status,
      @RequestParam(required = false) LocalDate awardDateFrom,
      @RequestParam(required = false) LocalDate awardDateTo,
      @PageableDefault(size = 20, sort = "awardDate") Pageable p) {
    access.requireScholarshipManagement(organizationId);
    return awards.list(
        organizationId,
        programId,
        status,
        awardDateFrom,
        awardDateTo,
        SafePageables.allow(p, Set.of("awardDate", "amount", "createdAt")));
  }

  @GetMapping("/{awardId}")
  public ScholarshipAwardResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID awardId) {
    access.requireScholarshipManagement(organizationId);
    return awards.detail(organizationId, awardId);
  }

  @PostMapping("/{awardId}/accept")
  public ScholarshipAwardResponse accept(
      @PathVariable UUID organizationId, @PathVariable UUID awardId) {
    return transition(organizationId, awardId, ScholarshipAwardStatus.ACCEPTED);
  }

  @PostMapping("/{awardId}/decline")
  public ScholarshipAwardResponse decline(
      @PathVariable UUID organizationId, @PathVariable UUID awardId) {
    return transition(organizationId, awardId, ScholarshipAwardStatus.DECLINED);
  }

  @PostMapping("/{awardId}/mark-disbursed")
  public ScholarshipAwardResponse disburse(
      @PathVariable UUID organizationId, @PathVariable UUID awardId) {
    return transition(organizationId, awardId, ScholarshipAwardStatus.DISBURSED);
  }

  @PostMapping("/{awardId}/cancel")
  public ScholarshipAwardResponse cancel(
      @PathVariable UUID organizationId, @PathVariable UUID awardId) {
    return transition(organizationId, awardId, ScholarshipAwardStatus.CANCELLED);
  }

  private ScholarshipAwardResponse transition(UUID org, UUID id, ScholarshipAwardStatus target) {
    access.requireScholarshipManagement(org);
    return awards.transition(org, id, target);
  }
}
