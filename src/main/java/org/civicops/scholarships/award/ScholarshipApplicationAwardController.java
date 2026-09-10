package org.civicops.scholarships.award;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.civicops.scholarships.award.dto.*;
import org.civicops.scholarships.security.ScholarshipAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
    "/api/v1/organizations/{organizationId}/scholarship-applications/{applicationId}/award")
@Tag(name = "Scholarship Awards")
public class ScholarshipApplicationAwardController {
  private final ScholarshipAwardService awards;
  private final ScholarshipAccessService access;

  public ScholarshipApplicationAwardController(
      ScholarshipAwardService a, ScholarshipAccessService x) {
    awards = a;
    access = x;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ScholarshipAwardResponse create(
      @PathVariable UUID organizationId,
      @PathVariable UUID applicationId,
      @Valid @RequestBody CreateScholarshipAwardRequest r) {
    access.requireScholarshipManagement(organizationId);
    return awards.create(organizationId, applicationId, access.userId(), r);
  }
}
