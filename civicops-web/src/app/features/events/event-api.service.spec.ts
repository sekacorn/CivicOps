import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { EventApiService } from "./event-api.service";

describe("EventApiService", () => {
  let api: EventApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(EventApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it("lists using supported filters and safe sort", () => {
    api
      .list("org", {
        page: 1,
        status: "REGISTRATION_OPEN",
        type: "FUNDRAISER",
        from: "2026-09-12T09:00",
        to: "2026-09-13T17:00",
      })
      .subscribe();
    const r = http.expectOne(
      (x) => x.url === "/api/v1/organizations/org/events",
    );
    expect(r.request.params.get("status")).toBe("REGISTRATION_OPEN");
    expect(r.request.params.get("type")).toBe("FUNDRAISER");
    expect(r.request.params.get("sort")).toBe("startDateTime,asc");
  });
  it("creates and patches events without generic status mutation", () => {
    const value = {
      name: "Cleanup",
      description: null,
      eventType: "COMMUNITY_OUTREACH" as const,
      location: null,
      startDateTime: "2026-09-12T13:00:00Z",
      endDateTime: "2026-09-12T15:00:00Z",
      capacity: null,
      registrationRequired: true,
      registrationDeadline: null,
      waitlistEnabled: true,
      linkedGrantId: null,
      linkedDonationCampaignId: null,
    };
    api.create("org", value).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/events").request.method,
    ).toBe("POST");
    api.update("org", "event", { name: "Updated" }).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/events/event").request.method,
    ).toBe("PATCH");
  });
  it("uses explicit lifecycle endpoints", () => {
    api.transition("org", "event", "open-registration").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/events/event/open-registration")
        .request.method,
    ).toBe("POST");
  });
  it("supports external and self registration", () => {
    api
      .register("org", "event", {
        attendeeName: "Pat",
        attendeeEmail: "pat@example.org",
        attendeePhone: null,
      })
      .subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/events/event/registrations")
        .request.body.attendeeName,
    ).toBe("Pat");
    api
      .selfRegister("org", "event", {
        attendeeName: null,
        attendeeEmail: null,
        attendeePhone: null,
      })
      .subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/events/event/registrations/me")
        .request.method,
    ).toBe("POST");
  });
  it("uses attendance and authoritative report endpoints", () => {
    api.registrationAction("org", "reg", "check-in").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/event-registrations/reg/check-in",
      ).request.method,
    ).toBe("POST");
    api.report("org", "event").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/events/event/report").request
        .method,
    ).toBe("GET");
  });
});
