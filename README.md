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

To use the shared Supabase database instead of the local one, also fill in the database values in
`backend/.env`. Never commit `.env`.

## Database migrations

The schema is managed by Flyway; Hibernate only validates it (`ddl-auto: validate`).

- Add a new file to `backend/src/main/resources/db/migration/`, named `V<next number>__<description>.sql`,
  for example `V2__create_products.sql`.
- Never edit a migration that is already on `main`; add a new one instead.
- Agree on the next version number in the team chat before opening a PR, so two branches do not both add `V3`.

## Accounts and roles

- The backend needs `JWT_SECRET` in `backend/.env`, at least 32 characters, or it does not start.
- Sign-up always creates a `CUSTOMER`. For one test account per role, set `DEV_SEED_PASSWORD` in
  `backend/.env` and run `mvn spring-boot:run -Dspring-boot.run.profiles=dev`. The accounts are
  `customer@store.test`, `sales.manager@store.test`, `product.manager@store.test` and `support.agent@store.test`.
- Each management area belongs to one role: `/api/sales/**`, `/api/product-manager/**` and `/api/support/**`.
  A new endpoint under these paths is protected automatically.
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
