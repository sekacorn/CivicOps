# CivicOps API Modules

All production application routes are versioned beneath `/api/v1`. OpenAPI at `/api-docs` is the authoritative field-level contract; this document maps module responsibilities.

| Module | Purpose | Primary roles | Main routes | Lifecycle / relationships |
|---|---|---|---|---|
| Auth | Registration, login, token rotation and logout | Public/current user | `/users`, `/auth` | JWT plus rotating refresh tokens |
| Core | Organizations, users and memberships | ORG_ADMIN, member | `/organizations`, `/users`, `/organizations/{id}/memberships` | Organization is the tenant boundary |
| Volunteers | Profiles, opportunities, shifts, assignments and hours | VOLUNTEER_COORDINATOR, VOLUNTEER | `/volunteers`, `/volunteer-opportunities`, `/volunteer-reports` | Opportunities may link to events/grants; only approved hours report |
| Grants | Awards, expenses, lifecycle and portfolio reporting | GRANT_MANAGER | `/grants`, `/grant-expenses`, `/grant-reports` | Financial values derive from authoritative expenses |
| Donations | Donors, gifts, reversals and campaigns | DONATION_MANAGER | `/donors`, `/donations`, `/donation-campaigns`, `/donation-reports` | Campaigns may link to events; gifts are immutable/reversible |
| Events | Lifecycle, registration, waitlist and attendance | EVENT_COORDINATOR, VOLUNTEER | `/events`, `/event-registrations`, `/event-reports` | Links grants, campaigns and volunteer opportunities |
| Cases | Clients, case files, notes, tasks, services and aggregates | CASE_MANAGER, CASE_WORKER | `/clients`, `/cases`, `/case-reports` | Detail is assignment/privacy controlled |
| Equipment | Assets, checkouts and maintenance | EQUIPMENT_MANAGER | `/equipment`, `/equipment-checkouts`, `/equipment-maintenance`, `/equipment-reports` | Serialized checkout and return workflows |
| Facilities | Facilities, spaces, blackouts and reservations | FACILITY_MANAGER | `/facilities`, `/facility-spaces`, `/facility-reservations`, `/facility-reports` | Time-overlap conflicts are locked |
| Scholarships | Programs, applications, review, selection and awards | SCHOLARSHIP_MANAGER, SCHOLARSHIP_REVIEWER | `/scholarship-*` | Applicant/reviewer privacy and selection limits |
| Food Pantry | Locations, lots, households, visits and inventory | PANTRY_MANAGER | `/food-pantries`, `/pantry-*`, `/food-pantry-reports` | FIFO inventory depletion and aggregate household reporting |
| Board | Members, meetings, agenda, motions, votes and minutes | BOARD_MANAGER, BOARD_MEMBER | `/board-*` | Governance lifecycle and private material controls |
| Grant Reporting | Templates, frozen evidence, review, finalization and exports | GRANT_MANAGER, ORG_ADMIN | `/grant-report-*`, `/grants/{id}/reports` | Composes privacy-safe operational evidence without mutating sources |

Paged collections use `PageResponse<T>`. Query parameters use zero-based `page`, bounded `size` (maximum 100), and endpoint-specific allowlisted `sort` fields. Bearer JWT authentication applies unless a route is explicitly public.
