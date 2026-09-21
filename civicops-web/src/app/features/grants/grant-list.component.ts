import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import { GrantStatus, GrantSummary } from "../../core/models/grant.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { GrantApiService, GrantFilters } from "./grant-api.service";

@Component({
  selector: "cop-grant-list",
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
        <p class="eyebrow">FUNDING</p>
        <h1>Grants</h1>
        <p>Track prospects, awards, deadlines, and accountable spending.</p>
      </div>
      @if (canManage()) {
        <a class="button button--primary" [routerLink]="['new']"
          >Create grant</a
        >
      }
    </header>
    <form
      class="filters panel"
      [formGroup]="filters"
      (ngSubmit)="applyFilters()"
      aria-label="Grant filters"
    >
      <label
        >Grantor<input formControlName="grantor" placeholder="Search grantor"
      /></label>
      <label
        >Status<select formControlName="status">
          <option value="">All statuses</option>
          @for (status of statuses; track status) {
            <option [value]="status">{{ status.replaceAll("_", " ") }}</option>
          }
        </select></label
      >
      <label
        >Restriction<select formControlName="restricted">
          <option value="">All grants</option>
          <option value="true">Restricted</option>
          <option value="false">Unrestricted</option>
        </select></label
      >
      <div class="filter-actions">
        <button class="button button--secondary" type="submit">
          Apply filters</button
        ><button
          class="button button--quiet"
          type="button"
          (click)="resetFilters()"
        >
          Reset
        </button>
      </div>
    </form>
    <cop-api-error [error]="error()" />
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else {
      @if (page()?.content?.length === 0) {
        <cop-async-state
          state="empty"
          detail="No grants match the current filters."
        />
      } @else {
        @if (page(); as results) {
          <div class="table-wrap panel">
            <table>
              <thead>
                <tr>
                  <th>
                    <button
                      type="button"
                      class="sort"
                      (click)="sort('grantName')"
                    >
                      Grant
                    </button>
                  </th>
                  <th>Grantor</th>
                  <th>Status</th>
                  <th class="number">Award</th>
                  <th>
                    <button
                      type="button"
                      class="sort"
                      (click)="sort('endDate')"
                    >
                      End date
                    </button>
                  </th>
                </tr>
              </thead>
              <tbody>
                @for (grant of results.content; track grant.id) {
                  <tr>
                    <th scope="row">
                      @if (canManage()) {
                        <a [routerLink]="[grant.id]">{{ grant.grantName }}</a>
                      } @else {
                        {{ grant.grantName }}
                      }
                    </th>
                    <td>{{ grant.grantorName }}</td>
                    <td><cop-status-badge [status]="grant.status" /></td>
                    <td class="number">{{ money(grant.awardAmount) }}</td>
                    <td>{{ grant.endDate ?? "—" }}</td>
                  </tr>
                }
              </tbody>
            </table>
            <cop-pagination
              [page]="results.page"
              [totalPages]="results.totalPages"
              [totalElements]="results.totalElements"
              [first]="results.first"
              [last]="results.last"
              (pageChange)="load($event)"
            />
          </div>
        }
      }
    }
  `,
  styles: `
    .filters {
      display: grid;
      grid-template-columns: 2fr 1fr 1fr auto;
      gap: 1rem;
      align-items: end;
      margin-bottom: 1rem;
    }
    .filters label {
      display: grid;
      gap: 0.35rem;
    }
    .filter-actions {
      display: flex;
      gap: 0.35rem;
    }
    .sort {
      padding: 0;
      color: inherit;
      background: none;
      border: 0;
      font: inherit;
      cursor: pointer;
    }
    @media (max-width: 850px) {
      .filters {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 520px) {
      .filters {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class GrantListComponent implements OnInit {
  private readonly api = inject(GrantApiService);
  private readonly errors = inject(ApiErrorService);
  private readonly route = inject(ActivatedRoute);
  private readonly builder = inject(FormBuilder);
  readonly organizations = inject(OrganizationContextService);
  readonly statuses: GrantStatus[] = [
    "PROSPECT",
    "APPLICATION_IN_PROGRESS",
    "SUBMITTED",
    "AWARDED",
    "ACTIVE",
    "CLOSED",
    "REJECTED",
    "WITHDRAWN",
  ];
  readonly loading = signal(true);
  readonly error = signal<ApiError | null>(null);
  readonly page = signal<PageResponse<GrantSummary> | null>(null);
  readonly canManage = () => this.organizations.hasAnyRole("GRANT_MANAGER");
  readonly filters = this.builder.nonNullable.group({
    grantor: "",
    status: "",
    restricted: "",
  });
  private sortField = "grantName";
  private sortDirection: "asc" | "desc" = "asc";

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    const organizationId = this.route.snapshot.paramMap.get("organizationId");
    if (!organizationId) return;
    const value = this.filters.getRawValue();
    const request: GrantFilters = {
      page,
      size: 20,
      sort: this.sortField,
      direction: this.sortDirection,
      grantor: value.grantor,
      status: value.status as GrantStatus | "",
      restricted: value.restricted === "" ? null : value.restricted === "true",
    };
    this.loading.set(true);
    this.error.set(null);
    this.api.list(organizationId, request).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(this.errors.from(error));
        this.loading.set(false);
      },
    });
  }

  applyFilters(): void {
    this.load(0);
  }
  resetFilters(): void {
    this.filters.reset();
    this.load(0);
  }
  sort(field: string): void {
    this.sortDirection =
      this.sortField === field && this.sortDirection === "asc" ? "desc" : "asc";
    this.sortField = field;
    this.load(0);
  }
  money(value: number): string {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: "USD",
    }).format(value);
  }
}
