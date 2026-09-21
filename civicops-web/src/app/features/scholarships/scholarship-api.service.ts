import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  ApplicantDetail,
  ApplicantSummary,
  ReviewAssignment,
  ScholarshipApplicationDetail,
  ScholarshipApplicationStatus,
  ScholarshipApplicationSummary,
  ScholarshipAward,
  ScholarshipDocument,
  ScholarshipProgram,
  ScholarshipProgramReport,
  ScholarshipProgramStatus,
  ScholarshipReview,
  ScholarshipReviewReport,
  ScholarshipSummary,
} from "../../core/models/scholarship.models";

@Injectable({ providedIn: "root" })
export class ScholarshipApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);

  private org(org: string): string {
    return `${this.base}/organizations/${org}`;
  }

  programs(
    org: string,
    filter: {
      status?: ScholarshipProgramStatus | "";
      academicYear?: string;
      page: number;
    },
  ) {
    let params = new HttpParams()
      .set("page", filter.page)
      .set("size", 20)
      .set("sort", "applicationDeadline,asc");
    if (filter.status) params = params.set("status", filter.status);
    if (filter.academicYear)
      params = params.set("academicYear", filter.academicYear);
    return this.http.get<PageResponse<ScholarshipProgram>>(
      `${this.org(org)}/scholarship-programs`,
      { params },
    );
  }

  program(org: string, programId: string) {
    return this.http.get<ScholarshipProgram>(
      `${this.org(org)}/scholarship-programs/${programId}`,
    );
  }

  createProgram(org: string, value: Record<string, unknown>) {
    return this.http.post<ScholarshipProgram>(
      `${this.org(org)}/scholarship-programs`,
      value,
    );
  }

  updateProgram(
    org: string,
    programId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.patch<ScholarshipProgram>(
      `${this.org(org)}/scholarship-programs/${programId}`,
      value,
    );
  }

  programAction(
    org: string,
    programId: string,
    action: "open" | "close" | "start-review" | "finalize-awards" | "cancel",
  ) {
    return this.http.post<ScholarshipProgram>(
      `${this.org(org)}/scholarship-programs/${programId}/${action}`,
      {},
    );
  }

  applicants(org: string, page = 0) {
    return this.http.get<PageResponse<ApplicantSummary>>(
      `${this.org(org)}/scholarship-applicants`,
      { params: { page, size: 20, sort: "lastName,asc" } },
    );
  }

  applicant(org: string, applicantId: string) {
    return this.http.get<ApplicantDetail>(
      `${this.org(org)}/scholarship-applicants/${applicantId}`,
    );
  }

  createApplicant(org: string, value: Record<string, unknown>) {
    return this.http.post<ApplicantDetail>(
      `${this.org(org)}/scholarship-applicants`,
      value,
    );
  }

  updateApplicant(
    org: string,
    applicantId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.patch<ApplicantDetail>(
      `${this.org(org)}/scholarship-applicants/${applicantId}`,
      value,
    );
  }

  applications(
    org: string,
    programId: string,
    filter: { status?: ScholarshipApplicationStatus | ""; page: number },
  ) {
    let params = new HttpParams()
      .set("page", filter.page)
      .set("size", 20)
      .set("sort", "createdAt,desc");
    if (filter.status) params = params.set("status", filter.status);
    return this.http.get<PageResponse<ScholarshipApplicationSummary>>(
      `${this.org(org)}/scholarship-programs/${programId}/applications`,
      { params },
    );
  }

  createApplication(
    org: string,
    programId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<ScholarshipApplicationDetail>(
      `${this.org(org)}/scholarship-programs/${programId}/applications`,
      value,
    );
  }

  application(org: string, applicationId: string) {
    return this.http.get<ScholarshipApplicationDetail>(
      `${this.org(org)}/scholarship-applications/${applicationId}`,
    );
  }

  updateApplication(
    org: string,
    applicationId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.patch<ScholarshipApplicationDetail>(
      `${this.org(org)}/scholarship-applications/${applicationId}`,
      value,
    );
  }

  applicationAction(
    org: string,
    applicationId: string,
    action: "submit" | "withdraw" | "finalist" | "select" | "not-select",
  ) {
    return this.http.post<ScholarshipApplicationDetail>(
      `${this.org(org)}/scholarship-applications/${applicationId}/${action}`,
      {},
    );
  }

  assignments(org: string, applicationId: string) {
    return this.http.get<PageResponse<ReviewAssignment>>(
      `${this.org(org)}/scholarship-applications/${applicationId}/reviewers`,
      { params: { page: 0, size: 50, sort: "assignedAt,asc" } },
    );
  }

  assignReviewer(
    org: string,
    applicationId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<ReviewAssignment>(
      `${this.org(org)}/scholarship-applications/${applicationId}/reviewers`,
      value,
    );
  }

  reviews(org: string, applicationId: string) {
    return this.http.get<PageResponse<ScholarshipReview>>(
      `${this.org(org)}/scholarship-applications/${applicationId}/reviews`,
      { params: { page: 0, size: 50, sort: "submittedAt,desc" } },
    );
  }

  documents(org: string, applicationId: string) {
    return this.http.get<PageResponse<ScholarshipDocument>>(
      `${this.org(org)}/scholarship-applications/${applicationId}/documents`,
      { params: { page: 0, size: 50, sort: "uploadedAt,desc" } },
    );
  }

  addDocument(
    org: string,
    applicationId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<ScholarshipDocument>(
      `${this.org(org)}/scholarship-applications/${applicationId}/documents`,
      value,
    );
  }

  verifyDocument(org: string, applicationId: string, documentId: string) {
    return this.http.post<ScholarshipDocument>(
      `${this.org(org)}/scholarship-applications/${applicationId}/documents/${documentId}/verify`,
      {},
    );
  }

  myReviews(org: string, page = 0) {
    return this.http.get<PageResponse<ReviewAssignment>>(
      `${this.org(org)}/scholarship-reviews/me`,
      { params: { page, size: 20, sort: "assignedAt,asc" } },
    );
  }

  reviewerApplication(org: string, assignmentId: string) {
    return this.http.get<unknown>(
      `${this.org(org)}/scholarship-review-assignments/${assignmentId}/application`,
    );
  }

  startReview(org: string, assignmentId: string) {
    return this.http.post<ReviewAssignment>(
      `${this.org(org)}/scholarship-review-assignments/${assignmentId}/start`,
      {},
    );
  }

  submitReview(
    org: string,
    assignmentId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<ScholarshipReview>(
      `${this.org(org)}/scholarship-review-assignments/${assignmentId}/submit-review`,
      value,
    );
  }

  createAward(
    org: string,
    applicationId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<ScholarshipAward>(
      `${this.org(org)}/scholarship-applications/${applicationId}/award`,
      value,
    );
  }

  awards(org: string, page = 0) {
    return this.http.get<PageResponse<ScholarshipAward>>(
      `${this.org(org)}/scholarship-awards`,
      { params: { page, size: 20, sort: "awardDate,desc" } },
    );
  }

  award(org: string, awardId: string) {
    return this.http.get<ScholarshipAward>(
      `${this.org(org)}/scholarship-awards/${awardId}`,
    );
  }

  awardAction(
    org: string,
    awardId: string,
    action: "accept" | "decline" | "mark-disbursed" | "cancel",
  ) {
    return this.http.post<ScholarshipAward>(
      `${this.org(org)}/scholarship-awards/${awardId}/${action}`,
      {},
    );
  }

  summary(org: string) {
    return this.http.get<ScholarshipSummary>(
      `${this.org(org)}/scholarship-reports/summary`,
    );
  }

  programReport(org: string, programId: string) {
    return this.http.get<ScholarshipProgramReport>(
      `${this.org(org)}/scholarship-reports/programs/${programId}`,
    );
  }

  reviewReport(org: string) {
    return this.http.get<ScholarshipReviewReport>(
      `${this.org(org)}/scholarship-reports/reviews`,
    );
  }
}
