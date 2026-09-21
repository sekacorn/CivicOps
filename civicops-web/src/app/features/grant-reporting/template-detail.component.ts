import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import {
  GrantReportTemplate,
  ReportSectionType,
  TemplateSection,
} from "../../core/models/grant-reporting.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { GrantReportingApiService } from "./grant-reporting-api.service";

@Component({
  selector: "cop-template-detail",
  imports: [ReactiveFormsModule, ApiErrorComponent],
  template: `
    @if (template(); as record) {
      <header class="page-header">
        <div>
          <p class="eyebrow">REPORT TEMPLATE</p>
          <h1>{{ record.name }}</h1>
          <p>{{ record.description }}</p>
        </div>
      </header>
    }
    <cop-api-error [error]="error()" />
    <section class="panel">
      <div class="heading">
        <h2>Template sections</h2>
        <button
          type="button"
          class="button button--secondary"
          (click)="adding.set(!adding())"
        >
          Add section
        </button>
      </div>
      @if (adding()) {
        <form [formGroup]="form" (ngSubmit)="add()">
          <label>Key<input formControlName="sectionKey" /></label
          ><label>Title<input formControlName="title" /></label
          ><label
            >Type<select formControlName="sectionType">
              @for (type of types; track type) {
                <option [value]="type">{{ type }}</option>
              }
            </select></label
          ><label
            >Sequence<input
              type="number"
              min="1"
              formControlName="sequenceNumber" /></label
          ><label
            >Maximum length<input
              type="number"
              min="1"
              formControlName="maxLength" /></label
          ><label
            >Instructions<textarea
              formControlName="instructions"
            ></textarea></label
          ><label class="check"
            ><input type="checkbox" formControlName="required" />Required</label
          ><button class="button button--primary" type="submit">
            Add section
          </button>
        </form>
      }
      <ol>
        @for (section of sections(); track section.id) {
          <li>
            <strong>{{ section.title }}</strong
            ><span
              >{{ section.sectionKey }} · {{ section.sectionType }} ·
              {{ section.required ? "Required" : "Optional" }}</span
            >
            <p>{{ section.instructions }}</p>
          </li>
        } @empty {
          <li>No sections configured.</li>
        }
      </ol>
    </section>
  `,
  styles: `
    .heading {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .heading h2 {
      margin: 0;
    }
    form {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
      margin: 1rem 0;
      padding: 1rem;
      background: var(--canvas);
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
    ol {
      padding-left: 1.5rem;
    }
    li {
      padding: 0.75rem;
      border-bottom: 1px solid var(--line);
    }
    li span {
      display: block;
      color: var(--ink-muted);
    }
    @media (max-width: 600px) {
      form {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class TemplateDetailComponent implements OnInit {
  private readonly api = inject(GrantReportingApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  readonly template = signal<GrantReportTemplate | null>(null);
  readonly sections = signal<TemplateSection[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly adding = signal(false);
  readonly types: ReportSectionType[] = [
    "NARRATIVE",
    "METRICS",
    "FINANCIAL",
    "OUTCOMES",
    "CHALLENGES",
    "DEMOGRAPHICS",
    "CUSTOM",
  ];
  organizationId = "";
  templateId = "";
  readonly form = this.fb.nonNullable.group({
    sectionKey: [
      "",
      [Validators.required, Validators.pattern(/^[a-z][a-z0-9_.-]*$/)],
    ],
    title: ["", Validators.required],
    instructions: [""],
    sequenceNumber: [1, [Validators.required, Validators.min(1)]],
    sectionType: ["NARRATIVE" as ReportSectionType, Validators.required],
    required: [true],
    maxLength: [5000],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.templateId = this.route.snapshot.paramMap.get("templateId") ?? "";
    this.load();
  }
  load() {
    forkJoin({
      template: this.api.template(this.organizationId, this.templateId),
      sections: this.api.templateSections(this.organizationId, this.templateId),
    }).subscribe({
      next: (r) => {
        this.template.set(r.template);
        this.sections.set(r.sections);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  add() {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.api
      .addTemplateSection(this.organizationId, this.templateId, {
        ...v,
        instructions: v.instructions || null,
        maxLength: v.maxLength || null,
      })
      .subscribe({
        next: () => {
          this.adding.set(false);
          this.form.reset({
            sequenceNumber: this.sections().length + 2,
            sectionType: "NARRATIVE",
            required: true,
            maxLength: 5000,
          });
          this.load();
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
}
