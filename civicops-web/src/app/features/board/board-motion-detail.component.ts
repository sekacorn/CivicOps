import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { forkJoin } from "rxjs";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import {
  BoardMotion,
  BoardVote,
  VoteChoice,
} from "../../core/models/board.models";
import { OrganizationContextService } from "../../core/organization/organization-context.service";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { BoardApiService } from "./board-api.service";

@Component({
  selector: "cop-board-motion-detail",
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ApiErrorComponent,
    StatusBadgeComponent,
  ],
  template: `
    <a class="back-link" [routerLink]="['..']">Back to board</a>
    <cop-api-error [error]="error()" />
    @if (motion(); as motion) {
      <header class="page-header">
        <div>
          <p class="eyebrow">BOARD MOTION</p>
          <h1>{{ motion.motionText }}</h1>
          <p>
            {{ motion.yesVotes }} yes · {{ motion.noVotes }} no ·
            {{ motion.abstainVotes }} abstain
          </p>
        </div>
        <cop-status-badge [status]="motion.status" />
      </header>
      @if (canManage()) {
        <form
          class="panel compact-form"
          [formGroup]="secondForm"
          (ngSubmit)="second()"
        >
          <h2>Second motion</h2>
          <label
            >Seconded by board member ID<input
              formControlName="secondedByBoardMemberId"
          /></label>
          <button
            class="button button--primary"
            [disabled]="secondForm.invalid"
          >
            Second
          </button>
        </form>
        <div class="actions">
          <button
            class="button button--secondary"
            (click)="motionAction('open')"
          >
            Open vote
          </button>
          <button
            class="button button--secondary"
            (click)="motionAction('close')"
          >
            Close vote
          </button>
          <button
            class="button button--secondary"
            (click)="motionAction('table')"
          >
            Table
          </button>
          <button
            class="button button--quiet"
            (click)="motionAction('withdraw')"
          >
            Withdraw
          </button>
        </div>
      }
      @if (canBoardMember()) {
        <form
          class="panel compact-form"
          [formGroup]="voteForm"
          (ngSubmit)="vote()"
        >
          <h2>Cast vote</h2>
          <label
            >Vote<select formControlName="choice">
              @for (choice of choices; track choice) {
                <option [value]="choice">{{ choice }}</option>
              }
            </select></label
          >
          <button class="button button--primary">Vote</button>
        </form>
      }
      @if (canManage()) {
        <form
          class="panel form-grid"
          [formGroup]="resolutionForm"
          (ngSubmit)="createResolution()"
        >
          <h2>Create resolution</h2>
          <label>Number<input formControlName="resolutionNumber" /></label>
          <label>Title<input formControlName="title" /></label>
          <label
            >Adopted date<input type="date" formControlName="adoptedDate"
          /></label>
          <label class="wide"
            >Text<textarea formControlName="text"></textarea>
          </label>
          <button
            class="button button--primary"
            [disabled]="resolutionForm.invalid"
          >
            Create resolution
          </button>
        </form>
      }
      <section class="panel table-wrap">
        <h2>Votes</h2>
        <table>
          <thead>
            <tr>
              <th>Member</th>
              <th>Choice</th>
              <th>Cast</th>
            </tr>
          </thead>
          <tbody>
            @for (vote of votes(); track vote.id) {
              <tr>
                <td>{{ vote.boardMemberId }}</td>
                <td>{{ vote.choice }}</td>
                <td>{{ vote.castAt }}</td>
              </tr>
            }
          </tbody>
        </table>
      </section>
    }
  `,
  styles: `
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    .compact-form,
    .form-grid {
      display: grid;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }
    .compact-form {
      grid-template-columns: 1fr auto;
      align-items: end;
    }
    .form-grid {
      grid-template-columns: repeat(3, 1fr);
    }
    .compact-form h2,
    .form-grid h2,
    .wide {
      grid-column: 1/-1;
    }
    label {
      display: grid;
      gap: 0.3rem;
    }
    @media (max-width: 650px) {
      .compact-form,
      .form-grid {
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
export class BoardMotionDetailComponent implements OnInit {
  private readonly api = inject(BoardApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  private readonly context = inject(OrganizationContextService);
  readonly motion = signal<BoardMotion | null>(null);
  readonly votes = signal<BoardVote[]>([]);
  readonly error = signal<ApiError | null>(null);
  organizationId = "";
  motionId = "";
  readonly choices: VoteChoice[] = ["YES", "NO", "ABSTAIN"];
  readonly voteForm = this.fb.nonNullable.group({
    choice: ["YES" as VoteChoice],
  });
  readonly secondForm = this.fb.nonNullable.group({
    secondedByBoardMemberId: ["", Validators.required],
  });
  readonly resolutionForm = this.fb.nonNullable.group({
    resolutionNumber: ["", Validators.required],
    title: ["", Validators.required],
    text: ["", Validators.required],
    adoptedDate: [new Date().toISOString().slice(0, 10), Validators.required],
  });

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.motionId = this.route.snapshot.paramMap.get("motionId") ?? "";
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
      motion: this.api.motion(this.organizationId, this.motionId),
      votes: this.api.votes(this.organizationId, this.motionId),
    }).subscribe({
      next: (result) => {
        this.motion.set(result.motion);
        this.votes.set(result.votes);
      },
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  motionAction(action: "open" | "close" | "withdraw" | "table"): void {
    this.api
      .motionAction(this.organizationId, this.motionId, action)
      .subscribe({
        next: (motion) => this.motion.set(motion),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  second(): void {
    if (this.secondForm.invalid) return;
    this.api
      .secondMotion(
        this.organizationId,
        this.motionId,
        this.secondForm.controls.secondedByBoardMemberId.value,
      )
      .subscribe({
        next: (motion) => this.motion.set(motion),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  vote(): void {
    this.api
      .vote(this.organizationId, this.motionId, this.voteForm.getRawValue())
      .subscribe({
        next: () => this.load(),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }

  createResolution(): void {
    if (this.resolutionForm.invalid) return;
    this.api
      .createResolution(
        this.organizationId,
        this.motionId,
        this.resolutionForm.getRawValue(),
      )
      .subscribe({
        next: () => this.load(),
        error: (error) => this.error.set(this.errors.from(error)),
      });
  }
}
