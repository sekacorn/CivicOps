import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import { ReservationDetail } from "../../core/models/facility.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { ConfirmationDialogComponent } from "../../shared/components/confirmation-dialog.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { FacilityApiService } from "./facility-api.service";

@Component({
  selector: "cop-reservation-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
    ConfirmationDialogComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a [routerLink]="['../..']">← Facilities</a
    ><cop-api-error [error]="error()" />
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (reservation()) {
      @let r = reservation()!;
      <header class="page-header">
        <div>
          <p class="eyebrow">RESERVATION</p>
          <h1>{{ r.title }}</h1>
          <p>{{ r.facilityName }} · {{ r.spaceName }}</p>
        </div>
        <cop-status-badge [status]="r.status" />
      </header>
      <section class="panel details">
        <div>
          <span>Starts</span><strong>{{ local(r.startDateTime) }}</strong>
        </div>
        <div>
          <span>Ends</span><strong>{{ local(r.endDateTime) }}</strong>
        </div>
        <div>
          <span>Expected attendance</span
          ><strong>{{ r.expectedAttendance ?? "—" }}</strong>
        </div>
        <div>
          <span>Requester</span
          ><strong>{{ r.requesterName || "Organization member" }}</strong>
        </div>
        <div>
          <span>Requester email</span
          ><strong>{{ r.requesterEmail || "—" }}</strong>
        </div>
        <div>
          <span>Purpose</span><strong>{{ r.purpose || "—" }}</strong>
        </div>
        @if (r.eventId) {
          <div>
            <span>Linked Event</span
            ><a
              [routerLink]="[
                '/organizations',
                organizationId,
                'events',
                r.eventId,
              ]"
              >Open Event</a
            >
          </div>
        }
      </section>
      <section class="panel actions-panel">
        <h2>Lifecycle</h2>
        @if (canApprove() && r.status === "PENDING") {
          <button class="button button--primary" (click)="action('approve')">
            Approve
          </button>
          <form [formGroup]="reasonForm" (ngSubmit)="reject()">
            <label>Rejection reason<input formControlName="reason" /></label
            ><button
              class="button button--danger"
              [disabled]="reasonForm.invalid"
            >
              Reject
            </button>
          </form>
        }
        @if (canApprove() && r.status === "APPROVED") {
          <button class="button button--primary" (click)="action('complete')">
            Complete
          </button>
        }
        @if (r.status === "PENDING" || r.status === "APPROVED") {
          <button
            class="button button--danger"
            (click)="confirmCancel.set(true)"
          >
            Cancel reservation
          </button>
        }
      </section>
      @if (confirmCancel()) {
        <cop-confirmation-dialog
          title="Cancel reservation"
          message="Cancellation preserves the reservation and decision history."
          confirmLabel="Cancel reservation"
          (confirmed)="cancel()"
          (cancelled)="confirmCancel.set(false)"
        />
      }
    }
  `,
  styles: `
    .details {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1rem;
    }
    .details div {
      display: grid;
    }
    .details span {
      color: var(--ink-muted);
    }
    .actions-panel {
      display: flex;
      align-items: end;
      gap: 0.75rem;
      flex-wrap: wrap;
      margin-top: 1rem;
    }
    .actions-panel h2 {
      width: 100%;
    }
    .actions-panel form {
      display: flex;
      align-items: end;
      gap: 0.5rem;
    }
    .actions-panel label {
      display: grid;
      gap: 0.3rem;
    }
    @media (max-width: 650px) {
      .details {
        grid-template-columns: 1fr;
      }
      .actions-panel,
      .actions-panel form {
        align-items: stretch;
        flex-direction: column;
      }
    }
  `,
})
export class ReservationDetailComponent implements OnInit {
  private readonly api = inject(FacilityApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly reservation = signal<ReservationDetail | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  readonly confirmCancel = signal(false);
  organizationId = "";
  reservationId = "";
  readonly reasonForm = this.fb.nonNullable.group({
    reason: ["", Validators.required],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.reservationId =
      this.route.snapshot.paramMap.get("reservationId") ?? "";
    this.load();
  }
  canApprove() {
    return this.context.hasAnyRole("ORG_ADMIN", "FACILITY_MANAGER");
  }
  local(v: string) {
    return new Date(v).toLocaleString();
  }
  load() {
    this.api.reservation(this.organizationId, this.reservationId).subscribe({
      next: (r) => {
        this.reservation.set(r);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(this.errors.from(e));
        this.loading.set(false);
      },
    });
  }
  action(action: "approve" | "complete") {
    this.api
      .reservationAction(this.organizationId, this.reservationId, action)
      .subscribe({
        next: (r) => this.reservation.set(r),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  reject() {
    if (this.reasonForm.invalid) return;
    this.api
      .reject(
        this.organizationId,
        this.reservationId,
        this.reasonForm.controls.reason.value,
      )
      .subscribe({
        next: (r) => this.reservation.set(r),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  cancel() {
    this.api
      .cancelReservation(this.organizationId, this.reservationId, null)
      .subscribe({
        next: (r) => {
          this.reservation.set(r);
          this.confirmCancel.set(false);
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
}
