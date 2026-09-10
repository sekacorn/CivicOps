# CivicOps

Open-source operations platform for nonprofits and community organizations.

## Overview

CivicOps is a modular monolith: one application with explicit domain boundaries, shared operational foundations, and a path to extraction only when operational needs justify it.

```
                    Angular
                       |
                    REST API
                       |
              CivicOps Backend (Spring Boot)
       +---------------+---------------+
       |               |               |
   Volunteers      Funding           Events
                 Grants + Donations
                       |
                 CivicOps Core
       +---------------+---------------+
       |               |               |
    Identity         Audit       Notifications
                       |
                  PostgreSQL
```

## Release candidate: 0.1.0

The original backend roadmap is complete. Version 0.1.0 provides a Java 21 / Spring Boot modular monolith, PostgreSQL 16 persistence, Flyway-managed schema, stable `/api/v1` REST contracts, OpenAPI UI, an Angular application shell, Docker Compose, Kubernetes starter manifests, and CI. Core implements organizations, users, memberships, role rules, BCrypt password hashing, JWT access tokens, rotating refresh tokens, and live organization authorization lookups.

Authentication follows this path:

```
Login -> JWT access token -> CivicOpsPrincipal -> organization membership -> role authorization -> service
```

Access tokens contain only the user ID and standard issuer/time/token claims. User activation and organization authorization are resolved from the database, so deactivation and membership changes take effect without waiting for an access token to expire.

Phase 3 adds organization-scoped volunteer profiles, normalized skills, opportunities, shifts, capacity-safe assignments, check-in/out, submitted and approved hours, self-service routes, and summary reporting. Volunteer totals are calculated from approved hour entries rather than stored as mutable counters.

Phase 4 adds organization-scoped grants, controlled grant lifecycles, active-grant expenses, overspending protection, exact decimal financial calculations, reporting deadlines, category totals, and portfolio reporting. Award and expense amounts use two-decimal `BigDecimal` values. Derived totals are calculated from authoritative expense records; utilization is rounded to two decimal places with `HALF_UP`.

Phase 5 adds privacy-aware donors, immutable monetary and in-kind donation records, auditable reversals, lightweight campaigns, restricted-purpose tracking, acknowledgement state, and date-range financial reporting. Anonymous gifts have no donor association and therefore require no fake identity data. Campaign totals are always derived from recorded, non-reversed donations.

Phases 6–12 add Event Management, Case Management, Equipment Checkout, Facility Reservation, Scholarship Management, Food Pantry Management, and Board Management. Phase 13 adds the Grant Reporting Assistant, an evidence-grounded composition layer over those operational modules. These modules retain their own lifecycle and privacy boundaries while reusing CivicOps authentication, organization isolation, reporting, and persistence foundations.

## Technology stack

Java 21, Spring Boot, Spring Data JPA, Spring Security, Bean Validation, PostgreSQL, Flyway, Maven, Angular, TypeScript, Docker, and Kubernetes.

## Local development

Prerequisites: Java 21, Maven 3.9+, Node 22+, Docker Desktop.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

The web shell is available at `http://localhost:4200`; health is available at `http://localhost:8080/actuator/health` and `http://localhost:8080/api/v1/health`. Swagger UI is at `http://localhost:8080/swagger-ui.html`; the OpenAPI JSON is at `http://localhost:8080/api-docs`.

To run the backend against a local PostgreSQL instance, export the `CIVICOPS_DATABASE_*` variables and `CIVICOPS_JWT_SECRET`, then run `mvn spring-boot:run`.

Required and optional authentication variables:

- `CIVICOPS_JWT_SECRET`: required, unique, at least 32 bytes
- `CIVICOPS_ACCESS_TOKEN_TTL`: optional ISO-8601 duration; defaults to `PT15M`
- `CIVICOPS_REFRESH_TOKEN_TTL`: optional ISO-8601 duration; defaults to `P30D`
- `CIVICOPS_DATABASE_URL`: JDBC URL; defaults to local PostgreSQL for direct development
- `CIVICOPS_DATABASE_USERNAME`: database user; defaults to `civicops` for direct development
- `CIVICOPS_DATABASE_PASSWORD`: database password; required by Docker Compose and should be externalized for every deployment
- `CIVICOPS_GRANT_REPORT_NARRATIVE_PROVIDER`: optional; defaults to `deterministic`

The application fails at startup when the signing secret is absent or too short. `.env.example` contains placeholders only and `.env` is ignored.

Build and verify with `mvn clean test` and `mvn verify`. Run directly with `mvn spring-boot:run`, or build the release image with `docker compose up --build`. Flyway applies immutable migrations at startup and Hibernate validates, rather than creates, the schema. Collection endpoints return the stable `PageResponse` envelope (`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`). Errors use a consistent timestamp/status/code/message/path contract with optional validation field errors.

See [API modules](docs/API_MODULES.md), [architecture](docs/ARCHITECTURE.md), [security](docs/SECURITY.md), and [release history](CHANGELOG.md).

## Project structure

```
src/main/java/org/civicops
  core/           # organizations, users, memberships, roles, health, and security seam
  shared/         # narrow technical primitives
  volunteers/     # volunteer profiles, opportunities, shifts, assignments, hours, reporting
  grants/         # grants, expenses, lifecycle, financial and portfolio reporting
  donations/      # donors, immutable gifts, campaigns, reversals, and reporting
  events/         # events, registration, waitlists, attendance, and reporting
  cases/          # clients, cases, notes, tasks, services, and reporting
  equipment/      # inventory, checkout, maintenance, and reporting
  facilities/     # facilities, spaces, availability, reservations, and reporting
  scholarships/   # programs, applicants, reviews, selections, awards, and reporting
  foodpantry/     # locations, item catalog, lot inventory, households, distributions, and reporting
  board/          # members, terms, meetings, governance actions, minutes, and reporting
  grantreporting/ # templates, frozen evidence, narratives, review, finalization, and export
civicops-web/     # Angular feature application
k8s/              # deployment starters; use managed PostgreSQL in production
```

## Security

No real credentials are committed. Passwords are BCrypt-hashed and never returned. Login failures use one generic response for unknown users, inactive users, and wrong passwords.

`POST /api/v1/users`, login, refresh, logout, health, and API documentation are public. User-profile reads are self-only. Organization reads require an active membership. Membership administration requires `ORG_ADMIN`; authenticated cross-organization requests receive `403 Forbidden`. Creating an organization also creates an `ORG_ADMIN` membership for its creator. User lookup by arbitrary email is no longer exposed.

