import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute, Router, provideRouter } from "@angular/router";
import { of } from "rxjs";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { GrantApiService } from "./grant-api.service";
import { GrantDetailComponent } from "./grant-detail.component";
import { GrantFormComponent } from "./grant-form.component";
import { GrantListComponent } from "./grant-list.component";
import { GrantReportingApiService } from "../grant-reporting/grant-reporting-api.service";

const page = {
  content: [
    {
      id: "grant-1",
      grantName: "Neighborhood Fund",
      grantorName: "City Foundation",
      grantNumber: "GF-1",
      awardAmount: 5000,
      startDate: "2026-01-01",
      endDate: "2026-12-31",
      reportingDeadline: "2027-01-31",
      status: "ACTIVE",
      restricted: false,
      createdAt: "",
      updatedAt: "",
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
};
const detail = {
  ...page.content[0],
  organizationId: "org-1",
  description: "Community programming",
  applicationDeadline: null,
  submittedDate: null,
  awardDate: null,
  restrictionDescription: null,
  primaryContactName: null,
  primaryContactEmail: null,
  notes: null,
  createdByUserId: "user-1",
};
const financial = {
  grantId: "grant-1",
  awardAmount: 5000,
  totalSpent: 1000,
  remainingBalance: 4000,
  utilizationPercent: 20,
};
const context = {
  hasAnyRole: vi.fn(() => true),
  selectedOrganizationId: () => "org-1",
};

describe("GrantListComponent", () => {
  let fixture: ComponentFixture<GrantListComponent>;
  const api = { list: vi.fn(() => of(page)) };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [GrantListComponent],
      providers: [
        provideRouter([]),
        { provide: GrantApiService, useValue: api },
        {
          provide: GrantReportingApiService,
          useValue: { reports: vi.fn(() => of({ ...page, content: [] })) },
        },
        { provide: OrganizationContextService, useValue: context },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => "org-1" } } },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(GrantListComponent);
    fixture.detectChanges();
  });

  it("renders backend grant list data and pagination metadata", () => {
    expect(fixture.nativeElement.textContent).toContain("Neighborhood Fund");
    expect(fixture.nativeElement.textContent).toContain("Page 1 of 1");
  });

  it("sends filters and sorting back through the API service", () => {
    fixture.componentInstance.filters.patchValue({
      grantor: "City",
      status: "ACTIVE",
    });
    fixture.componentInstance.applyFilters();
    fixture.componentInstance.sort("endDate");
    expect(api.list).toHaveBeenLastCalledWith(
      "org-1",
      expect.objectContaining({
        grantor: "City",
        status: "ACTIVE",
        sort: "endDate",
      }),
    );
  });

  it("hides create actions when the organization role cannot manage grants", () => {
    fixture.destroy();
    context.hasAnyRole.mockReturnValue(false);
    const readOnlyFixture = TestBed.createComponent(GrantListComponent);
    readOnlyFixture.detectChanges();
    expect(
      readOnlyFixture.nativeElement.querySelector('a[href$="/new"]'),
    ).toBeNull();
  });
});

describe("GrantFormComponent", () => {
  it("creates a valid grant and navigates to backend-returned detail", async () => {
    const api = {
      create: vi.fn(() => of(detail)),
      detail: vi.fn(),
      update: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [GrantFormComponent],
      providers: [
        provideRouter([]),
        { provide: GrantApiService, useValue: api },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (name: string) =>
                  name === "organizationId" ? "org-1" : null,
              },
            },
          },
        },
      ],
    }).compileComponents();
    const router = TestBed.inject(Router);
    vi.spyOn(router, "navigate").mockResolvedValue(true);
    const component =
      TestBed.createComponent(GrantFormComponent).componentInstance;
    component.ngOnInit();
    component.form.patchValue({
      grantName: "Neighborhood Fund",
      grantorName: "City Foundation",
      awardAmount: 5000,
    });
    component.save();
    expect(api.create).toHaveBeenCalledWith(
      "org-1",
      expect.objectContaining({
        grantName: "Neighborhood Fund",
        awardAmount: 5000,
      }),
    );
    expect(router.navigate).toHaveBeenCalledWith([
      "/organizations",
      "org-1",
      "grants",
      "grant-1",
    ]);
  });

  it("does not submit an invalid grant form", async () => {
    const api = { create: vi.fn(), detail: vi.fn(), update: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [GrantFormComponent],
      providers: [
        provideRouter([]),
        { provide: GrantApiService, useValue: api },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (name: string) =>
                  name === "organizationId" ? "org-1" : null,
              },
            },
          },
        },
      ],
    }).compileComponents();
    const component =
      TestBed.createComponent(GrantFormComponent).componentInstance;
    component.ngOnInit();
    component.save();
    expect(api.create).not.toHaveBeenCalled();
  });
});

describe("GrantDetailComponent", () => {
  it("renders authoritative financial values and valid lifecycle actions", async () => {
    const api = {
      detail: vi.fn(() => of(detail)),
      financialSummary: vi.fn(() => of(financial)),
      expenses: vi.fn(() => of({ ...page, content: [] })),
    };
    await TestBed.configureTestingModule({
      imports: [GrantDetailComponent],
      providers: [
        provideRouter([]),
        { provide: GrantApiService, useValue: api },
        {
          provide: GrantReportingApiService,
          useValue: { reports: vi.fn(() => of({ ...page, content: [] })) },
        },
        { provide: OrganizationContextService, useValue: context },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (name: string) =>
                  name === "organizationId" ? "org-1" : "grant-1",
              },
            },
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(GrantDetailComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("$4,000.00");
    expect(fixture.componentInstance.actions("ACTIVE")).toEqual([
      { label: "Close grant", endpoint: "close", terminal: true },
    ]);
    expect(fixture.componentInstance.actions("CLOSED")).toEqual([]);
  });
});
