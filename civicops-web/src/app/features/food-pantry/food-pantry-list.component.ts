import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  FoodCategory,
  HouseholdSummary,
  PantryDistributionReport,
  PantryHouseholdReport,
  PantryInventorySummary,
  PantryItem,
  PantryLocation,
  UnitType,
} from "../../core/models/food-pantry.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { FoodPantryApiService } from "./food-pantry-api.service";

@Component({
  selector: "cop-food-pantry-list",
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
        <p class="eyebrow">FOOD PANTRY</p>
        <h1>Pantries and inventory</h1>
        <p>Inventory, households, distribution visits, and pantry reporting.</p>
      </div>
      @if (canManage()) {
        <button
          class="button button--primary"
          (click)="showPantry.set(!showPantry())"
        >
          New pantry
        </button>
      }
    </header>
    <cop-api-error [error]="error()" />
    @if (inventoryReport(); as r) {
      <section class="metrics" aria-label="Pantry inventory summary">
        <article>
          <span>Item types</span><strong>{{ r.totalItemTypes }}</strong>
        </article>
        <article>
          <span>Available qty</span
          ><strong>{{ r.totalAvailableQuantity }}</strong>
        </article>
        <article>
          <span>Low stock</span><strong>{{ r.lowStockItemCount }}</strong>
        </article>
        <article>
          <span>Expired lots</span><strong>{{ r.expiredLotCount }}</strong>
        </article>
        <article>
          <span>Expiring soon</span
          ><strong>{{ r.expiringSoonLotCount }}</strong>
        </article>
      </section>
    }
    @if (canManage() && showPantry()) {
      <form
        class="panel form-grid"
        [formGroup]="pantryForm"
        (ngSubmit)="createPantry()"
      >
        <h2>Create pantry</h2>
        <label>Name<input formControlName="name" /></label>
        <label>Timezone<input formControlName="timezone" /></label>
        <label>City<input formControlName="city" /></label>
        <label>State<input formControlName="state" /></label>
        <label class="wide"
          >Description<textarea formControlName="description"></textarea>
        </label>
        <button class="button button--primary" [disabled]="pantryForm.invalid">
          Create pantry
        </button>
      </form>
    }
    @if (canManage() && showItem()) {
      <form
        class="panel form-grid"
        [formGroup]="itemForm"
        (ngSubmit)="createItem()"
      >
        <h2>Create item</h2>
        <label>Name<input formControlName="name" /></label>
        <label>SKU<input formControlName="sku" /></label>
        <label
          >Category<select formControlName="category">
            @for (category of categories; track category) {
              <option [value]="category">
                {{ category.replaceAll("_", " ") }}
              </option>
            }
          </select></label
        >
        <label
          >Unit<select formControlName="unitType">
            @for (unit of units; track unit) {
              <option [value]="unit">{{ unit }}</option>
            }
          </select></label
        >
        <label
          >Reorder threshold<input
            type="number"
            min="0"
            formControlName="reorderThreshold"
        /></label>
        <label
          ><input type="checkbox" formControlName="trackExpiration" /> Track
          expiration</label
        >
        <button class="button button--primary" [disabled]="itemForm.invalid">
          Create item
        </button>
      </form>
    }
    <div class="actions">
      @if (canManage()) {
        <button
          class="button button--secondary"
          (click)="showItem.set(!showItem())"
        >
          New item
        </button>
        <button
          class="button button--secondary"
          (click)="showHousehold.set(!showHousehold())"
        >
          New household
        </button>
      }
    </div>
    @if (canManage() && showHousehold()) {
      <form
        class="panel form-grid"
        [formGroup]="householdForm"
        (ngSubmit)="createHousehold()"
      >
        <h2>Create household</h2>
        <label>Household name<input formControlName="householdName" /></label>
        <label
          >First name<input formControlName="primaryContactFirstName"
        /></label>
        <label
          >Last name<input formControlName="primaryContactLastName"
        /></label>
        <label>Email<input formControlName="email" /></label>
        <label>Phone<input formControlName="phone" /></label>
        <label
          >Household size<input
            type="number"
            min="1"
            formControlName="householdSize"
        /></label>
        <button
          class="button button--primary"
          [disabled]="householdForm.invalid"
        >
          Create household
        </button>
      </form>
    }
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (pantries()?.content?.length === 0) {
      <cop-async-state
        state="empty"
        detail="No food pantries have been configured."
      />
    } @else if (pantries()) {
      @let page = pantries()!;
      <section class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Pantry</th>
              <th>Location</th>
              <th>Timezone</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            @for (pantry of page.content; track pantry.id) {
              <tr>
                <th scope="row">
                  <a [routerLink]="[pantry.id]">{{ pantry.name }}</a>
                </th>
                <td>{{ location(pantry) }}</td>
                <td>{{ pantry.timezone }}</td>
                <td>
                  <cop-status-badge
                    [status]="pantry.active ? 'ACTIVE' : 'INACTIVE'"
                  />
                </td>
              </tr>
            }
          </tbody>
        </table>
        <cop-pagination
          [page]="page.page"
          [totalPages]="page.totalPages"
          [totalElements]="page.totalElements"
          [first]="page.first"
          [last]="page.last"
          (pageChange)="loadPantries($event)"
        />
      </section>
    }
    <section class="columns">
      <article class="panel">
        <h2>Items</h2>
        <div class="stack-list">
          @for (item of items()?.content ?? []; track item.id) {
            <span
              >{{ item.name }} · {{ item.category.replaceAll("_", " ") }}</span
            >
          }
        </div>
      </article>
      @if (canManage()) {
        <article class="panel">
          <h2>Households</h2>
          <div class="stack-list">
            @for (
              household of households()?.content ?? [];
              track household.id
            ) {
              <a [routerLink]="['households', household.id]"
                >{{ household.displayName }} · {{ household.householdSize }}</a
              >
            }
          </div>
        </article>
      }
      <article class="panel">
        <h2>Distribution report</h2>
        @if (distributionReport(); as report) {
          <p>
            <strong>{{ report.visitsCompleted }}</strong> completed visits ·
            <strong>{{ report.householdsServed }}</strong> households ·
            <strong>{{ report.totalQuantityDistributed }}</strong> units
          </p>
        }
        @if (householdReport(); as report) {
          <p>{{ report.activeHouseholds }} active households</p>
        }
      </article>
    </section>
  `,
  styles: `
    .metrics,
    .form-grid,
    .columns {
      display: grid;
      gap: 0.75rem;
    }
    .metrics {
      grid-template-columns: repeat(5, 1fr);
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
    .form-grid {
      grid-template-columns: repeat(3, 1fr);
      margin-bottom: 1rem;
    }
    .columns {
      grid-template-columns: repeat(3, 1fr);
      margin-top: 1rem;
    }
    .actions {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    .wide,
    .form-grid h2 {
      grid-column: 1/-1;
    }
    label,
    .stack-list {
      display: grid;
      gap: 0.3rem;
    }
    .stack-list a,
    .stack-list span {
      padding: 0.65rem 0;
      border-bottom: 1px solid var(--line);
      text-decoration: none;
    }
    @media (max-width: 900px) {
      .metrics,
      .columns {
        grid-template-columns: repeat(2, 1fr);
      }
      .form-grid {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 560px) {
      .metrics,
      .form-grid,
      .columns {
        grid-template-columns: 1fr;
      }
      .wide,
      .form-grid h2 {
        grid-column: auto;
      }
    }
  `,
})
export class FoodPantryListComponent implements OnInit {
  private readonly api = inject(FoodPantryApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly pantries = signal<PageResponse<PantryLocation> | null>(null);
  readonly items = signal<PageResponse<PantryItem> | null>(null);
  readonly households = signal<PageResponse<HouseholdSummary> | null>(null);
  readonly inventoryReport = signal<PantryInventorySummary | null>(null);
  readonly distributionReport = signal<PantryDistributionReport | null>(null);
  readonly householdReport = signal<PantryHouseholdReport | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  readonly showPantry = signal(false);
  readonly showItem = signal(false);
  readonly showHousehold = signal(false);
  organizationId = "";
  readonly categories: FoodCategory[] = [
    "CANNED_GOODS",
    "GRAINS",
    "PRODUCE",
    "DAIRY",
    "MEAT",
    "FROZEN",
    "BAKERY",
    "BEVERAGES",
    "BABY_FOOD",
    "PERSONAL_CARE",
    "HOUSEHOLD",
    "OTHER",
  ];
  readonly units: UnitType[] = [
    "EACH",
    "CAN",
    "BOX",
    "BAG",
    "BOTTLE",
    "POUND",
    "OUNCE",
    "KILOGRAM",
    "LITER",
    "PACKAGE",
    "OTHER",
  ];
  readonly pantryForm = this.fb.nonNullable.group({
    name: ["", Validators.required],
    timezone: ["America/New_York", Validators.required],
    city: [""],
    state: [""],
    description: [""],
  });
  readonly itemForm = this.fb.nonNullable.group({
    name: ["", Validators.required],
    sku: [""],
    category: ["CANNED_GOODS" as FoodCategory],
    unitType: ["EACH" as UnitType],
    reorderThreshold: [0],
    trackExpiration: [true],
  });
  readonly householdForm = this.fb.nonNullable.group({
    householdName: [""],
    primaryContactFirstName: [""],
    primaryContactLastName: [""],
    email: [""],
    phone: [""],
    householdSize: [1, Validators.min(1)],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.loadPantries();
    this.loadSupportingData();
  }

  canManage(): boolean {
    return this.context.hasAnyRole("FOOD_PANTRY_MANAGER");
  }

  canReport(): boolean {
    return this.context.hasAnyRole("FOOD_PANTRY_MANAGER", "PROGRAM_MANAGER");
  }

  location(pantry: PantryLocation): string {
    return [pantry.city, pantry.state].filter(Boolean).join(", ") || "—";
  }

  loadPantries(page = 0): void {
    this.loading.set(true);
    this.api.pantries(this.organizationId, page).subscribe({
      next: (result) => {
        this.pantries.set(result);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(this.errors.from(error));
        this.loading.set(false);
      },
    });
  }

  loadSupportingData(): void {
    this.api.items(this.organizationId).subscribe({
      next: (items) => this.items.set(items),
      error: (error) => this.error.set(this.errors.from(error)),
    });
    if (this.canManage()) {
      this.api.households(this.organizationId).subscribe({
        next: (households) => this.households.set(households),
        error: (error) => this.error.set(this.errors.from(error)),
      });
    }
    if (this.canReport()) {
      forkJoin({
        inventory: this.api.inventoryReport(this.organizationId),
        distributions: this.api.distributionReport(this.organizationId),
        householdsReport: this.api.householdReport(this.organizationId),
      }).subscribe({
        next: (result) => {
          this.inventoryReport.set(result.inventory);
          this.distributionReport.set(result.distributions);
          this.householdReport.set(result.householdsReport);
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
    }
  }

  createPantry(): void {
    if (this.pantryForm.invalid) return;
    const value = this.pantryForm.getRawValue();
    this.api
      .createPantry(this.organizationId, {
        ...value,
        description: value.description || null,
        addressLine1: null,
        addressLine2: null,
        city: value.city || null,
        state: value.state || null,
        postalCode: null,
        country: "US",
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showPantry.set(false);
          this.loadPantries();
          this.loadSupportingData();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  createItem(): void {
    if (this.itemForm.invalid) return;
    const value = this.itemForm.getRawValue();
    this.api
      .createItem(this.organizationId, {
        ...value,
        sku: value.sku || null,
        description: null,
        reorderThreshold: value.reorderThreshold || null,
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showItem.set(false);
          this.loadSupportingData();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  createHousehold(): void {
    if (this.householdForm.invalid) return;
    const value = this.householdForm.getRawValue();
    this.api
      .createHousehold(this.organizationId, {
        ...value,
        externalReferenceNumber: null,
        householdName: value.householdName || null,
        primaryContactFirstName: value.primaryContactFirstName || null,
        primaryContactLastName: value.primaryContactLastName || null,
        email: value.email || null,
        phone: value.phone || null,
        address: null,
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showHousehold.set(false);
          this.loadSupportingData();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
