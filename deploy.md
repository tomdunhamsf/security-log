# Deployment

## Architecture

```
Browser → nginx:443 (HTTPS) → front:3000 (HTTP, internal)
                             → back:8080  (HTTPS, internal, self-signed cert)
                                        → db:5432 (PostgreSQL, internal)
```

nginx handles browser-facing TLS. The Next.js frontend and Spring Boot backend communicate over the internal Docker network. The browser never connects to port 3000 or 8080 directly.

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

Open **https://localhost** in your browser (accept the self-signed cert warning).
HTTP on port 80 redirects to HTTPS automatically.

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

`npm run dev` connects to `https://localhost:8080` by default and accepts the self-signed cert (TLS verification is off unless `BACKEND_TLS_VERIFY=true`).

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
| `front` | `BACKEND_TLS_VERIFY` | *(unset → false)* | Set to `true` only if the backend has a CA-signed cert |

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
- **nginx cert**: replace the self-signed cert with a CA-signed one (e.g. Let's Encrypt) and update `server_name` in `nginx/nginx.conf` to match your domain.
- **BACKEND_TLS_VERIFY**: currently `false` because the backend uses a self-signed certificate. For production, replace the Spring Boot keystore with a CA-signed certificate and set this to `true`.
- **Port 8080**: currently published to the host for debugging. In production, remove the `ports` entry from the `back` service.
- **Postgres credentials**: change `DB_USER` and `DB_PASSWORD` before any internet-facing deployment.
