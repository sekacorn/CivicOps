export type GrantReportStatus =
  "DRAFT" | "GENERATED" | "UNDER_REVIEW" | "FINALIZED" | "CANCELLED";
export type ReportSectionStatus =
  "PENDING" | "GENERATED" | "EDITED" | "APPROVED";
export type ReportSectionType =
  | "NARRATIVE"
  | "METRICS"
  | "FINANCIAL"
  | "OUTCOMES"
  | "CHALLENGES"
  | "DEMOGRAPHICS"
  | "CUSTOM";
export type EvidenceSourceModule =
  | "GRANT"
  | "VOLUNTEERS"
  | "DONATIONS"
  | "EVENTS"
  | "CASES"
  | "SCHOLARSHIPS"
  | "FOOD_PANTRY"
  | "MANUAL";
export type EvidenceValueState = "VERIFIED" | "MISSING" | "NOT_APPLICABLE";

export interface GrantReportTemplate {
  id: string;
  organizationId: string;
  grantId: string | null;
  name: string;
  description: string | null;
  active: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export interface TemplateSection {
  id: string;
  templateId: string;
  sectionKey: string;
  title: string;
  instructions: string | null;
  sequenceNumber: number;
  sectionType: ReportSectionType;
  required: boolean;
  maxLength: number | null;
}
export interface GrantReportSummary {
  id: string;
  grantId: string;
  grantName: string;
  templateId: string;
  reportingPeriodStart: string;
  reportingPeriodEnd: string;
  status: GrantReportStatus;
  generatedAt: string | null;
  finalizedAt: string | null;
  createdAt: string;
}
export interface GrantReportDetail extends GrantReportSummary {
  organizationId: string;
  templateName: string;
  selectedSources: EvidenceSourceModule[];
  createdBy: string;
  finalizedBy: string | null;
  updatedAt: string;
  version: number;
}
export interface GrantReportSection {
  id: string;
  reportId: string;
  templateSectionId: string;
  sequenceNumber: number;
  title: string;
  sectionType: ReportSectionType;
  required: boolean;
  generatedContent: string | null;
  editedContent: string | null;
  finalContent: string | null;
  evidenceSummary: string | null;
  status: ReportSectionStatus;
  generatedAt: string | null;
  version: number;
}
export interface ReportEvidence {
  id: string;
  sourceModule: EvidenceSourceModule;
  sourceType: string;
  sourceId: string | null;
  metricKey: string;
  metricLabel: string;
  valueState: EvidenceValueState;
  numericValue: number | null;
  monetaryValue: number | null;
  textValue: string | null;
  unit: string | null;
  periodStart: string | null;
  periodEnd: string | null;
  capturedAt: string;
  sourceReference: string;
  metadataJson: string | null;
  enteredBy: string | null;
}
export interface MissingEvidence {
  metricKey: string;
  metricLabel: string;
  sourceModule: EvidenceSourceModule;
  message: string;
}
