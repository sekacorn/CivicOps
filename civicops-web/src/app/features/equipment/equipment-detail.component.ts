import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import {
  EquipmentAssetDetail,
  EquipmentCheckoutDetail,
  EquipmentCheckoutSummary,
  EquipmentCondition,
  EquipmentMaintenance,
  MaintenanceType,
} from "../../core/models/equipment.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { ConfirmationDialogComponent } from "../../shared/components/confirmation-dialog.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { EquipmentApiService } from "./equipment-api.service";

@Component({
  selector: "cop-equipment-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
    ConfirmationDialogComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a routerLink="..">← Inventory</a><cop-api-error [error]="error()" />
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (asset()) {
      @let a = asset()!;
      <header class="page-header">
        <div>
          <p class="eyebrow">ASSET {{ a.assetTag }}</p>
          <h1>{{ a.name }}</h1>
          <p>
            {{ a.categoryName || "Uncategorized" }} ·
            {{ a.location || "Location not specified" }}
          </p>
        </div>
        <cop-status-badge [status]="a.status" />
      </header>
      <section class="metrics">
        <article>
          <span>Condition</span><strong>{{ a.condition }}</strong>
        </article>
        <article>
          <span>Manufacturer</span><strong>{{ a.manufacturer || "—" }}</strong>
        </article>
        <article>
          <span>Model</span><strong>{{ a.model || "—" }}</strong>
        </article>
        <article>
          <span>Serial</span><strong>{{ a.serialNumber || "—" }}</strong>
        </article>
      </section>
      @if (canManage()) {
        @if (
          a.status === "AVAILABLE" ||
          (a.status === "MAINTENANCE" && !hasActiveMaintenance())
        ) {
          <div class="actions">
            @if (a.status === "AVAILABLE") {
              <button
                class="button button--primary"
                (click)="showCheckout.set(!showCheckout())"
              >
                Check out
              </button>
            }
            <button
              class="button button--secondary"
              (click)="showMaintenance.set(!showMaintenance())"
            >
              Start maintenance
            </button>
            @if (a.status === "AVAILABLE") {
              <button
                class="button button--danger"
                (click)="confirm.set('retire')"
              >
                Retire
              </button>
            }
          </div>
        }
        @if (showCheckout()) {
          <form
            class="panel form-grid"
            [formGroup]="checkoutForm"
            (ngSubmit)="checkout()"
          >
            <h2>Check out asset</h2>
            <label
              >Member user ID<input formControlName="borrowerUserId" /></label
            ><label
              >External borrower name<input
                formControlName="borrowerName" /></label
            ><label
              >External borrower email<input
                type="email"
                formControlName="borrowerEmail" /></label
            ><label
              >Due in your local time<input
                type="datetime-local"
                formControlName="dueAt" /></label
            ><label
              >Checkout condition<select formControlName="checkoutCondition">
                @for (c of conditions; track c) {
                  <option [value]="c">{{ c }}</option>
                }
              </select></label
            ><label>Notes<textarea formControlName="notes"></textarea></label
            ><button
              class="button button--primary"
              [disabled]="checkoutForm.invalid"
            >
              Confirm checkout
            </button>
          </form>
        }
        @if (showMaintenance()) {
          <form
            class="panel form-grid"
            [formGroup]="maintenanceForm"
            (ngSubmit)="createMaintenance()"
          >
            <h2>Maintenance record</h2>
            <label
              >Type<select formControlName="maintenanceType">
                @for (t of maintenanceTypes; track t) {
                  <option [value]="t">{{ label(t) }}</option>
                }
              </select></label
            ><label>Description<input formControlName="description" /></label
            ><label>Vendor<input formControlName="vendor" /></label
            ><label
              >Cost<input
                type="number"
                min="0"
                step=".01"
                formControlName="cost" /></label
            ><button
              class="button button--primary"
              [disabled]="maintenanceForm.invalid"
            >
              Create maintenance
            </button>
          </form>
        }
        <section class="panel">
          <h2>Checkout history</h2>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Checked out</th>
                  <th>Due</th>
                  <th>Status</th>
                  <th>Overdue</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                @for (c of checkouts(); track c.id) {
                  <tr>
                    <td>{{ local(c.checkedOutAt) }}</td>
                    <td>{{ local(c.dueAt) }}</td>
                    <td><cop-status-badge [status]="c.status" /></td>
                    <td>{{ c.overdue ? "Yes" : "No" }}</td>
                    <td>
                      @if (c.status === "ACTIVE") {
                        <button
                          class="button button--secondary"
                          (click)="openCheckIn(c.id)"
                        >
                          Check in</button
                        ><button
                          class="button button--danger"
                          (click)="lostCheckout.set(c.id)"
                        >
                          Mark lost
                        </button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
        @if (checkIn(); as c) {
          <form
            class="panel form-grid"
            [formGroup]="checkInForm"
            (ngSubmit)="completeCheckIn(c.id)"
          >
            <h2>Check in {{ c.assetName }}</h2>
            <p class="wide private">
              Borrower: {{ c.borrowerName }}. Contact details are manager-only
              and are not shown in inventory summaries.
            </p>
            <label
              >Return condition<select formControlName="returnCondition">
                @for (x of conditions; track x) {
                  <option [value]="x">{{ x }}</option>
                }
              </select></label
            ><label>Notes<textarea formControlName="notes"></textarea></label
            ><button class="button button--primary">Complete check-in</button>
          </form>
        }
        <section class="panel maintenance">
          <h2>Maintenance history</h2>
          @for (m of maintenance(); track m.id) {
            <article>
              <div class="section-head">
                <strong
                  >{{ label(m.maintenanceType) }} · {{ m.description }}</strong
                ><cop-status-badge [status]="m.status" />
              </div>
              <p>
                {{ local(m.startedAt) }} · {{ m.vendor || "No vendor" }} ·
                {{ m.cost ?? 0 }}
              </p>
              @if (m.status === "OPEN") {
                <button
                  class="button button--secondary"
                  (click)="maintenanceAction(m, 'start')"
                >
                  Start
                </button>
              }
              @if (m.status === "IN_PROGRESS") {
                <select
                  [formControl]="maintenanceResult"
                  aria-label="Resulting equipment condition"
                >
                  @for (condition of conditions; track condition) {
                    <option [value]="condition">{{ condition }}</option>
                  }
                </select>
                <button
                  class="button button--primary"
                  (click)="completeMaintenance(m)"
                >
                  Complete
                </button>
              }
            </article>
          } @empty {
            <p>No maintenance history.</p>
          }
        </section>
      }
      @if (confirm() === "retire") {
        <cop-confirmation-dialog
          title="Retire equipment"
          message="Retirement is terminal and preserves all checkout and maintenance history."
          confirmLabel="Retire asset"
          (confirmed)="retire()"
          (cancelled)="confirm.set(null)"
        />
      }
      @if (lostCheckout()) {
        <cop-confirmation-dialog
          title="Mark equipment lost"
          message="The active checkout will close as lost and the asset will no longer be available."
          confirmLabel="Mark lost"
          (confirmed)="markLost()"
          (cancelled)="lostCheckout.set(null)"
        />
      }
    }
  `,
  styles: `
    .metrics {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .metrics article {
      display: grid;
      padding: 1rem;
      background: white;
      border: 1px solid var(--line);
    }
    .metrics span,
    .private {
      color: var(--ink-muted);
    }
    .actions {
      display: flex;
      gap: 0.6rem;
      flex-wrap: wrap;
      margin-bottom: 1rem;
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
    .form-grid label {
      display: grid;
      gap: 0.3rem;
    }
    .section-head {
      display: flex;
      justify-content: space-between;
      gap: 0.5rem;
    }
    .maintenance article {
      padding: 0.75rem 0;
      border-bottom: 1px solid var(--line);
    }
    @media (max-width: 750px) {
      .metrics,
      .form-grid {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 560px) {
      .metrics,
      .form-grid {
        grid-template-columns: 1fr;
      }
      .form-grid h2,
      .wide {
        grid-column: auto;
      }
    }
  `,
})
export class EquipmentDetailComponent implements OnInit {
  private readonly api = inject(EquipmentApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly asset = signal<EquipmentAssetDetail | null>(null);
  readonly checkouts = signal<EquipmentCheckoutSummary[]>([]);
  readonly maintenance = signal<EquipmentMaintenance[]>([]);
  readonly checkIn = signal<EquipmentCheckoutDetail | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  readonly showCheckout = signal(false);
  readonly showMaintenance = signal(false);
  readonly confirm = signal<"retire" | null>(null);
  readonly lostCheckout = signal<string | null>(null);
  organizationId = "";
  assetId = "";
  readonly conditions: EquipmentCondition[] = [
    "EXCELLENT",
    "GOOD",
    "FAIR",
    "DAMAGED",
    "UNUSABLE",
  ];
  readonly maintenanceTypes: MaintenanceType[] = [
    "INSPECTION",
    "REPAIR",
    "CLEANING",
    "CALIBRATION",
    "UPGRADE",
    "OTHER",
  ];
  readonly checkoutForm = this.fb.nonNullable.group({
    borrowerUserId: [""],
    borrowerName: [""],
    borrowerEmail: ["", Validators.email],
    dueAt: ["", Validators.required],
    checkoutCondition: ["GOOD" as EquipmentCondition],
    notes: [""],
  });
  readonly checkInForm = this.fb.nonNullable.group({
    returnCondition: ["GOOD" as EquipmentCondition],
    notes: [""],
  });
  readonly maintenanceForm = this.fb.nonNullable.group({
    maintenanceType: ["INSPECTION" as MaintenanceType],
    description: ["", Validators.required],
    vendor: [""],
    cost: [0],
  });
  readonly maintenanceResult = this.fb.nonNullable.control(
    "GOOD" as EquipmentCondition,
  );
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.assetId = this.route.snapshot.paramMap.get("assetId") ?? "";
    this.load();
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "EQUIPMENT_MANAGER");
  }
  label(v: string) {
    return v.replaceAll("_", " ");
  }
  hasActiveMaintenance() {
    return this.maintenance().some(
      (record) => record.status === "OPEN" || record.status === "IN_PROGRESS",
    );
  }
  local(v: string) {
    return new Date(v).toLocaleString();
  }
  load() {
    this.loading.set(true);
    this.api.asset(this.organizationId, this.assetId).subscribe({
      next: (a) => {
        this.asset.set(a);
        this.loading.set(false);
        if (this.canManage()) this.loadPrivateHistory();
      },
      error: (e) => {
        this.error.set(this.errors.from(e));
        this.loading.set(false);
      },
    });
  }
  loadPrivateHistory() {
    forkJoin({
      checkouts: this.api.checkouts(this.organizationId, this.assetId),
      maintenance: this.api.maintenance(this.organizationId, this.assetId),
    }).subscribe({
      next: (r) => {
        this.checkouts.set(r.checkouts.content);
        this.maintenance.set(r.maintenance.content);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  checkout() {
    if (this.checkoutForm.invalid) return;
    const v = this.checkoutForm.getRawValue();
    this.api
      .checkout(this.organizationId, this.assetId, {
        borrowerUserId: v.borrowerUserId || null,
        borrowerName: v.borrowerName || null,
        borrowerEmail: v.borrowerEmail || null,
        dueAt: new Date(v.dueAt).toISOString(),
        checkoutCondition: v.checkoutCondition,
        notes: v.notes || null,
      })
      .subscribe({
        next: () => {
          this.showCheckout.set(false);
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  openCheckIn(id: string) {
    this.api.checkoutDetail(this.organizationId, id).subscribe({
      next: (r) => this.checkIn.set(r),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  completeCheckIn(id: string) {
    const v = this.checkInForm.getRawValue();
    this.api
      .checkIn(this.organizationId, id, v.returnCondition, v.notes || null)
      .subscribe({
        next: () => {
          this.checkIn.set(null);
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  createMaintenance() {
    if (this.maintenanceForm.invalid) return;
    const v = this.maintenanceForm.getRawValue();
    this.api
      .createMaintenance(this.organizationId, this.assetId, {
        ...v,
        startedAt: null,
        cost: v.cost || null,
        vendor: v.vendor || null,
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showMaintenance.set(false);
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  maintenanceAction(m: EquipmentMaintenance, action: "start") {
    this.api.maintenanceAction(this.organizationId, m.id, action).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  completeMaintenance(m: EquipmentMaintenance) {
    this.api
      .completeMaintenance(
        this.organizationId,
        m.id,
        this.maintenanceResult.value,
      )
      .subscribe({
        next: () => this.load(),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  retire() {
    this.api.retire(this.organizationId, this.assetId).subscribe({
      next: (a) => {
        this.asset.set(a);
        this.confirm.set(null);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  markLost() {
    const id = this.lostCheckout();
    if (!id) return;
    this.api.markLost(this.organizationId, id).subscribe({
      next: () => {
        this.lostCheckout.set(null);
        this.load();
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
}
