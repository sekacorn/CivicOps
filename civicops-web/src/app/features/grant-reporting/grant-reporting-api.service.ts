import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  EvidenceSourceModule,
  GrantReportDetail,
  GrantReportSection,
  GrantReportSummary,
  GrantReportTemplate,
  MissingEvidence,
  ReportEvidence,
  ReportSectionType,
  TemplateSection,
} from "../../core/models/grant-reporting.models";

@Injectable({ providedIn: "root" })
export class GrantReportingApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  private org(id: string) {
    return `${this.base}/organizations/${id}`;
  }
  templates(org: string, page = 0) {
    return this.http.get<PageResponse<GrantReportTemplate>>(
      `${this.org(org)}/grant-report-templates`,
      {
        params: new HttpParams()
          .set("page", page)
          .set("size", 20)
          .set("sort", "name,asc"),
      },
    );
  }
  template(org: string, id: string) {
    return this.http.get<GrantReportTemplate>(
      `${this.org(org)}/grant-report-templates/${id}`,
    );
  }
  createTemplate(
    org: string,
    value: { name: string; description: string | null; grantId: string | null },
  ) {
    return this.http.post<GrantReportTemplate>(
      `${this.org(org)}/grant-report-templates`,
      value,
    );
  }
  updateTemplate(
    org: string,
    id: string,
    value: { name?: string; description?: string; active?: boolean },
  ) {
    return this.http.patch<GrantReportTemplate>(
      `${this.org(org)}/grant-report-templates/${id}`,
      value,
    );
  }
  templateSections(org: string, id: string) {
    return this.http.get<TemplateSection[]>(
      `${this.org(org)}/grant-report-templates/${id}/sections`,
    );
  }
  addTemplateSection(
    org: string,
    id: string,
    value: {
      sectionKey: string;
      title: string;
      instructions: string | null;
      sequenceNumber: number;
      sectionType: ReportSectionType;
      required: boolean;
      maxLength: number | null;
    },
  ) {
    return this.http.post<TemplateSection>(
      `${this.org(org)}/grant-report-templates/${id}/sections`,
      value,
    );
  }
  reports(org: string, grantId?: string, page = 0) {
    let params = new HttpParams()
      .set("page", page)
      .set("size", 20)
      .set("sort", "createdAt,desc");
    if (grantId) params = params.set("grantId", grantId);
    return this.http.get<PageResponse<GrantReportSummary>>(
      `${this.org(org)}/grant-reports`,
      { params },
    );
  }
  report(org: string, id: string) {
    return this.http.get<GrantReportDetail>(
      `${this.org(org)}/grant-reports/${id}`,
    );
  }
  createReport(
    org: string,
    grantId: string,
    value: {
      templateId: string;
      reportingPeriodStart: string;
      reportingPeriodEnd: string;
      selectedSources: EvidenceSourceModule[];
    },
  ) {
    return this.http.post<GrantReportDetail>(
      `${this.org(org)}/grants/${grantId}/reports`,
      value,
    );
  }
  action(
    org: string,
    id: string,
    action: "generate" | "regenerate" | "finalize",
  ) {
    return this.http.post<GrantReportDetail>(
      `${this.org(org)}/grant-reports/${id}/${action}`,
      {},
    );
  }
  sections(org: string, id: string) {
    return this.http.get<GrantReportSection[]>(
      `${this.org(org)}/grant-reports/${id}/sections`,
    );
  }
  editSection(org: string, id: string, editedContent: string) {
    return this.http.patch<GrantReportSection>(
      `${this.org(org)}/grant-report-sections/${id}`,
      { editedContent },
    );
  }
  approveSection(org: string, id: string) {
    return this.http.post<GrantReportSection>(
      `${this.org(org)}/grant-report-sections/${id}/approve`,
      {},
    );
  }
  evidence(org: string, id: string) {
    return this.http.get<ReportEvidence[]>(
      `${this.org(org)}/grant-reports/${id}/evidence`,
    );
  }
  missing(org: string, id: string) {
    return this.http.get<MissingEvidence[]>(
      `${this.org(org)}/grant-reports/${id}/missing-data`,
    );
  }
  addManualEvidence(
    org: string,
    id: string,
    value: {
      label: string;
      numericValue: number | null;
      monetaryValue: number | null;
      textValue: string | null;
      unit: string | null;
      sourceReference: string;
      notes: string | null;
    },
  ) {
    return this.http.post<ReportEvidence>(
      `${this.org(org)}/grant-reports/${id}/manual-evidence`,
      value,
    );
  }
  export(org: string, id: string, format: "json" | "markdown") {
    return this.http.get(`${this.org(org)}/grant-reports/${id}/export`, {
      params: { format },
      responseType: "blob",
    });
  }
}
