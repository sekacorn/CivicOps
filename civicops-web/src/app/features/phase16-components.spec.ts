import { TestBed } from "@angular/core/testing";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { of } from "rxjs";
import { OrganizationContextService } from "../core/organization/organization-context.service";
import { DonationApiService } from "./donations/donation-api.service";
import { DonationsComponent } from "./donations/donations.component";
import { EventApiService } from "./events/event-api.service";
import { EventDetailComponent } from "./events/event-detail.component";
import { EventFormComponent } from "./events/event-form.component";
import { GrantReportingApiService } from "./grant-reporting/grant-reporting-api.service";
import { ReportDetailComponent } from "./grant-reporting/report-detail.component";
import { VolunteerApiService } from "./volunteers/volunteer-api.service";
import { VolunteersComponent } from "./volunteers/volunteers.component";

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

describe("Phase 16.1 feature screens", () => {
  it("loads stored event timestamps as local datetime input values", async () => {
    const storedStart = "2026-09-25T22:00:00Z";
    const storedEnd = "2026-09-26T00:00:00Z";
    const localInput = (value: string) => {
      const date = new Date(value);
      return new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
        .toISOString()
        .slice(0, 16);
    };
    const api = {
      detail: vi.fn(() =>
        of({
          id: "record",
          organizationId: "org",
          name: "Community gathering",
          description: null,
          eventType: "COMMUNITY_OUTREACH",
          location: null,
          startDateTime: storedStart,
          endDateTime: storedEnd,
          capacity: 1,
          status: "DRAFT",
          registrationRequired: true,
          registrationDeadline: null,
          waitlistEnabled: true,
          linkedGrantId: null,
          linkedGrantName: null,
          linkedDonationCampaignId: "campaign",
          linkedDonationCampaignName: "Community campaign",
          createdAt: "",
          updatedAt: "",
          version: 0,
        }),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [EventFormComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: EventApiService, useValue: api },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(EventFormComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.controls.startDateTime.value).toBe(
      localInput(storedStart),
    );
    expect(fixture.componentInstance.form.controls.endDateTime.value).toBe(
      localInput(storedEnd),
    );
  });

  it("renders unlimited Event capacity and an explicit WAITLISTED state", async () => {
    const api = {
      detail: vi.fn(() =>
        of({
          id: "record",
          organizationId: "org",
          name: "Food drive",
          description: null,
          eventType: "FOOD_DISTRIBUTION",
          location: null,
          startDateTime: "2026-09-12T12:00:00Z",
          endDateTime: "2026-09-12T14:00:00Z",
          capacity: null,
          status: "REGISTRATION_OPEN",
          registrationRequired: true,
          registrationDeadline: null,
          waitlistEnabled: true,
          linkedGrantId: null,
          linkedGrantName: null,
          linkedDonationCampaignId: "campaign",
          linkedDonationCampaignName: "Community campaign",
          createdAt: "",
          updatedAt: "",
          version: 0,
        }),
      ),
      report: vi.fn(() =>
        of({
          capacity: null,
          registered: 1,
          waitlisted: 1,
          attended: 0,
          noShow: 0,
          cancelled: 0,
          remainingCapacity: null,
          attendanceRate: 0,
        }),
      ),
      registrations: vi.fn(() =>
        of(
          page([
            {
              id: "reg",
              attendeeName: "Pat",
              status: "WAITLISTED",
              registrationDate: "2026-09-12T10:00:00Z",
            },
          ]),
        ),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [EventDetailComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: EventApiService, useValue: api },
        {
          provide: OrganizationContextService,
          useValue: { hasAnyRole: vi.fn(() => true) },
        },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(EventDetailComponent);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain("Unlimited");
    expect(f.nativeElement.textContent).toContain("WAITLISTED");
    expect(
      f.nativeElement.querySelector('a[href$="/donations/campaigns/campaign"]')
        .textContent,
    ).toContain("Community campaign");
  });

  it("keeps donor contact fields out of the privacy-safe directory", async () => {
    const api = {
      donors: vi.fn(() =>
        of(
          page([
            {
              id: "d",
              donorType: "INDIVIDUAL",
              displayName: "Community supporter",
              anonymous: true,
              communicationOptOut: true,
              createdAt: "",
              updatedAt: "",
            },
          ]),
        ),
      ),
      campaigns: vi.fn(() => of(page([]))),
      donations: vi.fn(() => of(page([]))),
      summary: vi.fn(() =>
        of({
          totalDonations: 0,
          totalAmount: 0,
          averageDonation: 0,
          restrictedAmount: 0,
          unrestrictedAmount: 0,
          activeCampaigns: 0,
          uniqueDonors: 0,
        }),
      ),
      paymentMethods: vi.fn(() => of([])),
    };
    await TestBed.configureTestingModule({
      imports: [DonationsComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: DonationApiService, useValue: api },
        {
          provide: OrganizationContextService,
          useValue: { hasAnyRole: vi.fn(() => false) },
        },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(DonationsComponent);
    f.detectChanges();
    expect(f.nativeElement.textContent).toContain("Community supporter");
    expect(f.nativeElement.textContent).toContain(
      "privacy-safe donor summaries",
    );
    expect(f.nativeElement.querySelector('input[type="email"]')).toBeNull();
  });

  it("hides Volunteer management actions for read-only roles", async () => {
    const api = {
      volunteers: vi.fn(() => of(page([]))),
      opportunities: vi.fn(() => of(page([]))),
      hours: vi.fn(() => of(page([]))),
      report: vi.fn(() =>
        of({
          from: "2026-01-01",
          to: "2026-12-31",
          activeVolunteers: 0,
          approvedHours: 0,
          upcomingShifts: 0,
          openOpportunities: 0,
          openShiftCapacity: 0,
        }),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [VolunteersComponent],
      providers: [
        { provide: ActivatedRoute, useValue: route },
        { provide: VolunteerApiService, useValue: api },
        {
          provide: OrganizationContextService,
          useValue: { hasAnyRole: vi.fn(() => false) },
        },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(VolunteersComponent);
    f.detectChanges();
    expect(f.nativeElement.textContent).not.toContain("New volunteer");
    expect(f.nativeElement.textContent).not.toContain("Pending hours");
    expect(api.hours).not.toHaveBeenCalled();
  });

  it("makes finalized Grant Reports immutable while retaining exports", async () => {
    const api = {
      report: vi.fn(() =>
        of({
          id: "record",
          organizationId: "org",
          grantId: "grant",
          grantName: "Grant",
          templateId: "template",
          templateName: "Annual",
          reportingPeriodStart: "2026-01-01",
          reportingPeriodEnd: "2026-12-31",
          selectedSources: [],
          status: "FINALIZED",
          createdAt: "",
          updatedAt: "",
        }),
      ),
      sections: vi.fn(() => of([])),
      evidence: vi.fn(() => of([])),
      missing: vi.fn(() => of([])),
    };
    await TestBed.configureTestingModule({
      imports: [ReportDetailComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: route },
        { provide: GrantReportingApiService, useValue: api },
        {
          provide: OrganizationContextService,
          useValue: { hasAnyRole: vi.fn(() => true) },
        },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(ReportDetailComponent);
    f.detectChanges();
    expect(f.nativeElement.textContent).not.toContain("Regenerate");
    expect(f.nativeElement.textContent).not.toContain("Add manual evidence");
    expect(f.nativeElement.textContent).toContain("Export JSON");
    expect(f.nativeElement.textContent).toContain("Export Markdown");
  });
});
