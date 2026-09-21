import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { GrantReportingApiService } from "./grant-reporting-api.service";
import { ReportDetailComponent } from "./report-detail.component";

describe("GrantReportingApiService", () => {
  let api: GrantReportingApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(GrantReportingApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it("creates templates and sections", () => {
    api
      .createTemplate("org", {
        name: "Annual",
        description: null,
        grantId: "grant",
      })
      .subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/grant-report-templates").request
        .method,
    ).toBe("POST");
    api
      .addTemplateSection("org", "template", {
        sectionKey: "outcomes",
        title: "Outcomes",
        instructions: null,
        sequenceNumber: 1,
        sectionType: "OUTCOMES",
        required: true,
        maxLength: 5000,
      })
      .subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/grant-report-templates/template/sections",
      ).request.method,
    ).toBe("POST");
  });
  it("creates and generates reports through explicit actions", () => {
    api
      .createReport("org", "grant", {
        templateId: "template",
        reportingPeriodStart: "2026-01-01",
        reportingPeriodEnd: "2026-06-30",
        selectedSources: ["GRANT", "VOLUNTEERS"],
      })
      .subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/grants/grant/reports").request
        .method,
    ).toBe("POST");
    api.action("org", "report", "generate").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/grant-reports/report/generate")
        .request.method,
    ).toBe("POST");
  });
  it("edits, approves, and finalizes through domain endpoints", () => {
    api.editSection("org", "section", "Reviewed").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/grant-report-sections/section")
        .request.method,
    ).toBe("PATCH");
    api.approveSection("org", "section").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org/grant-report-sections/section/approve",
      ).request.method,
    ).toBe("POST");
    api.action("org", "report", "finalize").subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org/grant-reports/report/finalize")
        .request.method,
    ).toBe("POST");
  });
  it("exports JSON and Markdown as blobs", () => {
    api.export("org", "report", "markdown").subscribe();
    const r = http.expectOne((x) =>
      x.url.endsWith("/grant-reports/report/export"),
    );
    expect(r.request.responseType).toBe("blob");
    expect(r.request.params.get("format")).toBe("markdown");
  });
});

describe("Grant reporting evidence semantics", () => {
  const component = Object.create(
    ReportDetailComponent.prototype,
  ) as ReportDetailComponent;
  it("never renders missing evidence as numeric zero", () => {
    expect(component.evidenceValue({ valueState: "MISSING" } as never)).toBe(
      "Missing",
    );
  });
  it("distinguishes not-applicable evidence", () => {
    expect(
      component.evidenceValue({ valueState: "NOT_APPLICABLE" } as never),
    ).toBe("Not applicable");
  });
  it("preserves a verified numeric zero", () => {
    expect(
      component.evidenceValue({
        valueState: "VERIFIED",
        numericValue: 0,
        monetaryValue: null,
        textValue: null,
      } as never),
    ).toBe(0);
  });
});
