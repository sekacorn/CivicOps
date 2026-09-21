import { CurrencyPipe } from "@angular/common";
import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import { DonationDetail } from "../../core/models/operations.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { ConfirmationDialogComponent } from "../../shared/components/confirmation-dialog.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { DonationApiService } from "./donation-api.service";

@Component({
  selector: "cop-donation-detail",
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    ConfirmationDialogComponent,
    StatusBadgeComponent,
  ],
  template: `@if (donation(); as d) {
    <header class="page-header">
      <div>
        <p class="eyebrow">IMMUTABLE DONATION</p>
        <h1>{{ d.amount | currency }}</h1>
        <p>
          Received {{ d.donationDate }} via
          {{ d.paymentMethod.replaceAll("_", " ") }}
        </p>
      </div>
      <cop-status-badge [status]="d.status" />
    </header>
    <a [routerLink]="['/organizations', organizationId, 'donations']"
      >← Giving operations</a
    ><cop-api-error [error]="error()" />
    <section class="panel">
      <h2>Transaction record</h2>
      <dl>
        <div>
          <dt>Designation</dt>
          <dd>{{ d.designation || "Unspecified" }}</dd>
        </div>
        <div>
          <dt>Donor</dt>
          <dd>{{ d.anonymous ? "Anonymous" : d.donorId || "Not linked" }}</dd>
        </div>
        <div>
          <dt>Campaign</dt>
          <dd>{{ d.campaignId || "Not linked" }}</dd>
        </div>
        <div>
          <dt>Restriction</dt>
          <dd>
            {{
              d.restricted
                ? d.restrictionDescription || "Restricted"
                : "Unrestricted"
            }}
          </dd>
        </div>
        <div>
          <dt>Receipt</dt>
          <dd>{{ d.receiptNumber || "Not assigned" }}</dd>
        </div>
      </dl>
      @if (d.status === "RECORDED") {
        <form [formGroup]="form" (ngSubmit)="confirm.set(true)">
          <label
            >Reversal reason<textarea
              formControlName="reason"
            ></textarea></label
          ><button class="button button--danger" [disabled]="form.invalid">
            Reverse transaction
          </button>
        </form>
      } @else {
        <p>
          <strong>Reversed:</strong> {{ d.reversalReason }} · {{ d.reversedAt }}
        </p>
      }
    </section>
    @if (confirm()) {
      <cop-confirmation-dialog
        title="Reverse donation?"
        message="This preserves the original transaction and records an explicit reversal. Reporting values will refresh from the server."
        confirmLabel="Reverse donation"
        (cancelled)="confirm.set(false)"
        (confirmed)="reverse()"
      />
    }
  }`,
  styles: `
    dl {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.8rem;
    }
    dl div {
      padding: 0.75rem;
      background: var(--canvas);
    }
    dt {
      color: var(--ink-muted);
    }
    dd {
      margin: 0.2rem 0 0;
    }
    form,
    label {
      display: grid;
      gap: 0.5rem;
    }
    form {
      margin-top: 1rem;
    }
    @media (max-width: 600px) {
      dl {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class DonationDetailComponent implements OnInit {
  private readonly api = inject(DonationApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  readonly donation = signal<DonationDetail | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly confirm = signal(false);
  organizationId = "";
  donationId = "";
  readonly form = this.fb.nonNullable.group({
    reason: ["", [Validators.required, Validators.maxLength(500)]],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.donationId = this.route.snapshot.paramMap.get("donationId") ?? "";
    this.load();
  }
  load() {
    this.api.donation(this.organizationId, this.donationId).subscribe({
      next: (d) => this.donation.set(d),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  reverse() {
    this.confirm.set(false);
    this.api
      .reverse(
        this.organizationId,
        this.donationId,
        this.form.getRawValue().reason,
      )
      .subscribe({
        next: (d) => this.donation.set(d),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
}
