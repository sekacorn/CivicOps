import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin, of } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  Opportunity,
  VolunteerDetail,
  VolunteerHour,
  VolunteerReport,
  VolunteerShift,
  VolunteerSummary,
} from "../../core/models/operations.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { VolunteerApiService } from "./volunteer-api.service";
@Component({
  selector: "cop-volunteers",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    PaginationComponent,
    StatusBadgeComponent,
  ],
  template: `<header class="page-header">
      <div>
        <p class="eyebrow">PEOPLE & SERVICE</p>
        <h1>Volunteers</h1>
        <p>
          Profiles, opportunities, shifts, assignments, and approved service
          hours.
        </p>
      </div>
      <div class="actions">
        @if (canManageVolunteers()) {
          <button
            class="button button--secondary"
            (click)="showVolunteer.set(!showVolunteer())"
          >
            New volunteer
          </button>
        }
        @if (canManageOpportunities()) {
          <button
            class="button button--primary"
            (click)="showOpportunity.set(!showOpportunity())"
          >
            New opportunity
          </button>
        }
      </div>
    </header>
    <cop-api-error [error]="error()" />
    @if (showVolunteer()) {
      <form [formGroup]="volunteerForm" (ngSubmit)="createVolunteer()">
        <label>First name<input formControlName="firstName" /></label
        ><label>Last name<input formControlName="lastName" /></label
        ><label>Email<input type="email" formControlName="email" /></label
        ><label>Phone<input formControlName="phone" /></label
        ><label
          >Start date<input type="date" formControlName="startDate" /></label
        ><label
          >Skills<input
            formControlName="skills"
            placeholder="food service, outreach" /></label
        ><button class="button button--primary">Create volunteer</button>
      </form>
    }
    @if (showOpportunity()) {
      <form [formGroup]="opportunityForm" (ngSubmit)="createOpportunity()">
        <label>Title<input formControlName="title" /></label
        ><label>Location<input formControlName="location" /></label
        ><label
          >Starts<input
            type="datetime-local"
            formControlName="startAt" /></label
        ><label
          >Ends<input type="datetime-local" formControlName="endAt" /></label
        ><label
          >Maximum volunteers<input
            type="number"
            min="1"
            formControlName="maximumVolunteers" /></label
        ><label>Linked event ID<input formControlName="eventId" /></label
        ><label
          >Description<textarea formControlName="description"></textarea></label
        ><button class="button button--primary">Create opportunity</button>
      </form>
    }
    @if (report(); as r) {
      <section class="metrics" aria-label="Volunteer report">
        <article>
          <span>Active volunteers</span
          ><strong>{{ r.activeVolunteers }}</strong>
        </article>
        <article>
          <span>Approved hours</span><strong>{{ r.approvedHours }}</strong>
        </article>
        <article>
          <span>Open opportunities</span
          ><strong>{{ r.openOpportunities }}</strong>
        </article>
        <article>
          <span>Upcoming shifts</span><strong>{{ r.upcomingShifts }}</strong>
        </article>
        <article>
          <span>Open shift capacity</span
          ><strong>{{ r.openShiftCapacity }}</strong>
        </article>
      </section>
    }
    <div class="columns">
      <section class="panel">
        <h2>Volunteer directory</h2>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Status</th>
                <th>Skills</th>
              </tr>
            </thead>
            <tbody>
              @for (v of volunteers()?.content ?? []; track v.id) {
                <tr>
                  <th scope="row">
                    <button class="link" (click)="openVolunteer(v.id)">
                      {{ v.firstName }} {{ v.lastName }}
                    </button>
                  </th>
                  <td><cop-status-badge [status]="v.status" /></td>
                  <td>{{ v.skills.join(", ") || "—" }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        @if (volunteers(); as p) {
          <cop-pagination
            [page]="p.page"
            [totalPages]="p.totalPages"
            [totalElements]="p.totalElements"
            [first]="p.first"
            [last]="p.last"
            (pageChange)="load($event)"
          />
        }
      </section>
      <section class="panel">
        <h2>Opportunities</h2>
        @for (o of opportunities()?.content ?? []; track o.id) {
          <article>
            <button class="link" (click)="openOpportunity(o)">
              <strong>{{ o.title }}</strong>
            </button>
            <p>
              {{ local(o.startAt) }} · {{ o.location || "Location pending" }}
            </p>
            <cop-status-badge [status]="o.status" />
          </article>
        } @empty {
          <p>No opportunities.</p>
        }
      </section>
    </div>
    @if (selectedVolunteer(); as v) {
      <section class="panel detail">
        <div class="section-head">
          <h2>{{ v.firstName }} {{ v.lastName }}</h2>
          <cop-status-badge [status]="v.status" />
        </div>
        <p>{{ v.email }} · {{ v.phone || "No phone" }}</p>
        <p>Skills: {{ v.skills.join(", ") || "None recorded" }}</p>
        <h3>Record service hours</h3>
        <form [formGroup]="hoursForm" (ngSubmit)="submitHours(v.id)">
          <label
            >Service date<input
              type="date"
              formControlName="serviceDate" /></label
          ><label
            >Hours<input
              type="number"
              min="0.01"
              max="24"
              step="0.25"
              formControlName="hours" /></label
          ><label
            >Description<textarea
              formControlName="description"
            ></textarea></label
          ><button
            class="button button--primary"
            [disabled]="hoursForm.invalid"
          >
            Record hours
          </button>
        </form>
      </section>
    }
    @if (selectedOpportunity(); as o) {
      <section class="panel detail">
        <div class="section-head">
          <h2>{{ o.title }}</h2>
          <div class="actions">
            <cop-status-badge [status]="o.status" />
            @if (canManageOpportunities() && o.status === "DRAFT") {
              <button
                class="button button--primary"
                (click)="transition(o.id, 'open')"
              >
                Open
              </button>
            }
            @if (canManageOpportunities() && o.status === "OPEN") {
              <button
                class="button button--secondary"
                (click)="transition(o.id, 'close')"
              >
                Close
              </button>
            }
          </div>
        </div>
        <p>{{ o.description }}</p>
        <p>{{ local(o.startAt) }} – {{ local(o.endAt) }}</p>
        @if (o.eventId) {
          <p>
            Event:
            <a
              [routerLink]="[
                '/organizations',
                organizationId,
                'events',
                o.eventId,
              ]"
              >{{ o.eventId }}</a
            >
          </p>
        }
        <h3>Shifts</h3>
        @if (canManageOpportunities()) {
          <button
            class="button button--secondary"
            (click)="showShift.set(!showShift())"
          >
            Add shift
          </button>
        }
        @if (showShift()) {
          <form [formGroup]="shiftForm" (ngSubmit)="createShift(o.id)">
            <label>Title<input formControlName="title" /></label
            ><label
              >Starts<input
                type="datetime-local"
                formControlName="startAt" /></label
            ><label
              >Ends<input
                type="datetime-local"
                formControlName="endAt" /></label
            ><label
              >Capacity<input
                type="number"
                min="1"
                formControlName="capacity" /></label
            ><button class="button button--primary">Create shift</button>
          </form>
        }
        @for (s of shifts()?.content ?? []; track s.id) {
          <div class="shift">
            <strong>{{ s.title }}</strong> · {{ local(s.startAt) }} · Capacity
            {{ s.capacity }}
            @if (canManageOpportunities()) {
              <form
                class="inline"
                [formGroup]="assignmentForm"
                (ngSubmit)="assign(s.id)"
              >
                <label
                  >Volunteer<select formControlName="volunteerId">
                    <option value="">Select volunteer</option>
                    @for (v of volunteers()?.content ?? []; track v.id) {
                      <option [value]="v.id">
                        {{ v.firstName }} {{ v.lastName }}
                      </option>
                    }
                  </select></label
                ><button class="button button--secondary">Assign</button>
              </form>
            }
          </div>
        }
      </section>
    }
    @if (canManageVolunteers()) {
      <section class="panel detail">
        <h2>Pending hours</h2>
        @for (h of hours()?.content ?? []; track h.id) {
          <p>
            {{ h.serviceDate }} · {{ h.hours }} hours · Volunteer
            {{ h.volunteerId }}
            <button
              class="button button--primary"
              (click)="review(h, 'approve')"
            >
              Approve</button
            ><button
              class="button button--secondary"
              (click)="review(h, 'reject')"
            >
              Reject
            </button>
          </p>
        } @empty {
          <p>No pending hour entries.</p>
        }
      </section>
    }`,
  styles: `
    .actions,
    .section-head {
      display: flex;
      gap: 0.6rem;
      align-items: center;
      flex-wrap: wrap;
    }
    .section-head {
      justify-content: space-between;
    }
    .columns {
      display: grid;
      grid-template-columns: 1.4fr 1fr;
      gap: 1rem;
    }
    .columns article {
      padding: 0.8rem 0;
      border-bottom: 1px solid var(--line);
    }
    .columns article p {
      margin: 0.2rem 0;
      color: var(--ink-muted);
    }
    form {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
      padding: 1rem;
      margin-bottom: 1rem;
      background: white;
      border: 1px solid var(--line);
    }
    label {
      display: grid;
      gap: 0.3rem;
    }
    .link {
      padding: 0;
      color: #12647a;
      background: none;
      border: 0;
      text-align: left;
      cursor: pointer;
    }
    .detail {
      margin-top: 1rem;
    }
    .metrics {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 0.75rem;
      margin: 1rem 0;
    }
    .metrics article {
      display: grid;
      gap: 0.25rem;
      padding: 1rem;
      background: white;
      border: 1px solid var(--line);
    }
    .metrics span {
      color: var(--ink-muted);
    }
    .shift {
      padding: 0.8rem 0;
      border-bottom: 1px solid var(--line);
    }
    form.inline {
      display: flex;
      align-items: end;
      margin: 0.5rem 0;
      padding: 0.5rem;
    }
    .inline label {
      min-width: 14rem;
    }
    @media (max-width: 850px) {
      .columns,
      form,
      .metrics {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class VolunteersComponent implements OnInit {
  private readonly api = inject(VolunteerApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly volunteers = signal<PageResponse<VolunteerSummary> | null>(null);
  readonly opportunities = signal<PageResponse<Opportunity> | null>(null);
  readonly hours = signal<PageResponse<VolunteerHour> | null>(null);
  readonly report = signal<VolunteerReport | null>(null);
  readonly selectedVolunteer = signal<VolunteerDetail | null>(null);
  readonly selectedOpportunity = signal<Opportunity | null>(null);
  readonly shifts = signal<PageResponse<VolunteerShift> | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly showVolunteer = signal(false);
  readonly showOpportunity = signal(false);
  readonly showShift = signal(false);
  organizationId = "";
  readonly volunteerForm = this.fb.nonNullable.group({
    firstName: ["", Validators.required],
    lastName: ["", Validators.required],
    email: ["", [Validators.required, Validators.email]],
    phone: [""],
    startDate: [new Date().toISOString().slice(0, 10)],
    skills: [""],
  });
  readonly opportunityForm = this.fb.nonNullable.group({
    title: ["", Validators.required],
    description: [""],
    location: [""],
    startAt: ["", Validators.required],
    endAt: ["", Validators.required],
    maximumVolunteers: [10],
    eventId: [""],
  });
  readonly shiftForm = this.fb.nonNullable.group({
    title: ["", Validators.required],
    startAt: ["", Validators.required],
    endAt: ["", Validators.required],
    capacity: [5],
  });
  readonly assignmentForm = this.fb.nonNullable.group({
    volunteerId: ["", Validators.required],
  });
  readonly hoursForm = this.fb.nonNullable.group({
    serviceDate: [new Date().toISOString().slice(0, 10), Validators.required],
    hours: [1, [Validators.required, Validators.min(0.01), Validators.max(24)]],
    description: [""],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.load();
  }
  load(page = 0) {
    const year = new Date().getFullYear();
    forkJoin({
      volunteers: this.api.volunteers(this.organizationId, page),
      opportunities: this.api.opportunities(this.organizationId),
      hours: this.canManageVolunteers()
        ? this.api.hours(this.organizationId)
        : of(null),
      report: this.api.report(
        this.organizationId,
        `${year}-01-01`,
        `${year}-12-31`,
      ),
    }).subscribe({
      next: (r) => {
        this.volunteers.set(r.volunteers);
        this.opportunities.set(r.opportunities);
        this.hours.set(r.hours);
        this.report.set(r.report);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  canManageVolunteers() {
    return this.context.hasAnyRole("ORG_ADMIN", "VOLUNTEER_COORDINATOR");
  }
  canManageOpportunities() {
    return this.context.hasAnyRole(
      "ORG_ADMIN",
      "VOLUNTEER_COORDINATOR",
      "PROGRAM_MANAGER",
    );
  }
  createVolunteer() {
    if (this.volunteerForm.invalid) return;
    const v = this.volunteerForm.getRawValue();
    this.api
      .createVolunteer(this.organizationId, {
        ...v,
        userId: null,
        phone: v.phone || null,
        skills: v.skills
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean),
        status: "ACTIVE",
      })
      .subscribe({
        next: (r) => {
          this.showVolunteer.set(false);
          this.selectedVolunteer.set(r);
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  createOpportunity() {
    if (this.opportunityForm.invalid) return;
    const v = this.opportunityForm.getRawValue();
    this.api
      .createOpportunity(this.organizationId, {
        ...v,
        description: v.description || null,
        location: v.location || null,
        startAt: new Date(v.startAt).toISOString(),
        endAt: new Date(v.endAt).toISOString(),
        minimumVolunteers: null,
        eventId: v.eventId || null,
      })
      .subscribe({
        next: (r) => {
          this.showOpportunity.set(false);
          this.selectedOpportunity.set(r);
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  openVolunteer(id: string) {
    this.api
      .volunteer(this.organizationId, id)
      .subscribe((v) => this.selectedVolunteer.set(v));
  }
  openOpportunity(o: Opportunity) {
    this.selectedOpportunity.set(o);
    this.api
      .shifts(this.organizationId, o.id)
      .subscribe((s) => this.shifts.set(s));
  }
  transition(id: string, action: string) {
    this.api
      .transitionOpportunity(this.organizationId, id, action)
      .subscribe((r) => {
        this.selectedOpportunity.set(r);
        this.load();
      });
  }
  createShift(id: string) {
    const v = this.shiftForm.getRawValue();
    this.api
      .createShift(this.organizationId, id, {
        ...v,
        startAt: new Date(v.startAt).toISOString(),
        endAt: new Date(v.endAt).toISOString(),
      })
      .subscribe(() => this.openOpportunity(this.selectedOpportunity()!));
  }
  assign(shiftId: string) {
    if (this.assignmentForm.invalid) return;
    this.api
      .assign(
        this.organizationId,
        shiftId,
        this.assignmentForm.getRawValue().volunteerId,
      )
      .subscribe({
        next: () => this.assignmentForm.reset(),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  submitHours(volunteerId: string) {
    if (this.hoursForm.invalid) return;
    const value = this.hoursForm.getRawValue();
    this.api
      .submitHours(this.organizationId, {
        volunteerId,
        assignmentId: null,
        serviceDate: value.serviceDate,
        hours: value.hours,
        description: value.description || null,
      })
      .subscribe({
        next: () => {
          this.hoursForm.reset({
            serviceDate: new Date().toISOString().slice(0, 10),
            hours: 1,
            description: "",
          });
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  review(h: VolunteerHour, action: "approve" | "reject") {
    this.api
      .reviewHours(
        this.organizationId,
        h.id,
        action,
        action === "reject" ? "Not approved" : undefined,
      )
      .subscribe(() => this.load());
  }
  local(value: string) {
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(new Date(value));
  }
}
