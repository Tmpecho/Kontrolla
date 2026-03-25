# Kontrolla
System for managing food and alchohol compliance for resturants and bars in Norway.

## Docker Compose

Start the development stack:

```sh
docker compose up
```

The frontend will be available on `http://localhost:5173`.
The backend will be available on `http://localhost:8080`.

## GitHub Actions

The repository uses GitHub Actions for development-time CI:

- `CI` runs on pull requests and pushes to `main`.
- Backend changes run `./gradlew test`.
- Frontend changes run `npm ci`, `npm run lint:check`, `npm run build`, and `npm run test:unit:ci`.
- `E2E` is a separate Playwright workflow that runs on pushes to `main` and on manual dispatch.

Recommended branch protection is to require the `Status` job from the `CI` workflow before merging to `main`.

## Backend Profiles

The backend is split into explicit Spring profiles:

- `dev`: local development with MySQL defaults and bootstrap users
- `prod`: production configuration with environment-provided database and security settings
- `test`: H2-backed test configuration

The Docker Compose backend runs with the `dev` profile by default.

### Development Login

In `dev`, a bootstrap user is created automatically:

- email: `demo@example.com`
- password: `password123`

This bootstrap user does not exist in `prod`.
