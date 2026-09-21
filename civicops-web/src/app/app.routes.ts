import { Routes } from "@angular/router";
import { LoginComponent } from "./core/auth/login.component";
import { authGuard } from "./core/guards/auth.guard";
import { grantManageGuard } from "./core/guards/grant-manage.guard";
import { organizationGuard } from "./core/guards/organization.guard";
import { AppShellComponent } from "./core/layout/app-shell.component";
import { DashboardComponent } from "./features/dashboard/dashboard.component";
import { GrantDetailComponent } from "./features/grants/grant-detail.component";
import { GrantFormComponent } from "./features/grants/grant-form.component";
import { GrantListComponent } from "./features/grants/grant-list.component";
import { GrantReportingComponent } from "./features/grant-reporting/grant-reporting.component";
import { ReportDetailComponent } from "./features/grant-reporting/report-detail.component";
import { TemplateDetailComponent } from "./features/grant-reporting/template-detail.component";
import { VolunteersComponent } from "./features/volunteers/volunteers.component";
import { EventDetailComponent } from "./features/events/event-detail.component";
import { EventFormComponent } from "./features/events/event-form.component";
import { EventListComponent } from "./features/events/event-list.component";
import { CampaignDetailComponent } from "./features/donations/campaign-detail.component";
import { DonationDetailComponent } from "./features/donations/donation-detail.component";
import { DonationsComponent } from "./features/donations/donations.component";
import { CaseDetailComponent } from "./features/cases/case-detail.component";
import { CaseListComponent } from "./features/cases/case-list.component";
import { ClientDetailComponent } from "./features/cases/client-detail.component";
import { EquipmentDetailComponent } from "./features/equipment/equipment-detail.component";
import { EquipmentListComponent } from "./features/equipment/equipment-list.component";
import { FacilityDetailComponent } from "./features/facilities/facility-detail.component";
import { FacilityListComponent } from "./features/facilities/facility-list.component";
import { ReservationDetailComponent } from "./features/facilities/reservation-detail.component";

