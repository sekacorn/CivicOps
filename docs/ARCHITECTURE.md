# CivicOps Backend Architecture

## Modular monolith

CivicOps 0.1.0 is one Spring Boot deployment with explicit packages for Core, Volunteers, Grants, Donations, Events, Cases, Equipment, Facilities, Scholarships, Food Pantry, Board, and Grant Reporting. A modular monolith keeps transactions, deployment, and cross-module civic workflows straightforward while package boundaries prevent the application from becoming unrelated CRUD systems. Domain modules depend on Core/shared foundations; Grant Reporting composes public reporting capabilities without reverse dependencies.

## Multi-tenancy and security

Organizations are the tenant boundary. Organization-owned rows carry `organization_id`; repositories and services use organization-scoped lookups, and composite foreign keys prevent cross-organization associations. JWT access tokens identify a user, while active-user and active-membership state is resolved from PostgreSQL. Access services centralize role checks and inherited organization permissions.

## API and DTO boundaries

Controllers are versioned beneath `/api/v1` and expose DTOs, never JPA entities. Services map lazy relationships while a transaction is active; `spring.jpa.open-in-view=false` prevents accidental web-layer loading. Paged results use the stable `PageResponse<T>` envelope. API errors use a stable safe contract and never return persistence messages or stack traces.

## Persistence and transactions

PostgreSQL is authoritative. Flyway owns schema evolution and Hibernate runs with `ddl-auto=validate`. Write workflows use service transactions. Read mappings that touch lazy relations execute in read-only transactions. Monetary values use `BigDecimal` and the shared `Money` utility; pantry quantities use three-decimal precision.

## Concurrency

Optimistic versions protect ordinary aggregate updates. Pessimistic row locks serialize invariants that cannot tolerate races: grant spending, event capacity/waitlist promotion, equipment checkout, facility booking, scholarship selection, pantry depletion, board voting, donation references, and grant-report generation/finalization. PostgreSQL integration tests exercise these paths with real concurrent transactions.

## Reporting composition

Operational modules own their calculations and privacy-safe DTOs. Grant Reporting collects only linked, explicitly selected, or unambiguously scoped aggregates through evidence providers. It freezes values and provenance, generates deterministic narratives, keeps generated/human/final content distinct, and exports from immutable snapshots rather than live data.
