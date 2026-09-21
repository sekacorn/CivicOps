import { Component, computed, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import {
  GrantReportDetail,
  GrantReportSection,
  MissingEvidence,
  ReportEvidence,
} from "../../core/models/grant-reporting.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { ConfirmationDialogComponent } from "../../shared/components/confirmation-dialog.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { GrantReportingApiService } from "./grant-reporting-api.service";

@Component({
  selector: "cop-report-detail",
  imports: [
    ReactiveFormsModule,
    ApiErrorComponent,
    ConfirmationDialogComponent,
    StatusBadgeComponent,
  ],
  template: `
    @if (report(); as r) {
      <header class="page-header">
        <div>
          <p class="eyebrow">GRANT REPORT</p>
          <h1>{{ r.grantName }}</h1>
          <p>
            {{ r.templateName }} · {{ r.reportingPeriodStart }} –
            {{ r.reportingPeriodEnd }}
          </p>
        </div>
        <cop-status-badge [status]="r.status" />
      </header>
      <div class="actions">
        @if (canManage() && r.status !== "FINALIZED") {
          @if (r.status === "DRAFT") {
            <button class="button button--primary" (click)="act('generate')">
              Generate
            </button>
          } @else {
            <button
              class="button button--secondary"
              (click)="confirmRegenerate.set(true)"
            >
              Regenerate
            </button>
          }
          <button
            class="button button--danger"
            [disabled]="requiredPending()"
            (click)="confirmFinalize.set(true)"
          >
            Finalize
          </button>
        }
        <button class="button button--secondary" (click)="download('json')">
          Export JSON</button
        ><button
          class="button button--secondary"
          (click)="download('markdown')"
        >
          Export Markdown
        </button>
      </div>
    }
    <cop-api-error [error]="error()" />
    @if (requiredPending()) {
      <p class="notice" role="status">
        Required sections must be approved before finalization.
      </p>
    }
    <section class="panel">
      <h2>Report sections</h2>
      @for (section of sections(); track section.id) {
        <article class="section">
          <div class="section-head">
            <div>
              <h3>{{ section.sequenceNumber }}. {{ section.title }}</h3>
              <span
                >{{ section.required ? "Required" : "Optional" }} ·
                {{ section.sectionType }}</span
              >
            </div>
            <cop-status-badge [status]="section.status" />
          </div>
          <div class="provenance">
            <div>
              <h4>Generated content</h4>
              <p>{{ section.generatedContent || "Not generated" }}</p>
            </div>
            <div>
              <h4>Edited content</h4>
              <p>{{ section.editedContent || "No editor changes" }}</p>
            </div>
            <div>
              <h4>Final content</h4>
              <p>{{ section.finalContent || "Not approved" }}</p>
            </div>
          </div>
          @if (canManage() && report()?.status !== "FINALIZED") {
            <label
              >Edit section<textarea
                #edited
                [value]="
                  section.editedContent ?? section.generatedContent ?? ''
                "
              ></textarea>
            </label>
            <div class="actions">
              <button
                class="button button--secondary"
                (click)="edit(section, edited.value)"
              >
                Save edit</button
              ><button
                class="button button--primary"
                [disabled]="section.status === 'PENDING'"
                (click)="approve(section)"
              >
                Approve
              </button>
            </div>
          }
        </article>
      } @empty {
        <p>Generate this report to create its sections.</p>
      }
    </section>
    <section class="panel evidence">
      <div class="section-head">
        <h2>Evidence</h2>
        @if (canManage() && report()?.status !== "FINALIZED") {
          <button
            class="button button--secondary"
            (click)="manualOpen.set(!manualOpen())"
          >
            Add manual evidence
          </button>
        }
      </div>
      @if (manualOpen()) {
        <form [formGroup]="manualForm" (ngSubmit)="addManual()">
          <label>Label<input formControlName="label" /></label
          ><label>Text value<input formControlName="textValue" /></label
          ><label
            >Numeric value<input
              type="number"
              formControlName="numericValue" /></label
          ><label
            >Monetary value<input
              type="number"
              formControlName="monetaryValue" /></label
          ><label>Unit<input formControlName="unit" /></label
          ><label
            >Source reference<input formControlName="sourceReference" /></label
          ><label>Notes<textarea formControlName="notes"></textarea></label
          ><button class="button button--primary">Add manual evidence</button>
        </form>
      }
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Source</th>
              <th>Metric</th>
              <th>Value</th>
              <th>State</th>
              <th>Captured</th>
            </tr>
          </thead>
          <tbody>
            @for (item of evidence(); track item.id) {
              <tr>
                <td>{{ item.sourceModule }}</td>
                <th scope="row">{{ item.metricLabel }}</th>
                <td>{{ evidenceValue(item) }}</td>
                <td>
                  <cop-status-badge
                    [status]="
                      item.sourceModule === 'MANUAL'
                        ? 'MANUAL'
                        : item.valueState
                    "
                  />
                </td>
                <td>{{ capturedDate(item.capturedAt) }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      @if (missing().length) {
        <h3>Missing data</h3>
        <ul>
          @for (item of missing(); track item.metricKey) {
            <li>
              <strong>{{ item.metricLabel }}</strong> — {{ item.message }}
            </li>
          }
        </ul>
      }
    </section>
    @if (confirmRegenerate()) {
      <cop-confirmation-dialog
        title="Regenerate report?"
        message="Current generated system evidence and content may be replaced."
        confirmLabel="Regenerate"
        (cancelled)="confirmRegenerate.set(false)"
        (confirmed)="act('regenerate')"
      />
    }
    @if (confirmFinalize()) {
      <cop-confirmation-dialog
        title="Finalize report?"
        message="This report will become immutable after finalization."
        confirmLabel="Finalize"
        (cancelled)="confirmFinalize.set(false)"
        (confirmed)="act('finalize')"
      />
    }
  `,
  styles: `
    .actions,
    .section-head {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.65rem;
    }
    .section-head {
      justify-content: space-between;
    }
    .notice {
      padding: 0.8rem;
      background: #fff7df;
      border: 1px solid #e4cf8b;
    }
    .panel {
      margin-top: 1rem;
    }
    .section {
      padding: 1rem 0;
      border-bottom: 1px solid var(--line);
    }
    h3,
    h4 {
      margin: 0.2rem 0;
    }
    .section-head span {
      color: var(--ink-muted);
    }
    .provenance {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.75rem;
      margin: 0.8rem 0;
    }
    .provenance > div {
      padding: 0.8rem;
      background: var(--canvas);
      border-radius: 0.4rem;
    }
    .section label,
    form label {
      display: grid;
      gap: 0.3rem;
    }
    form {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.75rem;
      padding: 1rem;
      background: var(--canvas);
    }
    .evidence {
      margin-bottom: 2rem;
    }
    @media (max-width: 760px) {
      .provenance,
      form {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class ReportDetailComponent implements OnInit {
  private readonly api = inject(GrantReportingApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  readonly context = inject(OrganizationContextService);
  readonly report = signal<GrantReportDetail | null>(null);
  readonly sections = signal<GrantReportSection[]>([]);
  readonly evidence = signal<ReportEvidence[]>([]);
  readonly missing = signal<MissingEvidence[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly manualOpen = signal(false);
  readonly confirmRegenerate = signal(false);
  readonly confirmFinalize = signal(false);
  readonly requiredPending = computed(() =>
    this.sections().some((s) => s.required && s.status !== "APPROVED"),
  );
  organizationId = "";
  reportId = "";
  readonly manualForm = this.fb.group({
    label: ["", Validators.required],
    numericValue: [null as number | null],
    monetaryValue: [null as number | null],
    textValue: [""],
    unit: [""],
    sourceReference: ["", Validators.required],
    notes: [""],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.reportId = this.route.snapshot.paramMap.get("reportId") ?? "";
    this.load();
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "GRANT_MANAGER");
  }
  load() {
    forkJoin({
      report: this.api.report(this.organizationId, this.reportId),
      sections: this.api.sections(this.organizationId, this.reportId),
      evidence: this.api.evidence(this.organizationId, this.reportId),
      missing: this.api.missing(this.organizationId, this.reportId),
    }).subscribe({
      next: (r) => {
        this.report.set(r.report);
        this.sections.set(r.sections);
        this.evidence.set(r.evidence);
        this.missing.set(r.missing);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  act(action: "generate" | "regenerate" | "finalize") {
    this.confirmRegenerate.set(false);
    this.confirmFinalize.set(false);
    this.api.action(this.organizationId, this.reportId, action).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  edit(section: GrantReportSection, content: string) {
    this.api.editSection(this.organizationId, section.id, content).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  approve(section: GrantReportSection) {
    this.api.approveSection(this.organizationId, section.id).subscribe({
      next: () => this.load(),
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  addManual() {
    if (this.manualForm.invalid) return;
    const v = this.manualForm.getRawValue();
    this.api
      .addManualEvidence(this.organizationId, this.reportId, {
        label: v.label!,
        numericValue: v.numericValue,
        monetaryValue: v.monetaryValue,
        textValue: v.textValue || null,
        unit: v.unit || null,
        sourceReference: v.sourceReference!,
        notes: v.notes || null,
      })
      .subscribe({
        next: () => {
          this.manualOpen.set(false);
          this.manualForm.reset();
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  evidenceValue(item: ReportEvidence) {
    if (item.valueState === "MISSING") return "Missing";
    if (item.valueState === "NOT_APPLICABLE") return "Not applicable";
    return (
      item.monetaryValue ?? item.numericValue ?? item.textValue ?? "Verified"
    );
  }
  capturedDate(value: string) {
    return value.slice(0, 10);
  }
  download(format: "json" | "markdown") {
    this.api.export(this.organizationId, this.reportId, format).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `grant-report-${this.reportId}.${format === "json" ? "json" : "md"}`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
}
