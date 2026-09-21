import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  Availability,
  Facility,
  FacilityBlackout,
  FacilityReportSummary,
  FacilitySpace,
  FacilityType,
  FacilityUtilizationReport,
  OperatingHours,
  ReservationDetail,
  ReservationStatus,
  ReservationSummary,
} from "../../core/models/facility.models";

@Injectable({ providedIn: "root" })
export class FacilityApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  private org(org: string) {
    return `${this.base}/organizations/${org}`;
  }
  facilities(
    org: string,
    filter: {
      active?: string;
      type?: FacilityType | "";
      city?: string;
      page: number;
    },
  ) {
    let params = new HttpParams()
      .set("page", filter.page)
      .set("size", 20)
      .set("sort", "name,asc");
    for (const [key, value] of Object.entries(filter))
      if (key !== "page" && value !== "" && value !== undefined)
        params = params.set(key, String(value));
    return this.http.get<PageResponse<Facility>>(
      `${this.org(org)}/facilities`,
      { params },
    );
  }
  facility(org: string, id: string) {
    return this.http.get<Facility>(`${this.org(org)}/facilities/${id}`);
  }
  createFacility(org: string, value: Record<string, unknown>) {
    return this.http.post<Facility>(`${this.org(org)}/facilities`, value);
  }
  updateFacility(org: string, id: string, value: Record<string, unknown>) {
    return this.http.patch<Facility>(
      `${this.org(org)}/facilities/${id}`,
      value,
    );
  }
  spaces(org: string, facilityId: string) {
    return this.http.get<PageResponse<FacilitySpace>>(
      `${this.org(org)}/facilities/${facilityId}/spaces`,
      { params: { page: 0, size: 100, sort: "name,asc" } },
    );
  }
  allSpaces(org: string, facilityId?: string) {
    const params: Record<string, string | number> = {
      page: 0,
      size: 100,
      sort: "name,asc",
    };
    if (facilityId) params["facilityId"] = facilityId;
    return this.http.get<PageResponse<FacilitySpace>>(
      `${this.org(org)}/facility-spaces`,
      { params },
    );
  }
  createSpace(org: string, facilityId: string, value: Record<string, unknown>) {
    return this.http.post<FacilitySpace>(
      `${this.org(org)}/facilities/${facilityId}/spaces`,
      value,
    );
  }
  updateSpace(org: string, id: string, value: Record<string, unknown>) {
    return this.http.patch<FacilitySpace>(
      `${this.org(org)}/facility-spaces/${id}`,
      value,
    );
  }
  hours(org: string, facilityId: string) {
    return this.http.get<OperatingHours[]>(
      `${this.org(org)}/facilities/${facilityId}/operating-hours`,
    );
  }
  saveHours(
    org: string,
    facilityId: string,
    value: Array<Record<string, unknown>>,
  ) {
    return this.http.put<OperatingHours[]>(
      `${this.org(org)}/facilities/${facilityId}/operating-hours`,
      value,
    );
  }
  blackouts(org: string, facilityId: string) {
    return this.http.get<PageResponse<FacilityBlackout>>(
      `${this.org(org)}/facility-blackouts`,
      { params: { facilityId, page: 0, size: 50, sort: "startDateTime,asc" } },
    );
  }
  createBlackout(org: string, value: Record<string, unknown>) {
    return this.http.post<FacilityBlackout>(
      `${this.org(org)}/facility-blackouts`,
      value,
    );
  }
  cancelBlackout(org: string, id: string) {
    return this.http.post<FacilityBlackout>(
      `${this.org(org)}/facility-blackouts/${id}/cancel`,
      {},
    );
  }
  reservations(
    org: string,
    filter: {
      status?: ReservationStatus | "";
      facilityId?: string;
      spaceId?: string;
      eventId?: string;
      from?: string;
      to?: string;
      page: number;
    },
  ) {
    let params = new HttpParams()
      .set("page", filter.page)
      .set("size", 20)
      .set("sort", "startDateTime,asc");
    for (const [key, value] of Object.entries(filter))
      if (key !== "page" && value) params = params.set(key, String(value));
    return this.http.get<PageResponse<ReservationSummary>>(
      `${this.org(org)}/facility-reservations`,
      { params },
    );
  }
  myReservations(org: string, page = 0) {
    return this.http.get<PageResponse<ReservationSummary>>(
      `${this.org(org)}/facility-reservations/me`,
      { params: { page, size: 20, sort: "startDateTime,asc" } },
    );
  }
  reservation(org: string, id: string) {
    return this.http.get<ReservationDetail>(
      `${this.org(org)}/facility-reservations/${id}`,
    );
  }
  createReservation(org: string, value: Record<string, unknown>) {
    return this.http.post<ReservationDetail>(
      `${this.org(org)}/facility-reservations`,
      value,
    );
  }
  reservationAction(org: string, id: string, action: "approve" | "complete") {
    return this.http.post<ReservationDetail>(
      `${this.org(org)}/facility-reservations/${id}/${action}`,
      {},
    );
  }
  reject(org: string, id: string, reason: string) {
    return this.http.post<ReservationDetail>(
      `${this.org(org)}/facility-reservations/${id}/reject`,
      { reason },
    );
  }
  cancelReservation(org: string, id: string, reason: string | null) {
    return this.http.post<ReservationDetail>(
      `${this.org(org)}/facility-reservations/${id}/cancel`,
      { reason },
    );
  }
  availability(org: string, spaceId: string, start: string, end: string) {
    return this.http.get<Availability>(
      `${this.org(org)}/facility-spaces/${spaceId}/availability`,
      { params: { start, end } },
    );
  }
  report(org: string) {
    return this.http.get<FacilityReportSummary>(
      `${this.org(org)}/facility-reports/summary`,
    );
  }
  utilization(org: string) {
    return this.http.get<FacilityUtilizationReport>(
      `${this.org(org)}/facility-reports/utilization`,
    );
  }
}
