import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  BoardAttendance,
  BoardAttendanceReport,
  BoardAgendaItem,
  BoardCommittee,
  BoardMeeting,
  BoardMeetingStatus,
  BoardMemberDetail,
  BoardMemberSummary,
  BoardMinutes,
  BoardMotion,
  BoardQuorum,
  BoardResolution,
  BoardSummaryReport,
  BoardVote,
  BoardVotingReport,
} from "../../core/models/board.models";

@Injectable({ providedIn: "root" })
export class BoardApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);

  private org(org: string): string {
    return `${this.base}/organizations/${org}`;
  }

  members(
    org: string,
    filter: { status?: "ACTIVE" | "INACTIVE" | ""; page: number },
  ) {
    let params = new HttpParams()
      .set("page", filter.page)
      .set("size", 20)
      .set("sort", "lastName,asc");
    if (filter.status) params = params.set("status", filter.status);
    return this.http.get<PageResponse<BoardMemberSummary>>(
      `${this.org(org)}/board-members`,
      {
        params,
      },
    );
  }

  member(org: string, memberId: string) {
    return this.http.get<BoardMemberDetail>(
      `${this.org(org)}/board-members/${memberId}`,
    );
  }

  createMember(org: string, value: Record<string, unknown>) {
    return this.http.post<BoardMemberDetail>(
      `${this.org(org)}/board-members`,
      value,
    );
  }

  updateMember(org: string, memberId: string, value: Record<string, unknown>) {
    return this.http.patch<BoardMemberDetail>(
      `${this.org(org)}/board-members/${memberId}`,
      value,
    );
  }

  deactivateMember(org: string, memberId: string) {
    return this.http.post<BoardMemberDetail>(
      `${this.org(org)}/board-members/${memberId}/deactivate`,
      {},
    );
  }

  committees(org: string, page = 0) {
    return this.http.get<PageResponse<BoardCommittee>>(
      `${this.org(org)}/board-committees`,
      { params: { page, size: 20, sort: "name,asc" } },
    );
  }

  createCommittee(org: string, value: Record<string, unknown>) {
    return this.http.post<BoardCommittee>(
      `${this.org(org)}/board-committees`,
      value,
    );
  }

  meetings(
    org: string,
    filter: {
      status?: BoardMeetingStatus | "";
      from?: string;
      to?: string;
      page: number;
    },
  ) {
    let params = new HttpParams()
      .set("page", filter.page)
      .set("size", 20)
      .set("sort", "startDateTime,desc");
    if (filter.status) params = params.set("status", filter.status);
    if (filter.from) params = params.set("from", filter.from);
    if (filter.to) params = params.set("to", filter.to);
    return this.http.get<PageResponse<BoardMeeting>>(
      `${this.org(org)}/board-meetings`,
      {
        params,
      },
    );
  }

  meeting(org: string, meetingId: string) {
    return this.http.get<BoardMeeting>(
      `${this.org(org)}/board-meetings/${meetingId}`,
    );
  }

  createMeeting(org: string, value: Record<string, unknown>) {
    return this.http.post<BoardMeeting>(
      `${this.org(org)}/board-meetings`,
      value,
    );
  }

  updateMeeting(
    org: string,
    meetingId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.patch<BoardMeeting>(
      `${this.org(org)}/board-meetings/${meetingId}`,
      value,
    );
  }

  meetingAction(
    org: string,
    meetingId: string,
    action: "publish" | "start" | "complete" | "cancel",
  ) {
    return this.http.post<BoardMeeting>(
      `${this.org(org)}/board-meetings/${meetingId}/${action}`,
      {},
    );
  }

  attendance(org: string, meetingId: string) {
    return this.http.get<PageResponse<BoardAttendance>>(
      `${this.org(org)}/board-meetings/${meetingId}/attendance`,
      { params: { page: 0, size: 100, sort: "updatedAt,desc" } },
    );
  }

  recordAttendance(
    org: string,
    meetingId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<BoardAttendance>(
      `${this.org(org)}/board-meetings/${meetingId}/attendance`,
      value,
    );
  }

  updateAttendance(
    org: string,
    meetingId: string,
    memberId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.put<BoardAttendance>(
      `${this.org(org)}/board-meetings/${meetingId}/attendance/${memberId}`,
      value,
    );
  }

  selfAttendance(
    org: string,
    meetingId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.put<BoardAttendance>(
      `${this.org(org)}/board-meetings/${meetingId}/attendance/me`,
      value,
    );
  }

  quorum(org: string, meetingId: string) {
    return this.http.get<BoardQuorum>(
      `${this.org(org)}/board-meetings/${meetingId}/quorum`,
    );
  }

  agenda(org: string, meetingId: string) {
    return this.http.get<PageResponse<BoardAgendaItem>>(
      `${this.org(org)}/board-meetings/${meetingId}/agenda-items`,
      { params: { page: 0, size: 100, sort: "sortOrder,asc" } },
    );
  }

  createAgendaItem(
    org: string,
    meetingId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<BoardAgendaItem>(
      `${this.org(org)}/board-meetings/${meetingId}/agenda-items`,
      value,
    );
  }

  agendaAction(org: string, itemId: string, action: "complete" | "skip") {
    return this.http.post<BoardAgendaItem>(
      `${this.org(org)}/board-agenda-items/${itemId}/${action}`,
      {},
    );
  }

  motions(org: string, meetingId: string) {
    return this.http.get<PageResponse<BoardMotion>>(
      `${this.org(org)}/board-meetings/${meetingId}/motions`,
      { params: { page: 0, size: 50, sort: "createdAt,desc" } },
    );
  }

  motion(org: string, motionId: string) {
    return this.http.get<BoardMotion>(
      `${this.org(org)}/board-motions/${motionId}`,
    );
  }

  createMotion(org: string, meetingId: string, value: Record<string, unknown>) {
    return this.http.post<BoardMotion>(
      `${this.org(org)}/board-meetings/${meetingId}/motions`,
      value,
    );
  }

  motionAction(
    org: string,
    motionId: string,
    action: "open" | "close" | "withdraw" | "table",
  ) {
    const endpointAction =
      action === "open"
        ? "open-voting"
        : action === "close"
          ? "close-voting"
          : action;
    return this.http.post<BoardMotion>(
      `${this.org(org)}/board-motions/${motionId}/${endpointAction}`,
      {},
    );
  }

  secondMotion(org: string, motionId: string, secondedByBoardMemberId: string) {
    return this.http.post<BoardMotion>(
      `${this.org(org)}/board-motions/${motionId}/second`,
      { secondedByBoardMemberId },
    );
  }

  vote(org: string, motionId: string, value: Record<string, unknown>) {
    return this.http.post<BoardVote>(
      `${this.org(org)}/board-motions/${motionId}/votes/me`,
      value,
    );
  }

  votes(org: string, motionId: string) {
    return this.http.get<BoardVote[]>(
      `${this.org(org)}/board-motions/${motionId}/votes`,
    );
  }

  minutes(org: string, meetingId: string) {
    return this.http.get<BoardMinutes>(
      `${this.org(org)}/board-meetings/${meetingId}/minutes`,
    );
  }

  saveMinutes(org: string, meetingId: string, value: Record<string, unknown>) {
    return this.http.put<BoardMinutes>(
      `${this.org(org)}/board-meetings/${meetingId}/minutes`,
      value,
    );
  }

  minutesAction(org: string, meetingId: string, action: "submit" | "approve") {
    return this.http.post<BoardMinutes>(
      `${this.org(org)}/board-meetings/${meetingId}/minutes/${action}`,
      {},
    );
  }

  resolutions(org: string, page = 0) {
    return this.http.get<PageResponse<BoardResolution>>(
      `${this.org(org)}/board-resolutions`,
      { params: { page, size: 20, sort: "createdAt,desc" } },
    );
  }

  createResolution(
    org: string,
    motionId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<BoardResolution>(
      `${this.org(org)}/board-motions/${motionId}/resolution`,
      value,
    );
  }

  rescindResolution(org: string, resolutionId: string) {
    return this.http.post<BoardResolution>(
      `${this.org(org)}/board-resolutions/${resolutionId}/rescind`,
      {},
    );
  }

  summaryReport(org: string) {
    return this.http.get<BoardSummaryReport>(
      `${this.org(org)}/board-reports/summary`,
    );
  }

  attendanceReport(org: string) {
    return this.http.get<BoardAttendanceReport>(
      `${this.org(org)}/board-reports/attendance`,
    );
  }

  votingReport(org: string) {
    return this.http.get<BoardVotingReport>(
      `${this.org(org)}/board-reports/voting`,
    );
  }
}
