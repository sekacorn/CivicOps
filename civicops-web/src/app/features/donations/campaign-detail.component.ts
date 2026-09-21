import { CurrencyPipe } from "@angular/common";
import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import {
  CampaignFinancialSummary,
  DonationCampaign,
  PaymentMethod,
} from "../../core/models/operations.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { ConfirmationDialogComponent } from "../../shared/components/confirmation-dialog.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { DonationApiService } from "./donation-api.service";

@Component({
  selector: "cop-campaign-detail",
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    ConfirmationDialogComponent,
    StatusBadgeComponent,
  ],
  template: `
    @if (campaign(); as c) {
      <header class="page-header">
        <div>
          <p class="eyebrow">DONATION CAMPAIGN</p>
          <h1>{{ c.name }}</h1>
          <p>{{ c.description || "No description" }}</p>
        </div>
        <cop-status-badge [status]="c.status" />
      </header>
      <a [routerLink]="['/organizations', organizationId, 'donations']"
        >← Giving operations</a
      ><cop-api-error [error]="error()" />
      @if (financial(); as f) {
        <section class="metrics" aria-label="Campaign financial summary">
          <article>
            <span>Goal</span><strong>{{ f.goalAmount | currency }}</strong>
          </article>
          <article>
            <span>Raised</span><strong>{{ f.amountRaised | currency }}</strong>
          </article>
          <article>
            <span>Remaining</span
            ><strong>{{ f.remainingToGoal | currency }}</strong>
          </article>
          <article>
            <span>Progress</span><strong>{{ f.percentageOfGoal }}%</strong>
          </article>
          <article>
            <span>Donations</span><strong>{{ f.donationCount }}</strong>
          </article>
        </section>
      }
      @if (canManage()) {
        <section class="panel">
          <h2>Lifecycle</h2>
          <div class="actions">
            @if (c.status === "DRAFT") {
              <button
                class="button button--primary"
                (click)="transition('activate')"
              >
                Activate
              </button>
            }
            @if (c.status === "ACTIVE") {
              <button
                class="button button--danger"
                (click)="pending.set('close')"
              >
                Close campaign
              </button>
            }
            @if (c.status === "DRAFT" || c.status === "ACTIVE") {
              <button
                class="button button--danger"
                (click)="pending.set('cancel')"
              >
                Cancel campaign
              </button>
            }
            <button
              class="button button--secondary"
              (click)="showGift.set(!showGift())"
            >
              Record donation
            </button>
          </div>
        </section>
        @if (c.status === "DRAFT" || c.status === "ACTIVE") {
          <form class="panel" [formGroup]="editForm" (ngSubmit)="update()">
            <h2>Edit campaign</h2>
            <label>Name<input formControlName="name" /></label
            ><label
              >Goal amount<input
                type="number"
                min="0"
                step="0.01"
                formControlName="goalAmount" /></label
            ><label
              >Start date<input
                type="date"
                formControlName="startDate" /></label
            ><label
              >End date<input type="date" formControlName="endDate" /></label
            ><label
              >Description<textarea
                formControlName="description"
              ></textarea></label
            ><button class="button button--secondary">Save campaign</button>
          </form>
        }
      }
      @if (showGift()) {
        <form class="panel" [formGroup]="form" (ngSubmit)="record()">
          <h2>Record campaign donation</h2>
          <label
            >Amount<input
              type="number"
              min="0.01"
              step="0.01"
              formControlName="amount" /></label
          ><label
            >Date received<input
              type="date"
              formControlName="donationDate" /></label
          ><label
            >Payment method<select formControlName="paymentMethod">
              @for (m of methods; track m) {
                <option [value]="m">{{ m }}</option>
              }
            </select></label
          ><label>Donor ID (optional)<input formControlName="donorId" /></label
          ><label class="check"
            ><input
              type="checkbox"
              formControlName="anonymous"
            />Anonymous</label
          ><button class="button button--primary">Record transaction</button>
        </form>
      }
      @if (pending(); as action) {
        <cop-confirmation-dialog
          title="Confirm campaign lifecycle"
          [message]="
            action === 'close'
              ? 'Closing stops campaign activity but preserves financial history.'
              : 'Cancelling preserves all recorded transaction history.'
          "
          [confirmLabel]="
            action === 'close' ? 'Close campaign' : 'Cancel campaign'
          "
          (cancelled)="pending.set(null)"
          (confirmed)="transition(action)"
        />
      }
    }
  `,
  styles: `
    .metrics {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 0.75rem;
      margin: 1rem 0;
    }
    .metrics article {
      display: grid;
      gap: 0.3rem;
      padding: 1rem;
      background: white;
      border: 1px solid var(--line);
    }
    .metrics span {
      color: var(--ink-muted);
    }
    .actions {
      display: flex;
      gap: 0.6rem;
      flex-wrap: wrap;
    }
    form {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
      margin-top: 1rem;
    }
    label {
      display: grid;
      gap: 0.3rem;
    }
    .check {
      display: flex;
      align-items: center;
    }
    .check input {
      width: auto;
    }
    @media (max-width: 820px) {
      .metrics {
        grid-template-columns: repeat(2, 1fr);
      }
      form {
        grid-template-columns: 1fr;
      }
    }
    @media (max-width: 430px) {
      .metrics {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class CampaignDetailComponent implements OnInit {
  private readonly api = inject(DonationApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly campaign = signal<DonationCampaign | null>(null);
  readonly financial = signal<CampaignFinancialSummary | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly pending = signal<"close" | "cancel" | null>(null);
  readonly showGift = signal(false);
  organizationId = "";
  campaignId = "";
  readonly methods: PaymentMethod[] = [
    "CASH",
    "CHECK",
    "CARD",
    "ACH",
    "WIRE",
    "STOCK",
    "IN_KIND",
    "OTHER",
  ];
  readonly form = this.fb.nonNullable.group({
    amount: [0, [Validators.required, Validators.min(0.01)]],
    donationDate: [new Date().toISOString().slice(0, 10)],
    paymentMethod: ["CHECK" as PaymentMethod],
    donorId: [""],
    anonymous: [false],
  });
  readonly editForm = this.fb.nonNullable.group({
    name: ["", Validators.required],
    description: [""],
    goalAmount: [0, [Validators.required, Validators.min(0)]],
    startDate: [""],
    endDate: [""],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.campaignId = this.route.snapshot.paramMap.get("campaignId") ?? "";
    this.load();
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "DONATION_MANAGER");
  }
  load() {
    forkJoin({
      campaign: this.api.campaign(this.organizationId, this.campaignId),
      financial: this.api.campaignFinancial(
        this.organizationId,
        this.campaignId,
      ),
    }).subscribe({
      next: (r) => {
        this.campaign.set(r.campaign);
        this.financial.set(r.financial);
        this.editForm.patchValue({
          name: r.campaign.name,
          description: r.campaign.description ?? "",
          goalAmount: r.campaign.goalAmount,
          startDate: r.campaign.startDate ?? "",
          endDate: r.campaign.endDate ?? "",
        });
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  transition(action: "activate" | "close" | "cancel") {
    this.pending.set(null);
    this.api
      .transitionCampaign(this.organizationId, this.campaignId, action)
      .subscribe({
        next: () => this.load(),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  update() {
    if (this.editForm.invalid) return;
    const v = this.editForm.getRawValue();
    this.api
      .updateCampaign(this.organizationId, this.campaignId, {
        ...v,
        description: v.description || null,
        startDate: v.startDate || null,
        endDate: v.endDate || null,
      })
      .subscribe({
        next: () => this.load(),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  record() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.api
      .createDonation(this.organizationId, {
        ...v,
        donorId: v.donorId || null,
        campaignId: this.campaignId,
        inKindDescription: null,
        restricted: false,
        restrictionDescription: null,
        designation: null,
        referenceNumber: null,
        receiptNumber: null,
        acknowledgementStatus: "PENDING",
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showGift.set(false);
          this.form.reset();
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
}
