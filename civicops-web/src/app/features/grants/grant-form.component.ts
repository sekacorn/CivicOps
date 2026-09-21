import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { finalize } from "rxjs";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import { GrantMutation } from "../../core/models/grant.models";
import { GrantApiService } from "./grant-api.service";

@Component({
  selector: "cop-grant-form",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
  ],
  template: `
    <header class="page-header">
      <div>
        <p class="eyebrow">GRANTS</p>
        <h1>{{ grantId ? "Edit grant" : "Create grant" }}</h1>
        <p>
          {{
            grantId
              ? "Update grant metadata. Lifecycle status is managed separately."
              : "Add a funding prospect to this organization."
          }}
        </p>
      </div>
      <a class="button button--quiet" [routerLink]="grantId ? ['..'] : ['..']"
        >Cancel</a
      >
    </header>
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else {
      <cop-api-error [error]="error()" />
      <form
        class="panel grant-form"
        [formGroup]="form"
        (ngSubmit)="save()"
        novalidate
      >
        <div class="field field--wide">
          <label for="grantName">Grant name</label
          ><input id="grantName" formControlName="grantName" />
          @if (invalid("grantName")) {
            <p class="field-error">
              Grant name is required and must be 200 characters or fewer.
            </p>
          }
        </div>
        <div class="field">
          <label for="grantorName">Grantor</label
          ><input id="grantorName" formControlName="grantorName" />
          @if (invalid("grantorName")) {
            <p class="field-error">Grantor is required.</p>
          }
        </div>
        <div class="field">
          <label for="grantNumber">Grant number</label
          ><input id="grantNumber" formControlName="grantNumber" />
        </div>
        <div class="field">
          <label for="awardAmount">Award amount</label
          ><input
            id="awardAmount"
            type="number"
            min="0"
            step="0.01"
            formControlName="awardAmount"
          />
          @if (invalid("awardAmount")) {
            <p class="field-error">Enter an amount of zero or more.</p>
          }
        </div>
        <div class="field">
          <label for="applicationDeadline">Application deadline</label
          ><input
            id="applicationDeadline"
            type="date"
            formControlName="applicationDeadline"
          />
        </div>
        <div class="field">
          <label for="startDate">Start date</label
          ><input id="startDate" type="date" formControlName="startDate" />
        </div>
        <div class="field">
          <label for="endDate">End date</label
          ><input id="endDate" type="date" formControlName="endDate" />
        </div>
        <div class="field">
          <label for="reportingDeadline">Reporting deadline</label
          ><input
            id="reportingDeadline"
            type="date"
            formControlName="reportingDeadline"
          />
        </div>
        <div class="field field--wide">
          <label for="description">Description</label
          ><textarea
            id="description"
            rows="4"
            formControlName="description"
          ></textarea>
        </div>
        <label class="check field--wide"
          ><input type="checkbox" formControlName="restricted" />Restricted
          funding</label
        >
        @if (form.controls.restricted.value) {
          <div class="field field--wide">
            <label for="restrictionDescription">Restriction description</label
            ><textarea
              id="restrictionDescription"
              rows="2"
              formControlName="restrictionDescription"
            ></textarea>
          </div>
        }
        <div class="field">
          <label for="primaryContactName">Primary contact</label
          ><input
            id="primaryContactName"
            formControlName="primaryContactName"
          />
        </div>
        <div class="field">
          <label for="primaryContactEmail">Contact email</label
          ><input
            id="primaryContactEmail"
            type="email"
            formControlName="primaryContactEmail"
          />
          @if (invalid("primaryContactEmail")) {
            <p class="field-error">Enter a valid email address.</p>
          }
        </div>
        <div class="field field--wide">
          <label for="notes">Internal notes</label
          ><textarea id="notes" rows="3" formControlName="notes"></textarea>
        </div>
        <div class="actions field--wide">
          <button
            class="button button--primary"
            type="submit"
            [disabled]="saving()"
          >
            {{ saving() ? "Saving…" : "Save grant" }}
          </button>
        </div>
      </form>
    }
  `,
  styles: `
    .grant-form {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem 1.25rem;
      max-width: 60rem;
    }
    .field {
      display: grid;
      align-content: start;
      gap: 0.35rem;
    }
    .field--wide {
      grid-column: 1 / -1;
    }
    .check {
      display: flex;
      align-items: center;
      gap: 0.55rem;
      font-weight: 650;
    }
    .check input {
      width: auto;
    }
    .actions {
      display: flex;
      justify-content: flex-end;
      padding-top: 0.5rem;
    }
    @media (max-width: 650px) {
      .grant-form {
        grid-template-columns: 1fr;
      }
      .field--wide {
        grid-column: auto;
      }
    }
  `,
})
export class GrantFormComponent implements OnInit {
  private readonly builder = inject(FormBuilder);
  private readonly api = inject(GrantApiService);
  private readonly errors = inject(ApiErrorService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  grantId: string | null = null;
  organizationId = "";
  readonly form = this.builder.nonNullable.group({
    grantName: ["", [Validators.required, Validators.maxLength(200)]],
    grantorName: ["", [Validators.required, Validators.maxLength(200)]],
    grantNumber: ["", Validators.maxLength(100)],
    awardAmount: [0, [Validators.required, Validators.min(0)]],
    applicationDeadline: "",
    startDate: "",
    endDate: "",
    reportingDeadline: "",
    description: "",
    restricted: false,
    restrictionDescription: "",
    primaryContactName: ["", Validators.maxLength(200)],
    primaryContactEmail: ["", Validators.email],
    notes: "",
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.grantId = this.route.snapshot.paramMap.get("grantId");
    if (!this.grantId) return;
    this.loading.set(true);
    this.api.detail(this.organizationId, this.grantId).subscribe({
      next: (grant) => {
        this.form.patchValue({
          grantName: grant.grantName,
          grantorName: grant.grantorName,
          grantNumber: grant.grantNumber ?? "",
          awardAmount: grant.awardAmount,
          applicationDeadline: grant.applicationDeadline ?? "",
          startDate: grant.startDate ?? "",
          endDate: grant.endDate ?? "",
          reportingDeadline: grant.reportingDeadline ?? "",
          description: grant.description ?? "",
          restricted: grant.restricted,
          restrictionDescription: grant.restrictionDescription ?? "",
          primaryContactName: grant.primaryContactName ?? "",
          primaryContactEmail: grant.primaryContactEmail ?? "",
          notes: grant.notes ?? "",
        });
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(this.errors.from(error));
        this.loading.set(false);
      },
    });
  }

  invalid(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return control.touched && control.invalid;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    if (value.restricted && !value.restrictionDescription.trim()) {
      this.form.controls.restrictionDescription.setErrors({ required: true });
      this.form.controls.restrictionDescription.markAsTouched();
      return;
    }
    const mutation: GrantMutation = {
      ...value,
      grantNumber: value.grantNumber || null,
      description: value.description || null,
      applicationDeadline: value.applicationDeadline || null,
      startDate: value.startDate || null,
      endDate: value.endDate || null,
      reportingDeadline: value.reportingDeadline || null,
      restrictionDescription: value.restrictionDescription || null,
      primaryContactName: value.primaryContactName || null,
      primaryContactEmail: value.primaryContactEmail || null,
      notes: value.notes || null,
    };
    this.saving.set(true);
    this.error.set(null);
    const request = this.grantId
      ? this.api.update(this.organizationId, this.grantId, mutation)
      : this.api.create(this.organizationId, mutation);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (grant) =>
        void this.router.navigate([
          "/organizations",
          this.organizationId,
          "grants",
          grant.id,
        ]),
      error: (error) => {
        const problem = this.errors.from(error);
        this.errors.applyFieldErrors(this.form, problem);
        this.error.set(problem);
      },
    });
  }
}
