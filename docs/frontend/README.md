# Frontend Documentation

This folder contains the documentation required for the `frontend` module.

Files in this folder:

- `README.md`: quick start for developers
- `system-documentation.md`: architecture, module structure, and diagrams
- `test-data.md`: development credentials, example context, and dependencies

## Purpose

The frontend is a Vue 3 + TypeScript single-page application in [`frontend/`](../../frontend/). It provides:

- public marketing pages
- authentication and invitation acceptance
- the authenticated workspace for `IK-Mat` and `IK-Alkohol`
- API integration with the Spring Boot backend

## Prerequisites

- Node.js `^20.19.0` or `>=22.12.0`
- npm
- backend running locally on `http://localhost:8080`
- for full local stack: Docker and Docker Compose

## Dependency On Other Projects

The frontend depends on the backend module in this repository:

- API base URL is configured with `VITE_API_BASE_URL`
- login, CSRF, workspace bootstrap, and all business data come from backend endpoints
- organization and establishment context are resolved by the backend and returned in the login session

Without the backend, only static/public frontend routes can render meaningfully.

## Start The Frontend

From the repository root:

```sh
docker compose up
```

Or run only the frontend manually:

```sh
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`.

## Environment Variables

Defined in [`frontend/src/shared/config/env.ts`](../../frontend/src/shared/config/env.ts):

- `VITE_API_BASE_URL`: backend base URL, typically `http://localhost:8080`
- `VITE_DEFAULT_ORGANIZATION_ID`: optional dev fallback organization context
- `VITE_DEFAULT_ESTABLISHMENT_ID`: optional dev fallback establishment context
- `VITE_SHOW_DEV_LOGIN_HINT`: shows dev login hint in development when set to `true`

## Main Commands

```sh
cd frontend
npm run dev
npm run build
npm run test:unit
npm run test:e2e
npm run lint
```

## Testing Notes

- Unit tests use Vitest.
- End-to-end tests use Playwright.
- Most app flows require the backend to be running and seeded with development users.

See [`test-data.md`](./test-data.md) and [`system-documentation.md`](./system-documentation.md) for more detail.
