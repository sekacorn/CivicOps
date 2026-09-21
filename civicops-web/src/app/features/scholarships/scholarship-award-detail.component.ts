import { Component, inject, OnInit, signal } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import { ScholarshipAward } from "../../core/models/scholarship.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { ScholarshipApiService } from "./scholarship-api.service";

@Component({
  selector: "cop-scholarship-award-detail",
  imports: [RouterLink, ApiErrorComponent, StatusBadgeComponent],
  template: `
    <a class="back-link" [routerLink]="['..', '..']">Back to scholarships</a>
    <cop-api-error [error]="error()" />
    @if (award(); as award) {
      <header class="page-header">
        <div>
          <p class="eyebrow">SCHOLARSHIP AWARD</p>
          <h1>{{ award.applicantDisplayName }}</h1>
          <p>{{ award.programName }} · {{ money(award.amount) }}</p>
        </div>
        <cop-status-badge [status]="award.status" />
      </header>
      <div class="actions">
        <button class="button button--secondary" (click)="action('accept')">
          Accept
        </button>
        <button class="button button--secondary" (click)="action('decline')">
          Decline
        </button>
        <button
          class="button button--secondary"
          (click)="action('mark-disbursed')"
        >
          Mark disbursed
        </button>
        <button class="button button--quiet" (click)="action('cancel')">
          Cancel
        </button>
      </div>
      <section class="panel detail-grid">
        <div>
          <span>Award date</span><strong>{{ award.awardDate }}</strong>
        </div>
        <div>
          <span>Application</span><strong>{{ award.applicationId }}</strong>
        </div>
        <div>
          <span>Notes</span><strong>{{ award.notes || "—" }}</strong>
        </div>
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
    .detail-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
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
export class ScholarshipAwardDetailComponent implements OnInit {
  private readonly api = inject(ScholarshipApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly errors = inject(ApiErrorService);
  readonly award = signal<ScholarshipAward | null>(null);
  readonly error = signal<ApiError | null>(null);
  organizationId = "";
  awardId = "";

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.awardId = this.route.snapshot.paramMap.get("awardId") ?? "";
    this.load();
  }

  money(value: number): string {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: "USD",
    }).format(value);
  }

  load(): void {
    this.api.award(this.organizationId, this.awardId).subscribe({
      next: (award) => this.award.set(award),
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  action(action: "accept" | "decline" | "mark-disbursed" | "cancel"): void {
    this.api.awardAction(this.organizationId, this.awardId, action).subscribe({
      next: (award) => this.award.set(award),
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }
}
