import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  ReviewAssignment,
  ScholarshipApplicationDetail,
  ScholarshipDocument,
  ScholarshipDocumentType,
  ScholarshipReview,
} from "../../core/models/scholarship.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { ScholarshipApiService } from "./scholarship-api.service";

@Component({
  selector: "cop-scholarship-application-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a class="back-link" [routerLink]="['..', '..']">Back to program</a>
    <cop-api-error [error]="error()" />
    @if (application(); as application) {
      <header class="page-header">
        <div>
          <p class="eyebrow">SCHOLARSHIP APPLICATION</p>
          <h1>{{ application.applicant.displayName }}</h1>
          <p>
            {{ application.programName }} ·
            {{ application.applicant.schoolName || "No school" }}
          </p>
        </div>
        <cop-status-badge [status]="application.status" />
      </header>
      <section class="panel detail-grid">
        <div>
          <span>Email</span
          ><strong>{{
            canManage() ? application.applicant.email : "Restricted"
          }}</strong>
        </div>
        <div>
          <span>Phone</span
          ><strong>{{
            canManage() ? application.applicant.phone || "—" : "Restricted"
          }}</strong>
        </div>
        <div>
          <span>GPA</span><strong>{{ application.gpa ?? "—" }}</strong>
        </div>
        <div>
          <span>Requested</span
          ><strong>{{
            application.requestedAmount
              ? money(application.requestedAmount)
              : "—"
          }}</strong>
        </div>
        <div>
          <span>Eligibility</span
          ><strong>{{
            application.eligibilityConfirmed ? "Confirmed" : "Not confirmed"
          }}</strong>
        </div>
        <div>
          <span>Submitted</span
          ><strong>{{ application.submittedAt || "—" }}</strong>
        </div>
      </section>
      <section class="panel">
        <h2>Statements</h2>
        <p>{{ application.personalStatement || "No personal statement." }}</p>
        <p>
          {{
            application.financialNeedStatement || "No financial need statement."
          }}
        </p>
      </section>
      @if (canManage()) {
        <div class="actions">
          <button class="button button--secondary" (click)="action('submit')">
            Submit
          </button>
          <button class="button button--secondary" (click)="action('finalist')">
            Mark finalist
          </button>
          <button class="button button--secondary" (click)="action('select')">
            Select
          </button>
          <button
            class="button button--secondary"
            (click)="action('not-select')"
          >
            Not selected
          </button>
          <button class="button button--quiet" (click)="action('withdraw')">
            Withdraw
          </button>
        </div>
        <form
          class="panel form-grid"
          [formGroup]="reviewerForm"
          (ngSubmit)="assignReviewer()"
        >
          <h2>Assign reviewer</h2>
          <label
            >Reviewer user ID<input formControlName="reviewerUserId"
          /></label>
          <button
            class="button button--primary"
            [disabled]="reviewerForm.invalid"
          >
            Assign
          </button>
        </form>
        <form
          class="panel form-grid"
          [formGroup]="documentForm"
          (ngSubmit)="addDocument()"
        >
          <h2>Add document</h2>
          <label
            >Type<select formControlName="documentType">
              @for (type of documentTypes; track type) {
                <option [value]="type">{{ type.replaceAll("_", " ") }}</option>
              }
            </select></label
          >
          <label>File name<input formControlName="fileName" /></label>
          <label
            >Storage reference<input formControlName="externalStorageReference"
          /></label>
          <button
            class="button button--primary"
            [disabled]="documentForm.invalid"
          >
            Add document
          </button>
        </form>
        <form
          class="panel form-grid"
          [formGroup]="awardForm"
          (ngSubmit)="createAward()"
        >
          <h2>Create award</h2>
          <label
            >Amount<input
              type="number"
              min="0"
              step=".01"
              formControlName="amount"
          /></label>
          <label
            >Award date<input type="date" formControlName="awardDate"
          /></label>
          <label class="wide"
            >Notes<textarea formControlName="notes"></textarea>
          </label>
          <button class="button button--primary" [disabled]="awardForm.invalid">
            Create award
          </button>
        </form>
      }
      <section class="columns">
        <article class="panel">
          <h2>Reviewers</h2>
          <div class="stack-list">
            @for (
              assignment of assignments()?.content ?? [];
              track assignment.id
            ) {
              <span
                >{{ assignment.reviewerName }}
                <cop-status-badge [status]="assignment.status"
              /></span>
            }
          </div>
        </article>
        @if (canManage()) {
          <article class="panel">
            <h2>Documents</h2>
            <div class="stack-list">
              @for (document of documents()?.content ?? []; track document.id) {
                <button
                  type="button"
                  class="inline-row"
                  (click)="verifyDocument(document.id)"
                >
                  {{ document.fileName }} ·
                  {{ document.verified ? "Verified" : "Unverified" }}
                </button>
              }
            </div>
          </article>
          <article class="panel">
            <h2>Reviews</h2>
            <div class="stack-list">
              @for (review of reviews()?.content ?? []; track review.id) {
                <span>{{ review.score }} · {{ review.recommendation }}</span>
              }
            </div>
          </article>
        }
      </section>
    }
  `,
  styles: `
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    .detail-grid,
    .form-grid,
    .columns {
      display: grid;
      gap: 0.75rem;
    }
    .detail-grid {
      grid-template-columns: repeat(3, 1fr);
      margin-bottom: 1rem;
    }
    .detail-grid span {
      display: block;
      color: var(--ink-muted);
    }
    .form-grid {
      grid-template-columns: repeat(3, 1fr);
      margin-bottom: 1rem;
    }
    .columns {
      grid-template-columns: repeat(3, 1fr);
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
    .stack-list span,
    .inline-row {
      display: flex;
      justify-content: space-between;
      padding: 0.65rem 0;
      border: 0;
      border-bottom: 1px solid var(--line);
      background: none;
      text-align: left;
    }
    @media (max-width: 800px) {
      .detail-grid,
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
export class ScholarshipApplicationDetailComponent implements OnInit {
  private readonly api = inject(ScholarshipApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly application = signal<ScholarshipApplicationDetail | null>(null);
  readonly assignments = signal<PageResponse<ReviewAssignment> | null>(null);
  readonly documents = signal<PageResponse<ScholarshipDocument> | null>(null);
  readonly reviews = signal<PageResponse<ScholarshipReview> | null>(null);
  readonly error = signal<ApiError | null>(null);
  organizationId = "";
  applicationId = "";
  readonly documentTypes: ScholarshipDocumentType[] = [
    "TRANSCRIPT",
    "RECOMMENDATION",
    "ESSAY",
    "PROOF_OF_ENROLLMENT",
    "FINANCIAL_DOCUMENT",
    "OTHER",
  ];
  readonly reviewerForm = this.fb.nonNullable.group({
    reviewerUserId: ["", Validators.required],
  });
  readonly documentForm = this.fb.nonNullable.group({
    documentType: ["TRANSCRIPT" as ScholarshipDocumentType],
    fileName: ["", Validators.required],
    externalStorageReference: ["", Validators.required],
  });
  readonly awardForm = this.fb.nonNullable.group({
    amount: [0, Validators.min(1)],
    awardDate: [new Date().toISOString().slice(0, 10), Validators.required],
    notes: [""],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.applicationId =
      this.route.snapshot.paramMap.get("applicationId") ?? "";
    this.load();
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

  load(): void {
    forkJoin({
      application: this.api.application(
        this.organizationId,
        this.applicationId,
      ),
      assignments: this.api.assignments(
        this.organizationId,
        this.applicationId,
      ),
      documents: this.api.documents(this.organizationId, this.applicationId),
      reviews: this.api.reviews(this.organizationId, this.applicationId),
    }).subscribe({
      next: (result) => {
        this.application.set(result.application);
        this.assignments.set(result.assignments);
        this.documents.set(result.documents);
        this.reviews.set(result.reviews);
      },
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  action(
    action: "submit" | "withdraw" | "finalist" | "select" | "not-select",
  ): void {
    this.api
      .applicationAction(this.organizationId, this.applicationId, action)
      .subscribe({
        next: (application) => this.application.set(application),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  assignReviewer(): void {
    if (this.reviewerForm.invalid) return;
    this.api
      .assignReviewer(
        this.organizationId,
        this.applicationId,
        this.reviewerForm.getRawValue(),
      )
      .subscribe({
        next: () => {
          this.reviewerForm.reset();
          this.load();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  addDocument(): void {
    if (this.documentForm.invalid) return;
    this.api
      .addDocument(
        this.organizationId,
        this.applicationId,
        this.documentForm.getRawValue(),
      )
      .subscribe({
        next: () => {
          this.documentForm.reset({
            documentType: "TRANSCRIPT",
            fileName: "",
            externalStorageReference: "",
          });
          this.load();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  verifyDocument(documentId: string): void {
    this.api
      .verifyDocument(this.organizationId, this.applicationId, documentId)
      .subscribe({
        next: () => this.load(),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  createAward(): void {
    if (this.awardForm.invalid) return;
    const value = this.awardForm.getRawValue();
    this.api
      .createAward(this.organizationId, this.applicationId, {
        ...value,
        notes: value.notes || null,
      })
      .subscribe({
        next: () => this.load(),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
