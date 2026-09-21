import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { AuthService } from "../../core/auth/auth.service";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  Facility,
  FacilityReportSummary,
  FacilitySpace,
  FacilityType,
  FacilityUtilizationReport,
  ReservationStatus,
  ReservationSummary,
} from "../../core/models/facility.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { FacilityApiService } from "./facility-api.service";
import { facilityLocalToInstant } from "./facility-time";

@Component({
  selector: "cop-facility-list",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
    PaginationComponent,
    StatusBadgeComponent,
  ],
  template: `
    <header class="page-header">
      <div>
        <p class="eyebrow">FACILITIES</p>
        <h1>Facilities and reservations</h1>
        <p>
          Facility-zone scheduling, availability, approvals, and utilization.
        </p>
      </div>
      @if (canManage()) {
        <button
          class="button button--primary"
          (click)="showFacility.set(!showFacility())"
        >
          New facility
        </button>
      }
    </header>
    <cop-api-error [error]="error()" />
    @if (report(); as r) {
      <section class="metrics" aria-label="Facility report">
        <article>
          <span>Active facilities</span
          ><strong>{{ r.activeFacilities }}</strong>
        </article>
        <article>
          <span>Reservable spaces</span
          ><strong>{{ r.reservableSpaces }}</strong>
        </article>
        <article>
          <span>Pending</span><strong>{{ r.pendingReservations }}</strong>
        </article>
        <article>
          <span>Approved</span><strong>{{ r.approvedReservations }}</strong>
        </article>
        <article>
          <span>In period</span><strong>{{ r.reservationsInPeriod }}</strong>
        </article>
        <article>
          <span>Cancelled</span><strong>{{ r.cancelledReservations }}</strong>
        </article>
      </section>
    }
    @if (canManage() && showFacility()) {
      <form
        class="panel form-grid"
        [formGroup]="facilityForm"
        (ngSubmit)="createFacility()"
      >
        <h2>Create facility</h2>
        <label>Name<input formControlName="name" /></label
        ><label
          >Type<select formControlName="facilityType">
            @for (t of facilityTypes; track t) {
              <option [value]="t">{{ label(t) }}</option>
            }
          </select></label
        ><label
          >IANA timezone<input
            formControlName="timezone"
            placeholder="America/New_York" /></label
        ><label>Address<input formControlName="addressLine1" /></label
        ><label>City<input formControlName="city" /></label
        ><label>State<input formControlName="state" /></label
        ><label class="wide"
          >Description<textarea formControlName="description"></textarea></label
        ><button
          class="button button--primary"
          [disabled]="facilityForm.invalid"
        >
          Create facility
        </button>
      </form>
    }
    <form class="filters" [formGroup]="filters" (ngSubmit)="loadFacilities(0)">
      <label
        >Status<select formControlName="active">
          <option value="">All</option>
          <option value="true">Active</option>
          <option value="false">Inactive</option>
        </select></label
      ><label
        >Type<select formControlName="type">
          <option value="">All</option>
          @for (t of facilityTypes; track t) {
            <option [value]="t">{{ label(t) }}</option>
          }
        </select></label
      ><label>City<input formControlName="city" /></label
      ><button class="button button--secondary">Apply</button>
    </form>
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (facilities()?.content?.length === 0) {
      <cop-async-state
        state="empty"
        detail="No facilities match these filters."
      />
    } @else if (facilities()) {
      @let p = facilities()!;
      <section class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Facility</th>
              <th>Type</th>
              <th>Location</th>
              <th>Timezone</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            @for (f of p.content; track f.id) {
              <tr>
                <th scope="row">
                  <a [routerLink]="[f.id]">{{ f.name }}</a>
                </th>
                <td>{{ label(f.facilityType) }}</td>
                <td>{{ location(f) }}</td>
                <td>{{ f.timezone }}</td>
                <td>
                  <cop-status-badge
                    [status]="f.active ? 'ACTIVE' : 'INACTIVE'"
                  />
                </td>
              </tr>
            }
          </tbody>
        </table>
        <cop-pagination
          [page]="p.page"
          [totalPages]="p.totalPages"
          [totalElements]="p.totalElements"
          [first]="p.first"
          [last]="p.last"
          (pageChange)="loadFacilities($event)"
        />
      </section>
    }
    @if (canRequest()) {
      <div class="actions">
        <button
          class="button button--primary"
          (click)="showReservation.set(!showReservation())"
        >
          Request reservation
        </button>
      </div>
      @if (showReservation()) {
        <form
          class="panel form-grid"
          [formGroup]="reservationForm"
          (ngSubmit)="createReservation()"
        >
          <h2>Request reservation</h2>
          <p class="wide timezone-note">
            Times are interpreted in the selected facility’s IANA timezone, not
            the browser timezone.
          </p>
          <label
            >Facility<select
              formControlName="facilityId"
              (change)="loadSpaces()"
            >
              <option value="">Select facility</option>
              @for (f of facilities()?.content ?? []; track f.id) {
                <option [value]="f.id">{{ f.name }} · {{ f.timezone }}</option>
              }
            </select></label
          ><label
            >Space<select formControlName="facilitySpaceId">
              <option value="">Select space</option>
              @for (s of spaces(); track s.id) {
                <option [value]="s.id">
                  {{ s.name }} · capacity {{ s.capacity ?? "not limited" }}
                </option>
              }
            </select></label
          ><label>Title<input formControlName="title" /></label
          ><label
            >Facility-local start<input
              type="datetime-local"
              formControlName="startDateTime" /></label
          ><label
            >Facility-local end<input
              type="datetime-local"
              formControlName="endDateTime" /></label
          ><label
            >Expected attendance<input
              type="number"
              min="1"
              formControlName="expectedAttendance" /></label
          ><label>Event ID (optional)<input formControlName="eventId" /></label
          ><label class="wide"
            >Purpose<textarea formControlName="purpose"></textarea></label
          ><button
            class="button button--primary"
            [disabled]="reservationForm.invalid"
          >
            Check availability and request
          </button>
        </form>
      }
    }
    @if (canRequest() && !canManage()) {
      <section class="panel reservations">
        <h2>My reservations</h2>
        @if ((myReservations()?.content?.length ?? 0) === 0) {
          <p class="empty-state">You have no reservation requests.</p>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Reservation</th>
                  <th>Facility / space</th>
                  <th>Starts</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                @for (r of myReservations()?.content ?? []; track r.id) {
                  <tr>
                    <th scope="row">
                      <a [routerLink]="['reservations', r.id]">{{ r.title }}</a>
                    </th>
                    <td>{{ r.facilityName }} / {{ r.spaceName }}</td>
                    <td>{{ local(r.startDateTime) }}</td>
                    <td><cop-status-badge [status]="r.status" /></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    }
    <section class="panel reservations">
      <h2>Reservations</h2>
      <form
        class="reservation-filter"
        [formGroup]="reservationFilters"
        (ngSubmit)="loadReservations(0)"
      >
        <label
          >Status<select formControlName="status">
            <option value="">All</option>
            @for (s of reservationStatuses; track s) {
              <option [value]="s">{{ s }}</option>
            }
          </select></label
        ><button class="button button--secondary">Apply</button>
      </form>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Reservation</th>
              <th>Facility / space</th>
              <th>Starts</th>
              <th>Status</th>
              <th>Event</th>
            </tr>
          </thead>
          <tbody>
            @for (r of reservations()?.content ?? []; track r.id) {
              <tr>
                <th scope="row">
                  @if (canManage()) {
                    <a [routerLink]="['reservations', r.id]">{{ r.title }}</a>
                  } @else {
                    {{ r.title }}
                  }
                </th>
                <td>{{ r.facilityName }} / {{ r.spaceName }}</td>
                <td>{{ local(r.startDateTime) }}</td>
                <td><cop-status-badge [status]="r.status" /></td>
                <td>
                  @if (r.eventId) {
                    <a
                      [routerLink]="[
                        '/organizations',
                        organizationId,
                        'events',
                        r.eventId,
                      ]"
                      >Open Event</a
                    >
                  } @else {
                    —
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </section>
    @if (utilization(); as u) {
      <section class="panel utilization">
        <h2>Utilization</h2>
        <p>
          <strong>{{ u.reservationCount }}</strong> reservations ·
          <strong>{{ u.reservedHours }}</strong> reserved hours ·
          {{ u.approvals }} approvals · {{ u.rejections }} rejections
        </p>
      </section>
    }
  `,
  styles: `
    .metrics {
      display: grid;
      grid-template-columns: repeat(6, 1fr);
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .metrics article {
      padding: 1rem;
      background: white;
      border: 1px solid var(--line);
    }
    .metrics span,
    .timezone-note {
      display: block;
      color: var(--ink-muted);
    }
    .metrics strong {
      font-size: 1.3rem;
    }
    .panel {
      margin-bottom: 1rem;
    }
    .form-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
    }
    .form-grid h2,
    .wide {
      grid-column: 1/-1;
    }
    .form-grid label,
    .filters label,
    .reservation-filter label {
      display: grid;
      gap: 0.3rem;
    }
    .filters {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr auto;
      gap: 0.75rem;
      align-items: end;
      margin-bottom: 1rem;
    }
    .actions {
      margin: 1rem 0;
    }
    .reservations,
    .utilization {
      margin-top: 1rem;
    }
    .reservation-filter {
      display: flex;
      gap: 0.75rem;
      align-items: end;
      margin-bottom: 0.75rem;
    }
    @media (max-width: 900px) {
      .metrics {
        grid-template-columns: repeat(3, 1fr);
      }
      .form-grid {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 560px) {
      .metrics,
      .form-grid,
      .filters {
        grid-template-columns: 1fr;
      }
      .form-grid h2,
      .wide {
        grid-column: auto;
      }
    }
  `,
})
export class FacilityListComponent implements OnInit {
  private readonly api = inject(FacilityApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  private readonly auth = inject(AuthService);
  readonly facilities = signal<PageResponse<Facility> | null>(null);
  readonly spaces = signal<FacilitySpace[]>([]);
  readonly reservations = signal<PageResponse<ReservationSummary> | null>(null);
  readonly myReservations = signal<PageResponse<ReservationSummary> | null>(
    null,
  );
  readonly report = signal<FacilityReportSummary | null>(null);
  readonly utilization = signal<FacilityUtilizationReport | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  readonly showFacility = signal(false);
  readonly showReservation = signal(false);
  organizationId = "";
  readonly facilityTypes: FacilityType[] = [
    "COMMUNITY_CENTER",
    "OFFICE",
    "CHURCH",
    "SCHOOL",
    "WAREHOUSE",
    "OUTDOOR_SITE",
    "OTHER",
  ];
  readonly reservationStatuses: ReservationStatus[] = [
    "PENDING",
    "APPROVED",
    "REJECTED",
    "CANCELLED",
    "COMPLETED",
  ];
  readonly filters = this.fb.nonNullable.group({
    active: [""],
    type: ["" as FacilityType | ""],
    city: [""],
  });
  readonly reservationFilters = this.fb.nonNullable.group({
    status: ["" as ReservationStatus | ""],
  });
  readonly facilityForm = this.fb.nonNullable.group({
    name: ["", Validators.required],
    facilityType: ["COMMUNITY_CENTER" as FacilityType],
    timezone: ["America/New_York", Validators.required],
    addressLine1: [""],
    city: [""],
    state: [""],
    description: [""],
  });
  readonly reservationForm = this.fb.nonNullable.group({
    facilityId: ["", Validators.required],
    facilitySpaceId: ["", Validators.required],
    title: ["", Validators.required],
    startDateTime: ["", Validators.required],
    endDateTime: ["", Validators.required],
    expectedAttendance: [1, Validators.min(1)],
    eventId: [""],
    purpose: [""],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.loadFacilities();
    this.loadReservations();
    if (this.canRequest() && !this.canManage()) this.loadMyReservations();
    this.loadReports();
  }
  loadReports() {
    forkJoin({
      report: this.api.report(this.organizationId),
      utilization: this.api.utilization(this.organizationId),
    }).subscribe({
      next: (r) => {
        this.report.set(r.report);
        this.utilization.set(r.utilization);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "FACILITY_MANAGER");
  }
  canRequest() {
    return this.context.hasAnyRole(
      "ORG_ADMIN",
      "FACILITY_MANAGER",
      "EVENT_COORDINATOR",
      "PROGRAM_MANAGER",
    );
  }
  label(v: string) {
    return v.replaceAll("_", " ");
  }
  local(v: string) {
    return new Date(v).toLocaleString();
  }
  location(f: Facility) {
    return [f.city, f.state].filter(Boolean).join(", ") || "—";
  }
  loadFacilities(page = 0) {
    this.loading.set(true);
    this.api
      .facilities(this.organizationId, { ...this.filters.getRawValue(), page })
      .subscribe({
        next: (r) => {
          this.facilities.set(r);
          this.loading.set(false);
        },
        error: (e) => {
          this.error.set(this.errors.from(e));
          this.loading.set(false);
        },
      });
  }
  loadReservations(page = 0) {
    this.api
      .reservations(this.organizationId, {
        ...this.reservationFilters.getRawValue(),
        page,
      })
      .subscribe({
        next: (r) => this.reservations.set(r),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  loadMyReservations(page = 0) {
    this.api.myReservations(this.organizationId, page).subscribe({
      next: (r) => this.myReservations.set(r),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  loadSpaces() {
    const id = this.reservationForm.controls.facilityId.value;
    if (!id) {
      this.spaces.set([]);
      return;
    }
    this.api.allSpaces(this.organizationId, id).subscribe({
      next: (r) => this.spaces.set(r.content),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  createFacility() {
    if (this.facilityForm.invalid) return;
    const v = this.facilityForm.getRawValue();
    this.api
      .createFacility(this.organizationId, {
        ...v,
        description: v.description || null,
        addressLine1: v.addressLine1 || null,
        addressLine2: null,
        city: v.city || null,
        state: v.state || null,
        postalCode: null,
        country: null,
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showFacility.set(false);
          this.loadFacilities();
          this.loadReports();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  createReservation() {
    if (this.reservationForm.invalid) return;
    const v = this.reservationForm.getRawValue();
    const facility = this.facilities()?.content.find(
      (f) => f.id === v.facilityId,
    );
    if (!facility) return;
    const start = facilityLocalToInstant(v.startDateTime, facility.timezone);
    const end = facilityLocalToInstant(v.endDateTime, facility.timezone);
    if (new Date(start) >= new Date(end)) {
      this.error.set({
        timestamp: new Date().toISOString(),
        status: 400,
        code: "VALIDATION_ERROR",
        message: "Reservation end must be after its start.",
        path: "",
        fieldErrors: { endDateTime: "End must be after start" },
      });
      return;
    }
    this.api
      .availability(this.organizationId, v.facilitySpaceId, start, end)
      .subscribe({
        next: (a) => {
          if (!a.available) {
            this.error.set({
              timestamp: new Date().toISOString(),
              status: 422,
              code: "RESERVATION_CONFLICT",
              message: a.reason || "The space is not available.",
              path: "",
              fieldErrors: {},
            });
            return;
          }
          this.api
            .createReservation(this.organizationId, {
              facilitySpaceId: v.facilitySpaceId,
              requestedByUserId: this.auth.currentUser()?.id ?? null,
              requesterName: null,
              requesterEmail: null,
              eventId: v.eventId || null,
              title: v.title,
              purpose: v.purpose || null,
              startDateTime: start,
              endDateTime: end,
              expectedAttendance: v.expectedAttendance || null,
              notes: null,
            })
            .subscribe({
              next: () => {
                this.showReservation.set(false);
                this.loadReservations();
                if (!this.canManage()) this.loadMyReservations();
                this.loadReports();
              },
              error: (e) => this.error.set(this.errors.from(e)),
            });
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
}
