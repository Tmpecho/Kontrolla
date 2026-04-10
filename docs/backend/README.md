# Backend Documentation

This folder contains the documentation required for the `backend` module.

Files in this folder:

- `README.md`: quick start for developers
- `api.md`: endpoint documentation, payload attributes, Swagger location, and security notes
- `system-documentation.md`: architecture, module structure, and diagrams
- `test-data.md`: development credentials, database credentials, and test profile notes

## Purpose

The backend is a Spring Boot application in [`backend/`](../../backend/) that exposes the REST API used by the frontend and owns persistence, security, scheduling, tenancy, and domain logic.

## Prerequisites

- Java 25
- Docker and Docker Compose for the easiest local stack
- MySQL 8.x for local development outside test profile

## Dependency On Other Projects

The backend does not require the frontend to start, but the full product requires the frontend to consume the backend APIs. The backend depends on:

- MySQL in `dev` and `prod`
- H2 in `test`
- Flyway migrations for schema management

## Start The Backend

From the repository root:

```sh
docker compose up
```

Or run only the backend manually:

```sh
cd backend
./gradlew bootRun
```

Default local URL:

- backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Profiles

- `dev`: local development with bootstrap users and MySQL defaults
- `prod`: production configuration
- `test`: H2-backed automated tests

## Main Commands

```sh
cd backend
./gradlew test
./gradlew jacocoTestReport
./gradlew bootRun
```

## Code Documentation Requirement

The backend already contains Javadoc on controllers, request/response DTOs, services, and core classes under [`backend/src/main/java/`](../../backend/src/main/java/). Swagger/OpenAPI is enabled through `springdoc-openapi` and documented further in [`api.md`](./api.md).
