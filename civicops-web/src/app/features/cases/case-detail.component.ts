import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import {
  CaseDetail,
  CaseNote,
  CaseNoteType,
  CasePriority,
  CaseServiceRecord,
  CaseServiceType,
  CaseTask,
} from "../../core/models/case.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { ConfirmationDialogComponent } from "../../shared/components/confirmation-dialog.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { CaseApiService } from "./case-api.service";

@Component({
  selector: "cop-case-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
    ConfirmationDialogComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a routerLink="..">← Cases</a><cop-api-error [error]="error()" />
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (record()) {
      @let c = record()!;
      <header class="page-header">
        <div>
          <p class="eyebrow">CASE {{ c.caseNumber }}</p>
          <h1>{{ c.title }}</h1>
          <p>{{ label(c.caseType) }} · opened {{ c.openedDate }}</p>
        </div>
        <cop-status-badge [status]="c.status" />
      </header>
      <section class="metrics">
        <article>
          <span>Client</span
          ><a [routerLink]="['../clients', c.client.id]">{{
            c.client.displayName
          }}</a>
        </article>
        <article>
          <span>Priority</span><strong>{{ c.priority }}</strong>
        </article>
        <article>
          <span>Assigned worker</span
          ><strong>{{ c.assignedUserDisplayName || "Unassigned" }}</strong>
        </article>
        <article>
          <span>Program</span><strong>{{ c.programName || "—" }}</strong>
        </article>
      </section>
      @if (canManage()) {
        <section class="panel">
          <h2>Case controls</h2>
          <div class="actions">
            @if (c.status === "OPEN") {
              <button
                class="button button--primary"
                (click)="transition('start')"
              >
                Start case</button
              ><button
                class="button button--danger"
                (click)="confirm.set('cancel')"
              >
                Cancel case
              </button>
            }
            @if (c.status === "IN_PROGRESS") {
              <button
                class="button button--secondary"
                (click)="transition('hold')"
              >
                Place on hold</button
              ><button
                class="button button--danger"
                (click)="confirm.set('close')"
              >
                Close case
              </button>
            }
            @if (c.status === "ON_HOLD") {
              <button
                class="button button--primary"
                (click)="transition('resume')"
              >
                Resume case
              </button>
            }
          </div>
          <form class="inline" [formGroup]="assignForm" (ngSubmit)="assign()">
            <label
              >Eligible worker user ID<input formControlName="userId" /></label
            ><button
              class="button button--secondary"
              [disabled]="assignForm.invalid"
            >
              Assign worker
            </button>
          </form>
          <p class="hint">
            The backend validates organization membership and Case Worker
            eligibility.
          </p>
        </section>
      }
      <div class="columns">
        <section class="panel">
          <h2>Append note</h2>
          <form class="stack" [formGroup]="noteForm" (ngSubmit)="addNote()">
            <label
              >Type<select formControlName="noteType">
                @for (t of noteTypes; track t) {
                  <option [value]="t">{{ label(t) }}</option>
                }
              </select></label
            ><label>Note<textarea formControlName="content"></textarea></label
            ><label class="check"
              ><input type="checkbox" formControlName="privateNote" /> Private
              internal note</label
            ><button
              class="button button--primary"
              [disabled]="noteForm.invalid"
            >
              Add immutable note
            </button>
          </form>
          @for (n of notes(); track n.id) {
            <article>
              <div>
                <strong>{{ label(n.noteType) }}</strong> ·
                {{ n.authorDisplayName }} · {{ local(n.createdAt) }}
              </div>
              <p>{{ n.content }}</p>
              @if (n.privateNote) {
                <small>Private note</small>
              }
            </article>
          } @empty {
            <p>No notes recorded.</p>
          }
        </section>
        <section class="panel">
          <h2>Tasks</h2>
          <form class="stack" [formGroup]="taskForm" (ngSubmit)="addTask()">
            <label>Title<input formControlName="title" /></label
            ><label
              >Due date<input type="date" formControlName="dueDate" /></label
            ><label
              >Priority<select formControlName="priority">
                @for (p of priorities; track p) {
                  <option [value]="p">{{ p }}</option>
                }
              </select></label
            ><button
              class="button button--primary"
              [disabled]="taskForm.invalid"
            >
              Create task
            </button>
          </form>
          @for (t of tasks(); track t.id) {
            <article>
              <div class="section-head">
                <strong>{{ t.title }}</strong
                ><cop-status-badge [status]="t.status" />
              </div>
              <p>
                Due {{ t.dueDate || "not set" }}
                @if (t.overdue) {
                  · Overdue
                }
              </p>
              @if (t.status === "OPEN" || t.status === "IN_PROGRESS") {
                <div class="actions">
                  <button
                    class="button button--primary"
                    (click)="taskAction(t, 'complete')"
                  >
                    Complete</button
                  ><button
                    class="button button--secondary"
                    (click)="taskAction(t, 'cancel')"
                  >
                    Cancel
                  </button>
                </div>
              }
            </article>
          } @empty {
            <p>No tasks recorded.</p>
          }
        </section>
      </div>
      <section class="panel services">
        <h2>Service records</h2>
        <form
          class="service-form"
          [formGroup]="serviceForm"
          (ngSubmit)="addService()"
        >
          <label
            >Type<select formControlName="serviceType">
              @for (t of serviceTypes; track t) {
                <option [value]="t">{{ label(t) }}</option>
              }
            </select></label
          ><label>Date<input type="date" formControlName="serviceDate" /></label
          ><label>Description<input formControlName="description" /></label
          ><label
            >Quantity<input
              type="number"
              min="0"
              formControlName="quantity" /></label
          ><label>Unit<input formControlName="unit" /></label
          ><label
            >Value<input
              type="number"
              min="0"
              step=".01"
              formControlName="valueAmount" /></label
          ><button
            class="button button--primary"
            [disabled]="serviceForm.invalid"
          >
            Record service
          </button>
        </form>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Service</th>
                <th>Description</th>
                <th>Quantity</th>
                <th>Value</th>
              </tr>
            </thead>
            <tbody>
              @for (s of services(); track s.id) {
                <tr>
                  <td>{{ s.serviceDate }}</td>
                  <th scope="row">{{ label(s.serviceType) }}</th>
                  <td>{{ s.description || "—" }}</td>
                  <td>{{ s.quantity ?? "—" }} {{ s.unit || "" }}</td>
                  <td>{{ s.valueAmount ?? "—" }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
      @if (confirm(); as action) {
        <cop-confirmation-dialog
          title="Confirm terminal Case transition"
          [message]="
            action === 'close'
              ? 'Closing preserves all notes, tasks, and service history.'
              : 'Cancelling preserves the Case audit history.'
          "
          [confirmLabel]="action === 'close' ? 'Close case' : 'Cancel case'"
          (confirmed)="transition(action)"
          (cancelled)="confirm.set(null)"
        />
      }
    }
  `,
  styles: `
    .metrics {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .metrics article {
      display: grid;
      padding: 1rem;
      background: white;
      border: 1px solid var(--line);
    }
    .metrics span,
    .hint {
      color: var(--ink-muted);
    }
    .panel {
      margin-bottom: 1rem;
    }
    .actions,
    .section-head {
      display: flex;
      gap: 0.6rem;
      flex-wrap: wrap;
      align-items: center;
    }
    .inline {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 0.6rem;
      align-items: end;
      margin-top: 1rem;
    }
    .columns {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }
    .stack {
      display: grid;
      gap: 0.65rem;
      margin-bottom: 1rem;
    }
    .stack label,
    .inline label,
    .service-form label {
      display: grid;
      gap: 0.3rem;
    }
    .check {
      display: flex !important;
      align-items: center;
      gap: 0.4rem;
    }
    .check input {
      width: auto;
    }
    article {
      padding: 0.75rem 0;
      border-bottom: 1px solid var(--line);
    }
    .service-form {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.7rem;
      margin-bottom: 1rem;
    }
    @media (max-width: 850px) {
      .metrics {
        grid-template-columns: 1fr 1fr;
      }
      .columns {
        grid-template-columns: 1fr;
      }
      .service-form {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 560px) {
      .metrics,
      .service-form,
      .inline {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class CaseDetailComponent implements OnInit {
  private readonly api = inject(CaseApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly record = signal<CaseDetail | null>(null);
  readonly notes = signal<CaseNote[]>([]);
  readonly tasks = signal<CaseTask[]>([]);
  readonly services = signal<CaseServiceRecord[]>([]);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  readonly confirm = signal<"close" | "cancel" | null>(null);
  organizationId = "";
  caseId = "";
  readonly noteTypes: CaseNoteType[] = [
    "GENERAL",
    "CONTACT",
    "PROGRESS",
    "ASSESSMENT",
    "INTERNAL",
  ];
  readonly priorities: CasePriority[] = ["LOW", "NORMAL", "HIGH", "URGENT"];
  readonly serviceTypes: CaseServiceType[] = [
    "FOOD_ASSISTANCE",
    "HOUSING_ASSISTANCE",
    "TRANSPORTATION",
    "COUNSELING_REFERRAL",
    "EMPLOYMENT_SUPPORT",
    "EDUCATION_SUPPORT",
    "FINANCIAL_ASSISTANCE",
    "OTHER",
  ];
  readonly assignForm = this.fb.nonNullable.group({
    userId: ["", Validators.required],
  });
  readonly noteForm = this.fb.nonNullable.group({
    noteType: ["GENERAL" as CaseNoteType],
    content: ["", Validators.required],
    privateNote: [false],
  });
  readonly taskForm = this.fb.nonNullable.group({
    title: ["", Validators.required],
    dueDate: [""],
    priority: ["NORMAL" as CasePriority],
  });
  readonly serviceForm = this.fb.nonNullable.group({
    serviceType: ["OTHER" as CaseServiceType],
    serviceDate: [new Date().toISOString().slice(0, 10), Validators.required],
    description: [""],
    quantity: [0],
    unit: [""],
    valueAmount: [0],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.caseId = this.route.snapshot.paramMap.get("caseId") ?? "";
    this.load();
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "CASE_MANAGER");
  }
  label(v: string) {
    return v.replaceAll("_", " ");
  }
  local(v: string) {
    return new Date(v).toLocaleString();
  }
  load() {
    this.loading.set(true);
    forkJoin({
      record: this.api.case(this.organizationId, this.caseId),
      notes: this.api.notes(this.organizationId, this.caseId),
      tasks: this.api.tasks(this.organizationId, this.caseId),
      services: this.api.services(this.organizationId, this.caseId),
    }).subscribe({
      next: (r) => {
        this.record.set(r.record);
        this.notes.set(r.notes.content);
        this.tasks.set(r.tasks.content);
        this.services.set(r.services.content);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(this.errors.from(e));
        this.loading.set(false);
      },
    });
  }
  transition(action: "start" | "hold" | "resume" | "close" | "cancel") {
    this.api.transition(this.organizationId, this.caseId, action).subscribe({
      next: (r) => {
        this.record.set(r);
        this.confirm.set(null);
      },
      error: (e) => this.error.set(this.errors.from(e)),
    });
  }
  assign() {
    if (this.assignForm.invalid) return;
    this.api
      .assign(
        this.organizationId,
        this.caseId,
        this.assignForm.controls.userId.value,
      )
      .subscribe({
        next: (r) => this.record.set(r),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  addNote() {
    if (this.noteForm.invalid) return;
    this.api
      .addNote(this.organizationId, this.caseId, this.noteForm.getRawValue())
      .subscribe({
        next: (r) => {
          this.notes.update((v) => [r, ...v]);
          this.noteForm.reset({
            noteType: "GENERAL",
            content: "",
            privateNote: false,
          });
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  addTask() {
    if (this.taskForm.invalid) return;
    const v = this.taskForm.getRawValue();
    this.api
      .addTask(this.organizationId, this.caseId, {
        ...v,
        dueDate: v.dueDate || null,
        description: null,
        assignedUserId: null,
      })
      .subscribe({
        next: (r) => {
          this.tasks.update((x) => [r, ...x]);
          this.taskForm.reset({ title: "", dueDate: "", priority: "NORMAL" });
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  taskAction(task: CaseTask, action: "complete" | "cancel") {
    this.api
      .taskAction(this.organizationId, this.caseId, task.id, action)
      .subscribe({
        next: (r) =>
          this.tasks.update((all) => all.map((t) => (t.id === r.id ? r : t))),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  addService() {
    if (this.serviceForm.invalid) return;
    const v = this.serviceForm.getRawValue();
    this.api
      .addService(this.organizationId, this.caseId, {
        ...v,
        description: v.description || null,
        unit: v.unit || null,
        providedByUserId: null,
        notes: null,
      })
      .subscribe({
        next: (r) => {
          this.services.update((x) => [r, ...x]);
        },
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
}
