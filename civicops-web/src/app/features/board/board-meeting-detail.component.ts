import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  AttendanceStatus,
  BoardAgendaItem,
  BoardAttendance,
  BoardMeeting,
  BoardMinutes,
  BoardMotion,
  BoardQuorum,
} from "../../core/models/board.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { BoardApiService } from "./board-api.service";

@Component({
  selector: "cop-board-meeting-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a class="back-link" [routerLink]="['..', '..']">Back to board</a>
    <cop-api-error [error]="error()" />
    @if (meeting(); as meeting) {
      <header class="page-header">
        <div>
          <p class="eyebrow">BOARD MEETING</p>
          <h1>{{ meeting.title }}</h1>
          <p>{{ meeting.meetingType }} · {{ meeting.startDateTime }}</p>
        </div>
        <cop-status-badge [status]="meeting.status" />
      </header>
      @if (quorum(); as q) {
        <section class="panel detail-grid">
          <div>
            <span>Quorum required</span><strong>{{ q.quorumRequired }}</strong>
          </div>
          <div>
            <span>Present or remote</span
            ><strong>{{ q.presentOrRemote }}</strong>
          </div>
          <div>
            <span>Quorum</span
            ><strong>{{ q.quorumMet ? "Met" : "Not met" }}</strong>
          </div>
          <div>
            <span>Attendance rate</span><strong>{{ q.attendanceRate }}%</strong>
          </div>
        </section>
      }
      @if (canManage()) {
        <div class="actions">
          <button
            class="button button--secondary"
            (click)="meetingAction('publish')"
          >
            Publish
          </button>
          <button
            class="button button--secondary"
            (click)="meetingAction('start')"
          >
            Start
          </button>
          <button
            class="button button--secondary"
            (click)="meetingAction('complete')"
          >
            Complete
          </button>
          <button
            class="button button--quiet"
            (click)="meetingAction('cancel')"
          >
            Cancel
          </button>
        </div>
      }
      @if (canBoardMember()) {
        <form
          class="panel compact-form"
          [formGroup]="selfAttendanceForm"
          (ngSubmit)="saveSelfAttendance()"
        >
          <h2>My attendance</h2>
          <label
            >Status<select formControlName="attendanceStatus">
              @for (status of attendanceStatuses; track status) {
                <option [value]="status">{{ status }}</option>
              }
            </select></label
          >
          <label>Notes<input formControlName="notes" /></label>
          <button class="button button--primary">Save</button>
        </form>
      }
      @if (canManage()) {
        <form
          class="panel form-grid"
          [formGroup]="agendaForm"
          (ngSubmit)="createAgendaItem()"
        >
          <h2>Add agenda item</h2>
          <label>Title<input formControlName="title" /></label>
          <label>Type<input formControlName="itemType" /></label>
          <label
            >Order<input type="number" min="1" formControlName="sequenceNumber"
          /></label>
          <label class="wide"
            >Description<textarea formControlName="description"></textarea>
          </label>
          <button
            class="button button--primary"
            [disabled]="agendaForm.invalid"
          >
            Add item
          </button>
        </form>
        <form
          class="panel form-grid"
          [formGroup]="motionForm"
          (ngSubmit)="createMotion()"
        >
          <h2>Create motion</h2>
          <label
            >Moved by board member ID<input
              formControlName="movedByBoardMemberId"
          /></label>
          <label>Agenda item ID<input formControlName="agendaItemId" /></label>
          <label class="wide"
            >Motion text<textarea formControlName="motionText"></textarea>
          </label>
          <button
            class="button button--primary"
            [disabled]="motionForm.invalid"
          >
            Create motion
          </button>
        </form>
      }
      <section class="columns">
        <article class="panel">
          <h2>Agenda</h2>
          <div class="stack-list">
            @for (item of agenda()?.content ?? []; track item.id) {
              <span
                >{{ item.sequenceNumber }}. {{ item.title }} ·
                {{ item.status }}</span
              >
            }
          </div>
        </article>
        <article class="panel">
          <h2>Motions</h2>
          <div class="stack-list">
            @for (motion of motions()?.content ?? []; track motion.id) {
              <a [routerLink]="['..', '..', 'motions', motion.id]">
                {{ motion.motionText }}
                <cop-status-badge [status]="motion.status" />
              </a>
            }
          </div>
        </article>
        <article class="panel">
          <h2>Attendance</h2>
          <div class="stack-list">
            @for (record of attendance()?.content ?? []; track record.id) {
              <span
                >{{ record.memberName }} · {{ record.attendanceStatus }}</span
              >
            }
          </div>
        </article>
      </section>
      @if (canManage()) {
        <form
          class="panel minutes"
          [formGroup]="minutesForm"
          (ngSubmit)="saveMinutes()"
        >
          <h2>Minutes</h2>
          <textarea formControlName="content"></textarea>
          <div class="actions">
            <button class="button button--primary">Save minutes</button>
            @if (minutes(); as minutes) {
              <button
                type="button"
                class="button button--secondary"
                (click)="minutesAction('submit')"
              >
                Submit
              </button>
              <button
                type="button"
                class="button button--secondary"
                (click)="minutesAction('approve')"
              >
                Approve
              </button>
            }
          </div>
        </form>
      }
    }
  `,
  styles: `
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    .detail-grid,
    .compact-form,
    .form-grid,
    .columns {
      display: grid;
      gap: 0.75rem;
    }
    .detail-grid {
      grid-template-columns: repeat(4, 1fr);
      margin-bottom: 1rem;
    }
    .compact-form {
      grid-template-columns: 1fr 1fr auto;
      align-items: end;
      margin-bottom: 1rem;
    }
    .form-grid {
      grid-template-columns: repeat(3, 1fr);
      margin-bottom: 1rem;
    }
    .columns {
      grid-template-columns: repeat(3, 1fr);
      margin-bottom: 1rem;
    }
    .compact-form h2,
    .form-grid h2,
    .wide {
      grid-column: 1/-1;
    }
    label,
    .stack-list,
    .minutes {
      display: grid;
      gap: 0.3rem;
    }
    .stack-list a,
    .stack-list span {
      display: flex;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.65rem 0;
      border-bottom: 1px solid var(--line);
      text-decoration: none;
    }
    .minutes textarea {
      min-height: 12rem;
    }
    span {
      color: var(--ink-muted);
    }
    @media (max-width: 900px) {
      .detail-grid,
      .columns {
        grid-template-columns: repeat(2, 1fr);
      }
      .form-grid {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 560px) {
      .detail-grid,
      .compact-form,
      .form-grid,
      .columns {
        grid-template-columns: 1fr;
      }
      .compact-form h2,
      .form-grid h2,
      .wide {
        grid-column: auto;
      }
    }
  `,
})
export class BoardMeetingDetailComponent implements OnInit {
  private readonly api = inject(BoardApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly meeting = signal<BoardMeeting | null>(null);
  readonly quorum = signal<BoardQuorum | null>(null);
  readonly agenda = signal<PageResponse<BoardAgendaItem> | null>(null);
  readonly motions = signal<PageResponse<BoardMotion> | null>(null);
  readonly attendance = signal<PageResponse<BoardAttendance> | null>(null);
  readonly minutes = signal<BoardMinutes | null>(null);
  readonly error = signal<ApiError | null>(null);
  organizationId = "";
  meetingId = "";
  readonly attendanceStatuses: AttendanceStatus[] = [
    "PRESENT",
    "REMOTE",
    "ABSENT",
    "EXCUSED",
  ];
  readonly selfAttendanceForm = this.fb.nonNullable.group({
    attendanceStatus: ["PRESENT" as AttendanceStatus],
    notes: [""],
  });
  readonly agendaForm = this.fb.nonNullable.group({
    title: ["", Validators.required],
    itemType: ["DISCUSSION", Validators.required],
    sequenceNumber: [1, Validators.min(1)],
    description: [""],
  });
  readonly motionForm = this.fb.nonNullable.group({
    movedByBoardMemberId: ["", Validators.required],
    agendaItemId: [""],
    motionText: ["", Validators.required],
  });
  readonly minutesForm = this.fb.nonNullable.group({
    content: [""],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.meetingId = this.route.snapshot.paramMap.get("meetingId") ?? "";
    this.load();
  }

  canManage(): boolean {
    return this.context.hasAnyRole("BOARD_MANAGER");
  }

  canBoardMember(): boolean {
    return this.context.selectedRole() === "BOARD_MEMBER";
  }

  load(): void {
    forkJoin({
      meeting: this.api.meeting(this.organizationId, this.meetingId),
      quorum: this.api.quorum(this.organizationId, this.meetingId),
      agenda: this.api.agenda(this.organizationId, this.meetingId),
      motions: this.api.motions(this.organizationId, this.meetingId),
      attendance: this.api.attendance(this.organizationId, this.meetingId),
      minutes: this.api.minutes(this.organizationId, this.meetingId),
    }).subscribe({
      next: (result) => {
        this.meeting.set(result.meeting);
        this.quorum.set(result.quorum);
        this.agenda.set(result.agenda);
        this.motions.set(result.motions);
        this.attendance.set(result.attendance);
        this.minutes.set(result.minutes);
        this.minutesForm.patchValue({ content: result.minutes.content });
      },
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  meetingAction(action: "publish" | "start" | "complete" | "cancel"): void {
    this.api
      .meetingAction(this.organizationId, this.meetingId, action)
      .subscribe({
        next: (meeting) => {
          this.meeting.set(meeting);
          this.load();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  saveSelfAttendance(): void {
    const value = this.selfAttendanceForm.getRawValue();
    this.api
      .selfAttendance(this.organizationId, this.meetingId, {
        ...value,
        notes: value.notes || null,
      })
      .subscribe({
        next: () => this.load(),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  createAgendaItem(): void {
    if (this.agendaForm.invalid) return;
    const value = this.agendaForm.getRawValue();
    this.api
      .createAgendaItem(this.organizationId, this.meetingId, {
        ...value,
        description: value.description || null,
        presenter: null,
        estimatedMinutes: null,
      })
      .subscribe({
        next: () => this.load(),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  createMotion(): void {
    if (this.motionForm.invalid) return;
    const value = this.motionForm.getRawValue();
    this.api
      .createMotion(this.organizationId, this.meetingId, {
        ...value,
        agendaItemId: value.agendaItemId || null,
      })
      .subscribe({
        next: () => this.load(),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  saveMinutes(): void {
    this.api
      .saveMinutes(
        this.organizationId,
        this.meetingId,
        this.minutesForm.getRawValue(),
      )
      .subscribe({
        next: (minutes) => this.minutes.set(minutes),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  minutesAction(action: "submit" | "approve"): void {
    this.api
      .minutesAction(this.organizationId, this.meetingId, action)
      .subscribe({
        next: (minutes) => this.minutes.set(minutes),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
