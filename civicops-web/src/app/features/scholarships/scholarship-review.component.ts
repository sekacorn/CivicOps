import { Component, inject, OnInit, signal } from "@angular/core";
import { JsonPipe } from "@angular/common";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import {
  ReviewAssignment,
  ReviewRecommendation,
} from "../../core/models/scholarship.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { ScholarshipApiService } from "./scholarship-api.service";

@Component({
  selector: "cop-scholarship-review",
  imports: [
    JsonPipe,
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a class="back-link" [routerLink]="['..', '..']">Back to scholarships</a>
    <cop-api-error [error]="error()" />
    <header class="page-header">
      <div>
        <p class="eyebrow">SCHOLARSHIP REVIEW</p>
        <h1>Reviewer workspace</h1>
        <p>Reviewer-safe application materials and scoring.</p>
      </div>
      @if (assignment(); as a) {
        <cop-status-badge [status]="a.status" />
      }
    </header>
    <section class="panel">
      <h2>Application</h2>
      <pre>{{ application() | json }}</pre>
    </section>
    <div class="actions">
      <button class="button button--secondary" (click)="start()">
        Start review
      </button>
    </div>
    <form class="panel form-grid" [formGroup]="form" (ngSubmit)="submit()">
      <h2>Submit review</h2>
      <label
        >Score<input type="number" min="0" max="100" formControlName="score"
      /></label>
      <label
        >Recommendation<select formControlName="recommendation">
          @for (r of recommendations; track r) {
            <option [value]="r">{{ r.replaceAll("_", " ") }}</option>
          }
        </select></label
      >
      <label class="wide"
        >Comments<textarea formControlName="comments"></textarea>
      </label>
      <button class="button button--primary" [disabled]="form.invalid">
        Submit review
      </button>
    </form>
  `,
  styles: `
    .actions {
      margin-bottom: 1rem;
    }
    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
    }
    .wide,
    .form-grid h2 {
      grid-column: 1/-1;
    }
    label {
      display: grid;
      gap: 0.3rem;
    }
    pre {
      white-space: pre-wrap;
      overflow-wrap: anywhere;
    }
    @media (max-width: 560px) {
      .form-grid {
        grid-template-columns: 1fr;
      }
      .wide,
      .form-grid h2 {
        grid-column: auto;
      }
    }
  `,
})
export class ScholarshipReviewComponent implements OnInit {
  private readonly api = inject(ScholarshipApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  readonly assignment = signal<ReviewAssignment | null>(null);
  readonly application = signal<unknown>(null);
  readonly error = signal<ApiError | null>(null);
  organizationId = "";
  assignmentId = "";
  readonly recommendations: ReviewRecommendation[] = [
    "STRONGLY_RECOMMEND",
    "RECOMMEND",
    "NEUTRAL",
    "DO_NOT_RECOMMEND",
  ];
  readonly form = this.fb.nonNullable.group({
    score: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    recommendation: ["RECOMMEND" as ReviewRecommendation],
    comments: [""],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.assignmentId = this.route.snapshot.paramMap.get("assignmentId") ?? "";
    this.api
      .reviewerApplication(this.organizationId, this.assignmentId)
      .subscribe({
        next: (application) => this.application.set(application),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  start(): void {
    this.api.startReview(this.organizationId, this.assignmentId).subscribe({
      next: (assignment) => this.assignment.set(assignment),
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    this.api
      .submitReview(this.organizationId, this.assignmentId, {
        ...value,
        comments: value.comments || null,
      })
      .subscribe({
        next: () =>
          this.form.reset({
            score: 0,
            recommendation: "RECOMMEND",
            comments: "",
          }),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
