import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { BoardApiService } from "./board/board-api.service";
import { FoodPantryApiService } from "./food-pantry/food-pantry-api.service";
import { ScholarshipApiService } from "./scholarships/scholarship-api.service";

describe("Phase 16.3 API services", () => {
  let http: HttpTestingController;
  let scholarships: ScholarshipApiService;
  let pantry: FoodPantryApiService;
  let board: BoardApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    scholarships = TestBed.inject(ScholarshipApiService);
    pantry = TestBed.inject(FoodPantryApiService);
    board = TestBed.inject(BoardApiService);
  });

  afterEach(() => http.verify());

  it("uses scholarship program, application, review, and award endpoints", () => {
    scholarships
      .programs("org", { status: "OPEN", academicYear: "2026", page: 2 })
      .subscribe();
    const programs = http.expectOne((r) =>
      r.url.endsWith("/organizations/org/scholarship-programs"),
    );
    expect(programs.request.params.get("status")).toBe("OPEN");
    expect(programs.request.params.get("academicYear")).toBe("2026");

    scholarships.programAction("org", "program", "start-review").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/scholarship-programs/program/start-review",
      ).request.method,
    ).toBe("POST");

    scholarships.applicationAction("org", "app", "finalist").subscribe();
    http.expectOne(
      "/api/v1/organizations/org/scholarship-applications/app/finalist",
    );

    scholarships.submitReview("org", "assignment", { score: 90 }).subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/scholarship-review-assignments/assignment/submit-review",
      ).request.body.score,
    ).toBe(90);

    scholarships.awardAction("org", "award", "mark-disbursed").subscribe();
    http.expectOne(
      "/api/v1/organizations/org/scholarship-awards/award/mark-disbursed",
    );
  });

  it("uses food pantry inventory, household, distribution, and reporting contracts", () => {
    pantry.receipt("org", "pantry", { itemId: "item" }).subscribe();
    http.expectOne(
      "/api/v1/organizations/org/food-pantries/pantry/inventory-receipts",
    );

    pantry.adjustLot("org", "lot", { adjustmentType: "SPOILAGE" }).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/pantry-inventory/lot/adjust")
        .request.method,
    ).toBe("POST");

    pantry.householdVisits("org", "household").subscribe();
    http.expectOne((r) =>
      r.url.endsWith("/organizations/org/pantry-households/household/visits"),
    );

    pantry.distributionAction("org", "visit", "complete").subscribe();
    http.expectOne(
      "/api/v1/organizations/org/pantry-distribution-visits/visit/complete",
    );

    pantry
      .distributionReport("org", "pantry", "2026-01-01", "2026-12-31")
      .subscribe();
    const report = http.expectOne((r) =>
      r.url.endsWith("/organizations/org/food-pantry-reports/distributions"),
    );
    expect(report.request.params.get("pantryId")).toBe("pantry");
    expect(report.request.params.get("from")).toBe("2026-01-01");
    pantry.availability("org", "pantry").subscribe();
    http.expectOne(
      "/api/v1/organizations/org/food-pantries/pantry/inventory/availability",
    );
  });

  it("uses board lifecycle, self-service attendance, voting, minutes, and report contracts", () => {
    board.meetingAction("org", "meeting", "publish").subscribe();
    http.expectOne("/api/v1/organizations/org/board-meetings/meeting/publish");

    board
      .selfAttendance("org", "meeting", { attendanceStatus: "REMOTE" })
      .subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/board-meetings/meeting/attendance/me",
      ).request.method,
    ).toBe("PUT");

    board.motionAction("org", "motion", "open").subscribe();
    http.expectOne(
      "/api/v1/organizations/org/board-motions/motion/open-voting",
    );
    board.secondMotion("org", "motion", "member-2").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/board-motions/motion/second")
        .request.body.secondedByBoardMemberId,
    ).toBe("member-2");

    board.vote("org", "motion", { choice: "YES" }).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/board-motions/motion/votes/me")
        .request.body.choice,
    ).toBe("YES");

    board.saveMinutes("org", "meeting", { content: "Approved" }).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/board-meetings/meeting/minutes")
        .request.method,
    ).toBe("PUT");
    board.minutesAction("org", "meeting", "approve").subscribe();
    http.expectOne(
      "/api/v1/organizations/org/board-meetings/meeting/minutes/approve",
    );

    board.summaryReport("org").subscribe();
    http.expectOne("/api/v1/organizations/org/board-reports/summary");
  });
});