Refresh tokens are opaque random values. Only their SHA-256 hashes are stored. Every successful refresh revokes the presented token and creates a replacement; logout revokes the supplied refresh token. Expired, revoked, and reused tokens return a generic authentication error.

`SYSTEM_ADMIN` remains a platform role and is not stored in organization memberships. Platform-administration behavior is not implemented yet.

Swagger UI defines Bearer JWT authorization. Obtain an access token from `/api/v1/auth/login`, select **Authorize**, and paste the token.

## Volunteer API workflow

The examples use placeholders only. Save the IDs and tokens returned by each response in the corresponding shell variables.

```bash
API=http://localhost:8080/api/v1

# 1. Register and 2. log in
curl -X POST "$API/users" -H 'Content-Type: application/json' \
  -d '{"firstName":"Casey","lastName":"Coordinator","email":"casey@example.org","password":"replace-with-a-development-password"}'
curl -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"casey@example.org","password":"replace-with-a-development-password"}'
ACCESS_TOKEN='<access token from login>'

# 3. Create an organization
curl -X POST "$API/organizations" -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"Example Civic Group","organizationType":"NONPROFIT","country":"US"}'
ORGANIZATION_ID='<organization id>'

# 4. Create a volunteer
curl -X POST "$API/organizations/$ORGANIZATION_ID/volunteers" -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' -d '{"firstName":"Jordan","lastName":"Lee","email":"jordan@example.org","status":"ACTIVE","skills":["First Aid"]}'
VOLUNTEER_ID='<volunteer id>'

# 5. Create and 6. open an opportunity
curl -X POST "$API/organizations/$ORGANIZATION_ID/volunteer-opportunities" -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' -d '{"title":"Community Day","startAt":"2030-06-01T13:00:00Z","endAt":"2030-06-01T21:00:00Z","minimumVolunteers":1,"maximumVolunteers":10}'
OPPORTUNITY_ID='<opportunity id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/volunteer-opportunities/$OPPORTUNITY_ID/open" -H "Authorization: Bearer $ACCESS_TOKEN"

# 7. Create a shift and 8. register the volunteer
curl -X POST "$API/organizations/$ORGANIZATION_ID/volunteer-opportunities/$OPPORTUNITY_ID/shifts" -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' -d '{"title":"Afternoon","startAt":"2030-06-01T14:00:00Z","endAt":"2030-06-01T18:00:00Z","capacity":4}'
SHIFT_ID='<shift id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/volunteer-shifts/$SHIFT_ID/assignments" -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"volunteerId\":\"$VOLUNTEER_ID\"}"
ASSIGNMENT_ID='<assignment id>'

# 9. Check in and 10. check out
curl -X POST "$API/organizations/$ORGANIZATION_ID/volunteer-assignments/$ASSIGNMENT_ID/check-in" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/volunteer-assignments/$ASSIGNMENT_ID/check-out" -H "Authorization: Bearer $ACCESS_TOKEN"

# 11. Submit and 12. approve hours
curl -X POST "$API/organizations/$ORGANIZATION_ID/volunteer-hours" -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"volunteerId\":\"$VOLUNTEER_ID\",\"assignmentId\":\"$ASSIGNMENT_ID\",\"serviceDate\":\"2030-06-01\",\"hours\":4.00}"
HOUR_ENTRY_ID='<hour entry id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/volunteer-hours/$HOUR_ENTRY_ID/approve" -H "Authorization: Bearer $ACCESS_TOKEN"

# 13. Retrieve reporting
curl "$API/organizations/$ORGANIZATION_ID/volunteer-reports/summary?from=2030-06-01&to=2030-06-30" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Volunteer, opportunity, shift, assignment, and hour collections are paginated with `page`, `size`, and documented safe sort fields. Filters include volunteer `status`, `email`, and `skill`; opportunity/shift `from` and `to`; and hour/assignment status and volunteer identifiers.

## Grant Management

The grant lifecycle is explicit and cannot be changed through PATCH:

```text
Prospect -> Application in progress -> Submitted -> Awarded -> Active -> Expenses -> Reporting -> Closed
                                         |
                                         +-> Rejected
Prospect/Application in progress -> Withdrawn
```

Expenses are permitted only for `ACTIVE` grants. A grant row is locked while spending is calculated and an expense is inserted, preventing concurrent requests from exceeding the award. Closed grants cannot be edited, reopened, or receive expenses. JSON `null` follows the existing CivicOps PATCH convention and behaves as an omitted field.

The following placeholder-only workflow assumes the registration, login, and organization steps above have produced `ACCESS_TOKEN` and `ORGANIZATION_ID` values.

```bash
API=http://localhost:8080/api/v1

# Create a $50,000 prospect
curl -X POST "$API/organizations/$ORGANIZATION_ID/grants" \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"grantName":"Community Technology Grant","grantorName":"Example Foundation","awardAmount":50000.00,"startDate":"2030-07-01","endDate":"2031-06-30","reportingDeadline":"2031-07-31","restricted":true,"restrictionDescription":"Community technology programming"}'
GRANT_ID='<grant id>'

# Move through the controlled lifecycle
curl -X POST "$API/organizations/$ORGANIZATION_ID/grants/$GRANT_ID/start-application" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/grants/$GRANT_ID/submit" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/grants/$GRANT_ID/mark-awarded" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/grants/$GRANT_ID/activate" -H "Authorization: Bearer $ACCESS_TOKEN"

# Record $4,200 of supplies and $1,500 of transportation
curl -X POST "$API/organizations/$ORGANIZATION_ID/grants/$GRANT_ID/expenses" \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"amount":4200.00,"expenseDate":"2030-08-15","category":"SUPPLIES","description":"Program equipment and supplies"}'
curl -X POST "$API/organizations/$ORGANIZATION_ID/grants/$GRANT_ID/expenses" \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"amount":1500.00,"expenseDate":"2030-08-20","category":"TRANSPORTATION","description":"Participant transportation"}'

