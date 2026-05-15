# Deployment

## Architecture

```
Browser → front (port 3000) → back (port 8080) → db (port 5432)
```

All three services run as Docker containers. The browser only ever talks to the Next.js frontend on port 3000. The frontend proxies API calls to the Spring Boot backend. The backend stores logs in PostgreSQL.

---

## Prerequisites

- Docker Desktop running (Windows: check system tray for the whale icon)
- A `.env` file at the repo root (see below)

---

## .env file

The `.env` file at the repo root is read by the `back` service. It must contain:

```
OPEN_AI_KEY=sk-...
```

Override any other defaults here as well (see Configuration section).

**Never commit `.env` to source control.** It is git-ignored.

---

## Run everything

From the repo root:

```bash
docker compose up --build
```

- First run takes several minutes — Maven downloads dependencies, npm installs packages.
- Subsequent runs are faster thanks to Docker layer caching.
- The app is ready when you see the Spring Boot startup banner and the Next.js server log.

Open http://localhost:3000 in your browser.

To run in the background:

```bash
docker compose up --build -d
docker compose logs -f   # tail logs
```

To stop:

```bash
docker compose down
```

To stop and wipe the database volume:

```bash
docker compose down -v
```

---

## Running services individually

The `back/` directory has its own `docker-compose.yml` for running just the backend + database:

```bash
cd back
docker compose up --build
```

The frontend can still be run locally against it:

```bash
cd front
npm install
npm run dev
```

`npm run dev` sets `BACKEND_URL` to `https://localhost:8080` by default and accepts the self-signed cert.

---

## Configuration

| Service | Variable | Default in compose | Purpose |
|---------|----------|--------------------|---------|
| `back` | `DB_URL` | `jdbc:postgresql://db:5432/securitylog` | Postgres JDBC URL |
| `back` | `DB_USER` | `securitylog` | Postgres user |
| `back` | `DB_PASSWORD` | `securitylog` | Postgres password |
| `back` | `JWT_SECRET` | Dev placeholder | **Override in production** — Base64, ≥32 bytes |
| `back` | `OPEN_AI_KEY` | *(from .env)* | OpenAI API key |
| `front` | `BACKEND_URL` | `https://back:8080` | URL the Next.js server uses to reach the backend |
| `front` | `BACKEND_TLS_VERIFY` | `false` | Set to `true` only if the backend has a trusted cert |

To override any value without editing the compose file, add it to `.env`:

```
JWT_SECRET=<your-strong-secret>
DB_PASSWORD=<your-db-password>
```

---

## Production notes

- **JWT_SECRET**: generate a strong secret before deploying:
  ```bash
  openssl rand -base64 32
  ```
- **BACKEND_TLS_VERIFY**: currently `false` because the backend uses a self-signed certificate. For production, replace the keystore with a CA-signed certificate and set this to `true`.
- **Port 8080**: currently published to the host for debugging. In production, remove the `ports` entry from the `back` service so it is only reachable internally by the `front` container.
- **Postgres credentials**: change `DB_USER` and `DB_PASSWORD` before any internet-facing deployment.
