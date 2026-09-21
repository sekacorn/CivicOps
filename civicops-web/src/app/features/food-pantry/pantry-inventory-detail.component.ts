import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  InventoryAdjustmentType,
  InventoryLot,
  InventoryTransaction,
} from "../../core/models/food-pantry.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { FoodPantryApiService } from "./food-pantry-api.service";

@Component({
  selector: "cop-pantry-inventory-detail",
  imports: [ReactiveFormsModule, RouterLink, ApiErrorComponent],
  template: `
    <a class="back-link" [routerLink]="['..', '..']">Back to pantry</a>
    <cop-api-error [error]="error()" />
    @if (lot(); as lot) {
      <header class="page-header">
        <div>
          <p class="eyebrow">INVENTORY LOT</p>
          <h1>{{ lot.itemName }}</h1>
          <p>{{ lot.pantryName }} · remaining {{ lot.quantityRemaining }}</p>
        </div>
      </header>
      <section class="panel detail-grid">
        <div>
          <span>Lot</span><strong>{{ lot.lotNumber || "—" }}</strong>
        </div>
        <div>
          <span>Received</span><strong>{{ lot.receivedDate }}</strong>
        </div>
        <div>
          <span>Expires</span><strong>{{ lot.expirationDate || "—" }}</strong>
        </div>
        <div>
          <span>Source</span><strong>{{ lot.sourceType || "—" }}</strong>
        </div>
        <div>
          <span>Received qty</span><strong>{{ lot.quantityReceived }}</strong>
        </div>
        <div>
          <span>Remaining qty</span><strong>{{ lot.quantityRemaining }}</strong>
        </div>
      </section>
      @if (canManage()) {
        <form class="panel form-grid" [formGroup]="form" (ngSubmit)="adjust()">
          <h2>Adjust inventory</h2>
          <label
            >Type<select formControlName="adjustmentType">
              @for (type of adjustmentTypes; track type) {
                <option [value]="type">{{ type.replaceAll("_", " ") }}</option>
              }
            </select></label
          >
          <label
            >Quantity<input type="number" min="1" formControlName="quantity"
          /></label>
          <label>Notes<input formControlName="notes" /></label>
          <button class="button button--primary" [disabled]="form.invalid">
            Adjust
          </button>
        </form>
      }
      <section class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Type</th>
              <th>Quantity</th>
              <th>Occurred</th>
              <th>Notes</th>
            </tr>
          </thead>
          <tbody>
            @for (
              transaction of transactions()?.content ?? [];
              track transaction.id
            ) {
              <tr>
                <td>{{ transaction.transactionType }}</td>
                <td>{{ transaction.quantity }}</td>
                <td>{{ transaction.occurredAt }}</td>
                <td>{{ transaction.notes || "—" }}</td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  `,
  styles: `
    .detail-grid,
    .form-grid {
      display: grid;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .detail-grid {
      grid-template-columns: repeat(3, 1fr);
    }
    .form-grid {
      grid-template-columns: repeat(3, 1fr) auto;
      align-items: end;
    }
    .form-grid h2 {
      grid-column: 1/-1;
    }
    span {
      display: block;
      color: var(--ink-muted);
    }
    label {
      display: grid;
      gap: 0.3rem;
    }
    @media (max-width: 700px) {
      .detail-grid,
      .form-grid {
        grid-template-columns: 1fr;
      }
      .form-grid h2 {
        grid-column: auto;
      }
    }
  `,
})
export class PantryInventoryDetailComponent implements OnInit {
  private readonly api = inject(FoodPantryApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly lot = signal<InventoryLot | null>(null);
  readonly transactions = signal<PageResponse<InventoryTransaction> | null>(
    null,
  );
  readonly error = signal<ApiError | null>(null);
  organizationId = "";
  lotId = "";
  readonly adjustmentTypes: InventoryAdjustmentType[] = [
    "COUNT_INCREASE",
    "COUNT_DECREASE",
    "SPOILAGE",
    "EXPIRATION",
  ];
  readonly form = this.fb.nonNullable.group({
    adjustmentType: ["COUNT_DECREASE" as InventoryAdjustmentType],
    quantity: [1, Validators.min(1)],
    notes: [""],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.lotId = this.route.snapshot.paramMap.get("lotId") ?? "";
    this.load();
  }

  canManage(): boolean {
    return this.context.hasAnyRole("FOOD_PANTRY_MANAGER");
  }

  load(): void {
    forkJoin({
      lot: this.api.inventoryLot(this.organizationId, this.lotId),
      transactions: this.api.transactions(this.organizationId, this.lotId),
    }).subscribe({
      next: (result) => {
        this.lot.set(result.lot);
        this.transactions.set(result.transactions);
      },
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  adjust(): void {
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.api
      .adjustLot(this.organizationId, this.lotId, {
        ...value,
        notes: value.notes || null,
      })
      .subscribe({
        next: () => this.load(),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