# Expected: award 50000.00, spent 5700.00, remaining 44300.00, utilization 11.40
curl "$API/organizations/$ORGANIZATION_ID/grants/$GRANT_ID/financial-summary" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/grants/$GRANT_ID/expenses/by-category" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/grant-reports/summary" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Grant collections support `status`, case-insensitive partial `grantor`, `reportingDeadlineFrom`, `reportingDeadlineTo`, `startFrom`, `endBefore`, and `restricted`. Safe grant sort fields are `grantName`, `startDate`, `endDate`, `reportingDeadline`, and `createdAt`.

Expense collections support `category`, `from`, and `to`. Safe expense sort fields are `expenseDate`, `amount`, and `createdAt`. Both collections support `page` and `size`; page size is capped at 100 and unsupported sort fields return HTTP 400.

## Donation Management

```text
Donor -> Donation -> Campaign -> Restriction / Designation -> Acknowledgement -> Reporting
```

Anonymous gifts omit `donorId` and set `anonymous` to `true`; CivicOps does not create fake “Anonymous” people. Identified gifts require a donor from the same organization. Donation amounts and estimated in-kind values use `NUMERIC(19,2)`/`BigDecimal`. In-kind gifts also require a description of the goods or services and are not treated as inventory.

Recorded donations are not edited or deleted. A reversal records its reason, timestamp, and responsible user while preserving the original financial record. Reversed donations are excluded from campaign and organization totals. Receipt and external reference numbers are optional but organization-unique when supplied; identical donations without those identifiers remain valid.

The examples below use placeholders and assume `ACCESS_TOKEN` and `ORGANIZATION_ID` were obtained through the earlier authentication workflow.

```bash
API=http://localhost:8080/api/v1

# Create an individual donor
curl -X POST "$API/organizations/$ORGANIZATION_ID/donors" \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"donorType":"INDIVIDUAL","firstName":"Example","lastName":"Donor","email":"donor@example.org"}'
DONOR_ID='<donor id>'

# Create and activate a $25,000 campaign
curl -X POST "$API/organizations/$ORGANIZATION_ID/donation-campaigns" \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Community Food Drive","goalAmount":25000.00,"startDate":"2030-09-01","endDate":"2030-12-31"}'
CAMPAIGN_ID='<campaign id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/donation-campaigns/$CAMPAIGN_ID/activate" \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# Record identified ACH and card donations
curl -X POST "$API/organizations/$ORGANIZATION_ID/donations" \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"donorId\":\"$DONOR_ID\",\"amount\":10000.00,\"donationDate\":\"2030-09-15\",\"paymentMethod\":\"ACH\",\"campaignId\":\"$CAMPAIGN_ID\"}"
curl -X POST "$API/organizations/$ORGANIZATION_ID/donations" \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"donorId\":\"$DONOR_ID\",\"amount\":5000.00,\"donationDate\":\"2030-10-01\",\"paymentMethod\":\"CARD\",\"campaignId\":\"$CAMPAIGN_ID\"}"

# Record an anonymous restricted check donation without fake donor data
curl -X POST "$API/organizations/$ORGANIZATION_ID/donations" \
  -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"anonymous\":true,\"amount\":3750.00,\"donationDate\":\"2030-10-15\",\"paymentMethod\":\"CHECK\",\"campaignId\":\"$CAMPAIGN_ID\",\"restricted\":true,\"designation\":\"Food pantry supplies\"}"

# Expected campaign result: goal 25000, raised 18750, remaining 6250, progress 75.00%
curl "$API/organizations/$ORGANIZATION_ID/donation-campaigns/$CAMPAIGN_ID/financial-summary" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/donation-reports/summary?from=2030-09-01&to=2030-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/donation-reports/by-payment-method?from=2030-09-01&to=2030-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Donor filters are `type`, normalized `email`, and `anonymous`; safe sorts are `lastName`, `organizationName`, and `createdAt`. Donation filters are `donorId`, `campaignId`, `paymentMethod`, `restricted`, `from`, and `to`; safe sorts are `donationDate`, `amount`, and `createdAt`. Campaign filters are `status`, `from`, and `to`; safe sorts are `name`, `startDate`, `endDate`, and `createdAt`. Page size is capped at 100.

## Roadmap

1. Platform administration and account lifecycle workflows
2. Events, registration, waitlist, volunteer integration
3. Dashboards, audit events, notifications, and the remaining civic operations modules

## Contributing and license

Contributions are welcome. Add a license before the first public release; no license is declared yet.
# Event Management

Event Management is an organization-scoped module for community programming. Its lifecycle is:

```text
Draft → Published → Registration Open → Registration Closed → Completed
                 └────────────────────────────────────────────→ Cancelled
```

Events support an optional grant, donation campaign, and volunteer opportunity association while each module retains ownership of its own records. Registration is administrator-mediated for external attendees; authenticated organization members may register and cancel their own registration through the self-service routes. A capacity-aware, pessimistically locked registration flow puts overflow attendees on a FIFO waitlist and promotes the earliest waiting attendee when a registered attendee cancels.

```text
Grant ─┐
       ├─ Event ─ Attendees / Attendance
Campaign ─┘   └─ Volunteer Opportunity → Shifts → Assignments
```

Example placeholders (replace IDs and bearer token):

```bash
curl -X POST "$API/api/v1/organizations/$ORG/events" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"name":"Summer Community Cleanup","eventType":"COMMUNITY_OUTREACH","startDateTime":"2026-09-15T14:00:00Z","endDateTime":"2026-09-15T17:00:00Z","capacity":3,"registrationRequired":true,"waitlistEnabled":true}'
curl -X POST "$API/api/v1/organizations/$ORG/events/$EVENT/publish" -H "Authorization: Bearer $TOKEN"
curl -X POST "$API/api/v1/organizations/$ORG/events/$EVENT/open-registration" -H "Authorization: Bearer $TOKEN"
curl -X POST "$API/api/v1/organizations/$ORG/events/$EVENT/registrations" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"attendeeName":"Avery Citizen","attendeeEmail":"avery@example.org"}'
curl "$API/api/v1/organizations/$ORG/events/$EVENT/report" -H "Authorization: Bearer $TOKEN"
```

Event lists accept `status`, `type`, `from`, `to`, `grantId`, and `campaignId`; allowed sorts are `name`, `startDateTime`, `endDateTime`, and `createdAt`. Registration lists accept `status`, `attendeeEmail`, `from`, and `to`; allowed sorts are `registrationDate`, `attendeeName`, and `createdAt`.

Event ownership stops at the Event boundary: it owns lifecycle, registrations, and attendance. Grants retain grant lifecycle, awards, and expenses; Donation Campaigns retain campaign lifecycle and donations; Volunteer Management retains opportunities, shifts, assignments, and approved hours. Cancelling or completing an Event never deletes or automatically mutates records in those modules.

## Case Management

Case Management separates privacy-safe collection views from authorized detail views. `ORG_ADMIN` and `CASE_MANAGER` have organization-wide access. `CASE_WORKER` can read the client and operational history only for cases assigned to that worker. `PROGRAM_MANAGER` can read PII-free aggregate summary and service-delivery reports; unrelated roles cannot access Case Management data.

```text
Client → Case → Assigned worker
              ├─ Append-only notes
              ├─ Tasks → Completed / Cancelled
              └─ Historical service records

