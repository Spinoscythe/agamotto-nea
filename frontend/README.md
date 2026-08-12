# Agamotto frontend

Vite + React + TypeScript UI for Agamotto. Talks to the Spring Boot backend at the repo root (`src/main/java/...`).

## Layout

- `src/api` — HTTP client and typed API helpers (`authApi`, `projectsApi`, …). This is the only frontend API module; import from `@/api`.
- `src/` — pages, components, auth context
- Backend lives at the monorepo root (Maven/`pom.xml`), default URL `http://localhost:8080`

## Setup

```bash
pnpm install
cp .env.example .env   # VITE_API_BASE_URL=http://localhost:8080
pnpm dev
```

Run the Spring Boot app separately (e.g. `./mvnw spring-boot:run` from the repo root).
