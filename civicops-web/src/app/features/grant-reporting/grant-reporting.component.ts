import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  EvidenceSourceModule,
  GrantReportSummary,
  GrantReportTemplate,
} from "../../core/models/grant-reporting.models";
import { GrantSummary } from "../../core/models/grant.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { GrantApiService } from "../grants/grant-api.service";
import { GrantReportingApiService } from "./grant-reporting-api.service";

@Component({
  selector: "cop-grant-reporting",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    PaginationComponent,
    StatusBadgeComponent,
  ],
  template: `
    <header class="page-header">
      <div>
        <p class="eyebrow">ACCOUNTABILITY</p>
        <h1>Grant Reporting</h1>
        <p>Build evidence-backed reports from CivicOps operational records.</p>
      </div>
    </header>
    <cop-api-error [error]="error()" />
    <div class="workspace">
      <section class="panel">
        <div class="section-heading">
          <h2>Templates</h2>
          @if (canManage()) {
            <button
              class="button button--secondary"
              type="button"
              (click)="showTemplate.set(!showTemplate())"
            >
              New template
            </button>
          }
        </div>
        @if (showTemplate()) {
          <form [formGroup]="templateForm" (ngSubmit)="createTemplate()">
            <label>Name<input formControlName="name" /></label
            ><label
              >Description<textarea
                formControlName="description"
              ></textarea></label
            ><button class="button button--primary" type="submit">
              Create template
            </button>
          </form>
        }
        <div class="items">
          @for (template of templates()?.content ?? []; track template.id) {
            <a [routerLink]="['templates', template.id]"
              ><strong>{{ template.name }}</strong
              ><span
                >{{ template.grantId ? "Grant-specific" : "Reusable" }} ·
                {{ template.active ? "Active" : "Inactive" }}</span
              ></a
            >
          } @empty {
            <p>No report templates exist.</p>
          }
        </div>
      </section>
      <section class="panel">
        <div class="section-heading">
          <h2>Reports</h2>
          @if (canManage()) {
            <button
              class="button button--secondary"
              type="button"
              (click)="showReport.set(!showReport())"
            >
              Create report
            </button>
          }
        </div>
        @if (showReport()) {
          <form [formGroup]="reportForm" (ngSubmit)="createReport()">
            <label
              >Grant<select formControlName="grantId">
                @for (grant of grants(); track grant.id) {
                  <option [value]="grant.id">{{ grant.grantName }}</option>
                }
              </select></label
            ><label
              >Template<select formControlName="templateId">
                @for (
                  template of templates()?.content ?? [];
                  track template.id
                ) {
                  <option [value]="template.id">{{ template.name }}</option>
                }
              </select></label
            ><label
              >Period start<input
                type="date"
                formControlName="reportingPeriodStart" /></label
            ><label
              >Period end<input
                type="date"
                formControlName="reportingPeriodEnd"
            /></label>
            <fieldset>
              <legend>Evidence sources</legend>
              @for (source of sources; track source) {
                <label class="check"
                  ><input
                    type="checkbox"
                    [checked]="selectedSources().includes(source)"
                    (change)="toggleSource(source)"
                  />{{ source.replaceAll("_", " ") }}</label
                >
              }
            </fieldset>
            <button class="button button--primary" type="submit">
              Create report
            </button>
          </form>
        }
        <div class="items">
          @for (report of reports()?.content ?? []; track report.id) {
            <a [routerLink]="['reports', report.id]"
              ><strong>{{ report.grantName }}</strong
              ><span
                >{{ report.reportingPeriodStart }} –
                {{ report.reportingPeriodEnd }}</span
              ><cop-status-badge [status]="report.status"
            /></a>
          } @empty {
            <p>No grant reports exist.</p>
          }
        </div>
        @if (reports(); as page) {
          <cop-pagination
            [page]="page.page"
            [totalPages]="page.totalPages"
            [totalElements]="page.totalElements"
            [first]="page.first"
            [last]="page.last"
            (pageChange)="load($event)"
          />
        }
      </section>
    </div>
  `,
  styles: `
    .workspace {
      display: grid;
      grid-template-columns: 1fr 1.4fr;
      gap: 1rem;
    }
    .section-heading {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
    }
    .section-heading h2 {
      margin: 0;
    }
    .items {
      display: grid;
      margin-top: 1rem;
    }
    .items > a {
      display: grid;
      gap: 0.2rem;
      padding: 0.85rem 0;
      border-bottom: 1px solid var(--line);
      text-decoration: none;
    }
    .items span {
      color: var(--ink-muted);
      font-size: 0.85rem;
    }
    form {
      display: grid;
      gap: 0.75rem;
      padding: 1rem;
      margin-top: 1rem;
      background: var(--canvas);
      border-radius: 0.5rem;
    }
    label {
      display: grid;
      gap: 0.3rem;
    }
    fieldset {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      border: 1px solid var(--line);
    }
    .check {
      display: flex;
      align-items: center;
      gap: 0.3rem;
    }
    .check input {
      width: auto;
    }
    @media (max-width: 900px) {
      .workspace {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class GrantReportingComponent implements OnInit {
  private readonly api = inject(GrantReportingApiService);
  private readonly grantsApi = inject(GrantApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  readonly context = inject(OrganizationContextService);
  readonly templates = signal<PageResponse<GrantReportTemplate> | null>(null);
  readonly reports = signal<PageResponse<GrantReportSummary> | null>(null);
  readonly grants = signal<GrantSummary[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly showTemplate = signal(false);
  readonly showReport = signal(false);
  readonly selectedSources = signal<EvidenceSourceModule[]>([
    "GRANT",
    "VOLUNTEERS",
    "EVENTS",
    "DONATIONS",
  ]);
  readonly sources: EvidenceSourceModule[] = [
    "GRANT",
    "VOLUNTEERS",
    "EVENTS",
    "DONATIONS",
    "CASES",
    "SCHOLARSHIPS",
    "FOOD_PANTRY",
  ];
  organizationId = "";
  readonly templateForm = this.fb.nonNullable.group({
    name: ["", [Validators.required, Validators.maxLength(200)]],
    description: [""],
  });
  readonly reportForm = this.fb.nonNullable.group({
    grantId: ["", Validators.required],
    templateId: ["", Validators.required],
    reportingPeriodStart: ["", Validators.required],
    reportingPeriodEnd: ["", Validators.required],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.load();
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "GRANT_MANAGER");
  }
  load(page = 0) {
    this.error.set(null);
    forkJoin({
      templates: this.api.templates(this.organizationId),
      reports: this.api.reports(this.organizationId, undefined, page),
      grants: this.grantsApi.list(this.organizationId, {
        page: 0,
        size: 100,
        sort: "grantName",
        direction: "asc",
        restricted: null,
      }),
    }).subscribe({
      next: (r) => {
        this.templates.set(r.templates);
        this.reports.set(r.reports);
        this.grants.set(r.grants.content);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  toggleSource(source: EvidenceSourceModule) {
    this.selectedSources.update((items) =>
      items.includes(source)
        ? items.filter((item) => item !== source)
        : [...items, source],
    );
  }
  createTemplate() {
    if (this.templateForm.invalid) {
      this.templateForm.markAllAsTouched();
      return;
    }
    const v = this.templateForm.getRawValue();
    this.api
      .createTemplate(this.organizationId, {
        ...v,
        description: v.description || null,
        grantId: null,
      })
      .subscribe({
        next: () => {
          this.showTemplate.set(false);
          this.templateForm.reset();
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  createReport() {
    if (this.reportForm.invalid) return;
    const v = this.reportForm.getRawValue();
    if (v.reportingPeriodEnd < v.reportingPeriodStart) {
      this.reportForm.controls.reportingPeriodEnd.setErrors({
        dateRange: true,
      });
      return;
    }
    this.api
      .createReport(this.organizationId, v.grantId, {
        templateId: v.templateId,
        reportingPeriodStart: v.reportingPeriodStart,
        reportingPeriodEnd: v.reportingPeriodEnd,
        selectedSources: this.selectedSources(),
      })
      .subscribe({
        next: (r) =>
          void this.router.navigate(["reports", r.id], {
            relativeTo: this.route,
          }),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
}
