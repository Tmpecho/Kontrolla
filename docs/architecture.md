# Architecture

Kontrolla is a multi-tenant web application for internal control in Norwegian food and alcohol service establishments. The system is split into two product areas, `IK-mat` and `IK-alkohol`, which share common infrastructure for authentication, tenancy, checklists, reporting and more, while keeping domain workflows separate.

## Frontend

The frontend is a Vue 3 and TypeScript application under `frontend/`. It is responsible for routing, authenticated workspace navigation, and the user-facing flows for dashboards and compliance modules. Product areas are exposed as separate route spaces, while shared UI and API integration live in common modules.

## Backend

The backend is a Spring Boot application under `backend/`. It follows a feature-oriented structure where each domain owns its API layer, application services, domain model, and persistence code. Shared concerns such as security, exception handling, and base persistence utilities live in common packages.

Core backend modules currently include:

- `iam` for authentication, refresh tokens, and users
- `organizations` for tenant membership and access control
- `establishments` for establishment-level scoping within an organization
- `checklists` for versioned checklist definitions, recurring schedules, checklist runs, assignments, and audit-safe run snapshots

## Data and tenancy

Persistence is managed with JPA and Flyway. Organizations are the top-level tenant boundary, and establishments scope operational data below that. Checklist execution data is designed to be historically stable: definitions can version forward, while runs snapshot the exact items completed at that point in time.
