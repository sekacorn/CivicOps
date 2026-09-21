import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin, of } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  EventDetail,
  EventReport,
  RegistrationSummary,
} from "../../core/models/operations.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ConfirmationDialogComponent } from "../../shared/components/confirmation-dialog.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { EventApiService } from "./event-api.service";

@Component({
  selector: "cop-event-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    ConfirmationDialogComponent,
    StatusBadgeComponent,
  ],
  template: `@if (event(); as e) {
    <header class="page-header">
      <div>
        <p class="eyebrow">EVENT</p>
        <h1>{{ e.name }}</h1>
        <p>
          {{ local(e.startDateTime) }} – {{ local(e.endDateTime) }} ·
          {{ e.location || "Location pending" }}
        </p>
      </div>
      <div class="actions">
        <cop-status-badge [status]="e.status" />
        @if (
          canManage() && e.status !== "COMPLETED" && e.status !== "CANCELLED"
        ) {
          <a class="button button--secondary" routerLink="edit">Edit</a>
        }
      </div>
    </header>
    <cop-api-error [error]="error()" />
    <section class="metrics" aria-label="Event report">
      <article>
        <span>Capacity</span
        ><strong>{{
          report()?.capacity === null ? "Unlimited" : report()?.capacity
        }}</strong>
      </article>
      <article>
        <span>Registered</span><strong>{{ report()?.registered ?? 0 }}</strong>
      </article>
      <article>
        <span>Waitlisted</span><strong>{{ report()?.waitlisted ?? 0 }}</strong>
      </article>
      <article>
        <span>Attended</span><strong>{{ report()?.attended ?? 0 }}</strong>
      </article>
      <article>
        <span>Attendance rate</span
        ><strong>{{ report()?.attendanceRate ?? 0 }}%</strong>
      </article>
    </section>
    <div class="columns">
      @if (canManage()) {
        <section class="panel">
          <h2>Lifecycle</h2>
          <div class="actions">
            @for (a of actions(e); track a.endpoint) {
              <button
                class="button"
                [class.button--danger]="a.terminal"
                [class.button--primary]="!a.terminal"
                (click)="
                  a.terminal ? pendingAction.set(a) : transition(a.endpoint)
                "
              >
                {{ a.label }}
              </button>
            }
          </div>
          <h2>Relationships</h2>
          <p>
            Grant:
            @if (e.linkedGrantId) {
              <a
                [routerLink]="[
                  '/organizations',
                  organizationId,
                  'grants',
                  e.linkedGrantId,
                ]"
                >{{ e.linkedGrantName }}</a
              >
            } @else {
              Not linked
            }
          </p>
          <p>
            Campaign:
            @if (e.linkedDonationCampaignId) {
              <a
                [routerLink]="[
                  '/organizations',
                  organizationId,
                  'donations',
                  'campaigns',
                  e.linkedDonationCampaignId,
                ]"
                >{{ e.linkedDonationCampaignName }}</a
              >
            } @else {
              Not linked
            }
          </p>
        </section>
      }
      @if (canManage() && e.status === "REGISTRATION_OPEN") {
        <section class="panel">
          <h2>Register attendee</h2>
          <form [formGroup]="registrationForm" (ngSubmit)="register()">
            <label>Name<input formControlName="attendeeName" /></label
            ><label
              >Email<input
                type="email"
                formControlName="attendeeEmail" /></label
            ><label>Phone<input formControlName="attendeePhone" /></label
            ><button class="button button--primary">Register</button>
          </form>
          @if (registrationResult()) {
            <p role="status">
              Registration status: <strong>{{ registrationResult() }}</strong>
            </p>
          }
        </section>
      } @else if (e.status === "REGISTRATION_OPEN") {
        <section class="panel">
          <h2>Registration</h2>
          <p>Register your linked CivicOps user profile for this event.</p>
          <button class="button button--primary" (click)="selfRegister()">
            Register myself
          </button>
        </section>
      }
    </div>
    @if (canReadRegistrations()) {
      <section class="panel registrations">
        <h2>Registrations</h2>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Attendee</th>
                <th>Status</th>
                <th>Registered</th>
                <th>Attendance</th>
              </tr>
            </thead>
            <tbody>
              @for (r of registrations()?.content ?? []; track r.id) {
                <tr>
                  <th scope="row">{{ r.attendeeName }}</th>
                  <td><cop-status-badge [status]="r.status" /></td>
                  <td>{{ local(r.registrationDate) }}</td>
                  <td>
                    <div class="actions">
                      @if (r.status === "REGISTERED") {
                        <button
                          class="button button--secondary"
                          (click)="registrationAction(r.id, 'check-in')"
                        >
                          Check in</button
                        ><button
                          class="button button--danger"
                          (click)="registrationAction(r.id, 'cancel')"
                        >
                          Cancel
                        </button>
                      }
                      @if (r.status === "ATTENDED") {
                        <button
                          class="button button--secondary"
                          (click)="registrationAction(r.id, 'check-out')"
                        >
                          Check out
                        </button>
                      }
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    }
    @if (pendingAction(); as action) {
      <cop-confirmation-dialog
        title="Confirm event change"
        [message]="
          action.label + ' may be final and affects registration availability.'
        "
        [confirmLabel]="action.label"
        (cancelled)="pendingAction.set(null)"
        (confirmed)="transition(action.endpoint)"
      />
    }
  }`,
  styles: `
    .actions {
      display: flex;
      gap: 0.6rem;
      align-items: center;
      flex-wrap: wrap;
    }
    .metrics {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 0.75rem;
      margin: 1rem 0;
    }
    .metrics article {
      display: grid;
      gap: 0.3rem;
      padding: 1rem;
      background: white;
      border: 1px solid var(--line);
    }
    .metrics span {
      color: var(--ink-muted);
    }
    .metrics strong {
      font-size: 1.3rem;
    }
    .columns {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }
    form,
    label {
      display: grid;
      gap: 0.5rem;
    }
    .registrations {
      margin-top: 1rem;
    }
    @media (max-width: 900px) {
      .metrics {
        grid-template-columns: repeat(2, 1fr);
      }
      .columns {
        grid-template-columns: 1fr;
      }
    }
    @media (max-width: 500px) {
      .metrics {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class EventDetailComponent implements OnInit {
  private readonly api = inject(EventApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly event = signal<EventDetail | null>(null);
  readonly report = signal<EventReport | null>(null);
  readonly registrations = signal<PageResponse<RegistrationSummary> | null>(
    null,
  );
  readonly error = signal<ApiError | null>(null);
  readonly registrationResult = signal<string | null>(null);
  readonly pendingAction = signal<{
    label: string;
    endpoint: string;
    terminal: boolean;
  } | null>(null);
  organizationId = "";
  eventId = "";
  readonly registrationForm = this.fb.nonNullable.group({
    attendeeName: ["", Validators.required],
    attendeeEmail: ["", [Validators.required, Validators.email]],
    attendeePhone: [""],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.eventId = this.route.snapshot.paramMap.get("eventId") ?? "";
    this.load();
  }
  load() {
    forkJoin({
      event: this.api.detail(this.organizationId, this.eventId),
      report: this.api.report(this.organizationId, this.eventId),
      registrations: this.canReadRegistrations()
        ? this.api.registrations(this.organizationId, this.eventId)
        : of(null),
    }).subscribe({
      next: (r) => {
        this.event.set(r.event);
        this.report.set(r.report);
        this.registrations.set(r.registrations);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "EVENT_COORDINATOR");
  }
  canReadRegistrations() {
    return this.context.hasAnyRole(
      "ORG_ADMIN",
      "EVENT_COORDINATOR",
      "PROGRAM_MANAGER",
    );
  }
  actions(e: EventDetail) {
    const map: Record<
      string,
      { label: string; endpoint: string; terminal: boolean }[]
    > = {
      DRAFT: [
        { label: "Publish", endpoint: "publish", terminal: false },
        { label: "Cancel event", endpoint: "cancel", terminal: true },
      ],
      PUBLISHED: [
        {
          label: "Open registration",
          endpoint: "open-registration",
          terminal: false,
        },
        { label: "Cancel event", endpoint: "cancel", terminal: true },
      ],
      REGISTRATION_OPEN: [
        {
          label: "Close registration",
          endpoint: "close-registration",
          terminal: false,
        },
        { label: "Cancel event", endpoint: "cancel", terminal: true },
      ],
      REGISTRATION_CLOSED: [
        { label: "Complete", endpoint: "complete", terminal: true },
        { label: "Cancel event", endpoint: "cancel", terminal: true },
      ],
      COMPLETED: [],
      CANCELLED: [],
    };
    return map[e.status];
  }
  transition(endpoint: string) {
    this.pendingAction.set(null);
    this.api.transition(this.organizationId, this.eventId, endpoint).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  register() {
    if (this.registrationForm.invalid) return;
    const v = this.registrationForm.getRawValue();
    this.api
      .register(this.organizationId, this.eventId, {
        ...v,
        attendeePhone: v.attendeePhone || null,
      })
      .subscribe({
        next: (r) => {
          this.registrationResult.set(r.status);
          this.registrationForm.reset();
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  selfRegister() {
    this.api
      .selfRegister(this.organizationId, this.eventId, {
        attendeeName: null,
        attendeeEmail: null,
        attendeePhone: null,
      })
      .subscribe({
        next: (r) => this.registrationResult.set(r.status),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  registrationAction(id: string, action: "cancel" | "check-in" | "check-out") {
    this.api.registrationAction(this.organizationId, id, action).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  local(v: string) {
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(new Date(v));
  }
}