export const routes: Routes = [
  { path: "login", component: LoginComponent, title: "Sign in · CivicOps" },
  {
    path: "",
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: "dashboard",
        component: DashboardComponent,
        title: "Dashboard · CivicOps",
      },
      {
        path: "organizations/:organizationId/volunteers",
        component: VolunteersComponent,
        canActivate: [organizationGuard],
        title: "Volunteers · CivicOps",
      },
      {
        path: "organizations/:organizationId/events",
        canActivate: [organizationGuard],
        children: [
          {
            path: "",
            component: EventListComponent,
            title: "Events · CivicOps",
          },
          {
            path: "new",
            component: EventFormComponent,
            title: "Create event · CivicOps",
          },
          {
            path: ":eventId/edit",
            component: EventFormComponent,
            title: "Edit event · CivicOps",
          },
          {
            path: ":eventId",
            component: EventDetailComponent,
            title: "Event details · CivicOps",
          },
        ],
      },
      {
        path: "organizations/:organizationId/donations",
        canActivate: [organizationGuard],
        children: [
          {
            path: "",
            component: DonationsComponent,
            title: "Donations · CivicOps",
          },
          {
            path: "campaigns/:campaignId",
            component: CampaignDetailComponent,
            title: "Donation campaign · CivicOps",
          },
          {
            path: "transactions/:donationId",
            component: DonationDetailComponent,
            title: "Donation record · CivicOps",
          },
        ],
      },
      {
        path: "organizations/:organizationId/cases",
        canActivate: [organizationGuard],
        children: [
          { path: "", component: CaseListComponent, title: "Cases · CivicOps" },
          {
            path: "clients/:clientId",
            component: ClientDetailComponent,
            title: "Client detail · CivicOps",
          },
          {
            path: ":caseId",
            component: CaseDetailComponent,
            title: "Case detail · CivicOps",
          },
        ],
      },
      {
        path: "organizations/:organizationId/equipment",
        canActivate: [organizationGuard],
        children: [
          {
            path: "",
            component: EquipmentListComponent,
            title: "Equipment · CivicOps",
          },
          {
            path: ":assetId",
            component: EquipmentDetailComponent,
            title: "Equipment detail · CivicOps",
          },
        ],
      },
      {
        path: "organizations/:organizationId/facilities",
        canActivate: [organizationGuard],
        children: [
          {
            path: "",
            component: FacilityListComponent,
            title: "Facilities · CivicOps",
          },
          {
            path: "reservations/:reservationId",
            component: ReservationDetailComponent,
            title: "Reservation detail · CivicOps",
          },
          {
            path: ":facilityId",
            component: FacilityDetailComponent,
            title: "Facility detail · CivicOps",
          },
        ],
      },
      {
        path: "organizations/:organizationId/scholarships",
        canActivate: [organizationGuard],
        children: [
          {
            path: "",
            loadComponent: () =>
              import("./features/scholarships/scholarship-list.component").then(
                (m) => m.ScholarshipListComponent,
              ),
            title: "Scholarships · CivicOps",
          },
          {
            path: "reviews/:assignmentId",
            loadComponent: () =>
              import("./features/scholarships/scholarship-review.component").then(
                (m) => m.ScholarshipReviewComponent,
              ),
            title: "Scholarship review · CivicOps",
          },
          {
            path: "awards/:awardId",
            loadComponent: () =>
              import("./features/scholarships/scholarship-award-detail.component").then(
                (m) => m.ScholarshipAwardDetailComponent,
              ),
            title: "Scholarship award · CivicOps",
          },
          {
            path: "applications/:applicationId",
            loadComponent: () =>
              import("./features/scholarships/scholarship-application-detail.component").then(
                (m) => m.ScholarshipApplicationDetailComponent,
              ),
            title: "Scholarship application · CivicOps",
          },
          {
            path: ":programId",
            loadComponent: () =>
              import("./features/scholarships/scholarship-program-detail.component").then(
                (m) => m.ScholarshipProgramDetailComponent,
              ),
            title: "Scholarship program · CivicOps",
          },
        ],
      },
      {
        path: "organizations/:organizationId/food-pantry",
        canActivate: [organizationGuard],
        children: [
          {
            path: "",
            loadComponent: () =>
              import("./features/food-pantry/food-pantry-list.component").then(
                (m) => m.FoodPantryListComponent,
              ),
            title: "Food Pantry · CivicOps",
          },
          {
            path: "households/:householdId",
            loadComponent: () =>
              import("./features/food-pantry/pantry-household-detail.component").then(
                (m) => m.PantryHouseholdDetailComponent,
              ),
            title: "Pantry household · CivicOps",
          },
          {
            path: ":pantryId/inventory/:lotId",
            loadComponent: () =>
              import("./features/food-pantry/pantry-inventory-detail.component").then(
                (m) => m.PantryInventoryDetailComponent,
              ),
            title: "Pantry inventory · CivicOps",
          },
          {
            path: ":pantryId/distributions/:distributionId",
            loadComponent: () =>
              import("./features/food-pantry/pantry-distribution-detail.component").then(
                (m) => m.PantryDistributionDetailComponent,
              ),
            title: "Pantry distribution · CivicOps",
          },
          {
            path: ":pantryId",
            loadComponent: () =>
              import("./features/food-pantry/food-pantry-detail.component").then(
                (m) => m.FoodPantryDetailComponent,
              ),
            title: "Food pantry detail · CivicOps",
          },
        ],
      },
      {
        path: "organizations/:organizationId/board",
        canActivate: [organizationGuard],
        children: [
          {
            path: "",
            loadComponent: () =>
              import("./features/board/board-list.component").then(
                (m) => m.BoardListComponent,
              ),
            title: "Board · CivicOps",
          },
          {
            path: "meetings/:meetingId",
            loadComponent: () =>
              import("./features/board/board-meeting-detail.component").then(
                (m) => m.BoardMeetingDetailComponent,
              ),
            title: "Board meeting · CivicOps",
          },
          {
            path: "members/:memberId",
            loadComponent: () =>
              import("./features/board/board-member-detail.component").then(
                (m) => m.BoardMemberDetailComponent,
              ),
            title: "Board member · CivicOps",
          },
          {
            path: "motions/:motionId",
            loadComponent: () =>
              import("./features/board/board-motion-detail.component").then(
                (m) => m.BoardMotionDetailComponent,
              ),
            title: "Board motion · CivicOps",
          },
        ],
      },
      {
        path: "organizations/:organizationId/grant-reporting",
        canActivate: [organizationGuard],
        children: [
          {
            path: "",
            component: GrantReportingComponent,
            title: "Grant Reporting · CivicOps",
          },
          {
            path: "templates/:templateId",
            component: TemplateDetailComponent,
            canActivate: [grantManageGuard],
            title: "Report template · CivicOps",
          },
          {
            path: "reports/:reportId",
            component: ReportDetailComponent,
            title: "Grant report · CivicOps",
          },
        ],
      },
      {
        path: "organizations/:organizationId/grants",
        canActivate: [organizationGuard],
        children: [
          {
            path: "",
            component: GrantListComponent,
            title: "Grants · CivicOps",
          },
          {
            path: "new",
            component: GrantFormComponent,
            canActivate: [grantManageGuard],
            title: "Create grant · CivicOps",
          },
          {
            path: ":grantId",
            component: GrantDetailComponent,
            canActivate: [grantManageGuard],
            title: "Grant details · CivicOps",
          },
          {
            path: ":grantId/edit",
            component: GrantFormComponent,
            canActivate: [grantManageGuard],
            title: "Edit grant · CivicOps",
          },
        ],
      },
      { path: "", pathMatch: "full", redirectTo: "dashboard" },
    ],
  },
  { path: "**", redirectTo: "dashboard" },
];
