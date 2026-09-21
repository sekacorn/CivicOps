import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  DistributionSummary,
  HouseholdSummary,
  InventoryAvailability,
  InventoryLot,
  InventorySourceType,
  PantryItem,
  PantryLocation,
} from "../../core/models/food-pantry.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { FoodPantryApiService } from "./food-pantry-api.service";

@Component({
  selector: "cop-food-pantry-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
    PaginationComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a class="back-link" [routerLink]="['..']">Back to food pantry</a>
    <cop-api-error [error]="error()" />
    @if (pantry(); as pantry) {
      <header class="page-header">
        <div>
          <p class="eyebrow">FOOD PANTRY</p>
          <h1>{{ pantry.name }}</h1>
          <p>{{ location(pantry) }} · {{ pantry.timezone }}</p>
        </div>
        <cop-status-badge [status]="pantry.active ? 'ACTIVE' : 'INACTIVE'" />
      </header>
      @if (canManage()) {
        <div class="actions">
          <button
            class="button button--secondary"
            (click)="pantryAction('activate')"
          >
            Activate
          </button>
          <button
            class="button button--quiet"
            (click)="pantryAction('deactivate')"
          >
            Deactivate
          </button>
          <button
            class="button button--secondary"
            (click)="showReceipt.set(!showReceipt())"
          >
            Receive inventory
          </button>
          <button
            class="button button--primary"
            (click)="showVisit.set(!showVisit())"
          >
            New distribution
          </button>
        </div>
      }
      @if (showReceipt()) {
        <form
          class="panel form-grid"
          [formGroup]="receiptForm"
          (ngSubmit)="receiveInventory()"
        >
          <h2>Receive inventory</h2>
          <label
            >Item<select formControlName="itemId">
              <option value="">Select item</option>
              @for (item of items()?.content ?? []; track item.id) {
                <option [value]="item.id">{{ item.name }}</option>
              }
            </select></label
          >
          <label
            >Quantity<input
              type="number"
              min="1"
              formControlName="quantityReceived"
          /></label>
          <label
            >Received date<input type="date" formControlName="receivedDate"
          /></label>
          <label
            >Expiration date<input type="date" formControlName="expirationDate"
          /></label>
          <label
            >Source<select formControlName="sourceType">
              @for (source of sources; track source) {
                <option [value]="source">
                  {{ source.replaceAll("_", " ") }}
                </option>
              }
            </select></label
          >
          <label>Lot number<input formControlName="lotNumber" /></label>
          <button
            class="button button--primary"
            [disabled]="receiptForm.invalid"
          >
            Receive
          </button>
        </form>
      }
      @if (showVisit()) {
        <form
          class="panel form-grid"
          [formGroup]="visitForm"
          (ngSubmit)="createDistribution()"
        >
          <h2>Create distribution visit</h2>
          <label
            >Household<select formControlName="householdId">
              <option value="">External / anonymous recipient</option>
              @for (
                household of households()?.content ?? [];
                track household.id
              ) {
                <option [value]="household.id">
                  {{ household.displayName }}
                </option>
              }
            </select></label
          >
          <label
            >Recipient name<input formControlName="recipientDisplayName"
          /></label>
          <label
            >Household size<input
              type="number"
              min="1"
              formControlName="householdSizeAtVisit"
          /></label>
          <button class="button button--primary" [disabled]="visitForm.invalid">
            Create visit
          </button>
        </form>
      }
      <section class="columns">
        <article class="panel">
          <h2>Availability</h2>
          <div class="stack-list">
            @for (available of availability(); track available.itemId) {
              <span
                >{{ available.itemName }} · {{ available.availableQuantity }}
                {{ available.unitType }}
                @if (available.lowStock) {
                  · Low stock
                }
              </span>
            }
          </div>
        </article>
        <article class="panel">
          <h2>Distribution visits</h2>
          <div class="stack-list">
            @for (visit of distributions()?.content ?? []; track visit.id) {
              <a [routerLink]="['distributions', visit.id]">
                {{ visit.recipientDisplayName }} · {{ visit.visitDateTime }}
                <cop-status-badge [status]="visit.status" />
              </a>
            }
          </div>
        </article>
      </section>
      @if (loading()) {
        <cop-async-state state="loading" />
      } @else if (inventory()?.content?.length === 0) {
        <cop-async-state
          state="empty"
          detail="No inventory lots are available at this pantry."
        />
      } @else if (inventory()) {
        @let page = inventory()!;
        <section class="panel table-wrap">
          <table>
            <thead>
              <tr>
                <th>Item</th>
                <th>Lot</th>
                <th>Received</th>
                <th>Expires</th>
                <th>Remaining</th>
              </tr>
            </thead>
            <tbody>
              @for (lot of page.content; track lot.id) {
                <tr>
                  <th scope="row">
                    <a [routerLink]="['inventory', lot.id]">{{
                      lot.itemName
                    }}</a>
                  </th>
                  <td>{{ lot.lotNumber || "—" }}</td>
                  <td>{{ lot.receivedDate }}</td>
                  <td>{{ lot.expirationDate || "—" }}</td>
                  <td>{{ lot.quantityRemaining }}</td>
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
            (pageChange)="loadInventory($event)"
          />
        </section>
      }
    }
  `,
  styles: `
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    .form-grid,
    .columns {
      display: grid;
      gap: 0.75rem;
    }
    .form-grid {
      grid-template-columns: repeat(3, 1fr);
      margin-bottom: 1rem;
    }
    .columns {
      grid-template-columns: 1fr 1fr;
      margin-bottom: 1rem;
    }
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
      display: flex;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.65rem 0;
      border-bottom: 1px solid var(--line);
      text-decoration: none;
    }
    @media (max-width: 760px) {
      .form-grid,
      .columns {
        grid-template-columns: 1fr;
      }
      .form-grid h2 {
        grid-column: auto;
      }
    }
  `,
})
export class FoodPantryDetailComponent implements OnInit {
  private readonly api = inject(FoodPantryApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly pantry = signal<PantryLocation | null>(null);
  readonly inventory = signal<PageResponse<InventoryLot> | null>(null);
  readonly availability = signal<InventoryAvailability[]>([]);
  readonly distributions = signal<PageResponse<DistributionSummary> | null>(
    null,
  );
  readonly items = signal<PageResponse<PantryItem> | null>(null);
  readonly households = signal<PageResponse<HouseholdSummary> | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  readonly showReceipt = signal(false);
  readonly showVisit = signal(false);
  organizationId = "";
  pantryId = "";
  readonly sources: InventorySourceType[] = [
    "DONATION",
    "PURCHASE",
    "FOOD_BANK",
    "GOVERNMENT_PROGRAM",
    "TRANSFER",
    "OTHER",
  ];
  readonly receiptForm = this.fb.nonNullable.group({
    itemId: ["", Validators.required],
    quantityReceived: [1, Validators.min(1)],
    receivedDate: [new Date().toISOString().slice(0, 10), Validators.required],
    expirationDate: [""],
    sourceType: ["DONATION" as InventorySourceType],
    lotNumber: [""],
  });
  readonly visitForm = this.fb.nonNullable.group({
    householdId: [""],
    recipientDisplayName: ["", Validators.required],
    householdSizeAtVisit: [1, Validators.min(1)],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.pantryId = this.route.snapshot.paramMap.get("pantryId") ?? "";
    this.loadAll();
  }

  canManage(): boolean {
    return this.context.hasAnyRole("FOOD_PANTRY_MANAGER");
  }

  location(pantry: PantryLocation): string {
    return [pantry.city, pantry.state].filter(Boolean).join(", ") || "—";
  }

  loadAll(): void {
    forkJoin({
      pantry: this.api.pantry(this.organizationId, this.pantryId),
      availability: this.api.availability(this.organizationId, this.pantryId),
      items: this.api.items(this.organizationId),
    }).subscribe({
      next: (result) => {
        this.pantry.set(result.pantry);
        this.availability.set(result.availability);
        this.items.set(result.items);
        this.loadInventory();
      },
      error: (error) => this.error.set(this.errors.from(error)),
    });
    if (this.canManage()) {
      forkJoin({
        distributions: this.api.distributions(
          this.organizationId,
          this.pantryId,
        ),
        households: this.api.households(this.organizationId),
      }).subscribe({
        next: (result) => {
          this.distributions.set(result.distributions);
          this.households.set(result.households);
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
    }
  }

  loadInventory(page = 0): void {
    this.loading.set(true);
    this.api.inventory(this.organizationId, this.pantryId, page).subscribe({
      next: (result) => {
        this.inventory.set(result);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(this.errors.from(error));
        this.loading.set(false);
      },
    });
  }

  pantryAction(action: "activate" | "deactivate"): void {
    this.api
      .pantryAction(this.organizationId, this.pantryId, action)
      .subscribe({
        next: (pantry) => this.pantry.set(pantry),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  receiveInventory(): void {
    if (this.receiptForm.invalid) return;
    const value = this.receiptForm.getRawValue();
    this.api
      .receipt(this.organizationId, this.pantryId, {
        itemId: value.itemId,
        quantity: value.quantityReceived,
        receivedDate: value.receivedDate,
        expirationDate: value.expirationDate || null,
        sourceReference: null,
        lotNumber: value.lotNumber || null,
        sourceType: value.sourceType,
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showReceipt.set(false);
          this.loadAll();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  createDistribution(): void {
    if (this.visitForm.invalid) return;
    const value = this.visitForm.getRawValue();
    this.api
      .createDistribution(this.organizationId, this.pantryId, {
        householdId: value.householdId || null,
        recipientName: value.recipientDisplayName || null,
        visitDateTime: new Date().toISOString(),
        householdSizeAtVisit: value.householdSizeAtVisit,
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showVisit.set(false);
          this.loadAll();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
