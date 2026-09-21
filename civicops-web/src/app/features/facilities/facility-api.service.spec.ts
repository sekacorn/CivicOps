import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { FacilityApiService } from "./facility-api.service";

describe("FacilityApiService", () => {
  let api: FacilityApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(FacilityApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it("lists facilities with supported filters", () => {
    api
      .facilities("org", {
        active: "true",
        type: "COMMUNITY_CENTER",
        city: "Albany",
        page: 0,
      })
      .subscribe();
    const r = http.expectOne((x) =>
      x.url.endsWith("/organizations/org/facilities"),
    );
    expect(r.request.params.get("type")).toBe("COMMUNITY_CENTER");
    expect(r.request.params.get("city")).toBe("Albany");
  });
  it("manages spaces, hours, and blackouts through their contracts", () => {
    api.createSpace("org", "facility", { name: "Hall" }).subscribe();
    http.expectOne("/api/v1/organizations/org/facilities/facility/spaces");
    api
      .saveHours("org", "facility", [{ dayOfWeek: "MONDAY", closed: true }])
      .subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/facilities/facility/operating-hours",
      ).request.method,
    ).toBe("PUT");
    api.createBlackout("org", { facilityId: "facility" }).subscribe();
    http.expectOne("/api/v1/organizations/org/facility-blackouts");
  });
  it("checks availability before reservation creation", () => {
    api
      .availability(
        "org",
        "space",
        "2026-09-14T13:00:00Z",
        "2026-09-14T14:00:00Z",
      )
      .subscribe();
    const r = http.expectOne((x) =>
      x.url.endsWith("/facility-spaces/space/availability"),
    );
    expect(r.request.params.get("start")).toContain("2026-09-14");
    api.createReservation("org", { facilitySpaceId: "space" }).subscribe();
    http.expectOne("/api/v1/organizations/org/facility-reservations");
  });
  it("uses explicit approval, rejection, cancellation, and completion", () => {
    api.reservationAction("org", "r", "approve").subscribe();
    http.expectOne("/api/v1/organizations/org/facility-reservations/r/approve");
    api.reject("org", "r", "Conflict").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/facility-reservations/r/reject")
        .request.body.reason,
    ).toBe("Conflict");
    api.cancelReservation("org", "r", "No longer needed").subscribe();
    http.expectOne("/api/v1/organizations/org/facility-reservations/r/cancel");
    api.reservationAction("org", "r", "complete").subscribe();
    http.expectOne(
      "/api/v1/organizations/org/facility-reservations/r/complete",
    );
  });
  it("filters reservations by Event and loads backend reports", () => {
    api
      .reservations("org", { eventId: "event", status: "APPROVED", page: 0 })
      .subscribe();
    const r = http.expectOne((x) => x.url.endsWith("/facility-reservations"));
    expect(r.request.params.get("eventId")).toBe("event");
    api.report("org").subscribe();
    http.expectOne("/api/v1/organizations/org/facility-reports/summary");
    api.utilization("org").subscribe();
    http.expectOne("/api/v1/organizations/org/facility-reports/utilization");
  });
  it("loads the authenticated requester's reservations from the private-safe endpoint", () => {
    api.myReservations("org", 2).subscribe();
    const request = http.expectOne(
      (candidate) =>
        candidate.url === "/api/v1/organizations/org/facility-reservations/me",
    );
    expect(request.request.method).toBe("GET");
    expect(request.request.params.get("page")).toBe("2");
  });
});
