import { TestBed } from "@angular/core/testing";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { of } from "rxjs";
import { routes } from "../app.routes";
import { OrganizationContextService } from "../core/organization/organization-context.service";
import { BoardApiService } from "./board/board-api.service";
import { BoardMeetingDetailComponent } from "./board/board-meeting-detail.component";
import { FoodPantryApiService } from "./food-pantry/food-pantry-api.service";
import { FoodPantryListComponent } from "./food-pantry/food-pantry-list.component";
import { ScholarshipApiService } from "./scholarships/scholarship-api.service";
import { ScholarshipApplicationDetailComponent } from "./scholarships/scholarship-application-detail.component";

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
      get: (name: string) => {
        const values: Record<string, string> = {
          organizationId: "org",
          applicationId: "application",
          meetingId: "meeting",
        };
        return values[name] ?? "record";
      },
    },
  },
};

describe("Phase 16.3 Angular integration", () => {
  it("registers Scholarship, Food Pantry, and Board as lazy organization-scoped routes", () => {
    const shell = routes.find((candidate) => candidate.path === "");
    const children = shell?.children ?? [];
    for (const path of [
      "organizations/:organizationId/scholarships",
      "organizations/:organizationId/food-pantry",
      "organizations/:organizationId/board",
    ]) {
      const match = children.find((candidate) => candidate.path === path);
      expect(match).toBeTruthy();
      expect(match?.children?.[0].loadComponent).toBeTypeOf("function");
    }
  });

  it("hides scholarship applicant contact details from non-manager rendering", async () => {
    const api = {
      application: vi.fn(() =>
        of({
          id: "application",
          organizationId: "org",
          programId: "program",
          programName: "Future Leaders",
          status: "SUBMITTED",
          eligibilityConfirmed: true,
          eligibilityNotes: null,
          personalStatement: "Service matters",
          financialNeedStatement: null,
          gpa: 3.8,
          householdIncome: null,
          requestedAmount: 1000,
          submittedAt: "2026-09-01",
          createdAt: "",
          updatedAt: "",
          version: 0,
          applicant: {
            id: "applicant",
            organizationId: "org",
            userId: null,
            firstName: "Avery",
            lastName: "Applicant",
            preferredName: null,
            displayName: "Avery Applicant",
            email: "avery.private@example.test",
            phone: "555-0100",
            dateOfBirth: null,
            address: null,
            studentId: null,
            schoolName: "Central High",
            graduationYear: 2027,
            notes: null,
            createdAt: "",
            updatedAt: "",
            version: 0,
          },
        }),
      ),
      assignments: vi.fn(() => of(page([]))),
      documents: vi.fn(() => of(page([]))),
      reviews: vi.fn(() => of(page([]))),
    };
    await TestBed.configureTestingModule({
      imports: [ScholarshipApplicationDetailComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: ScholarshipApiService, useValue: api },
        {
          provide: OrganizationContextService,
          useValue: { hasAnyRole: vi.fn(() => false) },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(
      ScholarshipApplicationDetailComponent,
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("Restricted");
    expect(fixture.nativeElement.textContent).not.toContain(
      "avery.private@example.test",
    );
    expect(fixture.nativeElement.textContent).not.toContain("555-0100");
    expect(fixture.nativeElement.textContent).not.toContain("Assign reviewer");
  });

  it("keeps Food Pantry household directory hidden for inventory read roles", async () => {
    const api = {
      pantries: vi.fn(() =>
        of(
          page([
            {
              id: "pantry",
              name: "North Pantry",
              city: "Albany",
              state: "NY",
              active: true,
              timezone: "America/New_York",
            },
          ]),
        ),
      ),
      items: vi.fn(() => of(page([]))),
      households: vi.fn(() =>
        of(
          page([
            {
              id: "household",
              displayName: "Private Household",
              householdSize: 4,
              active: true,
              createdAt: "",
            },
          ]),
        ),
      ),
      inventoryReport: vi.fn(() =>
        of({
          organizationId: "org",
          pantryId: "pantry",
          totalItemTypes: 1,
          totalAvailableQuantity: 10,
          lowStockItemCount: 0,
          expiredLotCount: 0,
          expiringSoonLotCount: 0,
        }),
      ),
      distributionReport: vi.fn(() =>
        of({
          organizationId: "org",
          pantryId: "pantry",
          from: "",
          to: "",
          visitsCompleted: 1,
          householdsServed: 1,
          uniqueHouseholdsServed: 1,
          totalQuantityDistributed: 3,
          averageHouseholdSize: 4,
          quantityByCategory: [],
        }),
      ),
      householdReport: vi.fn(() =>
        of({
          organizationId: "org",
          from: "",
          to: "",
          activeHouseholds: 1,
          householdVisits: 1,
          householdsServedOnce: 1,
          householdsServedMultipleTimes: 0,
        }),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [FoodPantryListComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: FoodPantryApiService, useValue: api },
        {
          provide: OrganizationContextService,
          useValue: { hasAnyRole: vi.fn(() => false) },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(FoodPantryListComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("North Pantry");
    expect(fixture.nativeElement.textContent).not.toContain(
      "Private Household",
    );
    expect(fixture.nativeElement.textContent).not.toContain("New household");
  });

  it("shows Board self-attendance only for BOARD_MEMBER role", async () => {
    const api = {
      meeting: vi.fn(() =>
        of({
          id: "meeting",
          organizationId: "org",
          title: "September board meeting",
          meetingType: "REGULAR",
          startDateTime: "2026-09-19T13:00:00Z",
          endDateTime: "2026-09-19T14:00:00Z",
          location: null,
          virtualMeetingUrl: null,
          status: "PUBLISHED",
          quorumRequired: 2,
          createdByUserId: "user",
          createdAt: "",
          updatedAt: "",
          version: 0,
        }),
      ),
      quorum: vi.fn(() =>
        of({
          meetingId: "meeting",
          quorumRequired: 2,
          presentOrRemote: 1,
          quorumMet: false,
          attendanceRate: 50,
        }),
      ),
      agenda: vi.fn(() => of(page([]))),
      motions: vi.fn(() => of(page([]))),
      attendance: vi.fn(() => of(page([]))),
      minutes: vi.fn(() =>
        of({
          id: "minutes",
          meetingId: "meeting",
          content: "",
          status: "DRAFT",
        }),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [BoardMeetingDetailComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: BoardApiService, useValue: api },
        {
          provide: OrganizationContextService,
          useValue: {
            hasAnyRole: vi.fn(() => false),
            selectedRole: vi.fn(() => "BOARD_MEMBER"),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(BoardMeetingDetailComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain("My attendance");
    expect(fixture.nativeElement.textContent).not.toContain("Publish");
    expect(fixture.nativeElement.textContent).not.toContain("Add agenda item");
  });
});
