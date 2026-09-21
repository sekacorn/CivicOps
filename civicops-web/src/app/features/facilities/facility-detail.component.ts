import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import {
  DayOfWeek,
  Facility,
  FacilityBlackout,
  FacilitySpace,
  OperatingHours,
} from "../../core/models/facility.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { ConfirmationDialogComponent } from "../../shared/components/confirmation-dialog.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { FacilityApiService } from "./facility-api.service";
import { facilityLocalToInstant, formatInFacilityZone } from "./facility-time";

interface HoursRow {
  dayOfWeek: DayOfWeek;
  openTime: string;
  closeTime: string;
  closed: boolean;
}
const DAYS: DayOfWeek[] = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];

@Component({
  selector: "cop-facility-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
    ConfirmationDialogComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a routerLink="..">← Facilities</a><cop-api-error [error]="error()" />
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (facility()) {
      @let f = facility()!;
      <header class="page-header">
        <div>
          <p class="eyebrow">{{ label(f.facilityType) }}</p>
          <h1>{{ f.name }}</h1>
          <p>{{ location(f) }} · Times shown in {{ f.timezone }}</p>
        </div>
        <cop-status-badge [status]="f.active ? 'ACTIVE' : 'INACTIVE'" />
      </header>
      <section class="panel">
        <h2>Spaces</h2>
        @if (canManage()) {
          <form
            class="space-form"
            [formGroup]="spaceForm"
            (ngSubmit)="createSpace()"
          >
            <label>Name<input formControlName="name" /></label
            ><label
              >Capacity<input
                type="number"
                min="1"
                formControlName="capacity" /></label
            ><label
              >Location details<input
                formControlName="locationDetails" /></label
            ><label class="check"
              ><input type="checkbox" formControlName="reservable" />
              Reservable</label
            ><button
              class="button button--primary"
              [disabled]="spaceForm.invalid"
            >
              Create space
            </button>
          </form>
        }
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Space</th>
                <th>Capacity</th>
                <th>Reservable</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              @for (s of spaces(); track s.id) {
                <tr>
                  <th scope="row">{{ s.name }}</th>
                  <td>{{ s.capacity ?? "Not limited" }}</td>
                  <td>{{ s.reservable ? "Yes" : "No" }}</td>
                  <td>
                    <cop-status-badge
                      [status]="s.active ? 'ACTIVE' : 'INACTIVE'"
                    />
                    @if (canManage()) {
                      <button
                        class="button button--secondary"
                        (click)="toggleSpace(s)"
                      >
                        {{ s.active ? "Deactivate" : "Activate" }}
                      </button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
      <section class="panel">
        <h2>Weekly operating hours</h2>
        <p>
          Facility-local wall times in <strong>{{ f.timezone }}</strong
          >.
        </p>
        <div class="hours">
          @for (row of hoursRows(); track row.dayOfWeek) {
            <div class="hours-row">
              <strong>{{ label(row.dayOfWeek) }}</strong
              ><label
                >Open<input
                  type="time"
                  [value]="row.openTime"
                  [disabled]="row.closed || !canManage()"
                  (input)="setHour(row.dayOfWeek, 'openTime', $event)" /></label
              ><label
                >Close<input
                  type="time"
                  [value]="row.closeTime"
                  [disabled]="row.closed || !canManage()"
                  (input)="
                    setHour(row.dayOfWeek, 'closeTime', $event)
                  " /></label
              ><label class="check"
                ><input
                  type="checkbox"
                  [checked]="row.closed"
                  [disabled]="!canManage()"
                  (change)="setClosed(row.dayOfWeek, $event)"
                />
                Closed</label
              >
            </div>
          }
        </div>
        @if (canManage()) {
          <button class="button button--primary" (click)="saveHours()">
            Save operating hours
          </button>
        }
      </section>
      <section class="panel">
        <h2>Blackouts</h2>
        @if (canManage()) {
          <form
            class="blackout-form"
            [formGroup]="blackoutForm"
            (ngSubmit)="createBlackout()"
          >
            <label
              >Space (optional)<select formControlName="facilitySpaceId">
                <option value="">Whole facility</option>
                @for (s of spaces(); track s.id) {
                  <option [value]="s.id">{{ s.name }}</option>
                }
              </select></label
            ><label
              >Facility-local start<input
                type="datetime-local"
                formControlName="startDateTime" /></label
            ><label
              >Facility-local end<input
                type="datetime-local"
                formControlName="endDateTime" /></label
            ><label>Reason<input formControlName="reason" /></label
            ><button
              class="button button--primary"
              [disabled]="blackoutForm.invalid"
            >
              Create blackout
            </button>
          </form>
        }
        @for (b of blackouts(); track b.id) {
          <article>
            <div>
              <strong
                >{{ zoned(b.startDateTime, f.timezone) }} –
                {{ zoned(b.endDateTime, f.timezone) }}</strong
              >
              <p>
                {{ b.reason }} ·
                {{ b.facilitySpaceId ? "Selected space" : "Whole facility" }}
              </p>
            </div>
            <cop-status-badge [status]="b.active ? 'ACTIVE' : 'CANCELLED'" />
            @if (canManage() && b.active) {
              <button
                class="button button--danger"
                (click)="cancelBlackout.set(b.id)"
              >
                Cancel blackout
              </button>
            }
          </article>
        } @empty {
          <p>No blackouts recorded.</p>
        }
      </section>
      @if (cancelBlackout()) {
        <cop-confirmation-dialog
          title="Cancel blackout"
          message="Reservations remain unchanged; this removes the blackout from future availability checks."
          confirmLabel="Cancel blackout"
          (confirmed)="confirmCancelBlackout()"
          (cancelled)="cancelBlackout.set(null)"
        />
      }
    }
  `,
  styles: `
    .panel {
      margin-bottom: 1rem;
    }
    .space-form,
    .blackout-form {
      display: grid;
      grid-template-columns: repeat(4, 1fr) auto;
      gap: 0.7rem;
      align-items: end;
      margin-bottom: 1rem;
    }
    .space-form label,
    .blackout-form label,
    .hours-row label {
      display: grid;
      gap: 0.25rem;
    }
    .check {
      display: flex !important;
      align-items: center;
      gap: 0.4rem;
    }
    .check input {
      width: auto;
    }
    .hours {
      display: grid;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    .hours-row {
      display: grid;
      grid-template-columns: 9rem 1fr 1fr 7rem;
      gap: 0.75rem;
      align-items: center;
    }
    .panel article {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.8rem 0;
      border-bottom: 1px solid var(--line);
    }
    .panel article p {
      margin: 0.2rem 0;
      color: var(--ink-muted);
    }
    @media (max-width: 850px) {
      .space-form,
      .blackout-form {
        grid-template-columns: 1fr 1fr;
      }
      .hours-row {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 560px) {
      .space-form,
      .blackout-form,
      .hours-row {
        grid-template-columns: 1fr;
      }
      .panel article {
        align-items: flex-start;
        flex-direction: column;
      }
    }
  `,
})
export class FacilityDetailComponent implements OnInit {
  private readonly api = inject(FacilityApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly facility = signal<Facility | null>(null);
  readonly spaces = signal<FacilitySpace[]>([]);
  readonly blackouts = signal<FacilityBlackout[]>([]);
  readonly hoursRows = signal<HoursRow[]>(
    DAYS.map((dayOfWeek) => ({
      dayOfWeek,
      openTime: "09:00",
      closeTime: "17:00",
      closed: false,
    })),
  );
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  readonly cancelBlackout = signal<string | null>(null);
  organizationId = "";
  facilityId = "";
  readonly spaceForm = this.fb.nonNullable.group({
    name: ["", Validators.required],
    capacity: [1, Validators.min(1)],
    locationDetails: [""],
    reservable: [true],
  });
  readonly blackoutForm = this.fb.nonNullable.group({
    facilitySpaceId: [""],
    startDateTime: ["", Validators.required],
    endDateTime: ["", Validators.required],
    reason: ["", Validators.required],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.facilityId = this.route.snapshot.paramMap.get("facilityId") ?? "";
    this.load();
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "FACILITY_MANAGER");
  }
  label(v: string) {
    return v.replaceAll("_", " ");
  }
  location(f: Facility) {
    return (
      [f.addressLine1, f.city, f.state, f.postalCode]
        .filter(Boolean)
        .join(", ") || "Address not specified"
    );
  }
  zoned(v: string, z: string) {
    return formatInFacilityZone(v, z);
  }
  load() {
    this.loading.set(true);
    forkJoin({
      facility: this.api.facility(this.organizationId, this.facilityId),
      spaces: this.api.spaces(this.organizationId, this.facilityId),
      hours: this.api.hours(this.organizationId, this.facilityId),
      blackouts: this.api.blackouts(this.organizationId, this.facilityId),
    }).subscribe({
      next: (r) => {
        this.facility.set(r.facility);
        this.spaces.set(r.spaces.content);
        this.blackouts.set(r.blackouts.content);
        this.mergeHours(r.hours);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(this.errors.from(e));
        this.loading.set(false);
      },
    });
  }
  mergeHours(hours: OperatingHours[]) {
    this.hoursRows.set(
      DAYS.map((day) => {
        const h = hours.find((x) => x.dayOfWeek === day);
        return {
          dayOfWeek: day,
          openTime: h?.openTime?.slice(0, 5) || "09:00",
          closeTime: h?.closeTime?.slice(0, 5) || "17:00",
          closed: h?.closed ?? false,
        };
      }),
    );
  }
  setHour(day: DayOfWeek, key: "openTime" | "closeTime", event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.hoursRows.update((rows) =>
      rows.map((r) => (r.dayOfWeek === day ? { ...r, [key]: value } : r)),
    );
  }
  setClosed(day: DayOfWeek, event: Event) {
    const closed = (event.target as HTMLInputElement).checked;
    this.hoursRows.update((rows) =>
      rows.map((r) => (r.dayOfWeek === day ? { ...r, closed } : r)),
    );
  }
  saveHours() {
    const value = this.hoursRows().map((r) => ({
      dayOfWeek: r.dayOfWeek,
      openTime: r.closed ? null : r.openTime,
      closeTime: r.closed ? null : r.closeTime,
      closed: r.closed,
    }));
    this.api.saveHours(this.organizationId, this.facilityId, value).subscribe({
      next: (r) => this.mergeHours(r),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  createSpace() {
    if (this.spaceForm.invalid) return;
    const v = this.spaceForm.getRawValue();
    this.api
      .createSpace(this.organizationId, this.facilityId, {
        name: v.name,
        description: null,
        capacity: v.capacity || null,
        reservable: v.reservable,
        locationDetails: v.locationDetails || null,
        accessibilityNotes: null,
      })
      .subscribe({
        next: (r) => {
          this.spaces.update((x) => [...x, r]);
          this.spaceForm.reset({
            name: "",
            capacity: 1,
            locationDetails: "",
            reservable: true,
          });
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  toggleSpace(space: FacilitySpace) {
    this.api
      .updateSpace(this.organizationId, space.id, { active: !space.active })
      .subscribe({
        next: (updated) =>
          this.spaces.update((all) =>
            all.map((current) =>
              current.id === updated.id ? updated : current,
            ),
          ),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  createBlackout() {
    if (this.blackoutForm.invalid || !this.facility()) return;
    const v = this.blackoutForm.getRawValue();
    const start = facilityLocalToInstant(
      v.startDateTime,
      this.facility()!.timezone,
    );
    const end = facilityLocalToInstant(
      v.endDateTime,
      this.facility()!.timezone,
    );
    if (new Date(start) >= new Date(end)) {
      this.error.set({
        timestamp: new Date().toISOString(),
        status: 400,
        code: "VALIDATION_ERROR",
        message: "Blackout end must be after its start.",
        path: "",
        fieldErrors: { endDateTime: "End must be after start" },
      });
      return;
    }
    this.api
      .createBlackout(this.organizationId, {
        facilityId: this.facilityId,
        facilitySpaceId: v.facilitySpaceId || null,
        startDateTime: start,
        endDateTime: end,
        reason: v.reason,
      })
      .subscribe({
        next: (r) => this.blackouts.update((x) => [...x, r]),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  confirmCancelBlackout() {
    const id = this.cancelBlackout();
    if (!id) return;
    this.api.cancelBlackout(this.organizationId, id).subscribe({
      next: (r) => {
        this.blackouts.update((x) => x.map((b) => (b.id === r.id ? r : b)));
        this.cancelBlackout.set(null);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
}
