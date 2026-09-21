import { Component, inject, OnInit, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, Router, RouterLink } from "@angular/router";
import { ApiErrorService } from "../../core/http/api-error.service";
import { ApiError } from "../../core/models/api.models";
import { EventType } from "../../core/models/operations.models";
import { ApiErrorComponent } from "../../shared/components/api-error.component";
import { EventApiService, EventMutation } from "./event-api.service";

@Component({
  selector: "cop-event-form",
  imports: [ReactiveFormsModule, RouterLink, ApiErrorComponent],
  template: `<header class="page-header">
      <div>
        <p class="eyebrow">EVENTS</p>
        <h1>{{ eventId ? "Edit event" : "Create event" }}</h1>
        <p>Status changes use explicit lifecycle actions.</p>
      </div>
      <a class="button button--secondary" routerLink="..">Cancel</a>
    </header>
    <cop-api-error [error]="error()" />
    <form class="panel" [formGroup]="form" (ngSubmit)="save()">
      <label>Name<input formControlName="name" /></label
      ><label
        >Type<select formControlName="eventType">
          @for (t of types; track t) {
            <option [value]="t">{{ t.replaceAll("_", " ") }}</option>
          }
        </select></label
      ><label>Location<input formControlName="location" /></label
      ><label
        >Starts<input
          type="datetime-local"
          formControlName="startDateTime" /></label
      ><label
        >Ends<input
          type="datetime-local"
          formControlName="endDateTime" /></label
      ><label
        >Capacity<input
          type="number"
          min="1"
          formControlName="capacity"
          placeholder="Unlimited" /></label
      ><label
        >Registration deadline<input
          type="datetime-local"
          formControlName="registrationDeadline" /></label
      ><label>Linked Grant ID<input formControlName="linkedGrantId" /></label
      ><label
        >Linked campaign ID<input
          formControlName="linkedDonationCampaignId" /></label
      ><label class="wide"
        >Description<textarea formControlName="description"></textarea></label
      ><label class="check"
        ><input
          type="checkbox"
          formControlName="registrationRequired"
        />Registration required</label
      ><label class="check"
        ><input type="checkbox" formControlName="waitlistEnabled" />Waitlist
        enabled</label
      >
      <div class="wide">
        <button class="button button--primary" [disabled]="saving()">
          Save event
        </button>
      </div>
    </form>`,
  styles: `
    form {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0.8rem;
    }
    label {
      display: grid;
      gap: 0.3rem;
    }
    .wide {
      grid-column: 1/-1;
    }
    .check {
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }
    .check input {
      width: auto;
    }
    @media (max-width: 800px) {
      form {
        grid-template-columns: 1fr;
      }
      .wide {
        grid-column: auto;
      }
    }
  `,
})
export class EventFormComponent implements OnInit {
  private readonly api = inject(EventApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly errors = inject(ApiErrorService);
  readonly error = signal<ApiError | null>(null);
  readonly saving = signal(false);
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
  eventId = "";
  readonly form = this.fb.nonNullable.group({
    name: ["", Validators.required],
    description: [""],
    eventType: ["COMMUNITY_OUTREACH" as EventType, Validators.required],
    location: [""],
    startDateTime: ["", Validators.required],
    endDateTime: ["", Validators.required],
    capacity: [null as number | null],
    registrationRequired: [true],
    registrationDeadline: [""],
    waitlistEnabled: [true],
    linkedGrantId: [""],
    linkedDonationCampaignId: [""],
  });
  ngOnInit() {
    this.organizationId =
      this.route.snapshot.paramMap.get("organizationId") ?? "";
    this.eventId = this.route.snapshot.paramMap.get("eventId") ?? "";
    if (this.eventId)
      this.api.detail(this.organizationId, this.eventId).subscribe({
        next: (r) =>
          this.form.patchValue({
            name: r.name,
            description: r.description ?? "",
            eventType: r.eventType,
            location: r.location ?? "",
            startDateTime: this.inputDate(r.startDateTime),
            endDateTime: this.inputDate(r.endDateTime),
            capacity: r.capacity,
            registrationRequired: r.registrationRequired,
            registrationDeadline: r.registrationDeadline
              ? this.inputDate(r.registrationDeadline)
              : "",
            waitlistEnabled: r.waitlistEnabled,
            linkedGrantId: r.linkedGrantId ?? "",
            linkedDonationCampaignId: r.linkedDonationCampaignId ?? "",
          }),
        error: (e) => this.error.set(this.errors.from(e)),
      });
  }
  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (v.endDateTime <= v.startDateTime) {
      this.form.controls.endDateTime.setErrors({ dateRange: true });
      return;
    }
    const payload: EventMutation = {
      ...v,
      description: v.description || null,
      location: v.location || null,
      startDateTime: new Date(v.startDateTime).toISOString(),
      endDateTime: new Date(v.endDateTime).toISOString(),
      capacity: v.capacity || null,
      registrationDeadline: v.registrationDeadline
        ? new Date(v.registrationDeadline).toISOString()
        : null,
      linkedGrantId: v.linkedGrantId || null,
      linkedDonationCampaignId: v.linkedDonationCampaignId || null,
    };
    this.saving.set(true);
    const request = this.eventId
      ? this.api.update(this.organizationId, this.eventId, payload)
      : this.api.create(this.organizationId, payload);
    request.subscribe({
      next: (r) =>
        void this.router.navigate([
          "/organizations",
          this.organizationId,
          "events",
          r.id,
        ]),
      error: (e) => {
        this.error.set(this.errors.from(e));
        this.saving.set(false);
      },
    });
  }
  private inputDate(v: string) {
    const date = new Date(v);
    const localTime = new Date(
      date.getTime() - date.getTimezoneOffset() * 60_000,
    );
    return localTime.toISOString().slice(0, 16);
  }
}
