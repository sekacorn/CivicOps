import { Component, inject, OnInit, signal } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  DistributionSummary,
  HouseholdDetail,
} from "../../core/models/food-pantry.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { FoodPantryApiService } from "./food-pantry-api.service";

@Component({
  selector: "cop-pantry-household-detail",
  imports: [RouterLink, ApiErrorComponent, StatusBadgeComponent],
  template: `
    <a class="back-link" [routerLink]="['..', '..']">Back to food pantry</a>
    <cop-api-error [error]="error()" />
    @if (household(); as household) {
      <header class="page-header">
        <div>
          <p class="eyebrow">HOUSEHOLD</p>
          <h1>
            {{
              household.householdName ||
                household.primaryContactFirstName ||
                "Household"
            }}
          </h1>
          <p>Household size {{ household.householdSize }}</p>
        </div>
        <cop-status-badge [status]="household.active ? 'ACTIVE' : 'INACTIVE'" />
      </header>
      <div class="actions">
        <button class="button button--quiet" (click)="deactivate()">
          Deactivate
        </button>
      </div>
      <section class="panel detail-grid">
        <div>
          <span>Email</span><strong>{{ household.email || "—" }}</strong>
        </div>
        <div>
          <span>Phone</span><strong>{{ household.phone || "—" }}</strong>
        </div>
        <div>
          <span>Reference</span
          ><strong>{{ household.externalReferenceNumber || "—" }}</strong>
        </div>
        <div>
          <span>Address</span><strong>{{ household.address || "—" }}</strong>
        </div>
        <div>
          <span>Notes</span><strong>{{ household.notes || "—" }}</strong>
        </div>
      </section>
      <section class="panel table-wrap">
        <h2>Visits</h2>
        <table>
          <thead>
            <tr>
              <th>Recipient</th>
              <th>Date</th>
              <th>Status</th>
              <th>Size</th>
            </tr>
          </thead>
          <tbody>
            @for (visit of visits()?.content ?? []; track visit.id) {
              <tr>
                <th scope="row">{{ visit.recipientDisplayName }}</th>
                <td>{{ visit.visitDateTime }}</td>
                <td><cop-status-badge [status]="visit.status" /></td>
                <td>{{ visit.householdSizeAtVisit }}</td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  `,
  styles: `
    .actions {
      margin-bottom: 1rem;
    }
    .detail-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    span {
      display: block;
      color: var(--ink-muted);
    }
    @media (max-width: 700px) {
      .detail-grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class PantryHouseholdDetailComponent implements OnInit {
  private readonly api = inject(FoodPantryApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly errors = inject(ApiErrorService);
  readonly household = signal<HouseholdDetail | null>(null);
  readonly visits = signal<PageResponse<DistributionSummary> | null>(null);
  readonly error = signal<ApiError | null>(null);
  organizationId = "";
  householdId = "";

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.householdId = this.route.snapshot.paramMap.get("householdId") ?? "";
    this.load();
  }

  load(): void {
    forkJoin({
      household: this.api.household(this.organizationId, this.householdId),
      visits: this.api.householdVisits(this.organizationId, this.householdId),
    }).subscribe({
      next: (result) => {
        this.household.set(result.household);
        this.visits.set(result.visits);
      },
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  deactivate(): void {
    this.api
      .deactivateHousehold(this.organizationId, this.householdId)
      .subscribe({
        next: (household) => this.household.set(household),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
