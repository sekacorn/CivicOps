import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  Opportunity,
  VolunteerAssignment,
  VolunteerDetail,
  VolunteerHour,
  VolunteerReport,
  VolunteerShift,
  VolunteerSummary,
} from "../../core/models/operations.models";
@Injectable({ providedIn: "root" })
export class VolunteerApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  private org(id: string) {
    return `${this.base}/organizations/${id}`;
  }
  volunteers(org: string, page = 0) {
    return this.http.get<PageResponse<VolunteerSummary>>(
      `${this.org(org)}/volunteers`,
      {
        params: new HttpParams()
          .set("page", page)
          .set("size", 20)
          .set("sort", "lastName,asc"),
      },
    );
  }
  volunteer(org: string, id: string) {
    return this.http.get<VolunteerDetail>(`${this.org(org)}/volunteers/${id}`);
  }
  createVolunteer(org: string, value: object) {
    return this.http.post<VolunteerDetail>(
      `${this.org(org)}/volunteers`,
      value,
    );
  }
  opportunities(org: string, page = 0) {
    return this.http.get<PageResponse<Opportunity>>(
      `${this.org(org)}/volunteer-opportunities`,
      { params: { page, size: 20, sort: "startAt,asc" } },
    );
  }
  opportunity(org: string, id: string) {
    return this.http.get<Opportunity>(
      `${this.org(org)}/volunteer-opportunities/${id}`,
    );
  }
  createOpportunity(org: string, value: object) {
    return this.http.post<Opportunity>(
      `${this.org(org)}/volunteer-opportunities`,
      value,
    );
  }
  transitionOpportunity(org: string, id: string, action: string) {
    return this.http.post<Opportunity>(
      `${this.org(org)}/volunteer-opportunities/${id}/${action}`,
      {},
    );
  }
  shifts(org: string, opportunityId: string) {
    return this.http.get<PageResponse<VolunteerShift>>(
      `${this.org(org)}/volunteer-opportunities/${opportunityId}/shifts`,
    );
  }
  createShift(org: string, opportunityId: string, value: object) {
    return this.http.post<VolunteerShift>(
      `${this.org(org)}/volunteer-opportunities/${opportunityId}/shifts`,
      value,
    );
  }
  assign(org: string, shiftId: string, volunteerId: string) {
    return this.http.post<VolunteerAssignment>(
      `${this.org(org)}/volunteer-shifts/${shiftId}/assignments`,
      { volunteerId },
    );
  }
  hours(org: string, status = "SUBMITTED") {
    return this.http.get<PageResponse<VolunteerHour>>(
      `${this.org(org)}/volunteer-hours`,
      { params: { status } },
    );
  }
  submitHours(org: string, value: object) {
    return this.http.post<VolunteerHour>(
      `${this.org(org)}/volunteer-hours`,
      value,
    );
  }
  reviewHours(
    org: string,
    id: string,
    action: "approve" | "reject",
    reason?: string,
  ) {
    return this.http.post<VolunteerHour>(
      `${this.org(org)}/volunteer-hours/${id}/${action}`,
      action === "reject" ? { reason } : {},
    );
  }
  report(org: string, from: string, to: string) {
    return this.http.get<VolunteerReport>(
      `${this.org(org)}/volunteer-reports/summary`,
      { params: { from, to } },
    );
  }
}
