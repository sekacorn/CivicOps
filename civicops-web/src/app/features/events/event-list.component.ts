import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError, PageResponse } from "../../core/models/api.models";
import {
  EventStatus,
  EventSummary,
  EventType,
} from "../../core/models/operations.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { AsyncStateComponent } from "../../shared/components/async-state.component";
import { PaginationComponent } from "../../shared/components/pagination.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge.component";
import { EventApiService } from "./event-api.service";
import { OrganizationContextService } from "../../core/organization/organization-context.service";

@Component({
  selector: "cop-event-list",
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
        <p class="eyebrow">COMMUNITY</p>
        <h1>Events</h1>
        <p>Plan programs, manage registration, waitlists, and attendance.</p>
      </div>
      @if (canManage()) {
        <a class="button button--primary" routerLink="new">Create event</a>
      }
    </header>
    <cop-api-error [error]="error()" />
    <form class="filters" [formGroup]="filters" (ngSubmit)="load(0)">
      <label
        >Status<select formControlName="status">
          <option value="">All statuses</option>
          @for (s of statuses; track s) {
            <option [value]="s">{{ s.replaceAll("_", " ") }}</option>
          }
        </select></label
      ><label
        >Type<select formControlName="type">
          <option value="">All types</option>
          @for (t of types; track t) {
            <option [value]="t">{{ t.replaceAll("_", " ") }}</option>
          }
        </select></label
      ><label>From<input type="datetime-local" formControlName="from" /></label
      ><label>To<input type="datetime-local" formControlName="to" /></label
      ><button class="button button--secondary">Apply</button>
    </form>
    @if (loading()) {
      <cop-async-state state="loading" />
    } @else if (events()?.content?.length === 0) {
      <cop-async-state state="empty" detail="No events match these filters." />
    } @else {
      @if (events(); as p) {
        <section class="panel table-wrap">
          <table>
            <thead>
              <tr>
                <th>Event</th>
                <th>Type</th>
                <th>Status</th>
                <th>Starts</th>
                <th>Capacity</th>
              </tr>
            </thead>
            <tbody>
              @for (event of p.content; track event.id) {
                <tr>
                  <th scope="row">
                    <a [routerLink]="[event.id]">{{ event.name }}</a>
                  </th>
                  <td>{{ event.eventType.replaceAll("_", " ") }}</td>
                  <td><cop-status-badge [status]="event.status" /></td>
                  <td>{{ local(event.startDateTime) }}</td>
                  <td>
                    {{ event.capacity === null ? "Unlimited" : event.capacity }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
          <cop-pagination
            [page]="p.page"
            [totalPages]="p.totalPages"
            [totalElements]="p.totalElements"
            [first]="p.first"
            [last]="p.last"
            (pageChange)="load($event)"
          />
        </section>
      }
    }
  `,
  styles: `
    .filters {
      display: grid;
      grid-template-columns: repeat(4, 1fr) auto;
      gap: 0.75rem;
      align-items: end;
      margin-bottom: 1rem;
    }
    .filters label {
      display: grid;
      gap: 0.3rem;
    }
    @media (max-width: 900px) {
      .filters {
        grid-template-columns: 1fr 1fr;
      }
    }
    @media (max-width: 520px) {
      .filters {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class EventListComponent implements OnInit {
  private readonly api = inject(EventApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly errors = inject(ApiErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly context = inject(OrganizationContextService);
  readonly events = signal<PageResponse<EventSummary> | null>(null);
  readonly loading = signal(true);
  readonly error = signal<ApiError | null>(null);
  readonly statuses: EventStatus[] = [
    "DRAFT",
    "PUBLISHED",
    "REGISTRATION_OPEN",
    "REGISTRATION_CLOSED",
    "COMPLETED",
    "CANCELLED",
  ];
  readonly types: EventType[] = [
    "COMMUNITY_OUTREACH",
    "FUNDRAISER",
    "VOLUNTEER_ACTIVITY",
    "TRAINING",
    "WORKSHOP",
    "FOOD_DISTRIBUTION",
    "BOARD_MEETING",
    "PUBLIC_MEETING",
    "SCHOLARSHIP_EVENT",
    "OTHER",
  ];
  organizationId = "";
  readonly filters = this.fb.nonNullable.group({
    status: ["" as EventStatus | ""],
    type: ["" as EventType | ""],
    from: [""],
    to: [""],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.load();
  }
  canManage() {
    return this.context.hasAnyRole("ORG_ADMIN", "EVENT_COORDINATOR");
  }
  load(page = 0) {
    this.loading.set(true);
    this.api
      .list(this.organizationId, { ...this.filters.getRawValue(), page })
      .subscribe({
        next: (r) => {
          this.events.set(r);
          this.loading.set(false);
        },
        error: (e) => {
          this.error.set(this.errors.from(e));
          this.loading.set(false);
        },
      });
  }
  local(v: string) {
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    }).format(new Date(v));
  }
}
