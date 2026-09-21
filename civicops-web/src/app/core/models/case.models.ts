export type CaseStatus =
  "OPEN" | "IN_PROGRESS" | "ON_HOLD" | "CLOSED" | "CANCELLED";
export type CasePriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";
export type CaseType =
  | "GENERAL_ASSISTANCE"
  | "HOUSING"
  | "FOOD_ASSISTANCE"
  | "EMPLOYMENT"
  | "EDUCATION"
  | "FINANCIAL_ASSISTANCE"
  | "COMMUNITY_SERVICES"
  | "OTHER";
export type CaseNoteType =
  "GENERAL" | "CONTACT" | "PROGRESS" | "ASSESSMENT" | "INTERNAL";
export type CaseTaskStatus = "OPEN" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export type CaseServiceType =
  | "FOOD_ASSISTANCE"
  | "HOUSING_ASSISTANCE"
  | "TRANSPORTATION"
  | "COUNSELING_REFERRAL"
  | "EMPLOYMENT_SUPPORT"
  | "EDUCATION_SUPPORT"
  | "FINANCIAL_ASSISTANCE"
  | "OTHER";

export interface ClientSummary {
  id: string;
  displayName: string;
  active: boolean;
  externalReferenceNumber: string | null;
  createdAt: string;
}

export interface ClientDetail {
  id: string;
  organizationId: string;
  externalReferenceNumber: string | null;
  firstName: string;
  lastName: string;
  preferredName: string | null;
  dateOfBirth: string | null;
  email: string | null;
  phone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  preferredContactMethod: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CaseSummary {
  id: string;
  caseNumber: string;
  title: string;
  caseType: CaseType;
  priority: CasePriority;
  status: CaseStatus;
  openedDate: string;
  closedDate: string | null;
  clientId: string;
  clientDisplayName: string;
  assignedUserId: string | null;
  updatedAt: string;
}

export interface CaseDetail {
  id: string;
  organizationId: string;
  caseNumber: string;
  title: string;
  description: string | null;
  caseType: CaseType;
  priority: CasePriority;
  status: CaseStatus;
  openedDate: string;
  closedDate: string | null;
  client: ClientSummary;
  assignedUserId: string | null;
  assignedUserDisplayName: string | null;
  programName: string | null;
  intakeSource: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CaseNote {
  id: string;
  caseId: string;
  authorUserId: string;
  authorDisplayName: string;
  noteType: CaseNoteType;
  content: string;
  privateNote: boolean;
  createdAt: string;
}

export interface CaseTask {
  id: string;
  caseId: string;
  title: string;
  description: string | null;
  assignedUserId: string | null;
  dueDate: string | null;
  status: CaseTaskStatus;
  priority: CasePriority;
  overdue: boolean;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CaseServiceRecord {
  id: string;
  caseId: string;
  serviceType: CaseServiceType;
  description: string | null;
  serviceDate: string;
  quantity: number | null;
  unit: string | null;
  valueAmount: number | null;
  providedByUserId: string | null;
  notes: string | null;
  createdAt: string;
}

export interface CaseReportSummary {
  organizationId: string;
  from: string;
  to: string;
  activeClients: number;
  openCases: number;
  inProgressCases: number;
  onHoldCases: number;
  casesClosedInPeriod: number;
  overdueTasks: number;
}

export interface CaseServiceReport {
  serviceType: CaseServiceType;
  serviceCount: number;
  totalValueAmount: number;
}

export interface CaseWorkload {
  userId: string;
  displayName: string;
  openCases: number;
  inProgressCases: number;
  onHoldCases: number;
}
