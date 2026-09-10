# CivicOps Security Model

## Authentication

CivicOps signs HS256 JWT access tokens with a deployment-supplied secret of at least 32 bytes. Tokens validate issuer (`civicops-api`), signature, and standard time claims. Access tokens contain the user ID rather than cached organization roles. Passwords are BCrypt hashes and are never returned.

Refresh tokens are opaque random values; only SHA-256 hashes are stored. Successful refresh rotates and revokes the presented token. Logout accepts a refresh token so it can revoke credentials even when an access token has expired. Reuse, expiration, revocation, inactive users, and invalid credentials receive generic authentication errors.

## Authorization and tenant isolation

All application routes require authentication except registration, login, refresh, logout, health, and API documentation. Organization authorization requires a live active membership. Module access services enforce role policy, and organization-scoped repository methods plus composite database foreign keys prevent cross-tenant associations. `SYSTEM_ADMIN` is reserved; a platform administration surface is not part of 0.1.0.

## Privacy

Controllers return DTOs rather than entities. Collection and aggregate reporting contracts omit case notes/client PII, donor private data, attendee contacts, borrower details, household PII, applicant details/reviewer comments, private board material, and grant-report source PII. Sensitive details require dedicated permissions. Grant Reporting consumes aggregates and frozen provenance only.

## Secrets and operations

Never commit `.env`, database credentials, signing secrets, production logs, or tokens. Supply `CIVICOPS_JWT_SECRET`, `CIVICOPS_DATABASE_PASSWORD`, and deployment-specific datasource values through the runtime environment or a secret manager. Only health and info Actuator endpoints are exposed. TLS termination, secret rotation, backups, monitoring, rate limiting, and external identity federation are deployment responsibilities.

This document describes application controls; it does not assert any regulatory or compliance certification.
