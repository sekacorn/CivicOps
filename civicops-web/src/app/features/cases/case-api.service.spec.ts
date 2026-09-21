import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { CaseApiService } from "./case-api.service";

describe("CaseApiService", () => {
  let api: CaseApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CaseApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it("uses the privacy-safe client directory contract", () => {
    api.clients("org", 2, "Avery").subscribe();
    const r = http.expectOne(
      (x) => x.url === "/api/v1/organizations/org/clients",
    );
    expect(r.request.params.get("name")).toBe("Avery");
    expect(r.request.params.get("page")).toBe("2");
  });
  it("lists Cases with only supported filters", () => {
    api
      .cases("org", {
        status: "OPEN",
        priority: "URGENT",
        caseType: "HOUSING",
        openedFrom: "2026-01-01",
        openedTo: "2026-12-31",
        page: 1,
      })
      .subscribe();
    const r = http.expectOne((x) => x.url.endsWith("/organizations/org/cases"));
    expect(r.request.params.get("status")).toBe("OPEN");
    expect(r.request.params.get("sort")).toBe("openedDate,desc");
  });
  it("uses explicit assignment and lifecycle actions", () => {
    api.assign("org", "case", "worker").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/cases/case/assign").request
        .body,
    ).toEqual({ userId: "worker" });
    api.transition("org", "case", "close").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/cases/case/close").request
        .method,
    ).toBe("POST");
  });
  it("appends notes, tasks, and service records", () => {
    api.addNote("org", "case", { content: "Progress" }).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/cases/case/notes").request
        .method,
    ).toBe("POST");
    api.addTask("org", "case", { title: "Call" }).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/cases/case/tasks").request
        .method,
    ).toBe("POST");
    api.addService("org", "case", { serviceType: "OTHER" }).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/cases/case/services").request
        .method,
    ).toBe("POST");
  });
  it("loads PII-free aggregate reporting", () => {
    api.report("org", "2026-01-01", "2026-12-31").subscribe();
    const r = http.expectOne((x) => x.url.endsWith("/case-reports/summary"));
    expect(r.request.params.get("from")).toBe("2026-01-01");
    api.serviceReport("org").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/case-reports/services").request
        .method,
    ).toBe("GET");
  });
});
