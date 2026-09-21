import { CurrencyPipe } from "@angular/common";
import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  DonationCampaign,
  DonationReportSummary,
  DonationSummary,
  DonorDetail,
  DonorSummary,
  DonorType,
  PaymentMethod,
  PaymentMethodTotal,
} from "../../core/models/operations.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { DonationApiService } from "./donation-api.service";

@Component({
  selector: "cop-donations",
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    PaginationComponent,
    StatusBadgeComponent,
  ],
  template: `
    <header class="page-header">
      <div>
        <p class="eyebrow">DONATIONS</p>
        <h1>Giving operations</h1>
        <p>
          Donors, campaigns, immutable transactions, and authoritative
          reporting.
        </p>
      </div>
      @if (canManage()) {
        <div class="actions">
          <button
            class="button button--secondary"
            (click)="showDonor.set(!showDonor())"
          >
            New donor</button
          ><button
            class="button button--secondary"
            (click)="showCampaign.set(!showCampaign())"
          >
            New campaign</button
          ><button
            class="button button--primary"
            (click)="showDonation.set(!showDonation())"
          >
            Record donation
          </button>
        </div>
      }
    </header>
    <cop-api-error [error]="error()" />
    @if (report(); as r) {
      <section class="metrics" aria-label="Donation report summary">
        <article>
          <span>Total received</span
          ><strong>{{ r.totalAmount | currency }}</strong>
        </article>
        <article>
          <span>Transactions</span><strong>{{ r.totalDonations }}</strong>
        </article>
        <article>
          <span>Average gift</span
          ><strong>{{ r.averageDonation | currency }}</strong>
        </article>
        <article>
          <span>Restricted</span
          ><strong>{{ r.restrictedAmount | currency }}</strong>
        </article>
        <article>
          <span>Active campaigns</span><strong>{{ r.activeCampaigns }}</strong>
        </article>
      </section>
    }
    @if (showDonor()) {
      <form
        class="panel form-grid"
        [formGroup]="donorForm"
        (ngSubmit)="createDonor()"
      >
        <h2>New donor</h2>
        <label
          >Type<select formControlName="donorType">
            @for (t of donorTypes; track t) {
              <option [value]="t">{{ t }}</option>
            }
          </select></label
        ><label>First name<input formControlName="firstName" /></label
        ><label>Last name<input formControlName="lastName" /></label
        ><label
          >Organization name<input formControlName="organizationName" /></label
        ><label>Email<input type="email" formControlName="email" /></label
        ><label>Phone<input formControlName="phone" /></label
        ><label class="check"
          ><input type="checkbox" formControlName="anonymous" />Anonymous
          donor</label
        ><label class="check"
          ><input
            type="checkbox"
            formControlName="communicationOptOut"
          />Communication opt-out</label
        ><button class="button button--primary">Create donor</button>
      </form>
    }
    @if (showCampaign()) {
      <form
        class="panel form-grid"
        [formGroup]="campaignForm"
        (ngSubmit)="createCampaign()"
      >
        <h2>New campaign</h2>
        <label>Name<input formControlName="name" /></label
        ><label
          >Goal amount<input
            type="number"
            min="0"
            step="0.01"
            formControlName="goalAmount" /></label
        ><label
          >Start date<input type="date" formControlName="startDate" /></label
        ><label>End date<input type="date" formControlName="endDate" /></label
        ><label class="wide"
          >Description<textarea formControlName="description"></textarea></label
        ><button class="button button--primary">Create campaign</button>
      </form>
    }
    @if (showDonation()) {
      <form
        class="panel form-grid"
        [formGroup]="donationForm"
        (ngSubmit)="createDonation()"
      >
        <h2>Record donation</h2>
        <label
          >Amount<input
            type="number"
            min="0.01"
            step="0.01"
            formControlName="amount" /></label
        ><label
          >Received date<input
            type="date"
            formControlName="donationDate" /></label
        ><label
          >Payment method<select formControlName="paymentMethod">
            @for (m of paymentMethods; track m) {
              <option [value]="m">{{ m.replaceAll("_", " ") }}</option>
            }
          </select></label
        ><label
          >Donor<select formControlName="donorId">
            <option value="">No donor / anonymous</option>
            @for (d of donors()?.content ?? []; track d.id) {
              <option [value]="d.id">{{ d.displayName }}</option>
            }
          </select></label
        ><label
          >Campaign<select formControlName="campaignId">
            <option value="">No campaign</option>
            @for (c of campaigns()?.content ?? []; track c.id) {
              <option [value]="c.id">{{ c.name }}</option>
            }
          </select></label
        ><label>Designation<input formControlName="designation" /></label
        ><label class="check"
          ><input type="checkbox" formControlName="anonymous" />Anonymous
          gift</label
        ><label class="check"
          ><input type="checkbox" formControlName="restricted" />Restricted
          gift</label
        ><button class="button button--primary">
          Record immutable transaction
        </button>
      </form>
    }
    <div class="columns">
      <section class="panel">
        <h2>Campaigns</h2>
        @for (c of campaigns()?.content ?? []; track c.id) {
          <article>
            <a [routerLink]="['campaigns', c.id]"
              ><strong>{{ c.name }}</strong></a
            >
            <p>{{ c.goalAmount | currency }} goal</p>
            <cop-status-badge [status]="c.status" />
          </article>
        } @empty {
          <p>No campaigns.</p>
        }
      </section>
      <section class="panel">
        <h2>Donor directory</h2>
        <p class="privacy-note">This list uses privacy-safe donor summaries.</p>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Display name</th>
                <th>Type</th>
                <th>Preferences</th>
              </tr>
            </thead>
            <tbody>
              @for (d of donors()?.content ?? []; track d.id) {
                <tr>
                  <th scope="row">
                    @if (canManage()) {
                      <button class="link" (click)="openDonor(d.id)">
                        {{ d.displayName }}
                      </button>
                    } @else {
                      {{ d.displayName }}
                    }
                  </th>
                  <td>{{ d.donorType }}</td>
                  <td>
                    {{
                      d.anonymous
                        ? "Anonymous"
                        : d.communicationOptOut
                          ? "Opted out"
                          : "Contact permitted"
                    }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        @if (donors(); as p) {
          <cop-pagination
            [page]="p.page"
            [totalPages]="p.totalPages"
            [totalElements]="p.totalElements"
            [first]="p.first"
            [last]="p.last"
            (pageChange)="load($event)"
          />
        }
      </section>
    </div>
    @if (selectedDonor(); as d) {
      <section class="panel donations">
        <div class="section-head">
          <div>
            <h2>{{ d.displayName }}</h2>
            <p>Private donor detail — manager access only.</p>
          </div>
          <cop-status-badge
            [status]="d.communicationOptOut ? 'OPTED_OUT' : 'CONTACT_ALLOWED'"
          />
        </div>
        <form
          class="form-grid"
          [formGroup]="donorEditForm"
          (ngSubmit)="updateDonor(d.id)"
        >
          <label>First name<input formControlName="firstName" /></label
          ><label>Last name<input formControlName="lastName" /></label
          ><label
            >Organization name<input
              formControlName="organizationName" /></label
          ><label>Email<input type="email" formControlName="email" /></label
          ><label>Phone<input formControlName="phone" /></label
          ><label class="check"
            ><input
              type="checkbox"
              formControlName="communicationOptOut"
            />Communication opt-out</label
          ><label class="wide"
            >Notes<textarea formControlName="notes"></textarea></label
          ><button class="button button--primary">
            Save donor contact details
          </button>
        </form>
      </section>
    }
    <section class="panel donations">
      <h2>Transactions</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Amount</th>
              <th>Method</th>
              <th>Designation</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            @for (d of donations()?.content ?? []; track d.id) {
              <tr>
                <td>{{ d.donationDate }}</td>
                <th scope="row">
                  @if (canManage()) {
                    <a [routerLink]="['transactions', d.id]">{{
                      d.amount | currency
                    }}</a>
                  } @else {
                    {{ d.amount | currency }}
                  }
                </th>
                <td>{{ d.paymentMethod }}</td>
                <td>{{ d.designation || "Unspecified" }}</td>
                <td><cop-status-badge [status]="d.status" /></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </section>
    @if (paymentTotals().length) {
      <section class="panel donations">
        <h2>By payment method</h2>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Method</th>
                <th>Transactions</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              @for (p of paymentTotals(); track p.paymentMethod) {
                <tr>
                  <th scope="row">{{ p.paymentMethod }}</th>
                  <td>{{ p.donationCount }}</td>
                  <td>{{ p.amount | currency }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    }
  `,
  styles: `
    .actions,
    .section-head {
      display: flex;
      gap: 0.6rem;
      flex-wrap: wrap;
    }
    .section-head {
      align-items: center;
      justify-content: space-between;
    }
    .link {
      padding: 0;
      border: 0;
      background: none;
      color: #12647a;
      text-decoration: underline;
      cursor: pointer;
    }
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
    .metrics span,
    .privacy-note {
      color: var(--ink-muted);
    }
    .metrics strong {
      font-size: 1.25rem;
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
    label {
      display: grid;
      gap: 0.3rem;
    }
    .check {
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }
    .check input {
      width: auto;
    }
    .columns {
      display: grid;
      grid-template-columns: 1fr 2fr;
      gap: 1rem;
    }
    .columns article {
      padding: 0.7rem 0;
      border-bottom: 1px solid var(--line);
    }
    .columns article p {
      margin: 0.2rem 0;
    }
    .donations {
      margin-top: 1rem;
    }
    @media (max-width: 900px) {
      .metrics {
        grid-template-columns: repeat(2, 1fr);
      }
      .columns {
        grid-template-columns: 1fr;
      }
    }
    @media (max-width: 650px) {
      .form-grid,
      .metrics {
        grid-template-columns: 1fr;
      }
      .form-grid h2,
      .wide {
        grid-column: auto;
      }
    }
  `,
})
export class DonationsComponent implements OnInit {
  private readonly api = inject(DonationApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly donors = signal<PageResponse<DonorSummary> | null>(null);
  readonly campaigns = signal<PageResponse<DonationCampaign> | null>(null);
  readonly donations = signal<PageResponse<DonationSummary> | null>(null);
  readonly report = signal<DonationReportSummary | null>(null);
  readonly paymentTotals = signal<PaymentMethodTotal[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly selectedDonor = signal<DonorDetail | null>(null);
  readonly showDonor = signal(false);
  readonly showCampaign = signal(false);
  readonly showDonation = signal(false);
  readonly donorTypes: DonorType[] = [
    "INDIVIDUAL",
    "ORGANIZATION",
    "FOUNDATION",
    "GOVERNMENT",
    "OTHER",
  ];
  readonly paymentMethods: PaymentMethod[] = [
    "CASH",
    "CHECK",
    "CARD",
    "ACH",
    "WIRE",
    "STOCK",
    "IN_KIND",
    "OTHER",
  ];
  organizationId = "";
  readonly donorForm = this.fb.nonNullable.group({
    donorType: ["INDIVIDUAL" as DonorType],
    firstName: [""],
    lastName: [""],
    organizationName: [""],
    email: ["", Validators.email],
    phone: [""],
    anonymous: [false],
    communicationOptOut: [false],
  });
  readonly donorEditForm = this.fb.nonNullable.group({
    firstName: [""],
    lastName: [""],
    organizationName: [""],
    email: ["", Validators.email],
    phone: [""],
    communicationOptOut: [false],
    notes: [""],
  });
  readonly campaignForm = this.fb.nonNullable.group({
    name: ["", Validators.required],
    description: [""],
    goalAmount: [0, [Validators.required, Validators.min(0)]],
    startDate: [""],
    endDate: [""],
  });
  readonly donationForm = this.fb.nonNullable.group({
    amount: [0, [Validators.required, Validators.min(0.01)]],
    donationDate: [new Date().toISOString().slice(0, 10)],
    paymentMethod: ["CHECK" as PaymentMethod],
    donorId: [""],
    campaignId: [""],
    designation: [""],
    anonymous: [false],
    restricted: [false],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.load();
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "DONATION_MANAGER");
  }
  load(page = 0) {
    forkJoin({
      donors: this.api.donors(this.organizationId, page),
      campaigns: this.api.campaigns(this.organizationId),
      donations: this.api.donations(this.organizationId),
      report: this.api.summary(this.organizationId),
      payments: this.api.paymentMethods(this.organizationId),
    }).subscribe({
      next: (r) => {
        this.donors.set(r.donors);
        this.campaigns.set(r.campaigns);
        this.donations.set(r.donations);
        this.report.set(r.report);
        this.paymentTotals.set(r.payments);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  createDonor() {
    if (this.donorForm.invalid) return;
    const v = this.donorForm.getRawValue();
    this.api
      .createDonor(this.organizationId, {
        ...v,
        firstName: v.firstName || null,
        lastName: v.lastName || null,
        organizationName: v.organizationName || null,
        email: v.email || null,
        phone: v.phone || null,
        addressLine1: null,
        addressLine2: null,
        city: null,
        state: null,
        postalCode: null,
        country: null,
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showDonor.set(false);
          this.donorForm.reset();
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  createCampaign() {
    if (this.campaignForm.invalid) return;
    const v = this.campaignForm.getRawValue();
    this.api
      .createCampaign(this.organizationId, {
        ...v,
        description: v.description || null,
        startDate: v.startDate || null,
        endDate: v.endDate || null,
      })
      .subscribe({
        next: () => {
          this.showCampaign.set(false);
          this.campaignForm.reset();
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  openDonor(id: string) {
    this.api.donor(this.organizationId, id).subscribe({
      next: (d) => {
        this.selectedDonor.set(d);
        this.donorEditForm.patchValue({
          firstName: d.firstName ?? "",
          lastName: d.lastName ?? "",
          organizationName: d.organizationName ?? "",
          email: d.email ?? "",
          phone: d.phone ?? "",
          communicationOptOut: d.communicationOptOut,
          notes: d.notes ?? "",
        });
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  updateDonor(id: string) {
    if (this.donorEditForm.invalid) return;
    const v = this.donorEditForm.getRawValue();
    this.api
      .updateDonor(this.organizationId, id, {
        firstName: v.firstName || null,
        lastName: v.lastName || null,
        organizationName: v.organizationName || null,
        email: v.email || null,
        phone: v.phone || null,
        communicationOptOut: v.communicationOptOut,
        notes: v.notes || null,
      })
      .subscribe({
        next: (d) => {
          this.selectedDonor.set(d);
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  createDonation() {
    if (this.donationForm.invalid) return;
    const v = this.donationForm.getRawValue();
    this.api
      .createDonation(this.organizationId, {
        ...v,
        donorId: v.donorId || null,
        campaignId: v.campaignId || null,
        designation: v.designation || null,
        inKindDescription: null,
        restrictionDescription: null,
        referenceNumber: null,
        receiptNumber: null,
        acknowledgementStatus: "PENDING",
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showDonation.set(false);
          this.donationForm.reset();
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
}
