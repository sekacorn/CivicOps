# CivicOps frontend architecture

## Scope

The Angular application is a strict TypeScript, standalone-component client for CivicOps `/api/v1`. Phase 15 established the production foundation and Grants. Phase 16.1 implements Grant Reporting, Volunteers, Events, and Donations. Phase 16.2 adds Cases, Equipment, and Facilities without duplicating backend authority. Phase 16.3 completes the remaining frontend domains: Scholarships, Food Pantry, and Board Management.

## Structure

```text
civicops-web/src/app
├── core
│   ├── auth          session and token storage
│   ├── guards        authentication, organization, and grant-management UX guards
│   ├── http          API base URL, authentication interceptor, safe error mapping
│   ├── layout        responsive authenticated shell
│   ├── models        exact backend-facing contracts
│   └── organization  selected organization and membership context
├── shared
│   └── components    async states, errors, pagination, status, and confirmation
└── features
    ├── dashboard     backend-derived grant portfolio metrics
    ├── grants        list, form, detail, lifecycle, expenses, and financial summary
    ├── grant-reporting templates, frozen evidence, review, finalization, exports
    ├── volunteers    profiles, opportunities, shifts, assignment, hours, reports
    ├── events        list, detail, lifecycle, registration, attendance, reports
    ├── donations     donors, campaigns, immutable gifts, reversals, reports
    ├── cases         clients, assigned cases, notes, tasks, services, reports
    ├── equipment     inventory, checkout, maintenance, lifecycle, reports
    ├── facilities    spaces, hours, blackouts, reservations, availability, reports
    ├── scholarships  programs, applicants, reviewer queues, awards, reports
    ├── food-pantry   locations, catalog, lots, households, distributions, reports
    └── board         members, meetings, attendance, motions, minutes, reports
```

## Routing and guards

`/login` is public. The application shell protects `/dashboard` and organization feature routes with `authGuard`. `organizationGuard` accepts an organization route only when both the active organization and active current-user membership are present. `grantManageGuard` hides grant-management pages from read-only roles. Organization routes are `/grants`, `/grant-reporting`, `/volunteers`, `/events`, `/donations`, `/cases`, `/equipment`, `/facilities`, `/scholarships`, `/food-pantry`, and `/board`, with resource detail children where the backend supports them. Case client detail is nested under `/cases/clients/:clientId`; reservation detail is nested under `/facilities/reservations/:reservationId`; Scholarship application/review/award details, Pantry household/inventory/distribution details, and Board meeting/member/motion details are lazy-loaded children. These guards improve navigation; Spring Security remains authoritative.

## Authentication and refresh

`AuthService` implements login, `/auth/me` bootstrap, refresh, backend logout, and authenticated state. `TokenStorageService` is the only code that reads or writes browser token storage. This encapsulates the current backend contract in which both tokens are returned to JavaScript.

The functional HTTP interceptor attaches an access token only when a URL begins with the configured CivicOps API base path. Login, refresh, and logout are excluded. A module-level shared refresh observable provides single-flight behavior: the first eligible `401` starts refresh, concurrent failures wait for that result, and all retry with the replacement access token. A failed refresh clears tokens and organization state and redirects to login without recursively refreshing the refresh request.

## Organization context and authorization UX

After authentication, `OrganizationContextService` loads `/organizations` and `/auth/me/memberships`. It validates the persisted organization against both responses, selects the first valid organization when necessary, exposes the selected membership and role as signals, and clears stale selections. The shell derives visible navigation from the backend role vocabulary. `ORG_ADMIN` receives broad navigation; narrower roles see only relevant areas. Hidden links never replace backend authorization.

## API integration

The project uses thin handwritten Angular services because the current feature scope is small and no generated client existed. Services share a configurable `API_BASE_URL`; development uses the Angular proxy and production uses Nginx same-origin proxying. This avoids a large generated DTO surface while keeping transport logic out of components.

