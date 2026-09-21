import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  BoardCommittee,
  BoardMeeting,
  BoardMeetingStatus,
  BoardMeetingType,
  BoardMemberSummary,
  BoardSummaryReport,
  BoardVotingReport,
} from "../../core/models/board.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { BoardApiService } from "./board-api.service";

@Component({
  selector: "cop-board-list",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    AsyncStateComponent,
    PaginationComponent,
    StatusBadgeComponent,
  ],
  template: `
    <header class="page-header">
      <div>
        <p class="eyebrow">BOARD</p>
        <h1>Board governance</h1>
        <p>
          Members, meetings, agenda, attendance, motions, minutes, and
          resolutions.
        </p>
      </div>
      @if (canManage()) {
        <button
          class="button button--primary"
          (click)="showMeeting.set(!showMeeting())"
        >
          New meeting
        </button>
      }
    </header>
    <cop-api-error [error]="error()" />
    @if (summary(); as report) {
      <section class="metrics" aria-label="Board summary">
        <article>
          <span>Members</span><strong>{{ report.activeBoardMembers }}</strong>
        </article>
        <article>
          <span>Committees</span><strong>{{ report.activeCommittees }}</strong>
        </article>
        <article>
          <span>Meetings</span><strong>{{ report.meetingsInPeriod }}</strong>
        </article>
        <article>
          <span>Attendance</span
          ><strong>{{ report.averageAttendanceRate }}%</strong>
        </article>
        <article>
          <span>Quorum failures</span
          ><strong>{{ report.quorumFailures }}</strong>
        </article>
        <article>
          <span>Resolutions</span
          ><strong>{{ report.resolutionsAdopted }}</strong>
        </article>
      </section>
    }
    @if (canManage() && showMeeting()) {
      <form
        class="panel form-grid"
        [formGroup]="meetingForm"
        (ngSubmit)="createMeeting()"
      >
        <h2>Create meeting</h2>
        <label>Title<input formControlName="title" /></label>
        <label
          >Type<select formControlName="meetingType">
            @for (type of meetingTypes; track type) {
              <option [value]="type">{{ type }}</option>
            }
          </select></label
        >
        <label
          >Start<input type="datetime-local" formControlName="startDateTime"
        /></label>
        <label
          >End<input type="datetime-local" formControlName="endDateTime"
        /></label>
        <label
          >Quorum required<input
            type="number"
            min="1"
            formControlName="quorumRequired"
        /></label>
        <label>Location<input formControlName="location" /></label>
        <button class="button button--primary" [disabled]="meetingForm.invalid">
          Create meeting
        </button>
      </form>
    }
    @if (canManage() && showMember()) {
      <form
        class="panel form-grid"
        [formGroup]="memberForm"
        (ngSubmit)="createMember()"
      >
        <h2>Create board member</h2>
        <label>First name<input formControlName="firstName" /></label>
        <label>Last name<input formControlName="lastName" /></label>
        <label>Title<input formControlName="title" /></label>
        <label>Email<input formControlName="email" /></label>
        <label>Joined<input type="date" formControlName="joinedDate" /></label>
        <label>User ID<input formControlName="userId" /></label>
        <button class="button button--primary" [disabled]="memberForm.invalid">
          Create member
        </button>
      </form>
    }
    <form class="filters" [formGroup]="filters" (ngSubmit)="loadMeetings(0)">
      <label
        >Status<select formControlName="status">
          <option value="">All</option>
          @for (status of statuses; track status) {
            <option [value]="status">{{ status.replaceAll("_", " ") }}</option>
          }
        </select></label
      >
      <button class="button button--secondary">Apply</button>
      @if (canManage()) {
        <button
          type="button"
          class="button button--quiet"
          (click)="showMember.set(!showMember())"
        >
          New member
        </button>
      }
    </form>
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (meetings()?.content?.length === 0) {
      <cop-async-state
        state="empty"
        detail="No board meetings match these filters."
      />
    } @else if (meetings()) {
      @let page = meetings()!;
      <section class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>Meeting</th>
              <th>Type</th>
              <th>Start</th>
              <th>Status</th>
              <th>Quorum</th>
            </tr>
          </thead>
          <tbody>
            @for (meeting of page.content; track meeting.id) {
              <tr>
                <th scope="row">
                  <a [routerLink]="['meetings', meeting.id]">{{
                    meeting.title
                  }}</a>
                </th>
                <td>{{ meeting.meetingType }}</td>
                <td>{{ meeting.startDateTime }}</td>
                <td><cop-status-badge [status]="meeting.status" /></td>
                <td>{{ meeting.quorumRequired }}</td>
              </tr>
            }
          </tbody>
        </table>
        <cop-pagination
          [page]="page.page"
          [totalPages]="page.totalPages"
          [totalElements]="page.totalElements"
          [first]="page.first"
          [last]="page.last"
          (pageChange)="loadMeetings($event)"
        />
      </section>
    }
    <section class="columns">
      <article class="panel">
        <h2>Members</h2>
        <div class="stack-list">
          @for (member of members()?.content ?? []; track member.id) {
            <a [routerLink]="['members', member.id]"
              >{{ member.firstName }} {{ member.lastName }} ·
              {{ member.title || "Member" }}</a
            >
          }
        </div>
      </article>
      <article class="panel">
        <h2>Committees</h2>
        <div class="stack-list">
          @for (committee of committees()?.content ?? []; track committee.id) {
            <span
              >{{ committee.name }} ·
              {{ committee.active ? "Active" : "Inactive" }}</span
            >
          }
        </div>
      </article>
      <article class="panel">
        <h2>Voting</h2>
        @if (voting(); as voteReport) {
          <p>
            {{ voteReport.motionsPassed }} passed ·
            {{ voteReport.motionsFailed }} failed ·
            {{ voteReport.participationRate }}% participation
          </p>
        }
      </article>
    </section>
  `,
  styles: `
    .metrics,
    .form-grid,
    .filters,
    .columns {
      display: grid;
      gap: 0.75rem;
    }
    .metrics {
      grid-template-columns: repeat(6, 1fr);
      margin-bottom: 1rem;
    }
    .metrics article {
      padding: 1rem;
      background: white;
      border: 1px solid var(--line);
    }
    .metrics span {
      display: block;
      color: var(--ink-muted);
    }
    .form-grid {
      grid-template-columns: repeat(3, 1fr);
      margin-bottom: 1rem;
    }
    .filters {
      grid-template-columns: 1fr auto auto;
      align-items: end;
      margin-bottom: 1rem;
    }
    .columns {
      grid-template-columns: repeat(3, 1fr);
      margin-top: 1rem;
    }
    .form-grid h2 {
      grid-column: 1/-1;
    }
    label,
    .stack-list {
      display: grid;
      gap: 0.3rem;
    }
    .stack-list a,
    .stack-list span {
      padding: 0.65rem 0;
      border-bottom: 1px solid var(--line);
      text-decoration: none;
    }
    @media (max-width: 960px) {
      .metrics,
      .columns {
        grid-template-columns: repeat(2, 1fr);
      }
      .form-grid {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 560px) {
      .metrics,
      .form-grid,
      .filters,
      .columns {
        grid-template-columns: 1fr;
      }
      .form-grid h2 {
        grid-column: auto;
      }
    }
  `,
})
export class BoardListComponent implements OnInit {
  private readonly api = inject(BoardApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly meetings = signal<PageResponse<BoardMeeting> | null>(null);
  readonly members = signal<PageResponse<BoardMemberSummary> | null>(null);
  readonly committees = signal<PageResponse<BoardCommittee> | null>(null);
  readonly summary = signal<BoardSummaryReport | null>(null);
  readonly voting = signal<BoardVotingReport | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly loading = signal(true);
  readonly showMeeting = signal(false);
  readonly showMember = signal(false);
  organizationId = "";
  readonly statuses: BoardMeetingStatus[] = [
    "DRAFT",
    "PUBLISHED",
    "IN_PROGRESS",
    "COMPLETED",
    "CANCELLED",
  ];
  readonly meetingTypes: BoardMeetingType[] = [
    "REGULAR",
    "SPECIAL",
    "ANNUAL",
    "EMERGENCY",
    "COMMITTEE",
    "OTHER",
  ];
  readonly filters = this.fb.nonNullable.group({
    status: ["" as BoardMeetingStatus | ""],
  });
  readonly meetingForm = this.fb.nonNullable.group({
    title: ["", Validators.required],
    meetingType: ["REGULAR" as BoardMeetingType],
    startDateTime: ["", Validators.required],
    endDateTime: ["", Validators.required],
    quorumRequired: [1, Validators.min(1)],
    location: [""],
  });
  readonly memberForm = this.fb.nonNullable.group({
    userId: [""],
    firstName: ["", Validators.required],
    lastName: ["", Validators.required],
    title: [""],
    email: [""],
    joinedDate: [new Date().toISOString().slice(0, 10), Validators.required],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.loadMeetings();
    this.loadSupportingData();
  }

  canManage(): boolean {
    return this.context.hasAnyRole("BOARD_MANAGER");
  }

  loadMeetings(page = 0): void {
    this.loading.set(true);
    this.api
      .meetings(this.organizationId, { ...this.filters.getRawValue(), page })
      .subscribe({
        next: (result) => {
          this.meetings.set(result);
          this.loading.set(false);
        },
        error: (error) => {
          this.error.set(this.errors.from(error));
          this.loading.set(false);
        },
      });
  }

  loadSupportingData(): void {
    forkJoin({
      members: this.api.members(this.organizationId, { status: "", page: 0 }),
      committees: this.api.committees(this.organizationId),
      summary: this.api.summaryReport(this.organizationId),
      voting: this.api.votingReport(this.organizationId),
    }).subscribe({
      next: (result) => {
        this.members.set(result.members);
        this.committees.set(result.committees);
        this.summary.set(result.summary);
        this.voting.set(result.voting);
      },
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  createMeeting(): void {
    if (this.meetingForm.invalid) return;
    const value = this.meetingForm.getRawValue();
    this.api
      .createMeeting(this.organizationId, {
        ...value,
        startDateTime: new Date(value.startDateTime).toISOString(),
        endDateTime: new Date(value.endDateTime).toISOString(),
        location: value.location || null,
        virtualMeetingUrl: null,
      })
      .subscribe({
        next: () => {
          this.showMeeting.set(false);
          this.loadMeetings();
          this.loadSupportingData();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  createMember(): void {
    if (this.memberForm.invalid) return;
    const value = this.memberForm.getRawValue();
    this.api
      .createMember(this.organizationId, {
        ...value,
        userId: value.userId || null,
        title: value.title || null,
        email: value.email || null,
        phone: null,
        notes: null,
      })
      .subscribe({
        next: () => {
          this.showMember.set(false);
          this.loadSupportingData();
        },
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
