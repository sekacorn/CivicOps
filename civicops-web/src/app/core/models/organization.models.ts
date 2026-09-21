export type Role =
  | "SYSTEM_ADMIN"
  | "ORG_ADMIN"
  | "PROGRAM_MANAGER"
  | "GRANT_MANAGER"
  | "DONATION_MANAGER"
  | "VOLUNTEER_COORDINATOR"
  | "EVENT_COORDINATOR"
  | "CASE_MANAGER"
  | "CASE_WORKER"
  | "EQUIPMENT_MANAGER"
  | "FACILITY_MANAGER"
  | "SCHOLARSHIP_MANAGER"
  | "SCHOLARSHIP_REVIEWER"
  | "FOOD_PANTRY_MANAGER"
  | "BOARD_MANAGER"
  | "BOARD_MEMBER"
  | "VOLUNTEER"
  | "VIEWER";

export interface Organization {
  id: string;
  name: string;
  legalName: string | null;
  description: string | null;
  organizationType: string;
  email: string | null;
  phone: string | null;
  website: string | null;
  city: string | null;
  state: string | null;
  country: string;
  active: boolean;
}

export interface OrganizationMembership {
  id: string;
  organizationId: string;
  userId: string;
  role: Role;
  active: boolean;
  joinedAt: string;
  createdAt: string;
  updatedAt: string;
}
