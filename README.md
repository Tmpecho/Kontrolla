# Kontrolla
Kontrolla is a system for managing food and alcohol compliance for restaurants and bars in Norway.

The platform is divided into two main areas:

- `IK-Mat` for internal food-control routines
- `IK-Alkohol` for alcohol-control and licensing follow-up

Kontrolla is built around organizations and the establishments that belong to each organization. This makes it possible to manage compliance work centrally while still keeping daily operations tied to each individual restaurant, bar, or venue.

The main services in the product include checklists and operational routines for things like daily opening and closing tasks, temperature logging, cleaning routines, deviation handling, and follow-up of required documentation and compliance work across establishments.

## Technical Overview

Kontrolla consists of:

- a Spring Boot backend in [`backend/`](backend/)
- a Vue 3 frontend in [`frontend/`](frontend/)

## Getting Started

If you want to work on the project locally, start with the setup guide in [`docs/setup.md`](docs/setup.md).

## Docker Compose

Start the development stack:

```sh
docker compose up
```

The frontend will be available on `http://localhost:5173`.
The backend will be available on `http://localhost:8080`.

## Project Structure

- [`backend/`](backend/): Spring Boot API, database migrations, and backend tests
- [`frontend/`](frontend/): Vue app, shared UI code, and frontend tests
- [`docs/`](docs/): project documentation and setup notes

## GitHub Actions

The repository uses GitHub Actions for development-time CI:

- `CI` runs on pull requests and pushes to `main`.
- Backend changes run `./gradlew test`.
- Frontend changes run `npm ci`, `npm run lint:check`, `npm run build`, and `npm run test:unit:ci`.
- `E2E` is a separate Playwright workflow that runs on pushes to `main` and on manual dispatch.

Recommended branch protection is to require the `Status` job from the `CI` workflow before merging to `main`.

## Backend Profiles

The backend is split into explicit Spring profiles:

- `dev`: local development only, with MySQL defaults and bootstrap users
- `prod`: production configuration with environment-provided database and security settings
- `test`: H2-backed test configuration

The Docker Compose backend runs with the `dev` profile by default.
Startup now fails fast if dev bootstrap credentials or the insecure dev JWT secret are present outside `dev`, and `dev` cannot be combined with `prod`.
