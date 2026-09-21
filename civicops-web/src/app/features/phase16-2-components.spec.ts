import { HttpErrorResponse } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { of, throwError } from "rxjs";
import { OrganizationContextService } from "../core/organization/organization-context.service";
import { AuthService } from "../core/auth/auth.service";
import { CaseApiService } from "./cases/case-api.service";
import { CaseDetailComponent } from "./cases/case-detail.component";
import { CaseListComponent } from "./cases/case-list.component";
import { ClientDetailComponent } from "./cases/client-detail.component";
import { EquipmentApiService } from "./equipment/equipment-api.service";
import { EquipmentDetailComponent } from "./equipment/equipment-detail.component";
import { FacilityApiService } from "./facilities/facility-api.service";
import { FacilityListComponent } from "./facilities/facility-list.component";

const page = <T>(content: T[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  first: true,
  last: true,
});
const route = {
  snapshot: {
    paramMap: {
      get: (name: string) => (name === "organizationId" ? "org" : "record"),
    },
  },
};
const role = (allowed: boolean) => ({ hasAnyRole: vi.fn(() => allowed) });

describe("Phase 16.2 feature screens", () => {
  it("keeps PROGRAM_MANAGER on PII-free Case reporting without private requests", async () => {
    const api = {
      report: vi.fn(() =>
        of({
          activeClients: 3,
          openCases: 1,
          inProgressCases: 1,
          onHoldCases: 0,
          casesClosedInPeriod: 1,
          overdueTasks: 0,
        }),
      ),
      serviceReport: vi.fn(() => of([])),
      cases: vi.fn(),
      clients: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [CaseListComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: CaseApiService, useValue: api },
        {
          provide: OrganizationContextService,
          useValue: {
            hasAnyRole: vi.fn((...roles: string[]) =>
              roles.includes("PROGRAM_MANAGER"),
            ),
          },
        },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(CaseListComponent);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain("PII-free Case reporting");
    expect(api.report).toHaveBeenCalled();
    expect(api.cases).not.toHaveBeenCalled();
    expect(api.clients).not.toHaveBeenCalled();
    expect(f.nativeElement.textContent).not.toContain("Client directory");
  });

  it("shows assigned Case work with lifecycle, notes, tasks, and service records", async () => {
    const record = {
      id: "record",
      organizationId: "org",
      caseNumber: "C-1",
      title: "Housing support",
      description: null,
      caseType: "HOUSING",
      priority: "HIGH",
      status: "IN_PROGRESS",
      openedDate: "2026-09-01",
      closedDate: null,
      client: {
        id: "client",
        displayName: "Avery Client",
        active: true,
        externalReferenceNumber: "SAFE-1",
        createdAt: "",
      },
      assignedUserId: "worker",
      assignedUserDisplayName: "Case Worker",
      programName: null,
      intakeSource: null,
      createdByUserId: "admin",
      createdAt: "",
      updatedAt: "",
      version: 0,
    };
    const api = {
      case: vi.fn(() => of(record)),
      notes: vi.fn(() =>
        of(
          page([
            {
              id: "n",
              caseId: "record",
              authorUserId: "worker",
              authorDisplayName: "Case Worker",
              noteType: "PROGRESS",
              content: "Stable housing secured",
              privateNote: true,
              createdAt: "2026-09-14T12:00:00Z",
            },
          ]),
        ),
      ),
      tasks: vi.fn(() =>
        of(
          page([
            {
              id: "t",
              caseId: "record",
              title: "Follow up",
              description: null,
              assignedUserId: "worker",
              dueDate: "2026-09-20",
              status: "OPEN",
              priority: "NORMAL",
              overdue: false,
              completedAt: null,
              createdAt: "",
              updatedAt: "",
            },
          ]),
        ),
      ),
      services: vi.fn(() =>
        of(
          page([
            {
              id: "s",
              caseId: "record",
              serviceType: "HOUSING_ASSISTANCE",
              description: "Placement",
              serviceDate: "2026-09-14",
              quantity: 1,
              unit: "referral",
              valueAmount: 0,
              providedByUserId: "worker",
              notes: null,
              createdAt: "",
            },
          ]),
        ),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [CaseDetailComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: CaseApiService, useValue: api },
        { provide: OrganizationContextService, useValue: role(true) },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(CaseDetailComponent);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain("Place on hold");
    expect(f.nativeElement.textContent).toContain("Stable housing secured");
    expect(f.nativeElement.textContent).toContain("Follow up");
    expect(f.nativeElement.textContent).toContain("HOUSING ASSISTANCE");
  });

  it("renders an explicit forbidden Client-detail response", async () => {
    const api = {
      client: vi.fn(() =>
        throwError(
          () =>
            new HttpErrorResponse({
              status: 403,
              error: {
                timestamp: "",
                status: 403,
                code: "ACCESS_DENIED",
                message: "Case access is limited to the assigned worker",
                path: "",
                fieldErrors: {},
              },
            }),
        ),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [ClientDetailComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: CaseApiService, useValue: api },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(ClientDetailComponent);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain(
      "Case access is limited to the assigned worker",
    );
  });

  it("does not request borrower or maintenance history for read-only inventory roles", async () => {
    const api = {
      asset: vi.fn(() =>
        of({
          id: "record",
          organizationId: "org",
          assetTag: "EQ-1",
          name: "Projector",
          description: null,
          categoryId: null,
          categoryName: null,
          manufacturer: null,
          model: null,
          serialNumber: null,
          purchaseDate: null,
          purchaseValue: null,
          condition: "GOOD",
          status: "AVAILABLE",
          location: "Hall",
          notes: null,
          createdAt: "",
          updatedAt: "",
          version: 0,
        }),
      ),
      checkouts: vi.fn(),
      maintenance: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [EquipmentDetailComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: EquipmentApiService, useValue: api },
        { provide: OrganizationContextService, useValue: role(false) },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(EquipmentDetailComponent);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain("Projector");
    expect(f.nativeElement.textContent).not.toContain("Check out");
    expect(api.checkouts).not.toHaveBeenCalled();
    expect(api.maintenance).not.toHaveBeenCalled();
  });

  it("keeps Facility private controls hidden for VIEWER", async () => {
    const facility = {
      id: "facility",
      organizationId: "org",
      name: "Community Hall",
      description: null,
      facilityType: "COMMUNITY_CENTER",
      addressLine1: null,
      addressLine2: null,
      city: "Albany",
      state: "NY",
      postalCode: null,
      country: "US",
      active: true,
      timezone: "America/New_York",
      notes: null,
      createdAt: "",
      updatedAt: "",
      version: 0,
    };
    const api = {
      facilities: vi.fn(() => of(page([facility]))),
      reservations: vi.fn(() =>
        of(
          page([
            {
              id: "r",
              facilityId: "facility",
              facilityName: "Community Hall",
              spaceId: "space",
              spaceName: "Room",
              title: "Private request",
              startDateTime: "2026-09-20T13:00:00Z",
              endDateTime: "2026-09-20T14:00:00Z",
              expectedAttendance: 10,
              status: "PENDING",
              requestedAt: "",
              eventId: null,
            },
          ]),
        ),
      ),
      report: vi.fn(() =>
        of({
          activeFacilities: 1,
          reservableSpaces: 1,
          pendingReservations: 1,
          approvedReservations: 0,
          reservationsInPeriod: 1,
          cancelledReservations: 0,
        }),
      ),
      utilization: vi.fn(() =>
        of({
          reservationCount: 1,
          reservedHours: 1,
          approvals: 0,
          rejections: 0,
          cancellations: 0,
          mostUsedSpaces: [],
        }),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [FacilityListComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: FacilityApiService, useValue: api },
        { provide: OrganizationContextService, useValue: role(false) },
        { provide: AuthService, useValue: { currentUser: vi.fn(() => null) } },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(FacilityListComponent);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain("Community Hall");
    expect(f.nativeElement.textContent).not.toContain("New facility");
    expect(f.nativeElement.textContent).not.toContain("Request reservation");
    expect(
      f.nativeElement.querySelector('a[href$="/reservations/r"]'),
    ).toBeNull();
    expect(f.nativeElement.textContent).not.toContain("requesterEmail");
  });
});
