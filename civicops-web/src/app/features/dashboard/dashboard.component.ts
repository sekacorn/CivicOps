import { Component, inject, OnInit, signal } from "@angular/core";
import { RouterLink } from "@angular/router";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import { OrganizationGrantSummary } from "../../core/models/grant.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { GrantApiService } from "../grants/grant-api.service";

@Component({
  selector: "cop-dashboard",
  imports: [RouterLink, AsyncStateComponent, ApiErrorComponent],
  template: `
    <header class="page-header">
      <div>
        <p class="eyebrow">ORGANIZATION OVERVIEW</p>
        <h1>{{ organizations.selectedOrganization()?.name ?? "Dashboard" }}</h1>
        <p>Operational information from CivicOps records.</p>
      </div>
    </header>
    @if (!organizations.selectedOrganizationId()) {
      <cop-async-state
        state="empty"
        detail="Create or join an organization to begin using CivicOps."
      />
    } @else if (loading()) {
      <cop-async-state state="loading" />
    } @else {
      <cop-api-error [error]="error()" />
      @if (summary(); as data) {
        <section class="metrics" aria-label="Grant portfolio metrics">
          <article>
            <span>Total grants</span><strong>{{ data.totalGrants }}</strong>
          </article>
          <article>
            <span>Active grants</span><strong>{{ data.activeGrants }}</strong>
          </article>
          <article>
            <span>Total awarded</span
            ><strong>{{ money(data.totalAwarded) }}</strong>
          </article>
          <article>
            <span>Remaining balance</span
            ><strong>{{ money(data.remainingBalance) }}</strong>
          </article>
        </section>
        <section class="panel next-step">
          <div>
            <h2>Grant portfolio</h2>
            <p>
              {{ data.reportsDueSoon }} report{{
                data.reportsDueSoon === 1 ? "" : "s"
              }}
              due in the next 30 days.
            </p>
          </div>
          <a
            class="button button--primary"
            [routerLink]="[
              '/organizations',
              organizations.selectedOrganizationId(),
              'grants',
            ]"
            >Open grants</a
          >
        </section>
      }
    }
  `,
  styles: `
    .metrics {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 1rem;
      margin: 1.5rem 0;
    }
    .metrics article {
      display: grid;
      gap: 0.5rem;
      padding: 1.25rem;
      background: white;
      border: 1px solid var(--line);
      border-radius: 0.7rem;
    }
    .metrics span {
      color: var(--ink-muted);
      font-size: 0.82rem;
    }
    .metrics strong {
      font-size: 1.55rem;
    }
    .next-step {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
    }
    .next-step h2,
    .next-step p {
      margin: 0;
    }
    .next-step p {
      margin-top: 0.35rem;
      color: var(--ink-muted);
    }
    @media (max-width: 900px) {
      .metrics {
        grid-template-columns: repeat(2, 1fr);
      }
    }
    @media (max-width: 520px) {
      .metrics {
        grid-template-columns: 1fr;
      }
      .next-step {
        align-items: stretch;
        flex-direction: column;
      }
    }
  `,
})
export class DashboardComponent implements OnInit {
  readonly organizations = inject(OrganizationContextService);
  private readonly grants = inject(GrantApiService);
  private readonly errors = inject(ApiErrorService);
  readonly loading = signal(false);
  readonly summary = signal<OrganizationGrantSummary | null>(null);
  readonly error = signal<ApiError | null>(null);

  ngOnInit(): void {
    const organizationId = this.organizations.selectedOrganizationId();
    if (
      !organizationId ||
      !this.organizations.hasAnyRole(
        "GRANT_MANAGER",
        "PROGRAM_MANAGER",
        "VIEWER",
      )
    )
      return;
    this.loading.set(true);
    this.grants.portfolioSummary(organizationId).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(this.errors.from(error));
        this.loading.set(false);
      },
    });
  }

  money(value: number): string {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency: "USD",
    }).format(value);
  }
}
