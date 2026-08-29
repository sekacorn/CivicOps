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

## Current phase: Authentication and authorization

The repository provides a Java 21 / Spring Boot backend, PostgreSQL configuration, Flyway-managed Core schema, versioned REST APIs, OpenAPI UI, an Angular application shell, Docker Compose, Kubernetes starter manifests, and CI. Core implements organizations, users, memberships, role rules, BCrypt password hashing, JWT access tokens, rotating refresh tokens, and live organization authorization lookups.

Authentication follows this path:

```
Login -> JWT access token -> CivicOpsPrincipal -> organization membership -> role authorization -> service
```

Access tokens contain only the user ID and standard issuer/time/token claims. User activation and organization authorization are resolved from the database, so deactivation and membership changes take effect without waiting for an access token to expire.

## Technology stack

Java 21, Spring Boot, Spring Data JPA, Spring Security, Bean Validation, PostgreSQL, Flyway, Maven, Angular, TypeScript, Docker, and Kubernetes.

## Local development

Prerequisites: Java 21, Maven 3.9+, Node 22+, Docker Desktop.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

The web shell is available at `http://localhost:4200`; the API health endpoint is `http://localhost:8080/api/v1/health`. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

To run the backend against a local PostgreSQL instance, export the `CIVICOPS_DATABASE_*` variables and `CIVICOPS_JWT_SECRET`, then run `mvn spring-boot:run`.

Required and optional authentication variables:

- `CIVICOPS_JWT_SECRET`: required, unique, at least 32 bytes
- `CIVICOPS_ACCESS_TOKEN_TTL`: optional ISO-8601 duration; defaults to `PT15M`
- `CIVICOPS_REFRESH_TOKEN_TTL`: optional ISO-8601 duration; defaults to `P30D`

The application fails at startup when the signing secret is absent or too short. `.env.example` contains placeholders only and `.env` is ignored.

## Project structure

```
src/main/java/org/civicops
  core/           # organizations, users, memberships, roles, health, and security seam
  shared/         # narrow technical primitives
  volunteer/      # planned domain module
  grant/ donation/ event/ # planned funding and activity modules
civicops-web/     # Angular feature application
k8s/              # deployment starters; use managed PostgreSQL in production
```

## Security

No real credentials are committed. Passwords are BCrypt-hashed and never returned. Login failures use one generic response for unknown users, inactive users, and wrong passwords.

`POST /api/v1/users`, login, refresh, logout, health, and API documentation are public. User-profile reads are self-only. Organization reads require an active membership. Membership administration requires `ORG_ADMIN`; authenticated cross-organization requests receive `403 Forbidden`. Creating an organization also creates an `ORG_ADMIN` membership for its creator. User lookup by arbitrary email is no longer exposed.

Refresh tokens are opaque random values. Only their SHA-256 hashes are stored. Every successful refresh revokes the presented token and creates a replacement; logout revokes the supplied refresh token. Expired, revoked, and reused tokens return a generic authentication error.

`SYSTEM_ADMIN` remains a platform role and is not stored in organization memberships. Platform-administration behavior is not implemented yet.

Swagger UI defines Bearer JWT authorization. Obtain an access token from `/api/v1/auth/login`, select **Authorize**, and paste the token.

## Roadmap

1. Volunteer workflows: opportunities, shifts, assignments, hours approval
2. Platform administration and account lifecycle workflows
3. Grants, expenses, utilization, and reporting
4. Donations and donors
5. Events, registration, waitlist, volunteer integration
6. Dashboards, audit events, notifications, and the remaining civic operations modules

## Contributing and license

Contributions are welcome. Add a license before the first public release; no license is declared yet.
