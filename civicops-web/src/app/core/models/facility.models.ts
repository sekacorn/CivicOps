export type FacilityType =
  | "COMMUNITY_CENTER"
  | "OFFICE"
  | "CHURCH"
  | "SCHOOL"
  | "WAREHOUSE"
  | "OUTDOOR_SITE"
  | "OTHER";
export type ReservationStatus =
  "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED" | "COMPLETED";

export interface Facility {
  id: string;
  organizationId: string;
  name: string;
  description: string | null;
  facilityType: FacilityType;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  active: boolean;
  timezone: string;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface FacilitySpace {
  id: string;
  facilityId: string;
  facilityName: string;
  name: string;
  description: string | null;
  capacity: number | null;
  active: boolean;
  reservable: boolean;
  locationDetails: string | null;
  accessibilityNotes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface OperatingHours {
  id: string;
  dayOfWeek: DayOfWeek;
  openTime: string | null;
  closeTime: string | null;
  closed: boolean;
}
export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export interface FacilityBlackout {
  id: string;
  facilityId: string;
  facilitySpaceId: string | null;
  startDateTime: string;
  endDateTime: string;
  reason: string;
  active: boolean;
  cancelledAt: string | null;
  createdAt: string;
}

export interface ReservationSummary {
  id: string;
  facilityId: string;
  facilityName: string;
  spaceId: string;
  spaceName: string;
  title: string;
  startDateTime: string;
  endDateTime: string;
  expectedAttendance: number | null;
  status: ReservationStatus;
  requestedAt: string;
  eventId: string | null;
}

export interface ReservationDetail extends ReservationSummary {
  organizationId: string;
  requestedByUserId: string | null;
  requesterName: string | null;
  requesterEmail: string | null;
  purpose: string | null;
  approvedByUserId: string | null;
  approvedAt: string | null;
  rejectedByUserId: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface Availability {
  spaceId: string;
  start: string;
  end: string;
  available: boolean;
  reason: string | null;
}

export interface FacilityReportSummary {
  organizationId: string;
  from: string;
  to: string;
  activeFacilities: number;
  reservableSpaces: number;
  pendingReservations: number;
  approvedReservations: number;
  reservationsInPeriod: number;
  cancelledReservations: number;
}

export interface FacilityUtilizationReport {
  organizationId: string;
  from: string;
  to: string;
  reservationCount: number;
  reservedHours: number;
  approvals: number;
  rejections: number;
  cancellations: number;
  mostUsedSpaces: Array<{
    spaceId: string;
    spaceName: string;
    reservations: number;
    reservedHours: number;
  }>;
}