Open → In Progress → On Hold → In Progress → Closed
  └──────────────────────────────────────────→ Cancelled
```

Case status, assignment, client, organization, and case number are never changed through generic PATCH. Closed and cancelled cases become history-only: their client, notes, tasks, and service records remain readable and are not cascade-deleted. Case numbers and optional client external references are unique within an organization.

Example placeholders (assumes an authenticated organization administrator has assigned the `CASE_MANAGER` and `CASE_WORKER` memberships):

```bash
# Create a client and case
curl -X POST "$API/organizations/$ORGANIZATION_ID/clients" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"externalReferenceNumber":"CLIENT-100","firstName":"Avery","lastName":"Citizen","email":"avery@example.org"}'
CLIENT_ID='<client id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/cases" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"clientId\":\"$CLIENT_ID\",\"caseNumber\":\"CASE-100\",\"title\":\"Housing support\",\"caseType\":\"HOUSING\",\"priority\":\"HIGH\"}"
CASE_ID='<case id>'

# Assign, start, document work, and close
curl -X POST "$API/organizations/$ORGANIZATION_ID/cases/$CASE_ID/assign" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"userId\":\"$CASE_WORKER_USER_ID\"}"
curl -X POST "$API/organizations/$ORGANIZATION_ID/cases/$CASE_ID/start" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/cases/$CASE_ID/notes" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"noteType":"PROGRESS","content":"Initial assessment completed","privateNote":true}'
curl -X POST "$API/organizations/$ORGANIZATION_ID/cases/$CASE_ID/tasks" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"title":"Submit application","priority":"HIGH"}'
curl -X POST "$API/organizations/$ORGANIZATION_ID/cases/$CASE_ID/services" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"serviceType":"HOUSING_ASSISTANCE","serviceDate":"2030-05-01","valueAmount":125.50}'
curl -X POST "$API/organizations/$ORGANIZATION_ID/cases/$CASE_ID/close" -H "Authorization: Bearer $ACCESS_TOKEN"

# PII-free aggregate reports
curl "$API/organizations/$ORGANIZATION_ID/case-reports/summary?from=2030-01-01&to=2030-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/case-reports/workload" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/case-reports/services?from=2030-01-01&to=2030-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Client filters are `active`, normalized `email`, `externalReferenceNumber`, and `name`; safe sorts are `lastName` and `createdAt`. Case filters are `status`, `priority`, `caseType`, `clientId`, `assignedUserId`, `openedFrom`, and `openedTo`; safe sorts are `openedDate`, `priority`, `status`, `updatedAt`, and `caseNumber`. Tasks and service records support due/service date filters and safe allowlisted sorting. Collection page size is capped at 100.

## Equipment Checkout Management

Equipment Checkout Management maintains an organization-scoped inventory without deleting operational history. `ORG_ADMIN` and `EQUIPMENT_MANAGER` manage categories, assets, checkouts, returns, loss, maintenance, and reports. `PROGRAM_MANAGER`, `EVENT_COORDINATOR`, `VOLUNTEER_COORDINATOR`, and `VIEWER` may read inventory and aggregate reports; they cannot see borrower detail or mutate equipment. Other roles have no implicit equipment access.

Status and physical condition are separate. Overdue is derived at read time when an active checkout's due time is earlier than the application clock, so no scheduled status update can become stale.

```text
AVAILABLE → CHECKED_OUT → AVAILABLE
                      └→ MAINTENANCE → AVAILABLE

AVAILABLE → CHECKED_OUT → LOST
AVAILABLE → RETIRED
```

A damaged or unusable return enters `MAINTENANCE`; a usable maintenance result restores `AVAILABLE`. Lost and retired assets are not revived by maintenance. Retirement is rejected while an asset is checked out. Checkout and maintenance rows remain as historical records through every transition.

The checkout flow accepts either an active CivicOps member (`borrowerUserId`) or an external borrower (`borrowerName`, with an optional validated email). The asset row is pessimistically locked before availability is checked, and PostgreSQL also enforces at most one active checkout per asset.

Example placeholders (assumes an authenticated `EQUIPMENT_MANAGER`; replace IDs and future timestamps):

```bash
# Create a category and asset
curl -X POST "$API/organizations/$ORGANIZATION_ID/equipment-categories" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"name":"Laptops","description":"Portable computers"}'
CATEGORY_ID='<category id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/equipment" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"assetTag\":\"LAP-001\",\"name\":\"Community laptop\",\"categoryId\":\"$CATEGORY_ID\",\"condition\":\"GOOD\"}"
ASSET_ID='<asset id>'

# Check out to an external borrower and return it
curl -X POST "$API/organizations/$ORGANIZATION_ID/equipment/$ASSET_ID/checkouts" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"borrowerName":"Avery Citizen","borrowerEmail":"avery@example.org","dueAt":"2035-09-15T17:00:00Z","checkoutCondition":"GOOD"}'
CHECKOUT_ID='<checkout id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/equipment-checkouts/$CHECKOUT_ID/check-in" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"returnCondition":"DAMAGED","notes":"Screen requires repair"}'

# Record, start, and complete the repair
curl -X POST "$API/organizations/$ORGANIZATION_ID/equipment/$ASSET_ID/maintenance" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"maintenanceType":"REPAIR","description":"Replace screen","cost":75.25,"vendor":"Community Repair"}'
MAINTENANCE_ID='<maintenance id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/equipment-maintenance/$MAINTENANCE_ID/start" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/equipment-maintenance/$MAINTENANCE_ID/complete" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"resultingCondition":"GOOD"}'

# Inventory, utilization, and maintenance reports
curl "$API/organizations/$ORGANIZATION_ID/equipment-reports/summary" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/equipment-reports/utilization?from=2035-01-01&to=2035-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/equipment-reports/maintenance?from=2035-01-01&to=2035-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Equipment filters are `status`, `condition`, `categoryId`, `assetTag`, `name`, and `available`; safe sorts are `assetTag`, `name`, `status`, `condition`, and `createdAt`. Checkout filters are `status`, `assetId`, `borrowerUserId`, `overdue`, `checkedOutFrom`, `checkedOutTo`, `dueFrom`, and `dueTo`; safe sorts are `checkedOutAt`, `dueAt`, `checkedInAt`, and `createdAt`. Maintenance filters are `status`, `type`, `assetId`, `startedFrom`, and `startedTo`; safe sorts are `startedAt`, `completedAt`, and `createdAt`. Collection page size is capped at 100. Bearer authentication is required for every equipment route.

## Facility Reservation Management

Facility Reservation Management models buildings and reservable spaces in each facility's IANA timezone. A normal daily operating-hours definition provides the intentionally small recurring-calendar boundary; approved reservations and active facility-wide or space-specific blackouts determine availability.

```text
Facility → Space → Operating Hours → Availability → Reservation Request
                                                    ↓
                                                 PENDING
                                  ┌─────────────────┼──────────────┐
                               APPROVED          REJECTED       CANCELLED
                                  ↓
                               COMPLETED
