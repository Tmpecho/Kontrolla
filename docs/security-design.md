# Security design

Kontrolla is a multi-tenant web application with a Vue 3 frontend in `frontend/` and a Spring Boot API in `backend/`. The primary security goals are:

- preserve strict tenant isolation between organizations
- prevent unauthorized access to establishment and compliance data
- protect authentication state and session renewal
- keep compliance records trustworthy and attributable
- treat OWASP and accessibility as release criteria

## Current security model

Authentication is handled by the backend `iam` module. Users sign in with credentials, receive a short-lived bearer access token, and receive a refresh token in an `HttpOnly` cookie scoped to `/api/v1/auth`. Access tokens are sent by the frontend in the `Authorization` header. Refresh tokens are stored server-side only as SHA-256 hashes.

Authorization is enforced server-side. The main tenancy boundary is `Organization`, with `Establishment` as an operational sub-scope. Backend services enforce access through membership and role checks before loading or mutating tenant data. Client-side route guards improve UX, but they are not relied on for security.

## Tenant isolation

Tenant isolation currently depends on these patterns:

- organization membership checks in the organizations access service
- repository queries scoped by `organizationId` and, where needed, `establishmentId`
- role-based management rules for organization, establishment, checklist, and deviation operations
- integration tests that verify cross-tenant access is rejected

Any new endpoint should follow the same model:

1. resolve the authenticated user
2. verify organization membership and required role
3. query by tenant-scoped identifiers
4. add automated tests for same-tenant and cross-tenant access

## Frontend security posture

The frontend keeps the access token in memory instead of persistent browser storage. This reduces exposure from local storage theft. The frontend uses cookie-based refresh with `credentials: include`, but the security boundary remains the backend API.

Frontend code should continue to avoid unsafe HTML rendering, preserve semantic HTML where possible, and include accessible interaction patterns for dialogs, forms, and keyboard use.

## Accessibility baseline

Universal design should be treated as part of the security and trust model for the product. At minimum, interactive patterns should support:

- full keyboard operation
- visible focus indicators
- semantic controls instead of custom clickable containers
- programmatically associated form labels, hints, and errors
- understandable status and error feedback for assistive technology

## Operational note

Development defaults in the `dev` profile are intentionally convenient and must not be treated as production-safe. Production deployments should always supply environment-specific secrets, secure cookie settings, restricted origins, and release gates that include security and accessibility checks.
