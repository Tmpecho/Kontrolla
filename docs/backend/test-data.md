# Backend Test Data

## Development Database Credentials

Default local development values from [`docker-compose.yml`](../../docker-compose.yml) and [`backend/src/main/resources/application-dev.properties`](../../backend/src/main/resources/application-dev.properties):

- MySQL host: `localhost`
- MySQL port: `3306`
- database: `kontrolla`
- username: `kontrolla`
- password: `kontrolla`
- root password: `root`

JDBC default:

- `jdbc:mysql://localhost:3306/kontrolla`

## Development Users

Bootstrapped in the `dev` profile:

- `admin@example.com` / `password123`
- `demo@example.com` / `password123`
- `emma.larsen@example.com` / `password123`
- `noah.berg@example.com` / `password123`

Related bootstrap data:

- organization name: `Kontrolla Dev Org`
- establishment examples:
  - `Kontrolla Demo Restaurant`
  - `Kontrolla Demo Bar`

## Security Defaults In Development

Development-only defaults include:

- JWT secret: `change-me-change-me-change-me-change-me`
- refresh cookie secure flag: `false`
- frontend invite base URL: `http://localhost:5173`

These values are acceptable for local development only.

## Test Profile Data

Automated tests use [`backend/src/test/resources/application-test.properties`](../../backend/src/test/resources/application-test.properties):

- database: in-memory H2
- JDBC URL: `jdbc:h2:mem:kontrolla-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- username: `sa`
- password: empty
- Flyway: disabled
- Hibernate DDL: `create-drop`

## How To Use This Test Data

- use the `dev` users for manual login and frontend testing
- use MySQL credentials for local DB inspection
- use the `test` profile only for automated tests, not for normal manual development