`PageResponse<T>` exactly represents `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, and `last`. `ApiError` represents `timestamp`, `status`, `code`, `message`, `path`, and `fieldErrors`. Central error handling preserves safe backend messages, provides safe fallbacks for 400/401/403/404/409/422/500, and maps field errors onto reactive form controls.

## Shared UI and feature pattern

Small standalone components provide loading, empty, error, forbidden, status, and pagination presentation. Tables remain feature-owned so the project does not acquire an overly dynamic table framework. Feature components call typed services, store local async state in signals, use reactive forms for mutations, and reload authoritative backend summaries after changes.

Grant amounts are formatted in the browser only for display. The UI never calculates authoritative balances or utilization: grant financial and portfolio summaries come from backend reporting endpoints.

The same rule applies across Phase 16.1. Event capacity, waitlist promotion, attendance rates, volunteer capacity/hours, campaign totals, and donation totals are read back from backend DTOs. `null` Event capacity displays as `Unlimited`. Grant-report dates stay date-only strings; Event values remain timestamps. Currency pipes format numbers but never become a financial calculation source.

Grant Reporting keeps `VERIFIED`, `MISSING`, `NOT_APPLICABLE`, and manual provenance distinct. Missing evidence is never rendered as zero. Finalized reports hide every mutation while preserving JSON and Markdown exports. Donation transactions have no edit/delete UI: reversal is an explicit confirmed operation that retains history. Donor and Volunteer tables consume summary DTOs; contact details are loaded only from authorized detail endpoints.

Cross-module links are rendered only when backend DTOs expose identifiers: Grant details link to reports, Events link to Grants, Events display linked Donation Campaigns, and Event-backed Volunteer Opportunities retain their backend relationship. The client never manufactures an association.

Phase 16.2 follows the same contract-first pattern. Case collection rows consume privacy-safe summaries; Client PII, notes, and service history are requested only from authorized detail endpoints. `PROGRAM_MANAGER` receives aggregate Case summary and service reports without Client records, while `CASE_WORKER` remains constrained by backend assignment checks. Case assignment requires an explicit user ID because the backend does not expose an eligible-worker directory to non-administrators; the service validates organization membership and role. Notes and service records are append-only, tasks use explicit complete/cancel actions, and terminal Case transitions require confirmation.

Equipment collection and report DTOs omit borrower detail. Checkout and maintenance histories are requested only for `EQUIPMENT_MANAGER` or `ORG_ADMIN`. Checkout, check-in, mark-lost, retire, start-maintenance, and complete-maintenance remain explicit server-authorized commands. The client refreshes authoritative asset state after every command and displays 409/422 business errors instead of predicting availability from stale state.

Facilities preserve each Facility's IANA timezone as scheduling authority. `datetime-local` reservation and blackout values pass through explicit timezone conversion helpers before API submission; stored instants are formatted in the Facility zone for display. Availability is checked before reservation submission as useful feedback, while operating hours, blackouts, overlap, and capacity remain backend decisions. General reservation summaries contain no requester contact fields. Request-capable users load their own navigable records from `/facility-reservations/me`; only `FACILITY_MANAGER` and `ORG_ADMIN` receive organization-wide private detail and approval actions. Reservation-to-Event links use only backend-supplied IDs and never mutate Event lifecycle.

Phase 16.3 keeps that same contract-first stance. Scholarship screens expose program lists, status and academic-year filters, program lifecycle commands, applicant creation, application creation/detail, reviewer assignment, reviewer work queues, document metadata, selection actions, awards, and backend reports. Applicant contact fields, dates of birth, addresses, household income, private notes, and reviewer comments are kept out of collection and reviewer-safe views. `SCHOLARSHIP_MANAGER` and `ORG_ADMIN` receive management workflows; `SCHOLARSHIP_REVIEWER` receives only assigned review work; `PROGRAM_MANAGER` and `VIEWER` receive only safe summaries and reporting allowed by the backend. Scholarship deadline values are treated as date-only strings; the frontend does not convert them into browser-local instants.

Food Pantry screens expose pantry locations, item catalog, inventory receipts, lot detail and ledger transactions, availability, adjustments, household records, distribution visits, completion/cancellation, and pantry reports. Inventory balances, expiration, low-stock, and FEFO allocation remain backend-authoritative. The UI distinguishes requested distribution items from lot-specific transaction history when the backend exposes it, and it refreshes inventory and reporting after mutations instead of deducting stock optimistically. Household contact detail is requested only on manager detail pages; read-only inventory and reporting pages do not initialize private household detail endpoints. Pantry expiration dates remain date-only values interpreted by the pantry's configured IANA timezone.

Board screens expose member directories, member detail, committees, meetings, lifecycle commands, self-attendance for linked board members, quorum display, agenda items, motions, self-voting, minutes, resolutions, and governance reporting. `BOARD_MANAGER` and `ORG_ADMIN` receive governance administration; `BOARD_MEMBER` receives governance material, self-attendance, and own-vote actions; `PROGRAM_MANAGER` receives aggregate reporting where authorized. Member collection views omit contact detail, Board Member pages do not request manager-only vote ledgers or draft-only controls, and all vote eligibility, duplicate-vote prevention, quorum, and result calculations remain backend decisions.

## Accessibility and responsive behavior

Phase 16.1, 16.2, and 16.3 use semantic headings, labelled reactive form controls, table headers, keyboard-native buttons and links, text-bearing status badges, visible focus styling, and an `alertdialog` with `aria-modal`, `aria-labelledby`, and `aria-describedby`. Wide tables scroll inside their containers. Feature grids collapse at tablet and mobile breakpoints, including the target 820px and 390px layouts, while desktop layouts target 1440px.

## Development baseline

`.nvmrc` pins Node 22 LTS. Angular 19 is intentionally retained; audit findings that require an Angular major upgrade are deferred to release hardening rather than addressed with `npm audit fix --force`.

## Testing

Vitest runs Angular 19 tests through the Analog Angular Vite adapter and jsdom. Tests cover authentication and refresh, organization context/guards, standard API errors, API contracts for all Phase 16.1 through 16.3 modules, role-aware actions, privacy-safe donor/volunteer/Client/borrower/reservation/applicant/household/member views, Event waitlist and unlimited-capacity rendering, Facility timezone conversion, backend-authoritative reporting, Grant evidence semantics/finalized immutability, lazy route registration, and accessible confirmation behavior.
