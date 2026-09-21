import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import {
  DistributionDetail,
  InventoryAvailability,
} from "../../core/models/food-pantry.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { FoodPantryApiService } from "./food-pantry-api.service";

@Component({
  selector: "cop-pantry-distribution-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a class="back-link" [routerLink]="['..', '..']">Back to pantry</a>
    <cop-api-error [error]="error()" />
    @if (distribution(); as distribution) {
      <header class="page-header">
        <div>
          <p class="eyebrow">DISTRIBUTION VISIT</p>
          <h1>{{ distribution.recipientDisplayName }}</h1>
          <p>
            {{ distribution.pantryName }} · {{ distribution.visitDateTime }}
          </p>
        </div>
        <cop-status-badge [status]="distribution.status" />
      </header>
      <div class="actions">
        <button class="button button--secondary" (click)="action('complete')">
          Complete
        </button>
        <button class="button button--quiet" (click)="action('cancel')">
          Cancel
        </button>
      </div>
      <form
        class="panel form-grid"
        [formGroup]="itemForm"
        (ngSubmit)="addItem()"
      >
        <h2>Add distributed item</h2>
        <label
          >Item<select formControlName="itemId">
            <option value="">Select item</option>
            @for (item of availability(); track item.itemId) {
              <option [value]="item.itemId">
                {{ item.itemName }} · {{ item.availableQuantity }}
              </option>
            }
          </select></label
        >
        <label
          >Quantity<input type="number" min="1" formControlName="quantity"
        /></label>
        <button class="button button--primary" [disabled]="itemForm.invalid">
          Add item
        </button>
      </form>
      <section class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Item</th>
              <th>Quantity</th>
            </tr>
          </thead>
          <tbody>
            @for (item of distribution.items; track item.id) {
              <tr>
                <th scope="row">{{ item.itemName }}</th>
                <td>{{ item.quantity }}</td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  `,
  styles: `
    .actions {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr auto;
      gap: 0.75rem;
      align-items: end;
      margin-bottom: 1rem;
    }
    .form-grid h2 {
      grid-column: 1/-1;
    }
    label {
      display: grid;
      gap: 0.3rem;
    }
    @media (max-width: 650px) {
      .form-grid {
        grid-template-columns: 1fr;
      }
      .form-grid h2 {
        grid-column: auto;
      }
    }
  `,
})
export class PantryDistributionDetailComponent implements OnInit {
  private readonly api = inject(FoodPantryApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  readonly distribution = signal<DistributionDetail | null>(null);
  readonly availability = signal<InventoryAvailability[]>([]);
  readonly error = signal<ApiError | null>(null);
  organizationId = "";
  distributionId = "";
  readonly itemForm = this.fb.nonNullable.group({
    itemId: ["", Validators.required],
    quantity: [1, Validators.min(1)],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.distributionId =
      this.route.snapshot.paramMap.get("distributionId") ?? "";
    this.load();
  }

  load(): void {
    this.api.distribution(this.organizationId, this.distributionId).subscribe({
      next: (distribution) => {
        this.distribution.set(distribution);
        this.loadAvailability(distribution.pantryId);
      },
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  loadAvailability(pantryId: string): void {
    this.api.availability(this.organizationId, pantryId).subscribe({
      next: (availability) => this.availability.set(availability),
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  addItem(): void {
    if (this.itemForm.invalid) return;
    this.api
      .addDistributionItem(
        this.organizationId,
        this.distributionId,
        this.itemForm.getRawValue(),
      )
      .subscribe({
        next: (distribution) => this.distribution.set(distribution),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  action(action: "complete" | "cancel"): void {
    this.api
      .distributionAction(this.organizationId, this.distributionId, action)
      .subscribe({
        next: (distribution) => this.distribution.set(distribution),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
