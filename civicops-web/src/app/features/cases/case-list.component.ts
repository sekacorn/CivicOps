import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin, of } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  CasePriority,
  CaseReportSummary,
  CaseServiceReport,
  CaseStatus,
  CaseSummary,
  CaseType,
  CaseWorkload,
  ClientSummary,
} from "../../core/models/case.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { CaseApiService } from "./case-api.service";

@Component({
  selector: "cop-case-list",
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
        <p class="eyebrow">CASE MANAGEMENT</p>
        <h1>Cases</h1>
        <p>Privacy-aware client services, tasks, notes, and outcomes.</p>
      </div>
    </header>
    <cop-api-error [error]="error()" />
    @if (canReport() && report(); as r) {
      <section class="metrics" aria-label="Case report summary">
        <article>
          <span>Active clients</span><strong>{{ r.activeClients }}</strong>
        </article>
        <article>
          <span>Open</span><strong>{{ r.openCases }}</strong>
        </article>
        <article>
          <span>In progress</span><strong>{{ r.inProgressCases }}</strong>
        </article>
        <article>
          <span>On hold</span><strong>{{ r.onHoldCases }}</strong>
        </article>
        <article>
          <span>Closed in period</span
          ><strong>{{ r.casesClosedInPeriod }}</strong>
        </article>
        <article>
          <span>Overdue tasks</span><strong>{{ r.overdueTasks }}</strong>
        </article>
      </section>
      <section class="report-columns">
        <article class="panel">
          <h2>Services by type</h2>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Service</th>
                  <th>Records</th>
                  <th>Value</th>
                </tr>
              </thead>
              <tbody>
                @for (s of serviceReport(); track s.serviceType) {
                  <tr>
                    <th scope="row">{{ label(s.serviceType) }}</th>
                    <td>{{ s.serviceCount }}</td>
                    <td>{{ s.totalValueAmount }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </article>
        @if (canManage()) {
          <article class="panel">
            <h2>Worker workload</h2>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Worker</th>
                    <th>Open</th>
                    <th>In progress</th>
                    <th>On hold</th>
                  </tr>
                </thead>
                <tbody>
                  @for (w of workload(); track w.userId) {
                    <tr>
                      <th scope="row">{{ w.displayName }}</th>
                      <td>{{ w.openCases }}</td>
                      <td>{{ w.inProgressCases }}</td>
                      <td>{{ w.onHoldCases }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </article>
        }
      </section>
    }
    @if (reportOnly()) {
      <section class="panel">
        <h2>Aggregate reporting</h2>
        <p>
          This role receives only PII-free Case reporting. Client names, case
          notes, tasks, and service records are not requested.
        </p>
      </section>
    } @else {
      @if (canManage()) {
        <div class="actions">
          <button
            class="button button--secondary"
            type="button"
            (click)="showClient.set(!showClient())"
          >
            New client</button
          ><button
            class="button button--primary"
            type="button"
            (click)="showCase.set(!showCase())"
          >
            New case
          </button>
        </div>
        @if (showClient()) {
          <form
            class="panel form-grid"
            [formGroup]="clientForm"
            (ngSubmit)="createClient()"
          >
            <h2>Create client</h2>
            <label for="client-first"
              >First name<input
                id="client-first"
                formControlName="firstName"
                [attr.aria-invalid]="clientForm.controls.firstName.invalid"
            /></label>
            <label for="client-last"
              >Last name<input
                id="client-last"
                formControlName="lastName"
                [attr.aria-invalid]="clientForm.controls.lastName.invalid"
            /></label>
            <label
              >Preferred name<input formControlName="preferredName" /></label
            ><label
              >External reference<input
                formControlName="externalReferenceNumber"
            /></label>
            <label>Email<input type="email" formControlName="email" /></label
            ><label>Phone<input formControlName="phone" /></label>
            <button
              class="button button--primary"
              [disabled]="clientForm.invalid"
            >
              Create client
            </button>
          </form>
        }
        @if (showCase()) {
          <form
            class="panel form-grid"
            [formGroup]="caseForm"
            (ngSubmit)="createCase()"
          >
            <h2>Create case</h2>
            <label
              >Client<select formControlName="clientId">
                <option value="">Select client</option>
                @for (c of clients()?.content ?? []; track c.id) {
                  <option [value]="c.id">
                    {{ c.displayName }} ·
                    {{ c.externalReferenceNumber || "No reference" }}
                  </option>
                }
              </select></label
            >
            <label>Case number<input formControlName="caseNumber" /></label
            ><label>Title<input formControlName="title" /></label>
            <label
              >Type<select formControlName="caseType">
                @for (t of types; track t) {
                  <option [value]="t">{{ label(t) }}</option>
                }
              </select></label
            >
            <label
              >Priority<select formControlName="priority">
                @for (p of priorities; track p) {
                  <option [value]="p">{{ label(p) }}</option>
                }
              </select></label
            >
            <label
              >Opened date<input
                type="date"
                formControlName="openedDate" /></label
            ><label class="wide"
              >Description<textarea formControlName="description"></textarea>
            </label>
            <button
              class="button button--primary"
              [disabled]="caseForm.invalid"
            >
              Create case
            </button>
          </form>
        }
      }
      <form class="filters" [formGroup]="filters" (ngSubmit)="loadCases(0)">
        <label
          >Status<select formControlName="status">
            <option value="">All</option>
            @for (s of statuses; track s) {
              <option [value]="s">{{ label(s) }}</option>
            }
          </select></label
        ><label
          >Priority<select formControlName="priority">
            <option value="">All</option>
            @for (p of priorities; track p) {
              <option [value]="p">{{ label(p) }}</option>
            }
          </select></label
        ><label
          >Opened from<input type="date" formControlName="openedFrom" /></label
        ><label>Opened to<input type="date" formControlName="openedTo" /></label
        ><button class="button button--secondary">Apply</button>
      </form>
      @if (loading()) {
        <cop-async-state state="loading" />
      } @else if (cases()?.content?.length === 0) {
        <cop-async-state state="empty" detail="No cases match these filters." />
      } @else if (cases()) {
        @let page = cases()!;
        <section class="panel table-wrap">
          <table>
            <thead>
              <tr>
                <th>Case</th>
                <th>Client</th>
                <th>Type</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Opened</th>
              </tr>
            </thead>
            <tbody>
              @for (item of page.content; track item.id) {
                <tr>
                  <th scope="row">
                    <a [routerLink]="[item.id]"
                      >{{ item.caseNumber }} · {{ item.title }}</a
                    >
                  </th>
                  <td>{{ item.clientDisplayName }}</td>
                  <td>{{ label(item.caseType) }}</td>
                  <td>{{ item.priority }}</td>
                  <td><cop-status-badge [status]="item.status" /></td>
                  <td>{{ item.openedDate }}</td>
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
            (pageChange)="loadCases($event)"
          />
        </section>
      }
      @if (canManage()) {
        <section class="panel clients">
          <h2>Client directory</h2>
          <p class="privacy">
            Privacy-safe summaries only. Contact information is never included
            in this table.
          </p>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Client</th>
                  <th>Reference</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                @for (c of clients()?.content ?? []; track c.id) {
                  <tr>
                    <th scope="row">
                      <a [routerLink]="['clients', c.id]">{{
                        c.displayName
                      }}</a>
                    </th>
                    <td>{{ c.externalReferenceNumber || "—" }}</td>
                    <td>
                      <cop-status-badge
                        [status]="c.active ? 'ACTIVE' : 'INACTIVE'"
                      />
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }
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
      font-size: 1.35rem;
    }
    .actions {
      display: flex;
      gap: 0.6rem;
      margin-bottom: 1rem;
    }
    .report-columns {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      margin-bottom: 1rem;
    }
    .form-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .form-grid h2,
    .wide {
      grid-column: 1/-1;
    }
    .form-grid label,
    .filters label {
      display: grid;
      gap: 0.3rem;
    }
    .filters {
      display: grid;
      grid-template-columns: repeat(4, 1fr) auto;
      align-items: end;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .clients {
      margin-top: 1rem;
    }
    .privacy {
      color: var(--ink-muted);
    }
    @media (max-width: 1000px) {
      .metrics {
        grid-template-columns: repeat(3, 1fr);
      }
      .form-grid,
      .filters,
      .report-columns {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 560px) {
      .metrics,
      .form-grid,
      .filters,
      .report-columns {
        grid-template-columns: 1fr;
      }
      .form-grid h2,
      .wide {
        grid-column: auto;
      }
      .actions {
        flex-direction: column;
      }
    }
  `,
})
export class CaseListComponent implements OnInit {
  private readonly api = inject(CaseApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly cases = signal<PageResponse<CaseSummary> | null>(null);
  readonly clients = signal<PageResponse<ClientSummary> | null>(null);
  readonly report = signal<CaseReportSummary | null>(null);
  readonly serviceReport = signal<CaseServiceReport[]>([]);
  readonly workload = signal<CaseWorkload[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(false);
  readonly showClient = signal(false);
  readonly showCase = signal(false);
  organizationId = "";
  readonly statuses: CaseStatus[] = [
    "OPEN",
    "IN_PROGRESS",
    "ON_HOLD",
    "CLOSED",
    "CANCELLED",
  ];
  readonly priorities: CasePriority[] = ["LOW", "NORMAL", "HIGH", "URGENT"];
  readonly types: CaseType[] = [
    "GENERAL_ASSISTANCE",
    "HOUSING",
    "FOOD_ASSISTANCE",
    "EMPLOYMENT",
    "EDUCATION",
    "FINANCIAL_ASSISTANCE",
    "COMMUNITY_SERVICES",
    "OTHER",
  ];
  readonly filters = this.fb.nonNullable.group({
    status: ["" as CaseStatus | ""],
    priority: ["" as CasePriority | ""],
    openedFrom: [""],
    openedTo: [""],
  });
  readonly clientForm = this.fb.nonNullable.group({
    firstName: ["", Validators.required],
    lastName: ["", Validators.required],
    preferredName: [""],
    externalReferenceNumber: [""],
    email: ["", Validators.email],
    phone: [""],
  });
  readonly caseForm = this.fb.nonNullable.group({
    clientId: ["", Validators.required],
    caseNumber: ["", Validators.required],
    title: ["", Validators.required],
    caseType: ["GENERAL_ASSISTANCE" as CaseType],
    priority: ["NORMAL" as CasePriority],
    openedDate: [new Date().toISOString().slice(0, 10)],
    description: [""],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    if (this.reportOnly()) {
      this.loadReport();
      return;
    }
    this.loadCases();
    if (this.canManage()) this.loadClients();
    if (this.canReport()) this.loadReport();
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "CASE_MANAGER");
  }
  canReport() {
    return this.context.hasAnyRole(
      "ORG_ADMIN",
      "CASE_MANAGER",
      "PROGRAM_MANAGER",
    );
  }
  reportOnly() {
    return this.context.hasAnyRole("PROGRAM_MANAGER") && !this.canManage();
  }
  label(v: string) {
    return v.replaceAll("_", " ");
  }
  loadCases(page = 0) {
    this.loading.set(true);
    this.api
      .cases(this.organizationId, { ...this.filters.getRawValue(), page })
      .subscribe({
        next: (r) => {
          this.cases.set(r);
          this.loading.set(false);
        },
        error: (e) => {
          this.error.set(this.errors.from(e));
          this.loading.set(false);
        },
      });
  }
  loadClients() {
    this.api.clients(this.organizationId).subscribe({
      next: (r) => this.clients.set(r),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  loadReport() {
    const request = this.canManage()
      ? forkJoin({
          report: this.api.report(this.organizationId),
          services: this.api.serviceReport(this.organizationId),
          workload: this.api.workload(this.organizationId),
        })
      : forkJoin({
          report: this.api.report(this.organizationId),
          services: this.api.serviceReport(this.organizationId),
          workload: of([] as CaseWorkload[]),
        });
    request.subscribe({
      next: (r) => {
        this.report.set(r.report);
        this.serviceReport.set(r.services);
        this.workload.set(r.workload);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  createClient() {
    if (this.clientForm.invalid) return;
    const v = this.clientForm.getRawValue();
    this.api
      .createClient(this.organizationId, {
        ...v,
        preferredName: v.preferredName || null,
        externalReferenceNumber: v.externalReferenceNumber || null,
        dateOfBirth: null,
        email: v.email || null,
        phone: v.phone || null,
        addressLine1: null,
        addressLine2: null,
        city: null,
        state: null,
        postalCode: null,
        country: null,
        preferredContactMethod: null,
      })
      .subscribe({
        next: () => {
          this.showClient.set(false);
          this.clientForm.reset();
          this.loadClients();
          this.loadReport();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  createCase() {
    if (this.caseForm.invalid) return;
    const v = this.caseForm.getRawValue();
    this.api
      .createCase(this.organizationId, {
        ...v,
        description: v.description || null,
        programName: null,
        intakeSource: null,
      })
      .subscribe({
        next: () => {
          this.showCase.set(false);
          this.loadCases();
          this.loadReport();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
}