```

`ORG_ADMIN` and `FACILITY_MANAGER` manage facilities, spaces, hours, blackouts, approvals, and reports. `EVENT_COORDINATOR` and `PROGRAM_MANAGER` can read availability and create PENDING requests. `VIEWER` can read facility, space, availability, and privacy-safe reservation summaries. Requester email and private notes are restricted to facility management or the authenticated requester.

PENDING requests do not block the calendar. Approval pessimistically locks the space and rechecks operating hours, blackouts, and approved reservations. The overlap rule is `existing.start < requested.end && existing.end > requested.start`, so adjacent reservations such as 10:00–11:00 and 11:00–12:00 are allowed. A linked Event must belong to the same organization and fit entirely inside the reservation window; neither module changes the other's lifecycle.

Example placeholders (replace IDs, bearer token, dates, and the operating-hours day):

```bash
curl -X POST "$API/organizations/$ORGANIZATION_ID/facilities" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"name":"Hope Community Center","facilityType":"COMMUNITY_CENTER","timezone":"America/New_York","country":"US"}'
FACILITY_ID='<facility id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/facilities/$FACILITY_ID/spaces" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"name":"Training Room","capacity":30,"reservable":true}'
SPACE_ID='<space id>'
curl -X PUT "$API/organizations/$ORGANIZATION_ID/facilities/$FACILITY_ID/operating-hours" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '[{"dayOfWeek":"MONDAY","openTime":"09:00:00","closeTime":"21:00:00","closed":false}]'
curl "$API/organizations/$ORGANIZATION_ID/facility-spaces/$SPACE_ID/availability?start=2030-06-03T14:00:00Z&end=2030-06-03T16:00:00Z" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/facility-reservations" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"facilitySpaceId\":\"$SPACE_ID\",\"title\":\"Community Workshop\",\"startDateTime\":\"2030-06-03T14:00:00Z\",\"endDateTime\":\"2030-06-03T16:00:00Z\",\"expectedAttendance\":20}"
RESERVATION_ID='<reservation id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/facility-reservations/$RESERVATION_ID/approve" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/facility-reports/summary?from=2030-06-01&to=2030-06-30" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/facility-reports/utilization?from=2030-06-01&to=2030-06-30" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Facility filters are `active`, `type`, and `city`; safe sorts are `name` and `createdAt`. Space filters are `facilityId`, `active`, `reservable`, and `minimumCapacity`; safe sorts are `name`, `capacity`, and `createdAt`. Reservation filters are `status`, `facilityId`, `spaceId`, `requesterUserId`, `eventId`, `from`, and `to`; safe sorts are `startDateTime`, `endDateTime`, `requestedAt`, and `createdAt`. Blackout filters are `facilityId`, `spaceId`, `from`, and `to`; safe sorts are `startDateTime` and `createdAt`. Page size is capped at 100.

## Scholarship Management

Scholarship Management keeps program, applicant, review, selection, and award history within one organization while separating public-safe summaries from sensitive applicant and reviewer details.

```text
Scholarship Program → Application Window → Applicants → Submitted Applications
                                                       ↓
                                             Review Assignments
                                                       ↓
                                              Scores / Comments
                                                       ↓
                                                   Finalists
                                                       ↓
                                                   Selection
                                                       ↓
                                             Scholarship Awards
```

Programs follow `DRAFT → OPEN → CLOSED → REVIEWING → AWARDED`; draft, open, closed, or reviewing programs may be cancelled where the lifecycle permits. Applications follow `DRAFT → SUBMITTED → UNDER_REVIEW → FINALIST → SELECTED`, with explicit not-selected and withdrawal outcomes. Generic PATCH cannot alter lifecycle state, and submitted applications are immutable through ordinary editing.

`ORG_ADMIN` and `SCHOLARSHIP_MANAGER` manage programs, applicants, reviewer assignments, selections, awards, and private reporting. `SCHOLARSHIP_REVIEWER` sees only explicitly assigned review work and their own comments. `PROGRAM_MANAGER` may read PII-free aggregate reporting. `VIEWER` may read program and applicant summaries but not applicant private detail. Applicant email, phone, date of birth, address, household income, financial statements, notes, and reviewer comments are excluded from ordinary summaries.

The program row is pessimistically locked during selection. If `numberOfAwards` is configured, concurrent selections cannot exceed it. Award amounts use two-decimal `BigDecimal` values and the shared Money utility. Awards track `OFFERED → ACCEPTED → DISBURSED`, `OFFERED → DECLINED`, and allowed cancellation transitions; CivicOps does not initiate payment transfers. Scholarship documents store metadata and external storage references only—never raw document bytes.

Application deadlines use UTC calendar dates and allow submission through the configured deadline date. Reviewer scores are constrained to `0.00–100.00`.

Example placeholders:

