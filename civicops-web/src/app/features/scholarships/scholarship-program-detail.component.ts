import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  ApplicantSummary,
  ScholarshipApplicationStatus,
  ScholarshipApplicationSummary,
  ScholarshipProgram,
  ScholarshipProgramReport,
} from "../../core/models/scholarship.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { ScholarshipApiService } from "./scholarship-api.service";

@Component({
  selector: "cop-scholarship-program-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
    PaginationComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a class="back-link" [routerLink]="['..']">Back to scholarships</a>
    <cop-api-error [error]="error()" />
    @if (program(); as p) {
      <header class="page-header">
        <div>
          <p class="eyebrow">SCHOLARSHIP PROGRAM</p>
          <h1>{{ p.name }}</h1>
          <p>
            {{ p.academicYear || "No academic year" }} · deadline
            {{ p.applicationDeadline }}
          </p>
        </div>
        <cop-status-badge [status]="p.status" />
      </header>
      @if (canManage()) {
        <div class="actions">
          <button class="button button--secondary" (click)="action('open')">
            Open
          </button>
          <button class="button button--secondary" (click)="action('close')">
            Close
          </button>
          <button
            class="button button--secondary"
            (click)="action('start-review')"
          >
            Start review
          </button>
          <button
            class="button button--secondary"
            (click)="action('finalize-awards')"
          >
            Finalize awards
          </button>
          <button class="button button--quiet" (click)="action('cancel')">
            Cancel
          </button>
        </div>
      }
      @if (report(); as r) {
        <section class="metrics">
          <article>
            <span>Applications</span><strong>{{ r.applications }}</strong>
          </article>
          <article>
            <span>Submitted</span><strong>{{ r.submitted }}</strong>
          </article>
          <article>
            <span>Eligible</span><strong>{{ r.eligible }}</strong>
          </article>
          <article>
            <span>Finalists</span><strong>{{ r.finalists }}</strong>
          </article>
          <article>
            <span>Selected</span><strong>{{ r.selected }}</strong>
          </article>
          <article>
            <span>Awarded</span><strong>{{ money(r.totalAwardAmount) }}</strong>
          </article>
        </section>
      }
      @if (canManage()) {
        <form
          class="panel form-grid"
          [formGroup]="applicationForm"
          (ngSubmit)="createApplication()"
        >
          <h2>Create application</h2>
          <label
            >Applicant<select formControlName="applicantId">
              <option value="">Select applicant</option>
              @for (
                applicant of applicants()?.content ?? [];
                track applicant.id
              ) {
                <option [value]="applicant.id">
                  {{ applicant.displayName }}
                </option>
              }
            </select></label
          >
          <label
            >GPA<input
              type="number"
              min="0"
              max="4"
              step=".01"
              formControlName="gpa"
          /></label>
          <label
            >Requested amount<input
              type="number"
              min="0"
              step=".01"
              formControlName="requestedAmount"
          /></label>
          <label class="wide"
            >Personal statement<textarea
              formControlName="personalStatement"
            ></textarea>
          </label>
          <button
            class="button button--primary"
            [disabled]="applicationForm.invalid"
          >
            Create application
          </button>
        </form>
      }
      <form
        class="filters"
        [formGroup]="filters"
        (ngSubmit)="loadApplications(0)"
      >
        <label
          >Status<select formControlName="status">
            <option value="">All</option>
            @for (status of statuses; track status) {
              <option [value]="status">
                {{ status.replaceAll("_", " ") }}
              </option>
            }
          </select></label
        >
        <button class="button button--secondary">Apply</button>
      </form>
      @if (loading()) {
        <cop-async-state state="loading" />
      } @else if (applications()?.content?.length === 0) {
        <cop-async-state
          state="empty"
          detail="No applications are attached to this program."
        />
      } @else if (applications()) {
        @let page = applications()!;
        <section class="panel table-wrap">
          <table>
            <thead>
              <tr>
                <th>Applicant</th>
                <th>Status</th>
                <th>Submitted</th>
                <th>Eligible</th>
              </tr>
            </thead>
            <tbody>
              @for (application of page.content; track application.id) {
                <tr>
                  <th scope="row">
                    <a [routerLink]="['..', 'applications', application.id]">{{
                      application.applicantDisplayName
                    }}</a>
                  </th>
                  <td><cop-status-badge [status]="application.status" /></td>
                  <td>{{ application.submittedAt || "—" }}</td>
                  <td>{{ application.eligibilityConfirmed ? "Yes" : "No" }}</td>
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
            (pageChange)="loadApplications($event)"
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
    .metrics,
    .form-grid,
    .filters {
      display: grid;
      gap: 0.75rem;
    }
    .metrics {
      grid-template-columns: repeat(6, 1fr);
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
    .filters {
      grid-template-columns: 1fr auto;
      align-items: end;
      margin-bottom: 1rem;
    }
    .form-grid h2,
    .wide {
      grid-column: 1/-1;
    }
    label {
      display: grid;
      gap: 0.3rem;
    }
    @media (max-width: 900px) {
      .metrics {
        grid-template-columns: repeat(3, 1fr);
      }
      .form-grid {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 560px) {
      .metrics,
      .form-grid,
      .filters {
        grid-template-columns: 1fr;
      }
      .wide,
      .form-grid h2 {
        grid-column: auto;
      }
    }
  `,
})
export class ScholarshipProgramDetailComponent implements OnInit {
  private readonly api = inject(ScholarshipApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly program = signal<ScholarshipProgram | null>(null);
  readonly report = signal<ScholarshipProgramReport | null>(null);
  readonly applicants = signal<PageResponse<ApplicantSummary> | null>(null);
  readonly applications =
    signal<PageResponse<ScholarshipApplicationSummary> | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  organizationId = "";
  programId = "";
  readonly statuses: ScholarshipApplicationStatus[] = [
    "DRAFT",
    "SUBMITTED",
    "UNDER_REVIEW",
    "FINALIST",
    "SELECTED",
    "NOT_SELECTED",
    "WITHDRAWN",
  ];
  readonly filters = this.fb.nonNullable.group({
    status: ["" as ScholarshipApplicationStatus | ""],
  });
  readonly applicationForm = this.fb.nonNullable.group({
    applicantId: ["", Validators.required],
    gpa: [0],
    requestedAmount: [0],
    personalStatement: [""],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.programId = this.route.snapshot.paramMap.get("programId") ?? "";
    this.loadAll();
  }

  canManage(): boolean {
    return this.context.hasAnyRole("SCHOLARSHIP_MANAGER");
  }

  money(value: number): string {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: "USD",
    }).format(value);
  }

  loadAll(): void {
    forkJoin({
      program: this.api.program(this.organizationId, this.programId),
      report: this.api.programReport(this.organizationId, this.programId),
      applicants: this.api.applicants(this.organizationId),
    }).subscribe({
      next: (result) => {
        this.program.set(result.program);
        this.report.set(result.report);
        this.applicants.set(result.applicants);
        this.loadApplications();
      },
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  loadApplications(page = 0): void {
    this.loading.set(true);
    this.api
      .applications(this.organizationId, this.programId, {
        ...this.filters.getRawValue(),
        page,
      })
      .subscribe({
        next: (result) => {
          this.applications.set(result);
          this.loading.set(false);
        },
        error: (error) => {
          this.error.set(this.errors.from(error));
          this.loading.set(false);
        },
      });
  }

  action(
    action: "open" | "close" | "start-review" | "finalize-awards" | "cancel",
  ): void {
    this.api
      .programAction(this.organizationId, this.programId, action)
      .subscribe({
        next: (program) => {
          this.program.set(program);
          this.loadAll();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  createApplication(): void {
    if (this.applicationForm.invalid) return;
    const value = this.applicationForm.getRawValue();
    this.api
      .createApplication(this.organizationId, this.programId, {
        ...value,
        eligibilityConfirmed: false,
        eligibilityNotes: null,
        personalStatement: value.personalStatement || null,
        financialNeedStatement: null,
        gpa: value.gpa || null,
        householdIncome: null,
        requestedAmount: value.requestedAmount || null,
      })
      .subscribe({
        next: () => {
          this.applicationForm.reset();
          this.loadApplications();
          this.loadAll();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
