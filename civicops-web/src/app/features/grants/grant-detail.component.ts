import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  ExpenseCategory,
  GrantDetail,
  GrantExpense,
  GrantFinancialSummary,
  GrantStatus,
} from "../../core/models/grant.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { GrantApiService } from "./grant-api.service";
import { GrantReportingApiService } from "../grant-reporting/grant-reporting-api.service";
import { GrantReportSummary } from "../../core/models/grant-reporting.models";

interface LifecycleAction {
  label: string;
  endpoint: string;
  terminal?: boolean;
}

@Component({
  selector: "cop-grant-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
    StatusBadgeComponent,
  ],
  template: `
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else {
      @if (grant(); as record) {
        <header class="page-header">
          <div>
            <p class="eyebrow">GRANT</p>
            <h1>{{ record.grantName }}</h1>
            <p>
              {{ record.grantorName }}
              @if (record.grantNumber) {
                · {{ record.grantNumber }}
              }
            </p>
          </div>
          <div class="header-actions">
            <cop-status-badge [status]="record.status" />
            @if (record.status !== "CLOSED") {
              <a class="button button--secondary" routerLink="edit">Edit</a>
            }
          </div>
        </header>
        <cop-api-error [error]="error()" />
        @if (financial(); as money) {
          <section class="metrics" aria-label="Grant financial summary">
            <article>
              <span>Award amount</span
              ><strong>{{ currency(money.awardAmount) }}</strong>
            </article>
            <article>
              <span>Total spent</span
              ><strong>{{ currency(money.totalSpent) }}</strong>
            </article>
            <article>
              <span>Remaining</span
              ><strong>{{ currency(money.remainingBalance) }}</strong>
            </article>
            <article>
              <span>Utilization</span
              ><strong>{{ money.utilizationPercent }}%</strong>
            </article>
          </section>
        }
        <div class="detail-grid">
          <section class="panel">
            <h2>Grant details</h2>
            <dl>
              <div>
                <dt>Status</dt>
                <dd>{{ record.status.replaceAll("_", " ") }}</dd>
              </div>
              <div>
                <dt>Start date</dt>
                <dd>{{ record.startDate ?? "Not set" }}</dd>
              </div>
              <div>
                <dt>End date</dt>
                <dd>{{ record.endDate ?? "Not set" }}</dd>
              </div>
              <div>
                <dt>Reporting deadline</dt>
                <dd>{{ record.reportingDeadline ?? "Not set" }}</dd>
              </div>
              <div>
                <dt>Funding</dt>
                <dd>{{ record.restricted ? "Restricted" : "Unrestricted" }}</dd>
              </div>
              <div>
                <dt>Primary contact</dt>
                <dd>{{ record.primaryContactName ?? "Not set" }}</dd>
              </div>
            </dl>
            @if (record.description) {
              <h3>Description</h3>
              <p>{{ record.description }}</p>
            }
          </section>
          <section class="panel">
            <h2>Lifecycle</h2>
            <p>
              Use explicit actions to advance the grant. The backend remains
              authoritative for transition rules.
            </p>
            <div class="lifecycle-actions">
              @for (action of actions(record.status); track action.endpoint) {
                <button
                  class="button"
                  [class.button--danger]="action.terminal"
                  [class.button--primary]="!action.terminal"
                  type="button"
                  [disabled]="working()"
                  (click)="transition(action)"
                >
                  {{ action.label }}
                </button>
              }
              @if (actions(record.status).length === 0) {
                <p class="muted">No further transitions are available.</p>
              }
            </div>
          </section>
        </div>
        <section class="panel expenses">
          <div class="section-heading">
            <div>
              <h2>Expenses</h2>
              <p>Recorded expenses and balances come directly from CivicOps.</p>
            </div>
            @if (record.status === "ACTIVE") {
              <button
                class="button button--secondary"
                type="button"
                (click)="showExpenseForm.set(!showExpenseForm())"
              >
                {{ showExpenseForm() ? "Cancel" : "Add expense" }}
              </button>
            }
          </div>
          @if (showExpenseForm()) {
            <form
              class="expense-form"
              [formGroup]="expenseForm"
              (ngSubmit)="addExpense()"
            >
              <label
                >Amount<input
                  type="number"
                  min="0.01"
                  step="0.01"
                  formControlName="amount"
              /></label>
              <label
                >Date<input type="date" formControlName="expenseDate"
              /></label>
              <label
                >Category<select formControlName="category">
                  @for (category of categories; track category) {
                    <option [value]="category">
                      {{ category.replaceAll("_", " ") }}
                    </option>
                  }
                </select></label
              >
              <label class="wide"
                >Description<input formControlName="description"
              /></label>
              <label>Vendor<input formControlName="vendor" /></label
              ><label
                >Reference<input formControlName="referenceNumber"
              /></label>
              <div class="wide form-actions">
                <button
                  class="button button--primary"
                  type="submit"
                  [disabled]="working()"
                >
                  Record expense
                </button>
              </div>
            </form>
          }
          @if (expenses()?.content?.length === 0) {
            <cop-async-state
              state="empty"
              detail="No expenses have been recorded for this grant."
            />
          } @else {
            @if (expenses(); as page) {
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Category</th>
                      <th>Description</th>
                      <th>Vendor</th>
                      <th class="number">Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (expense of page.content; track expense.id) {
                      <tr>
                        <td>{{ expense.expenseDate }}</td>
                        <td>{{ expense.category.replaceAll("_", " ") }}</td>
                        <th scope="row">{{ expense.description }}</th>
                        <td>{{ expense.vendor ?? "—" }}</td>
                        <td class="number">{{ currency(expense.amount) }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          }
        </section>
        <section class="panel expenses">
          <div class="section-heading">
            <div>
              <h2>Grant reports</h2>
              <p>Evidence-backed reporting records for this grant.</p>
            </div>
            <a
              class="button button--secondary"
              [routerLink]="[
                '/organizations',
                organizationId,
                'grant-reporting',
              ]"
              >Create or view reports</a
            >
          </div>
          @for (report of reports()?.content ?? []; track report.id) {
            <p>
              <a
                [routerLink]="[
                  '/organizations',
                  organizationId,
                  'grant-reporting',
                  'reports',
                  report.id,
                ]"
                >{{ report.reportingPeriodStart }} –
                {{ report.reportingPeriodEnd }}</a
              >
              · {{ report.status.replaceAll("_", " ") }}
            </p>
          } @empty {
            <p class="muted">No reports have been created for this grant.</p>
          }
        </section>
      }
    }
  `,
  styles: `
    .header-actions,
    .section-heading {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .metrics {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1rem;
      margin: 1.5rem 0;
    }
    .metrics article {
      display: grid;
      gap: 0.4rem;
      padding: 1rem 1.15rem;
      background: white;
      border: 1px solid var(--line);
      border-radius: 0.65rem;
    }
    .metrics span,
    .muted {
      color: var(--ink-muted);
    }
    .metrics strong {
      font-size: 1.35rem;
    }
    .detail-grid {
      display: grid;
      grid-template-columns: 1.35fr 1fr;
      gap: 1rem;
    }
    h2 {
      margin-top: 0;
    }
    h3 {
      margin-bottom: 0.35rem;
      font-size: 1rem;
    }
    dl {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }
    dl div {
      display: grid;
      gap: 0.2rem;
    }
    dt {
      color: var(--ink-muted);
      font-size: 0.78rem;
    }
    dd {
      margin: 0;
      font-weight: 650;
    }
    .lifecycle-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-top: 1rem;
    }
    .expenses {
      margin-top: 1rem;
    }
    .section-heading {
      justify-content: space-between;
    }
    .section-heading h2,
    .section-heading p {
      margin: 0;
    }
    .section-heading p {
      margin-top: 0.25rem;
      color: var(--ink-muted);
    }
    .expense-form {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem 1rem;
      padding: 1rem;
      margin: 1rem 0;
      background: var(--canvas);
      border-radius: 0.5rem;
    }
    .expense-form label {
      display: grid;
      gap: 0.3rem;
    }
    .expense-form .wide {
      grid-column: 1 / -1;
    }
    .form-actions {
      display: flex;
      justify-content: flex-end;
    }
    @media (max-width: 800px) {
      .metrics {
        grid-template-columns: 1fr 1fr;
      }
      .detail-grid {
        grid-template-columns: 1fr;
      }
    }
    @media (max-width: 560px) {
      .metrics,
      .expense-form,
      dl {
        grid-template-columns: 1fr;
      }
      .expense-form .wide {
        grid-column: auto;
      }
      .header-actions {
        align-items: flex-end;
        flex-direction: column;
      }
    }
  `,
})
export class GrantDetailComponent implements OnInit {
  private readonly api = inject(GrantApiService);
  private readonly reportingApi = inject(GrantReportingApiService);
  private readonly errors = inject(ApiErrorService);
  private readonly route = inject(ActivatedRoute);
  private readonly builder = inject(FormBuilder);
  readonly organizations = inject(OrganizationContextService);
  readonly loading = signal(true);
  readonly working = signal(false);
  readonly showExpenseForm = signal(false);
  readonly error = signal<ApiError | null>(null);
  readonly grant = signal<GrantDetail | null>(null);
  readonly financial = signal<GrantFinancialSummary | null>(null);
  readonly expenses = signal<PageResponse<GrantExpense> | null>(null);
  readonly reports = signal<PageResponse<GrantReportSummary> | null>(null);
  readonly categories: ExpenseCategory[] = [
    "PERSONNEL",
    "SUPPLIES",
    "EQUIPMENT",
    "TRAVEL",
    "TRANSPORTATION",
    "PROGRAM_SERVICES",
    "FACILITIES",
    "CONTRACTORS",
    "ADMINISTRATIVE",
    "OTHER",
  ];
  organizationId = "";
  grantId = "";
  readonly expenseForm = this.builder.nonNullable.group({
    amount: [0, [Validators.required, Validators.min(0.01)]],
    expenseDate: ["", Validators.required],
    category: ["PERSONNEL" as ExpenseCategory, Validators.required],
    description: ["", [Validators.required, Validators.maxLength(500)]],
    vendor: ["", Validators.maxLength(200)],
    referenceNumber: ["", Validators.maxLength(100)],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.grantId = this.route.snapshot.paramMap.get("grantId") ?? "";
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      grant: this.api.detail(this.organizationId, this.grantId),
      financial: this.api.financialSummary(this.organizationId, this.grantId),
      expenses: this.api.expenses(this.organizationId, this.grantId),
      reports: this.reportingApi.reports(this.organizationId, this.grantId),
    }).subscribe({
      next: ({ grant, financial, expenses, reports }) => {
        this.grant.set(grant);
        this.financial.set(financial);
        this.expenses.set(expenses);
        this.reports.set(reports);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(this.errors.from(error));
        this.loading.set(false);
      },
    });
  }

  actions(status: GrantStatus): LifecycleAction[] {
    return (
      {
        PROSPECT: [
          { label: "Start application", endpoint: "start-application" },
          { label: "Withdraw", endpoint: "withdraw", terminal: true },
        ],
        APPLICATION_IN_PROGRESS: [
          { label: "Submit", endpoint: "submit" },
          { label: "Withdraw", endpoint: "withdraw", terminal: true },
        ],
        SUBMITTED: [
          { label: "Mark awarded", endpoint: "mark-awarded" },
          { label: "Reject", endpoint: "reject", terminal: true },
        ],
        AWARDED: [{ label: "Activate", endpoint: "activate" }],
        ACTIVE: [{ label: "Close grant", endpoint: "close", terminal: true }],
        CLOSED: [],
        REJECTED: [],
        WITHDRAWN: [],
      } as Record<GrantStatus, LifecycleAction[]>
    )[status];
  }

  transition(action: LifecycleAction): void {
    if (
      action.terminal &&
      !window.confirm(`${action.label}? This lifecycle change may be final.`)
    )
      return;
    this.working.set(true);
    this.error.set(null);
    this.api
      .transition(this.organizationId, this.grantId, action.endpoint)
      .subscribe({
        next: () => {
          this.working.set(false);
          this.reload();
        },
        error: (error) => {
          this.error.set(this.errors.from(error));
          this.working.set(false);
        },
      });
  }

  addExpense(): void {
    if (this.expenseForm.invalid) {
      this.expenseForm.markAllAsTouched();
      return;
    }
    const value = this.expenseForm.getRawValue();
    this.working.set(true);
    this.error.set(null);
    this.api
      .addExpense(this.organizationId, this.grantId, {
        ...value,
        vendor: value.vendor || null,
        referenceNumber: value.referenceNumber || null,
      })
      .subscribe({
        next: () => {
          this.working.set(false);
          this.showExpenseForm.set(false);
          this.expenseForm.reset({
            amount: 0,
            expenseDate: "",
            category: "PERSONNEL",
            description: "",
            vendor: "",
            referenceNumber: "",
          });
          this.reload();
        },
        error: (error) => {
          const problem = this.errors.from(error);
          this.errors.applyFieldErrors(this.expenseForm, problem);
          this.error.set(problem);
          this.working.set(false);
        },
      });
  }

  currency(value: number): string {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: "USD",
    }).format(value);
  }
}
