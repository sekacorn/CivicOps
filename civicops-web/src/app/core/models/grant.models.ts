export type GrantStatus =
  | "PROSPECT"
  | "APPLICATION_IN_PROGRESS"
  | "SUBMITTED"
  | "AWARDED"
  | "ACTIVE"
  | "CLOSED"
  | "REJECTED"
  | "WITHDRAWN";

export type ExpenseCategory =
  | "PERSONNEL"
  | "SUPPLIES"
  | "EQUIPMENT"
  | "TRAVEL"
  | "TRANSPORTATION"
  | "PROGRAM_SERVICES"
  | "FACILITIES"
  | "CONTRACTORS"
  | "ADMINISTRATIVE"
  | "OTHER";

export interface GrantSummary {
  id: string;
  grantName: string;
  grantorName: string;
  grantNumber: string | null;
  awardAmount: number;
  startDate: string | null;
  endDate: string | null;
  reportingDeadline: string | null;
  status: GrantStatus;
  restricted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface GrantDetail extends GrantSummary {
  organizationId: string;
  description: string | null;
  applicationDeadline: string | null;
  submittedDate: string | null;
  awardDate: string | null;
  restrictionDescription: string | null;
  primaryContactName: string | null;
  primaryContactEmail: string | null;
  notes: string | null;
  createdByUserId: string;
}

export interface GrantMutation {
  grantName: string;
  grantorName: string;
  grantNumber?: string | null;
  description?: string | null;
  awardAmount: number;
  applicationDeadline?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  reportingDeadline?: string | null;
  restricted: boolean;
  restrictionDescription?: string | null;
  primaryContactName?: string | null;
  primaryContactEmail?: string | null;
  notes?: string | null;
}

export interface GrantFinancialSummary {
  grantId: string;
  awardAmount: number;
  totalSpent: number;
  remainingBalance: number;
  utilizationPercent: number;
}

export interface OrganizationGrantSummary {
  totalGrants: number;
  activeGrants: number;
  totalAwarded: number;
  totalSpent: number;
  remainingBalance: number;
  averageUtilizationPercent: number;
  reportsDueSoon: number;
}

export interface GrantExpense {
  id: string;
  grantId: string;
  amount: number;
  expenseDate: string;
  category: ExpenseCategory;
  description: string;
  vendor: string | null;
  referenceNumber: string | null;
  notes: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateGrantExpense {
  amount: number;
  expenseDate: string;
  category: ExpenseCategory;
  description: string;
  vendor?: string | null;
  referenceNumber?: string | null;
  notes?: string | null;
}
