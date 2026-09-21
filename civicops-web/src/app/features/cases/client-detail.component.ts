import { Component, inject, OnInit, signal } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import { ClientDetail } from "../../core/models/case.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { CaseApiService } from "./case-api.service";

@Component({
  selector: "cop-client-detail",
  imports: [
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a [routerLink]="['../..']">← Cases</a><cop-api-error [error]="error()" />
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (client()) {
      @let c = client()!;
      <header class="page-header">
        <div>
          <p class="eyebrow">PRIVATE CLIENT DETAIL</p>
          <h1>{{ c.preferredName || c.firstName }} {{ c.lastName }}</h1>
          <p>Access is restricted by the Case Management privacy policy.</p>
        </div>
        <cop-status-badge [status]="c.active ? 'ACTIVE' : 'INACTIVE'" />
      </header>
      <section class="panel detail-grid">
        <div>
          <span>External reference</span
          ><strong>{{ c.externalReferenceNumber || "—" }}</strong>
        </div>
        <div>
          <span>Date of birth</span><strong>{{ c.dateOfBirth || "—" }}</strong>
        </div>
        <div>
          <span>Email</span><strong>{{ c.email || "—" }}</strong>
        </div>
        <div>
          <span>Phone</span><strong>{{ c.phone || "—" }}</strong>
        </div>
        <div class="wide">
          <span>Address</span><strong>{{ address(c) }}</strong>
        </div>
        <div>
          <span>Preferred contact</span
          ><strong>{{ c.preferredContactMethod || "—" }}</strong>
        </div>
      </section>
    }
  `,
  styles: `
    .detail-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1rem;
    }
    .detail-grid div {
      display: grid;
      gap: 0.2rem;
    }
    .detail-grid span {
      color: var(--ink-muted);
    }
    .wide {
      grid-column: 1/-1;
    }
    @media (max-width: 650px) {
      .detail-grid {
        grid-template-columns: 1fr;
      }
      .wide {
        grid-column: auto;
      }
    }
  `,
})
export class ClientDetailComponent implements OnInit {
  private readonly api = inject(CaseApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly errors = inject(ApiErrorService);
  readonly client = signal<ClientDetail | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  organizationId = "";
  clientId = "";
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.clientId = this.route.snapshot.paramMap.get("clientId") ?? "";
    this.api.client(this.organizationId, this.clientId).subscribe({
      next: (r) => {
        this.client.set(r);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(this.errors.from(e));
        this.loading.set(false);
      },
    });
  }
  address(c: ClientDetail) {
    return (
      [c.addressLine1, c.addressLine2, c.city, c.state, c.postalCode, c.country]
        .filter(Boolean)
        .join(", ") || "—"
    );
  }
}
