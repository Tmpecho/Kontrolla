# Frontend Test Data

## Development Login Credentials

When the backend runs with the `dev` profile, these users are bootstrapped automatically:

- `demo@example.com` / `password123`
- `admin@example.com` / `password123`
- `emma.larsen@example.com` / `password123`
- `noah.berg@example.com` / `password123`

These values come from [`backend/src/main/resources/application-dev.properties`](../../backend/src/main/resources/application-dev.properties).

## Backend Connection Data

Frontend local development typically uses:

- frontend URL: `http://localhost:5173`
- backend URL: `http://localhost:8080`
- environment variable: `VITE_API_BASE_URL=http://localhost:8080`

## Organization And Establishment Context

Most workspace pages depend on:

- `organizationId`
- `establishmentId`

How to get them:

- log in and inspect the `appContext` returned by `/api/v1/auth/login`
- or use backend responses from organization and establishment endpoints

Optional development overrides:

- `VITE_DEFAULT_ORGANIZATION_ID`
- `VITE_DEFAULT_ESTABLISHMENT_ID`

## Test Scenarios

Recommended manual test scenarios:

- log in as `demo@example.com` and verify workspace bootstrap
- upload a document and verify document list, download, and acknowledgement
- create a deviation and update its status
- create a temperature unit and add a log entry
- update serving hours in `IK-Alkohol`
- review notification count after actions that trigger notifications

## Dependency Reminder

Frontend test data is only usable when the backend module and its database are running.