```bash
API=http://localhost:8080/api/v1

curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-programs" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"name":"Community Scholars","academicYear":"2030-31","applicationOpenDate":"2030-01-01","applicationDeadline":"2030-03-01","awardAmount":2500.00,"numberOfAwards":2}'
PROGRAM_ID='<program id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-programs/$PROGRAM_ID/open" -H "Authorization: Bearer $ACCESS_TOKEN"

curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-applicants" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"firstName":"Avery","lastName":"Citizen","email":"avery@example.org","schoolName":"Central High","graduationYear":2030}'
APPLICANT_ID='<applicant id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-programs/$PROGRAM_ID/applications" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"applicantId\":\"$APPLICANT_ID\",\"eligibilityConfirmed\":true,\"personalStatement\":\"Community service statement\",\"gpa\":3.75,\"requestedAmount\":2500.00}"
APPLICATION_ID='<application id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-applications/$APPLICATION_ID/submit" -H "Authorization: Bearer $ACCESS_TOKEN"

curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-programs/$PROGRAM_ID/close" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-programs/$PROGRAM_ID/start-review" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-applications/$APPLICATION_ID/reviewers" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"reviewerUserId\":\"$REVIEWER_USER_ID\"}"
ASSIGNMENT_ID='<assignment id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-review-assignments/$ASSIGNMENT_ID/submit-review" -H "Authorization: Bearer $REVIEWER_TOKEN" -H 'Content-Type: application/json' -d '{"score":92.50,"recommendation":"STRONGLY_RECOMMEND","comments":"Strong community record"}'

curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-applications/$APPLICATION_ID/finalist" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-applications/$APPLICATION_ID/select" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/scholarship-applications/$APPLICATION_ID/award" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"amount":2500.00,"awardDate":"2030-04-01"}'

curl "$API/organizations/$ORGANIZATION_ID/scholarship-reports/summary" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/scholarship-reports/programs/$PROGRAM_ID" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/scholarship-reports/reviews" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Program filters are `status`, `academicYear`, `openFrom`, and `openTo`; safe sorts are `name`, `applicationDeadline`, and `createdAt`. Applicant filters are `schoolName`, `graduationYear`, and manager-only normalized `email`; safe sorts are `lastName`, `graduationYear`, and `createdAt`. Application filters are `status`, `applicantId`, submission dates, and `eligibilityConfirmed`; safe sorts are `submittedAt`, `status`, and `createdAt`. Award filters are `programId`, `status`, and award-date range; safe sorts are `awardDate`, `amount`, and `createdAt`. Collection page size is capped at 100.

## Food Pantry Management

Food Pantry Management provides organization-scoped pantry locations, a reusable item catalog, lot-level inventory, optional household records, atomic distribution visits, adjustment history, and aggregate reporting. It intentionally remains separate from Donation Management: monetary and in-kind donor records belong to Donations, while received physical inventory and its operational ledger belong to Food Pantry Management. A receipt may keep a source reference without transferring ownership of donor financial records.

```text
Receipt → Inventory Lot → Available Stock → Distribution Visit
                                           ↓
                           FEFO allocation and completion
                                           ↓
                           Inventory transaction ledger → Reports

Expired / spoiled stock → Explicit adjustment → Historical transaction
```

Distribution visits are created in `OPEN` state. Requested lines reserve no stock until completion. Completion locks the visit and eligible lots, validates the entire request, and consumes inventory in deterministic first-expiring-first-out order: expiration date, received date, then lot ID. The operation is all-or-nothing, so insufficient stock leaves every lot unchanged. A cancelled visit consumes nothing. Quantities use scale-three `BigDecimal` values and can never become negative.

Expiration is evaluated using the pantry location's IANA timezone. A lot whose expiration date is today remains usable; it becomes expired on the following local day. “Expiring soon” means today through seven local calendar days. Low-stock status is calculated separately for each pantry and item against the item's optional reorder threshold.

`ORG_ADMIN` and `FOOD_PANTRY_MANAGER` manage locations, catalog items, receipts, adjustments, households, and distributions. `PROGRAM_MANAGER` can read aggregate reports and inventory; `VIEWER` can read inventory. Household contact details, address, eligibility notes, and other private detail are absent from collection summaries and reports. Other organization roles receive no implicit pantry access, and all routes enforce organization membership.

Example placeholders (assumes an authenticated pantry manager):

```bash
API=http://localhost:8080/api/v1

# Create a pantry and catalog item
curl -X POST "$API/organizations/$ORGANIZATION_ID/food-pantries" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"name":"Hope Pantry","timezone":"America/New_York"}'
PANTRY_ID='<pantry id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/pantry-items" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"sku":"BEANS-15OZ","name":"Canned beans","category":"CANNED_GOODS","unitType":"CAN","trackExpiration":true,"reorderThreshold":10}'
ITEM_ID='<item id>'

# Receive a lot and inspect available inventory
curl -X POST "$API/organizations/$ORGANIZATION_ID/food-pantries/$PANTRY_ID/inventory-receipts" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"itemId\":\"$ITEM_ID\",\"lotNumber\":\"LOT-A\",\"quantity\":20,\"receivedDate\":\"2030-01-01\",\"expirationDate\":\"2030-06-01\",\"sourceType\":\"PURCHASE\"}"
LOT_ID='<lot id>'
curl "$API/organizations/$ORGANIZATION_ID/food-pantries/$PANTRY_ID/inventory/availability" -H "Authorization: Bearer $ACCESS_TOKEN"

# Create a household and complete an atomic distribution
curl -X POST "$API/organizations/$ORGANIZATION_ID/pantry-households" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"externalReference":"HH-100","displayName":"Citizen Household","householdSize":4}'
HOUSEHOLD_ID='<household id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/food-pantries/$PANTRY_ID/distribution-visits" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"householdId\":\"$HOUSEHOLD_ID\",\"visitDateTime\":\"2030-02-01T15:00:00Z\",\"householdSizeAtVisit\":4}"
VISIT_ID='<visit id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/pantry-distribution-visits/$VISIT_ID/items" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"itemId\":\"$ITEM_ID\",\"quantity\":8}"
curl -X POST "$API/organizations/$ORGANIZATION_ID/pantry-distribution-visits/$VISIT_ID/complete" -H "Authorization: Bearer $ACCESS_TOKEN"

