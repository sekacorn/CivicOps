import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  ApplicantSummary,
  ReviewAssignment,
  ScholarshipAward,
  ScholarshipProgram,
  ScholarshipProgramStatus,
  ScholarshipReviewReport,
  ScholarshipSummary,
} from "../../core/models/scholarship.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { ScholarshipApiService } from "./scholarship-api.service";

@Component({
  selector: "cop-scholarship-list",
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
        <p class="eyebrow">SCHOLARSHIPS</p>
        <h1>Scholarship programs</h1>
        <p>Programs, applications, reviews, awards, and reviewer queues.</p>
      </div>
      @if (canManage()) {
        <button
          class="button button--primary"
          (click)="showProgram.set(!showProgram())"
        >
          New program
        </button>
      }
    </header>
    <cop-api-error [error]="error()" />
    @if (summary(); as s) {
      <section class="metrics" aria-label="Scholarship summary">
        <article>
          <span>Active programs</span><strong>{{ s.activePrograms }}</strong>
        </article>
        <article>
          <span>Submitted</span><strong>{{ s.applicationsSubmitted }}</strong>
        </article>
        <article>
          <span>Under review</span
          ><strong>{{ s.applicationsUnderReview }}</strong>
        </article>
        <article>
          <span>Finalists</span><strong>{{ s.finalists }}</strong>
        </article>
        <article>
          <span>Selected</span><strong>{{ s.selectedApplicants }}</strong>
        </article>
        <article>
          <span>Total awarded</span><strong>{{ money(s.totalAwarded) }}</strong>
        </article>
      </section>
    }
    @if (canManage() && showProgram()) {
      <form
        class="panel form-grid"
        [formGroup]="programForm"
        (ngSubmit)="createProgram()"
      >
        <h2>Create program</h2>
        <label>Name<input formControlName="name" /></label>
        <label
          >Academic year<input
            formControlName="academicYear"
            placeholder="2026-2027"
        /></label>
        <label
          >Award amount<input
            type="number"
            min="0"
            step=".01"
            formControlName="awardAmount"
        /></label>
        <label
          >Number of awards<input
            type="number"
            min="1"
            formControlName="numberOfAwards"
        /></label>
        <label
          >Opens<input type="date" formControlName="applicationOpenDate"
        /></label>
        <label
          >Deadline<input type="date" formControlName="applicationDeadline"
        /></label>
        <label class="wide"
          >Eligibility<textarea
            formControlName="eligibilityDescription"
          ></textarea>
        </label>
        <label class="wide"
          >Description<textarea formControlName="description"></textarea>
        </label>
        <button class="button button--primary" [disabled]="programForm.invalid">
          Create program
        </button>
      </form>
    }
    @if (canManage() && showApplicant()) {
      <form
        class="panel form-grid"
        [formGroup]="applicantForm"
        (ngSubmit)="createApplicant()"
      >
        <h2>Create applicant</h2>
        <label>First name<input formControlName="firstName" /></label>
        <label>Last name<input formControlName="lastName" /></label>
        <label>Email<input formControlName="email" /></label>
        <label>Phone<input formControlName="phone" /></label>
        <label>School<input formControlName="schoolName" /></label>
        <label
          >Graduation year<input type="number" formControlName="graduationYear"
        /></label>
        <button
          class="button button--primary"
          [disabled]="applicantForm.invalid"
        >
          Create applicant
        </button>
      </form>
    }
    <form class="filters" [formGroup]="filters" (ngSubmit)="loadPrograms(0)">
      <label
        >Status<select formControlName="status">
          <option value="">All statuses</option>
          @for (status of statuses; track status) {
            <option [value]="status">{{ label(status) }}</option>
          }
        </select></label
      >
      <label>Academic year<input formControlName="academicYear" /></label>
      <button class="button button--secondary">Apply</button>
      @if (canManage()) {
        <button
          type="button"
          class="button button--quiet"
          (click)="showApplicant.set(!showApplicant())"
        >
          New applicant
        </button>
      }
    </form>
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (programs()?.content?.length === 0) {
      <cop-async-state
        state="empty"
        detail="No scholarship programs match these filters."
      />
    } @else if (programs()) {
      @let p = programs()!;
      <section class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Program</th>
              <th>Year</th>
              <th>Deadline</th>
              <th>Status</th>
              <th>Awards</th>
            </tr>
          </thead>
          <tbody>
            @for (program of p.content; track program.id) {
              <tr>
                <th scope="row">
                  <a [routerLink]="[program.id]">{{ program.name }}</a>
                </th>
                <td>{{ program.academicYear || "—" }}</td>
                <td>{{ program.applicationDeadline }}</td>
                <td><cop-status-badge [status]="program.status" /></td>
                <td>
                  {{ program.numberOfAwards ?? "—" }} ·
                  {{ program.awardAmount ? money(program.awardAmount) : "—" }}
                </td>
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
          (pageChange)="loadPrograms($event)"
        />
      </section>
    }
    <section class="columns">
      @if (canReview()) {
        <article class="panel">
          <h2>My review queue</h2>
          @if ((reviews()?.content?.length ?? 0) === 0) {
            <p class="empty-state">No review assignments are waiting.</p>
          } @else {
            <div class="stack-list">
              @for (
                assignment of reviews()?.content ?? [];
                track assignment.id
              ) {
                <a [routerLink]="['reviews', assignment.id]">
                  <span>{{ assignment.reviewerName }}</span>
                  <cop-status-badge [status]="assignment.status" />
                </a>
              }
            </div>
          }
        </article>
      }
      @if (canManage()) {
        <article class="panel">
          <h2>Applicants</h2>
          <div class="stack-list">
            @for (
              applicant of applicants()?.content ?? [];
              track applicant.id
            ) {
              <span
                >{{ applicant.displayName }} ·
                {{ applicant.schoolName || "No school" }}</span
              >
            }
          </div>
        </article>
        <article class="panel">
          <h2>Awards</h2>
          <div class="stack-list">
            @for (award of awards()?.content ?? []; track award.id) {
              <a [routerLink]="['awards', award.id]">
                <span
                  >{{ award.applicantDisplayName }} ·
                  {{ money(award.amount) }}</span
                >
                <cop-status-badge [status]="award.status" />
              </a>
            }
          </div>
        </article>
      }
      @if (reviewReport(); as report) {
        <article class="panel">
          <h2>Reviews</h2>
          <p>
            <strong>{{ report.reviewsCompleted }}</strong> completed ·
            <strong>{{ report.outstandingReviews }}</strong> outstanding ·
            average
            <strong>{{ report.averageScore || 0 }}</strong>
          </p>
        </article>
      }
    </section>
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
      font-size: 1.25rem;
    }
    .form-grid,
    .filters,
    .columns {
      display: grid;
      gap: 0.75rem;
    }
    .form-grid {
      grid-template-columns: repeat(3, 1fr);
      margin-bottom: 1rem;
    }
    .filters {
      grid-template-columns: 1fr 1fr auto auto;
      align-items: end;
      margin-bottom: 1rem;
    }
    .columns {
      grid-template-columns: repeat(3, 1fr);
      margin-top: 1rem;
    }
    .form-grid h2,
    .wide {
      grid-column: 1/-1;
    }
    label {
      display: grid;
      gap: 0.3rem;
    }
    .stack-list {
      display: grid;
      gap: 0.5rem;
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
    @media (max-width: 960px) {
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
      .filters,
      .columns {
        grid-template-columns: 1fr;
      }
      .form-grid h2,
      .wide {
        grid-column: auto;
      }
    }
  `,
})
export class ScholarshipListComponent implements OnInit {
  private readonly api = inject(ScholarshipApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly programs = signal<PageResponse<ScholarshipProgram> | null>(null);
  readonly applicants = signal<PageResponse<ApplicantSummary> | null>(null);
  readonly reviews = signal<PageResponse<ReviewAssignment> | null>(null);
  readonly awards = signal<PageResponse<ScholarshipAward> | null>(null);
  readonly summary = signal<ScholarshipSummary | null>(null);
  readonly reviewReport = signal<ScholarshipReviewReport | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  readonly showProgram = signal(false);
  readonly showApplicant = signal(false);
  organizationId = "";
  readonly statuses: ScholarshipProgramStatus[] = [
    "DRAFT",
    "OPEN",
    "CLOSED",
    "REVIEWING",
    "AWARDED",
    "CANCELLED",
  ];
  readonly filters = this.fb.nonNullable.group({
    status: ["" as ScholarshipProgramStatus | ""],
    academicYear: [""],
  });
  readonly programForm = this.fb.nonNullable.group({
    name: ["", Validators.required],
    academicYear: [""],
    awardAmount: [0],
    numberOfAwards: [1],
    applicationOpenDate: [
      new Date().toISOString().slice(0, 10),
      Validators.required,
    ],
    applicationDeadline: [
      new Date().toISOString().slice(0, 10),
      Validators.required,
    ],
    eligibilityDescription: [""],
    description: [""],
  });
  readonly applicantForm = this.fb.nonNullable.group({
    firstName: ["", Validators.required],
    lastName: ["", Validators.required],
    email: ["", [Validators.required, Validators.email]],
    phone: [""],
    schoolName: [""],
    graduationYear: [new Date().getFullYear()],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.loadPrograms();
    this.loadSupportingData();
  }

  canManage(): boolean {
    return this.context.hasAnyRole("SCHOLARSHIP_MANAGER");
  }

  canReview(): boolean {
    return this.context.hasAnyRole("SCHOLARSHIP_REVIEWER");
  }

  label(value: string): string {
    return value.replaceAll("_", " ");
  }

  money(value: number): string {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: "USD",
    }).format(value);
  }

  loadPrograms(page = 0): void {
    this.loading.set(true);
    this.api
      .programs(this.organizationId, { ...this.filters.getRawValue(), page })
      .subscribe({
        next: (result) => {
          this.programs.set(result);
          this.loading.set(false);
        },
        error: (error) => {
          this.error.set(this.errors.from(error));
          this.loading.set(false);
        },
      });
  }

  loadSupportingData(): void {
    const calls = {
      summary: this.api.summary(this.organizationId),
      reviews: this.api.myReviews(this.organizationId),
      reviewReport: this.api.reviewReport(this.organizationId),
      applicants: this.api.applicants(this.organizationId),
      awards: this.api.awards(this.organizationId),
    };
    forkJoin(calls).subscribe({
      next: (result) => {
        this.summary.set(result.summary);
        this.reviews.set(result.reviews);
        this.reviewReport.set(result.reviewReport);
        this.applicants.set(result.applicants);
        this.awards.set(result.awards);
      },
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  createProgram(): void {
    if (this.programForm.invalid) return;
    const value = this.programForm.getRawValue();
    this.api
      .createProgram(this.organizationId, {
        ...value,
        academicYear: value.academicYear || null,
        awardAmount: value.awardAmount || null,
        numberOfAwards: value.numberOfAwards || null,
        eligibilityDescription: value.eligibilityDescription || null,
        description: value.description || null,
      })
      .subscribe({
        next: () => {
          this.showProgram.set(false);
          this.loadPrograms();
          this.loadSupportingData();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  createApplicant(): void {
    if (this.applicantForm.invalid) return;
    const value = this.applicantForm.getRawValue();
    this.api
      .createApplicant(this.organizationId, {
        ...value,
        userId: null,
        preferredName: null,
        phone: value.phone || null,
        dateOfBirth: null,
        address: null,
        studentId: null,
        schoolName: value.schoolName || null,
        graduationYear: value.graduationYear || null,
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showApplicant.set(false);
          this.loadSupportingData();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
