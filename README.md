# Kontrolla
System for managing food and alchohol compliance for resturants and bars in Norway.

## Docker Compose

Start the development stack:

```sh
docker compose up
```

The frontend will be available on `http://localhost:5173`.
The backend will be available on `http://localhost:8080`.

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
