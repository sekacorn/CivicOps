import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  CaseDetail,
  CaseNote,
  CasePriority,
  CaseReportSummary,
  CaseServiceRecord,
  CaseServiceReport,
  CaseStatus,
  CaseSummary,
  CaseTask,
  CaseType,
  CaseWorkload,
  ClientDetail,
  ClientSummary,
} from "../../core/models/case.models";

export interface CaseFilters {
  status?: CaseStatus | "";
  priority?: CasePriority | "";
  caseType?: CaseType | "";
  clientId?: string;
  assignedUserId?: string;
  openedFrom?: string;
  openedTo?: string;
  page: number;
}

@Injectable({ providedIn: "root" })
export class CaseApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  private org(org: string) {
    return `${this.base}/organizations/${org}`;
  }
  clients(org: string, page = 0, name = "") {
    let params = new HttpParams()
      .set("page", page)
      .set("size", 20)
      .set("sort", "lastName,asc");
    if (name) params = params.set("name", name);
    return this.http.get<PageResponse<ClientSummary>>(
      `${this.org(org)}/clients`,
      { params },
    );
  }
  client(org: string, id: string) {
    return this.http.get<ClientDetail>(`${this.org(org)}/clients/${id}`);
  }
  createClient(org: string, value: Record<string, unknown>) {
    return this.http.post<ClientDetail>(`${this.org(org)}/clients`, value);
  }
  cases(org: string, filter: CaseFilters) {
    let params = new HttpParams()
      .set("page", filter.page)
      .set("size", 20)
      .set("sort", "openedDate,desc");
    for (const [key, value] of Object.entries(filter)) {
      if (key !== "page" && value) params = params.set(key, String(value));
    }
    return this.http.get<PageResponse<CaseSummary>>(`${this.org(org)}/cases`, {
      params,
    });
  }
  case(org: string, id: string) {
    return this.http.get<CaseDetail>(`${this.org(org)}/cases/${id}`);
  }
  createCase(org: string, value: Record<string, unknown>) {
    return this.http.post<CaseDetail>(`${this.org(org)}/cases`, value);
  }
  updateCase(org: string, id: string, value: Record<string, unknown>) {
    return this.http.patch<CaseDetail>(`${this.org(org)}/cases/${id}`, value);
  }
  assign(org: string, id: string, userId: string) {
    return this.http.post<CaseDetail>(`${this.org(org)}/cases/${id}/assign`, {
      userId,
    });
  }
  transition(
    org: string,
    id: string,
    action: "start" | "hold" | "resume" | "close" | "cancel",
  ) {
    return this.http.post<CaseDetail>(
      `${this.org(org)}/cases/${id}/${action}`,
      {},
    );
  }
  notes(org: string, caseId: string) {
    return this.http.get<PageResponse<CaseNote>>(
      `${this.org(org)}/cases/${caseId}/notes`,
      {
        params: { page: 0, size: 50, sort: "createdAt,desc" },
      },
    );
  }
  addNote(org: string, caseId: string, value: Record<string, unknown>) {
    return this.http.post<CaseNote>(
      `${this.org(org)}/cases/${caseId}/notes`,
      value,
    );
  }
  tasks(org: string, caseId: string) {
    return this.http.get<PageResponse<CaseTask>>(
      `${this.org(org)}/cases/${caseId}/tasks`,
      {
        params: { page: 0, size: 50, sort: "dueDate,asc" },
      },
    );
  }
  addTask(org: string, caseId: string, value: Record<string, unknown>) {
    return this.http.post<CaseTask>(
      `${this.org(org)}/cases/${caseId}/tasks`,
      value,
    );
  }
  taskAction(
    org: string,
    caseId: string,
    taskId: string,
    action: "complete" | "cancel",
  ) {
    return this.http.post<CaseTask>(
      `${this.org(org)}/cases/${caseId}/tasks/${taskId}/${action}`,
      {},
    );
  }
  services(org: string, caseId: string) {
    return this.http.get<PageResponse<CaseServiceRecord>>(
      `${this.org(org)}/cases/${caseId}/services`,
      {
        params: { page: 0, size: 50, sort: "serviceDate,desc" },
      },
    );
  }
  addService(org: string, caseId: string, value: Record<string, unknown>) {
    return this.http.post<CaseServiceRecord>(
      `${this.org(org)}/cases/${caseId}/services`,
      value,
    );
  }
  report(org: string, from?: string, to?: string) {
    let params = new HttpParams();
    if (from) params = params.set("from", from);
    if (to) params = params.set("to", to);
    return this.http.get<CaseReportSummary>(
      `${this.org(org)}/case-reports/summary`,
      { params },
    );
  }
  workload(org: string) {
    return this.http.get<CaseWorkload[]>(
      `${this.org(org)}/case-reports/workload`,
    );
  }
  serviceReport(org: string, from?: string, to?: string) {
    let params = new HttpParams();
    if (from) params = params.set("from", from);
    if (to) params = params.set("to", to);
    return this.http.get<CaseServiceReport[]>(
      `${this.org(org)}/case-reports/services`,
      { params },
    );
  }
}
