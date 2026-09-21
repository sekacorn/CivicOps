import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { VolunteerApiService } from "./volunteer-api.service";

describe("VolunteerApiService", () => {
  let api: VolunteerApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(VolunteerApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it("loads privacy-safe volunteer summaries", () => {
    api.volunteers("org", 1).subscribe();
    const r = http.expectOne((x) => x.url.endsWith("/volunteers"));
    expect(r.request.params.get("page")).toBe("1");
    expect(r.request.params.get("sort")).toBe("lastName,asc");
  });
  it("creates and transitions opportunities", () => {
    api.createOpportunity("org", { title: "Serve" }).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/volunteer-opportunities")
        .request.method,
    ).toBe("POST");
    api.transitionOpportunity("org", "opp", "open").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/volunteer-opportunities/opp/open",
      ).request.method,
    ).toBe("POST");
  });
  it("creates shifts and assignments using backend capacity authority", () => {
    api.createShift("org", "opp", { capacity: 2 }).subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/volunteer-opportunities/opp/shifts",
      ).request.method,
    ).toBe("POST");
    api.assign("org", "shift", "volunteer").subscribe();
    const r = http.expectOne(
      "/api/v1/organizations/org/volunteer-shifts/shift/assignments",
    );
    expect(r.request.body).toEqual({ volunteerId: "volunteer" });
  });
  it("submits and reviews hours through explicit endpoints", () => {
    api.hours("org").subscribe();
    const list = http.expectOne((request) =>
      request.url.endsWith("/volunteer-hours"),
    );
    expect(list.request.params.get("status")).toBe("SUBMITTED");
    api
      .submitHours("org", {
        volunteerId: "v",
        serviceDate: "2026-09-12",
        hours: 2,
      })
      .subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/volunteer-hours").request
        .method,
    ).toBe("POST");
    api.reviewHours("org", "hours", "approve").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/volunteer-hours/hours/approve")
        .request.method,
    ).toBe("POST");
  });
  it("requests date-bounded reporting", () => {
    api.report("org", "2026-01-01", "2026-12-31").subscribe();
    const r = http.expectOne((x) =>
      x.url.endsWith("/volunteer-reports/summary"),
    );
    expect(r.request.params.get("from")).toBe("2026-01-01");
    expect(r.request.params.get("to")).toBe("2026-12-31");
  });
});
