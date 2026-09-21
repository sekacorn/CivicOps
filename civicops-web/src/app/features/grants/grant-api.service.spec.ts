import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { GrantApiService } from "./grant-api.service";

describe("GrantApiService", () => {
  let service: GrantApiService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GrantApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it("sends backend pagination, sorting, and supported filters", () => {
    service
      .list("org-1", {
        page: 2,
        size: 20,
        sort: "endDate",
        direction: "desc",
        status: "ACTIVE",
        grantor: "Civic Fund",
        restricted: true,
      })
      .subscribe();
    const request = http.expectOne(
      (candidate) => candidate.url === "/api/v1/organizations/org-1/grants",
    );
    expect(request.request.params.get("page")).toBe("2");
    expect(request.request.params.get("sort")).toBe("endDate,desc");
    expect(request.request.params.get("status")).toBe("ACTIVE");
    expect(request.request.params.get("grantor")).toBe("Civic Fund");
    request.flush({
      content: [],
      page: 2,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: false,
      last: true,
    });
  });

  it("creates and safely edits through the documented endpoints", () => {
    const grant = {
      grantName: "Neighborhood Fund",
      grantorName: "City",
      awardAmount: 1000,
      restricted: false,
    };
    service.create("org-1", grant).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org-1/grants").request.method,
    ).toBe("POST");
    service.update("org-1", "grant-1", grant).subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org-1/grants/grant-1").request
        .method,
    ).toBe("PATCH");
  });

  it("uses explicit lifecycle action endpoints", () => {
    service.transition("org-1", "grant-1", "start-application").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org-1/grants/grant-1/start-application",
      ).request.method,
    ).toBe("POST");
  });

  it("records expenses and reloadable authoritative financial data from separate endpoints", () => {
    service
      .addExpense("org-1", "grant-1", {
        amount: 25,
        expenseDate: "2026-09-10",
        category: "SUPPLIES",
        description: "Paper",
      })
      .subscribe();
    expect(
      http.expectOne("/api/v1/organizations/org-1/grants/grant-1/expenses")
        .request.method,
    ).toBe("POST");
    service.financialSummary("org-1", "grant-1").subscribe();
    expect(
      http.expectOne(
        "/api/v1/organizations/org-1/grants/grant-1/financial-summary",
      ).request.method,
    ).toBe("GET");
  });
});