# Record spoilage and retrieve aggregate reports
curl -X POST "$API/organizations/$ORGANIZATION_ID/pantry-inventory/$LOT_ID/adjust" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"adjustmentType":"SPOILAGE","quantity":1,"reason":"Container damaged"}'
curl "$API/organizations/$ORGANIZATION_ID/food-pantry-reports/inventory-summary?pantryId=$PANTRY_ID" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/food-pantry-reports/distributions?pantryId=$PANTRY_ID&from=2030-01-01&to=2030-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/food-pantry-reports/waste?pantryId=$PANTRY_ID&from=2030-01-01&to=2030-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Inventory filters are `itemId`, `expired`, `expiringSoon`, `available`, and `lotNumber`; safe sorts are `receivedDate`, `expirationDate`, `quantityRemaining`, and `createdAt`. Household filters are `active`, `externalReference`, and `displayName`; distribution filters are `status`, `householdId`, `from`, and `to`. Collection page size is capped at 100. OpenAPI documents the pantry routes and shared Bearer JWT scheme.

## Board Management

Board Management preserves the people, terms, decisions, attendance, and approved records that make up an organization's governance history.

```text
Board
  ├── Members ── Terms / Officer Assignments / Committees
  └── Meeting
        ├── Attendance ── Quorum
        ├── Agenda
        ├── Motion ── Voting ── Result ── Resolution
        └── Minutes
```

`BOARD_MANAGER` is an application authorization role for governance administration. `BOARD_MEMBER` provides authenticated access to permitted governance material, self-attendance, and self-voting. These roles are separate from officer designations such as Chair, Secretary, or Treasurer. `ORG_ADMIN` inherits Board Management authority through the existing organization-role policy. `PROGRAM_MANAGER` can read PII-free aggregate summary and voting reports; `VIEWER` and unrelated module roles have no Board access by default.

Members may link to a real CivicOps user or remain external without a fabricated login. A linked user must have an active membership in the same organization when linked. Member deactivation, term completion/resignation/removal, officer assignment endings, committee membership endings, resolution rescission, and all terminal governance workflows preserve history; Board APIs expose no destructive delete operations.

Meetings follow `DRAFT → PUBLISHED → IN_PROGRESS → COMPLETED`, with cancellation from Draft or Published. Terminal meetings cannot be reopened or edited. Only eligible active members with an active term covering the meeting's UTC calendar date count toward attendance and voting eligibility. Quorum is the configured positive integer; `PRESENT` and `REMOTE` attendance count toward it. Board-member self-attendance is supported, while the full attendance ledger remains manager-only.

Motions follow `PROPOSED → SECONDED → VOTING → PASSED/FAILED`; a proposed motion may be withdrawn and a seconded motion may be tabled. Closing requires quorum and uses CivicOps' initial simple-majority baseline: `YES > NO` passes, while abstentions count as participation but not as Yes or No. This baseline is not a substitute for an organization's bylaws. Each linked member casts only their own immutable vote. A pessimistic motion lock plus a database unique constraint prevents duplicate concurrent votes and protects terminal results.

Minutes follow `DRAFT → SUBMITTED → APPROVED`. Approved minutes are immutable in the normal workflow and readable by Board Members; drafts and individual vote records are manager-only. General member lists omit email, phone, and notes. Aggregate reports omit individual vote choices and member contact data.

Example governance workflow (replace placeholders and use a future meeting date):

```bash
API=http://localhost:8080/api/v1

# Create an external member and an active term
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-members" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"firstName":"Avery","lastName":"Citizen","email":"avery@example.org","joinedDate":"2030-01-01"}'
BOARD_MEMBER_ID='<board member id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-members/$BOARD_MEMBER_ID/terms" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"termStart":"2030-01-01","termEnd":"2032-12-31"}'

# Create and publish a meeting
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-meetings" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"title":"Annual Governance Meeting","meetingType":"ANNUAL","startDateTime":"2030-06-01T18:00:00Z","endDateTime":"2030-06-01T20:00:00Z","location":"Board Room","quorumRequired":3}'
MEETING_ID='<meeting id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-meetings/$MEETING_ID/publish" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-meetings/$MEETING_ID/attendance" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"boardMemberId\":\"$BOARD_MEMBER_ID\",\"attendanceStatus\":\"PRESENT\"}"
curl "$API/organizations/$ORGANIZATION_ID/board-meetings/$MEETING_ID/quorum" -H "Authorization: Bearer $ACCESS_TOKEN"

# Add an agenda item, begin the meeting, and move a motion
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-meetings/$MEETING_ID/agenda-items" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"sequenceNumber":1,"title":"Annual budget","itemType":"MOTION","estimatedMinutes":20}'
AGENDA_ITEM_ID='<agenda item id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-meetings/$MEETING_ID/start" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-meetings/$MEETING_ID/motions" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"agendaItemId\":\"$AGENDA_ITEM_ID\",\"motionText\":\"Adopt the annual budget\",\"movedByBoardMemberId\":\"$BOARD_MEMBER_ID\"}"
MOTION_ID='<motion id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-motions/$MOTION_ID/second" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"secondedByBoardMemberId\":\"$SECOND_BOARD_MEMBER_ID\"}"
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-motions/$MOTION_ID/open-voting" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-motions/$MOTION_ID/votes/me" -H "Authorization: Bearer $BOARD_MEMBER_TOKEN" -H 'Content-Type: application/json' -d '{"choice":"YES"}'
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-motions/$MOTION_ID/close-voting" -H "Authorization: Bearer $ACCESS_TOKEN"

# Preserve the adopted decision and approve minutes
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-motions/$MOTION_ID/resolution" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"resolutionNumber":"2030-01","title":"Annual Budget","text":"The annual budget is adopted.","adoptedDate":"2030-06-01"}'
curl -X PUT "$API/organizations/$ORGANIZATION_ID/board-meetings/$MEETING_ID/minutes" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"content":"Approved governance record."}'
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-meetings/$MEETING_ID/minutes/submit" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-meetings/$MEETING_ID/minutes/approve" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/board-meetings/$MEETING_ID/complete" -H "Authorization: Bearer $ACCESS_TOKEN"

# Retrieve privacy-aware governance reporting
curl "$API/organizations/$ORGANIZATION_ID/board-reports/summary?from=2030-01-01&to=2030-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/board-reports/attendance?from=2030-01-01&to=2030-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/board-reports/voting?from=2030-01-01&to=2030-12-31" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Board-member filters are `active`, `officerRole`, and `termStatus`; safe sorts are `lastName`, `joinedDate`, and `createdAt`. Committee filtering supports `active`; safe sorts are `name` and `createdAt`. Meeting filters are `status`, `meetingType`, `from`, and `to`; safe sorts are `startDateTime`, `status`, and `createdAt`. Motions filter by meeting and status; safe sorts are `openedAt`, `status`, and `createdAt`. Resolutions filter by `status`, `adoptedFrom`, and `adoptedTo`; safe sorts are `adoptedDate`, `resolutionNumber`, and `createdAt`. Paginated endpoints use the shared maximum page size of 100. OpenAPI exposes all Board routes under tagged operations and the shared Bearer JWT security scheme.

## Grant Reporting Assistant

The Grant Reporting Assistant is a deterministic reporting and drafting layer—not a generic chatbot. It composes existing, authoritative module reporting services into a reproducible grant report:

```text
Grant → Inclusive Reporting Period → Template
                                  ↓
                         Evidence Providers
       ┌──────────────┬────────────┼────────────┬───────────┐
  Grant Financials  Volunteers  Events/Donations  Cases  Scholarships/Pantry
                                  ↓
                       Frozen Evidence Snapshot
                                  ↓
                 Deterministic Narrative Generation
                                  ↓
                   Human Edit → Approval → Finalize
                                  ↓
                         JSON / Markdown Export
