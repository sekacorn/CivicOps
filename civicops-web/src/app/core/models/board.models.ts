import { PageResponse } from "./api.models";
export type BoardMeetingStatus =
  "DRAFT" | "PUBLISHED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export type BoardMeetingType =
  "REGULAR" | "SPECIAL" | "ANNUAL" | "EMERGENCY" | "COMMITTEE" | "OTHER";
export type AttendanceStatus = "PRESENT" | "REMOTE" | "ABSENT" | "EXCUSED";
export type MotionStatus =
  | "PROPOSED"
  | "SECONDED"
  | "VOTING"
  | "PASSED"
  | "FAILED"
  | "WITHDRAWN"
  | "TABLED";
export type VoteChoice = "YES" | "NO" | "ABSTAIN";
export interface BoardMemberSummary {
  id: string;
  userId: string | null;
  firstName: string;
  lastName: string;
  title: string | null;
  active: boolean;
  joinedDate: string;
  createdAt: string;
}
export interface BoardMemberDetail extends BoardMemberSummary {
  organizationId: string;
  email: string | null;
  phone: string | null;
  notes: string | null;
  updatedAt: string;
  version: number;
}
export interface BoardTerm {
  id: string;
  memberId: string;
  termStart: string;
  termEnd: string;
  status: string;
  createdAt: string;
}
export interface BoardOfficerAssignment {
  id: string;
  memberId: string;
  officerRole: string;
  otherTitle: string | null;
  startDate: string;
  endDate: string | null;
  active: boolean;
}
export interface BoardCommittee {
  id: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
}
export interface BoardCommitteeMembership {
  id: string;
  committeeId: string;
  boardMemberId: string;
  boardMemberName: string;
  role: string | null;
  startDate: string;
  endDate: string | null;
  active: boolean;
}
export interface BoardMeeting {
  id: string;
  organizationId: string;
  title: string;
  meetingType: BoardMeetingType;
  startDateTime: string;
  endDateTime: string;
  location: string | null;
  virtualMeetingUrl: string | null;
  status: BoardMeetingStatus;
  quorumRequired: number;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export interface BoardAttendance {
  id: string;
  meetingId: string;
  boardMemberId: string;
  memberName: string;
  attendanceStatus: AttendanceStatus;
  checkedInAt: string;
  notes: string | null;
}
export interface BoardQuorum {
  meetingId: string;
  quorumRequired: number;
  presentOrRemote: number;
  quorumMet: boolean;
  attendanceRate: number;
}
export interface BoardAgendaItem {
  id: string;
  meetingId: string;
  sequenceNumber: number;
  title: string;
  description: string | null;
  itemType: string;
  presenter: string | null;
  estimatedMinutes: number | null;
  status: string;
  createdAt: string;
  version: number;
}
export interface BoardMotion {
  id: string;
  meetingId: string;
  agendaItemId: string | null;
  motionText: string;
  movedByBoardMemberId: string;
  secondedByBoardMemberId: string | null;
  status: MotionStatus;
  openedAt: string | null;
  closedAt: string | null;
  yesVotes: number;
  noVotes: number;
  abstainVotes: number;
  createdAt: string;
  version: number;
}
export interface BoardVote {
  id: string;
  motionId: string;
  boardMemberId: string;
  choice: VoteChoice;
  castAt: string;
}
export interface BoardMinutes {
  id: string;
  meetingId: string;
  content: string;
  status: "DRAFT" | "SUBMITTED" | "APPROVED";
  preparedByUserId: string;
  approvedByUserId: string | null;
  submittedAt: string | null;
  approvedAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export interface BoardResolution {
  id: string;
  meetingId: string;
  motionId: string | null;
  resolutionNumber: string;
  title: string;
  text: string;
  adoptedDate: string;
  status: "ADOPTED" | "RESCINDED";
  rescindedAt: string | null;
  createdAt: string;
}
export interface BoardSummaryReport {
  organizationId: string;
  from: string;
  to: string;
  activeBoardMembers: number;
  activeCommittees: number;
  meetingsInPeriod: number;
  averageAttendanceRate: number;
  quorumFailures: number;
  motionsPassed: number;
  motionsFailed: number;
  resolutionsAdopted: number;
}
export interface BoardAttendanceReport {
  organizationId: string;
  from: string;
  to: string;
  totalMeetings: number;
  quorumMet: number;
  quorumNotMet: number;
  remoteAttendances: number;
  members: {
    boardMemberId: string;
    memberName: string;
    meetingsRecorded: number;
    present: number;
    remote: number;
    absent: number;
    excused: number;
    attendanceRate: number;
  }[];
}
export interface BoardVotingReport {
  organizationId: string;
  from: string;
  to: string;
  motionsPassed: number;
  motionsFailed: number;
  yesVotes: number;
  noVotes: number;
  abstainVotes: number;
  participationRate: number;
  resolutionsAdopted: number;
}
export type BoardPage<T> = PageResponse<T>;
