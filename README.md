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

# 2. Backend
cd backend
mvn spring-boot:run          # tests: mvn test

# 3. Frontend (new terminal)
cd frontend
npm install
npm run dev                  # tests: npm test
```

Open http://localhost:5173. The home page shows "Backend is up" when everything is connected.

## Shared Supabase database

Local development uses the Docker Postgres by default. To run the backend against the shared Supabase database (team testing and demos):

1. Get invited to the `cs308-group` organization on Supabase (project **CS308 Store**, region eu-west-2).
2. Copy `backend/.env.example` to `backend/.env` and fill in the values from Supabase → **Connect** → **Direct** → **Session pooler**:
   ```
   DB_URL=jdbc:postgresql://aws-0-eu-west-2.pooler.supabase.com:5432/postgres?sslmode=require
   DB_USERNAME=postgres.njetjmvewckolkkpbruo
   DB_PASSWORD=<ask a teammate privately>
   ```
3. Run the backend from `backend/` with `mvn spring-boot:run`. The log should show `Database: jdbc:postgresql://aws-0-eu-west-2.pooler.supabase.com...`.
4. To go back to local Docker, delete or rename `backend/.env`.

Rules:
- `.env` is gitignored. Never commit it and never share the password in the repo or the group chat.
- Do not change tables from the Supabase dashboard. All schema changes go through Flyway migrations.
- Only run code from `main` against Supabase. Test migrations from unmerged branches on local Docker; a migration that changes during review would break Flyway on the shared database.

## Database migrations

The schema is managed by Flyway; Hibernate only validates it (`ddl-auto: validate`).

- Add a new file to `backend/src/main/resources/db/migration/`, named `V<next number>__<description>.sql`,
  for example `V2__create_products.sql`.
- Never edit a migration that is already on `main`; add a new one instead.
- Agree on the next version number in the team chat before opening a PR, so two branches do not both add `V3`.

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
