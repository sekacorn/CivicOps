import { Component, inject, OnInit, signal } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import { BoardMemberDetail } from "../../core/models/board.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { BoardApiService } from "./board-api.service";

@Component({
  selector: "cop-board-member-detail",
  imports: [RouterLink, ApiErrorComponent, StatusBadgeComponent],
  template: `
    <a class="back-link" [routerLink]="['..', '..']">Back to board</a>
    <cop-api-error [error]="error()" />
    @if (member(); as member) {
      <header class="page-header">
        <div>
          <p class="eyebrow">BOARD MEMBER</p>
          <h1>{{ member.firstName }} {{ member.lastName }}</h1>
          <p>{{ member.title || "Member" }} · joined {{ member.joinedDate }}</p>
        </div>
        <cop-status-badge [status]="member.active ? 'ACTIVE' : 'INACTIVE'" />
      </header>
      <div class="actions">
        <button class="button button--quiet" (click)="deactivate()">
          Deactivate
        </button>
      </div>
      <section class="panel detail-grid">
        <div>
          <span>Email</span><strong>{{ member.email || "—" }}</strong>
        </div>
        <div>
          <span>Phone</span><strong>{{ member.phone || "—" }}</strong>
        </div>
        <div>
          <span>User ID</span><strong>{{ member.userId || "—" }}</strong>
        </div>
        <div>
          <span>Notes</span><strong>{{ member.notes || "—" }}</strong>
        </div>
      </section>
    }
  `,
  styles: `
    .actions {
      margin-bottom: 1rem;
    }
    .detail-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 0.75rem;
    }
    span {
      display: block;
      color: var(--ink-muted);
    }
    @media (max-width: 700px) {
      .detail-grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class BoardMemberDetailComponent implements OnInit {
  private readonly api = inject(BoardApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly errors = inject(ApiErrorService);
  readonly member = signal<BoardMemberDetail | null>(null);
  readonly error = signal<ApiError | null>(null);
  organizationId = "";
  memberId = "";

  ngOnInit(): void {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.memberId = this.route.snapshot.paramMap.get("memberId") ?? "";
    this.load();
  }

  load(): void {
    this.api.member(this.organizationId, this.memberId).subscribe({
      next: (member) => this.member.set(member),
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }

  deactivate(): void {
    this.api.deactivateMember(this.organizationId, this.memberId).subscribe({
      next: (member) => this.member.set(member),
      error: (error) => this.error.set(this.errors.from(error)),
    });
  }
}
