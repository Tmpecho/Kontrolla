# Setup

This guide covers the local setup for working on Kontrolla.

## Prerequisites

- Docker and Docker Compose
- Java 25
- Node.js and npm

## Start the project

1. Start the local development stack:

```sh
docker compose up
```

2. Open the apps:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

## Run services manually

If you want to run parts of the stack outside Docker:

```sh
cd backend && ./gradlew bootRun
cd frontend && npm run dev
```

## Run tests

```sh
cd backend && ./gradlew test
cd frontend && npm run test:unit
```
