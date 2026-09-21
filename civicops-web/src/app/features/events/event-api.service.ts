import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  EventDetail,
  EventReport,
  EventStatus,
  EventSummary,
  EventType,
  RegistrationDetail,
  RegistrationStatus,
  RegistrationSummary,
} from "../../core/models/operations.models";

export interface EventFilters {
  status?: EventStatus | "";
  type?: EventType | "";
  from?: string;
  to?: string;
  grantId?: string;
  campaignId?: string;
  page: number;
}
export interface EventMutation {
  name: string;
  description: string | null;
  eventType: EventType;
  location: string | null;
  startDateTime: string;
  endDateTime: string;
  capacity: number | null;
  registrationRequired: boolean;
  registrationDeadline: string | null;
  waitlistEnabled: boolean;
  linkedGrantId: string | null;
  linkedDonationCampaignId: string | null;
}

@Injectable({ providedIn: "root" })
export class EventApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  private events(org: string) {
    return `${this.base}/organizations/${org}/events`;
  }
  list(org: string, filter: EventFilters) {
    let p = new HttpParams()
      .set("page", filter.page)
      .set("size", 20)
      .set("sort", "startDateTime,asc");
    if (filter.status) p = p.set("status", filter.status);
    if (filter.type) p = p.set("type", filter.type);
    if (filter.from) p = p.set("from", new Date(filter.from).toISOString());
    if (filter.to) p = p.set("to", new Date(filter.to).toISOString());
    if (filter.grantId) p = p.set("grantId", filter.grantId);
    if (filter.campaignId) p = p.set("campaignId", filter.campaignId);
    return this.http.get<PageResponse<EventSummary>>(this.events(org), {
      params: p,
    });
  }
  detail(org: string, id: string) {
    return this.http.get<EventDetail>(`${this.events(org)}/${id}`);
  }
  create(org: string, value: EventMutation) {
    return this.http.post<EventDetail>(this.events(org), value);
  }
  update(org: string, id: string, value: Partial<EventMutation>) {
    return this.http.patch<EventDetail>(`${this.events(org)}/${id}`, value);
  }
  transition(org: string, id: string, action: string) {
    return this.http.post<EventDetail>(
      `${this.events(org)}/${id}/${action}`,
      {},
    );
  }
  registrations(org: string, eventId: string, status?: RegistrationStatus) {
    let params = new HttpParams()
      .set("page", 0)
      .set("size", 50)
      .set("sort", "registrationDate,asc");
    if (status) params = params.set("status", status);
    return this.http.get<PageResponse<RegistrationSummary>>(
      `${this.events(org)}/${eventId}/registrations`,
      { params },
    );
  }
  register(
    org: string,
    eventId: string,
    value: {
      attendeeName: string;
      attendeeEmail: string;
      attendeePhone: string | null;
    },
  ) {
    return this.http.post<RegistrationDetail>(
      `${this.events(org)}/${eventId}/registrations`,
      value,
    );
  }
  selfRegister(
    org: string,
    eventId: string,
    value: {
      attendeeName: string | null;
      attendeeEmail: string | null;
      attendeePhone: string | null;
    },
  ) {
    return this.http.post<RegistrationDetail>(
      `${this.events(org)}/${eventId}/registrations/me`,
      value,
    );
  }
  registrationAction(
    org: string,
    id: string,
    action: "cancel" | "check-in" | "check-out",
  ) {
    return this.http.post<RegistrationDetail>(
      `${this.base}/organizations/${org}/event-registrations/${id}/${action}`,
      {},
    );
  }
  report(org: string, eventId: string) {
    return this.http.get<EventReport>(
      `${this.base}/organizations/${org}/events/${eventId}/report`,
    );
  }
}