```

System evidence uses a controlled metric catalog and records its module, source type/reference, reporting period, capture time, unit, and value state. Grant financial evidence reuses Grant Reporting calculations. Events, donations, and volunteer hours are included only through grant-linked relationships. Case, scholarship, and food-pantry evidence is available only when explicitly selected and uses aggregate reporting APIs—never client, household, applicant, donor, or attendee identity data.

`VERIFIED` zero means the reporting service authoritatively returned zero. `MISSING` means CivicOps lacks a relevant linkage or value and is never silently converted to zero. `NOT_APPLICABLE` remains distinct. Authorized managers may add evidence with `MANUAL` provenance and a required citation; exactly one numeric, monetary, or text value is allowed.

Generation freezes evidence and preserves it even if operational data later changes. Regeneration is an explicit pre-finalization action that replaces system snapshots and generated content while preserving manual evidence. Generated content, human-edited content, and approved final content are stored separately. A report moves through `DRAFT → GENERATED → UNDER_REVIEW → FINALIZED`; all required sections must be approved before finalization. Finalized reports, sections, evidence, period, template, and grant relationship are immutable. Exports read only frozen report data and never query live operational metrics.

The default `GrantNarrativeProvider` is offline and deterministic. It states exact supplied values, inclusive dates, missing-data warnings, and `SYSTEM` versus `MANUAL` provenance without inference or embellishment. Provider selection uses `CIVICOPS_GRANT_REPORT_NARRATIVE_PROVIDER` (default `deterministic`); the interface and conditional configuration are the extension seam for a future AI-backed provider. No external provider, network access, API key, or AI SDK is required in Phase 13, and structured evidence always remains the source of truth.

`GRANT_MANAGER` and `ORG_ADMIN` can manage templates, drafts, evidence, review, finalization, and export. `PROGRAM_MANAGER` has read-only access to finalized reports and their aggregate evidence. Other roles have no implicit access, and all relationships are organization scoped.

Example workflow (replace placeholders):

```bash
API=http://localhost:8080/api/v1

# Create a reusable template and ordered sections
curl -X POST "$API/organizations/$ORGANIZATION_ID/grant-report-templates" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"name":"Foundation Progress Report"}'
TEMPLATE_ID='<template id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/grant-report-templates/$TEMPLATE_ID/sections" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"sectionKey":"executive-summary","title":"Executive Summary","sequenceNumber":1,"sectionType":"NARRATIVE","required":true,"maxLength":5000}'
curl -X POST "$API/organizations/$ORGANIZATION_ID/grant-report-templates/$TEMPLATE_ID/sections" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"sectionKey":"financials","title":"Financials","sequenceNumber":2,"sectionType":"FINANCIAL","required":true}'

# Create a report. Dates are inclusive; sensitive organization aggregates require explicit selection.
curl -X POST "$API/organizations/$ORGANIZATION_ID/grants/$GRANT_ID/reports" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d "{\"templateId\":\"$TEMPLATE_ID\",\"reportingPeriodStart\":\"2026-01-01\",\"reportingPeriodEnd\":\"2026-06-30\",\"selectedSources\":[\"GRANT\",\"VOLUNTEERS\",\"EVENTS\",\"DONATIONS\"]}"
REPORT_ID='<report id>'
curl -X POST "$API/organizations/$ORGANIZATION_ID/grant-reports/$REPORT_ID/generate" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/grant-reports/$REPORT_ID/evidence" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/grant-reports/$REPORT_ID/missing-data" -H "Authorization: Bearer $ACCESS_TOKEN"

# Cite manual evidence, review a section, approve, and finalize
curl -X POST "$API/organizations/$ORGANIZATION_ID/grant-reports/$REPORT_ID/manual-evidence" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"label":"Community partners","numericValue":4,"unit":"partners","sourceReference":"Signed partner roster dated 2026-06-30"}'
SECTION_ID='<section id>'
curl -X PATCH "$API/organizations/$ORGANIZATION_ID/grant-report-sections/$SECTION_ID" -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' -d '{"editedContent":"Human-reviewed factual narrative."}'
curl -X POST "$API/organizations/$ORGANIZATION_ID/grant-report-sections/$SECTION_ID/approve" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -X POST "$API/organizations/$ORGANIZATION_ID/grant-reports/$REPORT_ID/finalize" -H "Authorization: Bearer $ACCESS_TOKEN"

# Stable exports use the frozen evidence and reviewed sections
curl "$API/organizations/$ORGANIZATION_ID/grant-reports/$REPORT_ID/export?format=json" -H "Authorization: Bearer $ACCESS_TOKEN"
curl "$API/organizations/$ORGANIZATION_ID/grant-reports/$REPORT_ID/export?format=markdown" -H "Authorization: Bearer $ACCESS_TOKEN"
```

Template list sorts are `name` and `createdAt`; report list sorts are `reportingPeriodStart`, `reportingPeriodEnd`, `status`, and `createdAt`. Page size is capped by the shared safe-pageable policy. OpenAPI exposes template, section, report, evidence, manual evidence, generation/regeneration, approval/finalization, and export operations under the shared Bearer JWT scheme.
