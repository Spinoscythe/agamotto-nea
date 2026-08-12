# Agamotto

School project: Spring Boot API + Vite/React frontend.

## Structure

```
agamotto/
├── pom.xml / src/main/java/...   # Spring Boot backend (HTTP under /api/...)
├── frontend/                     # Vite React app
│   └── src/api/                  # Frontend HTTP client (import @/api)
└── ...
```

There is **one** frontend API module: `frontend/src/api`. The backend is the Maven project at the repo root (not a second Node `api` package).

## Run

**Backend** (repo root):

```bash
./mvnw spring-boot:run
```

**Frontend**:

```bash
cd frontend
pnpm install
cp .env.example .env   # VITE_API_BASE_URL=http://localhost:8080
pnpm dev
```

Frontend defaults to `http://localhost:8080` when `VITE_API_BASE_URL` is unset.
