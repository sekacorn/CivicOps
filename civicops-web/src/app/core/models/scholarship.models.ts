import { PageResponse } from "./api.models";

export type ScholarshipProgramStatus =
  "DRAFT" | "OPEN" | "CLOSED" | "REVIEWING" | "AWARDED" | "CANCELLED";
export type ScholarshipApplicationStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "UNDER_REVIEW"
  | "FINALIST"
  | "SELECTED"
  | "NOT_SELECTED"
  | "WITHDRAWN";
export type ScholarshipAwardStatus =
  "OFFERED" | "ACCEPTED" | "DECLINED" | "DISBURSED" | "CANCELLED";
export type ReviewAssignmentStatus =
  "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export type ReviewRecommendation =
  "STRONGLY_RECOMMEND" | "RECOMMEND" | "NEUTRAL" | "DO_NOT_RECOMMEND";
export type ScholarshipDocumentType =
  | "TRANSCRIPT"
  | "RECOMMENDATION"
  | "ESSAY"
  | "PROOF_OF_ENROLLMENT"
  | "FINANCIAL_DOCUMENT"
  | "OTHER";

export interface ScholarshipProgram {
  id: string;
  organizationId: string;
  name: string;
  description: string | null;
  academicYear: string | null;
  applicationOpenDate: string;
  applicationDeadline: string;
  awardAmount: number | null;
  numberOfAwards: number | null;
  eligibilityDescription: string | null;
  status: ScholarshipProgramStatus;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export type ScholarshipProgramPage = PageResponse<ScholarshipProgram>;
export interface ApplicantSummary {
  id: string;
  displayName: string;
  schoolName: string | null;
  graduationYear: number | null;
}
export interface ApplicantDetail extends ApplicantSummary {
  organizationId: string;
  userId: string | null;
  firstName: string;
  lastName: string;
  preferredName: string | null;
  email: string;
  phone: string | null;
  dateOfBirth: string | null;
  address: string | null;
  studentId: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export interface ScholarshipApplicationSummary {
  id: string;
  programId: string;
  programName: string;
  applicantId: string;
  applicantDisplayName: string;
  status: ScholarshipApplicationStatus;
  eligibilityConfirmed: boolean;
  submittedAt: string | null;
  createdAt: string;
}
export interface ScholarshipApplicationDetail extends Omit<
  ScholarshipApplicationSummary,
  "applicantId" | "applicantDisplayName"
> {
  organizationId: string;
  applicant: ApplicantDetail;
  eligibilityNotes: string | null;
  personalStatement: string | null;
  financialNeedStatement: string | null;
  gpa: number | null;
  householdIncome: number | null;
  requestedAmount: number | null;
  updatedAt: string;
  version: number;
}
export interface ReviewerApplication {
  id: string;
  programId: string;
  programName: string;
  applicantId: string;
  applicantDisplayName: string;
  schoolName: string | null;
  graduationYear: number | null;
  status: ScholarshipApplicationStatus;
  eligibilityConfirmed: boolean;
  personalStatement: string | null;
  financialNeedStatement: string | null;
  gpa: number | null;
  requestedAmount: number | null;
  submittedAt: string | null;
}
export interface ScholarshipDocument {
  id: string;
  documentType: ScholarshipDocumentType;
  fileName: string;
  externalStorageReference: string;
  uploadedAt: string;
  verified: boolean;
  verifiedByUserId: string | null;
}
export interface ReviewAssignment {
  id: string;
  applicationId: string;
  reviewerUserId: string;
  reviewerName: string;
  status: ReviewAssignmentStatus;
  assignedAt: string;
  completedAt: string | null;
}
export interface ScholarshipReview {
  id: string;
  assignmentId: string;
  reviewerUserId: string;
  score: number;
  recommendation: ReviewRecommendation;
  comments: string | null;
  submittedAt: string;
}
export interface ScholarshipAward {
  id: string;
  programId: string;
  programName: string;
  applicationId: string;
  applicantId: string;
  applicantDisplayName: string;
  amount: number;
  awardDate: string;
  status: ScholarshipAwardStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export interface ScholarshipSummary {
  organizationId: string;
  activePrograms: number;
  applicationsSubmitted: number;
  applicationsUnderReview: number;
  finalists: number;
  selectedApplicants: number;
  awardsOffered: number;
  totalAwarded: number;
}
export interface ScholarshipProgramReport {
  programId: string;
  programName: string;
  applications: number;
  submitted: number;
  eligible: number;
  underReview: number;
  finalists: number;
  selected: number;
  notSelected: number;
  awardCount: number;
  totalAwardAmount: number;
}
export interface ScholarshipReviewReport {
  organizationId: string;
  reviewsAssigned: number;
  reviewsCompleted: number;
  outstandingReviews: number;
  averageScore: number;
}
