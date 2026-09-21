import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  AssetStatus,
  EquipmentAssetSummary,
  EquipmentCategory,
  EquipmentCondition,
  EquipmentInventoryReport,
  EquipmentMaintenanceReport,
  EquipmentUtilizationReport,
} from "../../core/models/equipment.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { EquipmentApiService } from "./equipment-api.service";

@Component({
  selector: "cop-equipment-list",
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
        <p class="eyebrow">EQUIPMENT</p>
        <h1>Inventory</h1>
        <p>Assets, checkouts, maintenance, and authoritative utilization.</p>
      </div>
      @if (canManage()) {
        <button
          class="button button--primary"
          (click)="showAsset.set(!showAsset())"
        >
          New asset
        </button>
      }
    </header>
    <cop-api-error [error]="error()" />
    @if (report(); as r) {
      <section class="metrics" aria-label="Equipment report">
        <article>
          <span>Total</span><strong>{{ r.totalAssets }}</strong>
        </article>
        <article>
          <span>Available</span><strong>{{ r.availableAssets }}</strong>
        </article>
        <article>
          <span>Checked out</span><strong>{{ r.checkedOutAssets }}</strong>
        </article>
        <article>
          <span>Maintenance</span><strong>{{ r.maintenanceAssets }}</strong>
        </article>
        <article>
          <span>Lost / retired</span
          ><strong>{{ r.lostAssets }} / {{ r.retiredAssets }}</strong>
        </article>
        <article>
          <span>Overdue</span><strong>{{ r.overdueCheckouts }}</strong>
        </article>
      </section>
    }
    @if (canManage()) {
      <div class="actions">
        <button
          class="button button--secondary"
          (click)="showCategory.set(!showCategory())"
        >
          New category
        </button>
      </div>
      @if (showCategory()) {
        <form
          class="panel compact"
          [formGroup]="categoryForm"
          (ngSubmit)="createCategory()"
        >
          <h2>Create category</h2>
          <label>Name<input formControlName="name" /></label
          ><label>Description<input formControlName="description" /></label
          ><button
            class="button button--primary"
            [disabled]="categoryForm.invalid"
          >
            Create category
          </button>
        </form>
      }
      @if (showAsset()) {
        <form
          class="panel form-grid"
          [formGroup]="assetForm"
          (ngSubmit)="createAsset()"
        >
          <h2>Create asset</h2>
          <label>Asset tag<input formControlName="assetTag" /></label
          ><label>Name<input formControlName="name" /></label
          ><label
            >Category<select formControlName="categoryId">
              <option value="">No category</option>
              @for (c of categories(); track c.id) {
                <option [value]="c.id">{{ c.name }}</option>
              }
            </select></label
          ><label
            >Condition<select formControlName="condition">
              @for (c of conditions; track c) {
                <option [value]="c">{{ c }}</option>
              }
            </select></label
          ><label>Location<input formControlName="location" /></label
          ><label
            >Purchase value<input
              type="number"
              min="0"
              step=".01"
              formControlName="purchaseValue" /></label
          ><label class="wide"
            >Description<textarea
              formControlName="description"
            ></textarea></label
          ><button
            class="button button--primary"
            [disabled]="assetForm.invalid"
          >
            Create asset
          </button>
        </form>
      }
    }
    <form class="filters" [formGroup]="filters" (ngSubmit)="loadAssets(0)">
      <label
        >Status<select formControlName="status">
          <option value="">All</option>
          @for (s of statuses; track s) {
            <option [value]="s">{{ label(s) }}</option>
          }
        </select></label
      ><label
        >Condition<select formControlName="condition">
          <option value="">All</option>
          @for (c of conditions; track c) {
            <option [value]="c">{{ c }}</option>
          }
        </select></label
      ><label
        >Category<select formControlName="categoryId">
          <option value="">All</option>
          @for (c of categories(); track c.id) {
            <option [value]="c.id">{{ c.name }}</option>
          }
        </select></label
      ><label>Name<input formControlName="name" /></label
      ><button class="button button--secondary">Apply</button>
    </form>
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (assets()?.content?.length === 0) {
      <cop-async-state
        state="empty"
        detail="No equipment matches these filters."
      />
    } @else if (assets()) {
      @let p = assets()!;
      <section class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Asset</th>
              <th>Name</th>
              <th>Category</th>
              <th>Condition</th>
              <th>Status</th>
              <th>Location</th>
            </tr>
          </thead>
          <tbody>
            @for (a of p.content; track a.id) {
              <tr>
                <th scope="row">
                  <a [routerLink]="[a.id]">{{ a.assetTag }}</a>
                </th>
                <td>{{ a.name }}</td>
                <td>{{ a.categoryName || "—" }}</td>
                <td>{{ a.condition }}</td>
                <td><cop-status-badge [status]="a.status" /></td>
                <td>{{ a.location || "—" }}</td>
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
          (pageChange)="loadAssets($event)"
        />
      </section>
    }
    @if (utilization(); as u) {
      <section class="reports">
        <article class="panel">
          <h2>Utilization</h2>
          <p>
            <strong>{{ u.checkoutCount }}</strong> checkouts ·
            <strong>{{ u.currentlyOverdue }}</strong> currently overdue
          </p>
        </article>
        @if (maintenanceReport(); as m) {
          <article class="panel">
            <h2>Maintenance</h2>
            <p>
              <strong>{{ m.openMaintenance }}</strong> open · total cost
              <strong>{{ m.totalCost }}</strong>
            </p>
          </article>
        }
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
    .metrics span {
      display: block;
      color: var(--ink-muted);
    }
    .metrics strong {
      font-size: 1.3rem;
    }
    .actions {
      margin-bottom: 1rem;
    }
    .compact {
      display: grid;
      grid-template-columns: 1fr 2fr auto;
      align-items: end;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .compact h2,
    .form-grid h2,
    .wide {
      grid-column: 1/-1;
    }
    .compact label,
    .form-grid label,
    .filters label {
      display: grid;
      gap: 0.3rem;
    }
    .form-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .filters {
      display: grid;
      grid-template-columns: repeat(4, 1fr) auto;
      gap: 0.75rem;
      align-items: end;
      margin-bottom: 1rem;
    }
    .reports {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      margin-top: 1rem;
    }
    @media (max-width: 950px) {
      .metrics {
        grid-template-columns: repeat(3, 1fr);
      }
      .form-grid,
      .filters {
        grid-template-columns: 1fr 1fr;
      }
      .compact {
        grid-template-columns: 1fr;
      }
    }
    @media (max-width: 560px) {
      .metrics,
      .form-grid,
      .filters,
      .reports {
        grid-template-columns: 1fr;
      }
      .form-grid h2,
      .wide {
        grid-column: auto;
      }
    }
  `,
})
export class EquipmentListComponent implements OnInit {
  private readonly api = inject(EquipmentApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly assets = signal<PageResponse<EquipmentAssetSummary> | null>(null);
  readonly categories = signal<EquipmentCategory[]>([]);
  readonly report = signal<EquipmentInventoryReport | null>(null);
  readonly utilization = signal<EquipmentUtilizationReport | null>(null);
  readonly maintenanceReport = signal<EquipmentMaintenanceReport | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  readonly showAsset = signal(false);
  readonly showCategory = signal(false);
  organizationId = "";
  readonly statuses: AssetStatus[] = [
    "AVAILABLE",
    "CHECKED_OUT",
    "MAINTENANCE",
    "LOST",
    "RETIRED",
  ];
  readonly conditions: EquipmentCondition[] = [
    "EXCELLENT",
    "GOOD",
    "FAIR",
    "DAMAGED",
    "UNUSABLE",
  ];
  readonly filters = this.fb.nonNullable.group({
    status: ["" as AssetStatus | ""],
    condition: ["" as EquipmentCondition | ""],
    categoryId: [""],
    name: [""],
  });
  readonly categoryForm = this.fb.nonNullable.group({
    name: ["", Validators.required],
    description: [""],
  });
  readonly assetForm = this.fb.nonNullable.group({
    assetTag: ["", Validators.required],
    name: ["", Validators.required],
    categoryId: [""],
    condition: ["GOOD" as EquipmentCondition],
    location: [""],
    purchaseValue: [0],
    description: [""],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.loadAssets();
    this.loadReports();
  }
  loadReports() {
    forkJoin({
      categories: this.api.categories(this.organizationId),
      report: this.api.inventoryReport(this.organizationId),
      utilization: this.api.utilizationReport(this.organizationId),
      maintenance: this.api.maintenanceReport(this.organizationId),
    }).subscribe({
      next: (r) => {
        this.categories.set(r.categories.content);
        this.report.set(r.report);
        this.utilization.set(r.utilization);
        this.maintenanceReport.set(r.maintenance);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "EQUIPMENT_MANAGER");
  }
  label(v: string) {
    return v.replaceAll("_", " ");
  }
  loadAssets(page = 0) {
    this.loading.set(true);
    this.api
      .assets(this.organizationId, { ...this.filters.getRawValue(), page })
      .subscribe({
        next: (r) => {
          this.assets.set(r);
          this.loading.set(false);
        },
        error: (e) => {
          this.error.set(this.errors.from(e));
          this.loading.set(false);
        },
      });
  }
  createCategory() {
    if (this.categoryForm.invalid) return;
    this.api
      .createCategory(this.organizationId, this.categoryForm.getRawValue())
      .subscribe({
        next: (r) => {
          this.categories.update((x) => [...x, r]);
          this.showCategory.set(false);
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  createAsset() {
    if (this.assetForm.invalid) return;
    const v = this.assetForm.getRawValue();
    this.api
      .createAsset(this.organizationId, {
        ...v,
        categoryId: v.categoryId || null,
        description: v.description || null,
        manufacturer: null,
        model: null,
        serialNumber: null,
        purchaseDate: null,
        purchaseValue: v.purchaseValue || null,
        location: v.location || null,
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showAsset.set(false);
          this.loadAssets();
          this.loadReports();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
}
