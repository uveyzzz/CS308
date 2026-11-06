# CS308 Online Store

The shared GitHub repository for the CS308 Group Project (Sabancı University, Software Engineering).

| Part | Stack |
|---|---|
| Backend | Java 21, Spring Boot 3.5 (Web, JPA, Security, Validation), Flyway |
| Database | PostgreSQL 16 (local Docker for development, Supabase for the shared/demo DB) |
| Frontend | React 19, Vite, React Router |
| Tests | JUnit 5 + Mockito + MockMvc (backend), Vitest + Testing Library (frontend) |

## Project layout

```
backend/    Spring Boot API        (http://localhost:8080, all routes under /api)
frontend/   React app              (http://localhost:5173, proxies /api to the backend)
docker-compose.yml                  local PostgreSQL
.github/    CI, PR and bug templates
```

## Getting started

Requirements: JDK 21, Maven 3.9+, Node 22+, Docker.

```bash
# 1. Database
docker compose up -d

# 2. Backend (first time: copy backend/.env.example to backend/.env and set JWT_SECRET)
cd backend
mvn spring-boot:run          # tests: mvn test

# 3. Frontend (new terminal)
cd frontend
npm install
npm run dev                  # tests: npm test
```

Open http://localhost:5173. The home page shows "Backend is up" when everything is connected.

`backend/.env` is gitignored. Never commit it.

## Connecting to Supabase

Local development uses the Docker Postgres from `docker-compose.yml`. The shared/demo environment uses Supabase: project **CS308 Store** in the `cs308-group` organization, region eu-west-2. Ask a teammate for an invite and for the database password; never share the password in the repo or the group chat.

1. In the Supabase dashboard open **Connect** and copy the **Session pooler** connection (port 5432).
2. Add these lines to `backend/.env`:

   ```properties
   DB_URL=jdbc:postgresql://aws-0-eu-west-2.pooler.supabase.com:5432/postgres?sslmode=require
   DB_USERNAME=postgres.njetjmvewckolkkpbruo
   DB_PASSWORD=<database-password>
   DB_POOL_SIZE=3
   ```

3. Start the backend. The log should show `Database: jdbc:postgresql://aws-0-eu-west-2.pooler.supabase.com...`, and Flyway applies any new migrations on startup. Remove the lines to go back to Docker.

The pooler has a small connection limit that the whole team shares, so use Docker for daily work.

## Database migrations

The schema is managed by Flyway; Hibernate only validates it (`ddl-auto: validate`).

- All schema changes are files in `backend/src/main/resources/db/migration/`, named
  `V<next number>__<description>.sql`. Never change the schema by hand in the Supabase dashboard.
- Never edit a migration that is already on `main`; add a new one instead.
- Agree on the next version number in the team chat before opening a PR, so two branches do not both add `V3`.
- Only run migrations that are final against Supabase: once applied there, a file cannot change.
- Every new table needs `ALTER TABLE <name> ENABLE ROW LEVEL SECURITY;` in its migration. Supabase can expose
  the public schema through its Data API; with this line that API sees nothing, and the backend still works.

## Accounts and roles

- The backend needs `JWT_SECRET` in `backend/.env`, at least 32 characters, or it does not start.
- Sign-up always creates a `CUSTOMER`. The staff accounts come from the `V2` migration:
  `sales.manager@store.test`, `product.manager@store.test` and `support.agent@store.test`.
  Their password is shared in the team chat, never in the repo.
- Login tokens carry the user id and role. A role change applies after the next login.
- Admin endpoints live under `/api/admin/products/**` (`PRODUCT_MANAGER`), `/api/admin/sales/**` (`SALES_MANAGER`)
  and `/api/admin/support/**` (`SUPPORT_AGENT`). Put `@PreAuthorize("hasRole('...')")` on each admin controller.
  `SecurityConfig` also guards these paths, so a forgotten annotation cannot open one, and any other
  `/api/admin/**` path is closed until it gets a rule.
- Anyone can read `/api/products/**` and `/api/categories/**`, but every other method there needs `PRODUCT_MANAGER`.
  Customer actions such as reviews need their own path, for example `/api/reviews`.

## How we work

- **Jira:** every change belongs to a Jira issue (key like `SCRUM-14`).
- **Branches:** `main` is protected. Work on `feature/SCRUM-14-product-search` or `bugfix/SCRUM-20-cart-total`.
- **Commits:** start with the issue key: `SCRUM-14: add product search endpoint`. This links the commit to Jira.
- **Pull requests:** at least one review from a teammate working on a *different* feature, and green CI.
- **Tests:** every story comes with unit tests.
- **Bugs:** report them as Jira bugs (or GitHub issues with the bug template).
- **AI use:** allowed, but you must be able to explain every line you commit; demos include individual technical questions.

## API conventions

- All endpoints live under `/api`. Public: `/api/health`, `/api/auth/**`, `GET /api/products/**`, `GET /api/categories/**`.
  Everything else requires authentication.
- Errors always have the shape `{ "status", "message", "fieldErrors", "timestamp" }`.
- Frontend code calls the backend through `frontend/src/api/client.js` (`apiFetch`), never `fetch` directly.
