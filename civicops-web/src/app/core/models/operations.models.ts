export type VolunteerStatus =
  "APPLICANT" | "ACTIVE" | "INACTIVE" | "SUSPENDED" | "ARCHIVED";
export interface VolunteerSummary {
  id: string;
  firstName: string;
  lastName: string;
  status: VolunteerStatus;
  startDate: string | null;
  skills: string[];
  createdAt: string;
}
export interface VolunteerDetail extends VolunteerSummary {
  organizationId: string;
  userId: string | null;
  email: string;
  phone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  notes: string | null;
  updatedAt: string;
}
export type OpportunityStatus =
  "DRAFT" | "OPEN" | "CLOSED" | "CANCELLED" | "COMPLETED";
export interface Opportunity {
  id: string;
  organizationId: string;
  title: string;
  description: string | null;
  location: string | null;
  startAt: string;
  endAt: string;
  minimumVolunteers: number | null;
  maximumVolunteers: number | null;
  status: OpportunityStatus;
  eventId: string | null;
}
export interface VolunteerShift {
  id: string;
  opportunityId: string;
  title: string;
  startAt: string;
  endAt: string;
  capacity: number;
}
export type AssignmentStatus =
  "REGISTERED" | "CANCELLED" | "CHECKED_IN" | "COMPLETED" | "NO_SHOW";
export interface VolunteerAssignment {
  id: string;
  volunteerId: string;
  shiftId: string;
  status: AssignmentStatus;
  checkedInAt: string | null;
  checkedOutAt: string | null;
}
export type HourStatus = "SUBMITTED" | "APPROVED" | "REJECTED";
export interface VolunteerHour {
  id: string;
  volunteerId: string;
  assignmentId: string | null;
  serviceDate: string;
  hours: number;
  description: string | null;
  status: HourStatus;
  reviewedByUserId: string | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
}
export interface VolunteerReport {
  from: string;
  to: string;
  activeVolunteers: number;
  approvedHours: number;
  upcomingShifts: number;
  openOpportunities: number;
  openShiftCapacity: number;
}

export type EventStatus =
  | "DRAFT"
  | "PUBLISHED"
  | "REGISTRATION_OPEN"
  | "REGISTRATION_CLOSED"
  | "COMPLETED"
  | "CANCELLED";
export type EventType =
  | "COMMUNITY_OUTREACH"
  | "FUNDRAISER"
  | "VOLUNTEER_ACTIVITY"
  | "TRAINING"
  | "WORKSHOP"
  | "FOOD_DISTRIBUTION"
  | "BOARD_MEETING"
  | "PUBLIC_MEETING"
  | "SCHOLARSHIP_EVENT"
  | "OTHER";
export interface EventSummary {
  id: string;
  name: string;
  eventType: EventType;
  startDateTime: string;
  endDateTime: string;
  capacity: number | null;
  status: EventStatus;
}
export interface EventDetail extends EventSummary {
  organizationId: string;
  description: string | null;
  location: string | null;
  registrationRequired: boolean;
  registrationDeadline: string | null;
  waitlistEnabled: boolean;
  linkedGrantId: string | null;
  linkedGrantName: string | null;
  linkedDonationCampaignId: string | null;
  linkedDonationCampaignName: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export type RegistrationStatus =
  "REGISTERED" | "WAITLISTED" | "CANCELLED" | "ATTENDED" | "NO_SHOW";
export interface RegistrationSummary {
  id: string;
  attendeeName: string;
  status: RegistrationStatus;
  registrationDate: string;
}
export interface RegistrationDetail extends RegistrationSummary {
  eventId: string;
  organizationId: string;
  registeredUserId: string | null;
  attendeeEmail: string | null;
  attendeePhone: string | null;
  checkedInAt: string | null;
  checkedOutAt: string | null;
}
export interface EventReport {
  eventId: string;
  capacity: number | null;
  registered: number;
  waitlisted: number;
  attended: number;
  noShow: number;
  cancelled: number;
  remainingCapacity: number | null;
  attendanceRate: number;
  linkedGrantId: string | null;
  linkedGrantName: string | null;
  linkedCampaignId: string | null;
  linkedCampaignName: string | null;
  campaignGoal: number | null;
  campaignRaised: number | null;
  campaignDonationCount: number;
}

export type DonorType =
  "INDIVIDUAL" | "ORGANIZATION" | "FOUNDATION" | "GOVERNMENT" | "OTHER";
export interface DonorSummary {
  id: string;
  donorType: DonorType;
  displayName: string;
  anonymous: boolean;
  communicationOptOut: boolean;
  createdAt: string;
  updatedAt: string;
}
export interface DonorDetail extends DonorSummary {
  organizationId: string;
  firstName: string | null;
  lastName: string | null;
  organizationName: string | null;
  email: string | null;
  phone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  notes: string | null;
}
export type CampaignStatus = "DRAFT" | "ACTIVE" | "CLOSED" | "CANCELLED";
export interface DonationCampaign {
  id: string;
  organizationId: string;
  name: string;
  description: string | null;
  goalAmount: number;
  startDate: string | null;
  endDate: string | null;
  status: CampaignStatus;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}
export type DonationStatus = "RECORDED" | "REVERSED";
export type PaymentMethod =
  "CASH" | "CHECK" | "CARD" | "ACH" | "WIRE" | "STOCK" | "IN_KIND" | "OTHER";
export interface DonationSummary {
  id: string;
  donorId: string | null;
  campaignId: string | null;
  anonymous: boolean;
  amount: number;
  donationDate: string;
  paymentMethod: PaymentMethod;
  restricted: boolean;
  designation: string | null;
  acknowledgementStatus: string;
  status: DonationStatus;
  createdAt: string;
}
export interface DonationDetail extends DonationSummary {
  organizationId: string;
  inKindDescription: string | null;
  restrictionDescription: string | null;
  referenceNumber: string | null;
  receiptNumber: string | null;
  notes: string | null;
  reversedAt: string | null;
  reversedByUserId: string | null;
  reversalReason: string | null;
  createdByUserId: string;
  updatedAt: string;
}
export interface PaymentMethodTotal {
  paymentMethod: PaymentMethod;
  amount: number;
  donationCount: number;
}
export interface CampaignFinancialSummary {
  campaignId: string;
  campaignName: string;
  goalAmount: number;
  amountRaised: number;
  remainingToGoal: number;
  percentageOfGoal: number;
  donationCount: number;
}
export interface DonationReportSummary {
  totalDonations: number;
  totalAmount: number;
  averageDonation: number;
  restrictedAmount: number;
  unrestrictedAmount: number;
  activeCampaigns: number;
  uniqueDonors: number;
}
