# Security Log Viewer — Front-End

A Next.js 16 / TypeScript application for ingesting and browsing Zscaler web proxy logs.
All source files live in `./front/`.

---

## Architecture

```
Browser  ──▶  Next.js (localhost:3000)  ──▶  Backend API (https://localhost:8080)
```

The Next.js app acts as a **Backend-for-Frontend (BFF)**:
- All user-facing pages are served by Next.js.
- The browser never calls `https://localhost:8080` directly.
- Next.js API routes proxy every request to the backend, attaching the auth token from an
  HttpOnly cookie so it never touches client-side JavaScript.

---

## Routes

| Path | Description |
|------|-------------|
| `/` | Login form (redirects to `/dashboard` if already authenticated) |
| `/dashboard` | Landing page after login — links to Upload and View Logs |
| `/upload` | Client-side Zscaler log validation, then file upload |
| `/logs` | Paginated list of ingested log file names |
| `/logs/[filename]` | Tabular view of a single log file |

### API Routes (server-side proxies)

| Method | Path | Backend endpoint |
|--------|------|-----------------|
| `POST` | `/api/login` | `POST https://localhost:8080/auth` |
| `POST` | `/api/logout` | Clears the `auth_token` cookie locally |
| `POST` | `/api/ingest` | `POST https://localhost:8080/ingest` |
| `GET` | `/api/display` | `GET https://localhost:8080/display` |
| `GET` | `/api/display/[filename]` | `GET https://localhost:8080/display/<filename>` |
| `POST` | `/api/display/[filename]/analyze` | `POST https://localhost:8080/display/<filename>/analyze` |

---

## Authentication Flow

1. User submits credentials to `/api/login`.
2. The route forwards them to `https://localhost:8080/auth` and reads `{ token: "..." }`.
3. The token is stored as an **HttpOnly, SameSite=Lax** cookie (`auth_token`, 8-hour TTL).
4. All subsequent API proxy calls extract the cookie and forward it as `Authorization: Bearer <token>`.
5. `/api/logout` clears the cookie.
6. `src/middleware.ts` enforces authentication on `/dashboard`, `/upload`, and `/logs`,
   redirecting unauthenticated requests back to `/`.

---

## Zscaler Log Validation

Client-side validation runs **before** the file is sent to the server.

**Required fields per log line** (key=value format):
`action`, `reason`, `username`, `url`, `status`

**Zscaler-distinctive fields** (at least one expected):
`sourcetype`, `transactionid`, `urlcategory`, `urlcat`, `urlsupercategory`,
`urlclass`, `malwareclass`, `realm`, `reqsize`, `respsize`

**Format reference:** https://help.zscaler.com/zia/nss-feed-output-format-web-logs

A typical line looks like:
```
Oct  7 16:00:05 2024 reason=Allowed action=Allowed transactionid=4567890123 \
  duration=1000 sourcetype=zscalernss-web username=user@example.com \
  realm=ZScaler url="http://example.com/" status=200 reqmethod=GET \
  urlcategory=News serverip=1.2.3.4 clientip=10.0.0.1
```

---

## Log Table Display

The log viewer (`/logs/[filename]`) handles both response formats from the backend:
- **Raw text** — parsed line-by-line into `key=value` pairs.
- **JSON** — accepted as `Array<{[key: string]: string}>` or `{ entries: [...] }`.

Columns are ordered: preferred Zscaler fields first (action, reason, username, url, status, …),
then any remaining fields alphabetically. Long values are truncated and revealed on hover.

### Threat Analysis

An **Analyze** button in the header sends `POST /api/display/[filename]/analyze` (empty body).
The backend responds with:
```json
{ "description": "problem description", "certainty": 85, "rows": [0, 3, 7] }
```
- If `certainty > 0`: an orange banner shows the threat description and certainty percentage, and each row listed in `rows` (0-indexed) is highlighted in light red.
- If `certainty === 0`: the result is silently ignored.

---

## Development

```bash
cd front
npm install
npm run dev     # starts on http://localhost:3000
```

`NODE_TLS_REJECT_UNAUTHORIZED=0` is set automatically via `cross-env` in the `dev` script so
that the Next.js server process can reach `https://localhost:8080` with a self-signed certificate.

### Environment variables (server-side)

| Variable | Default | Purpose |
|----------|---------|---------|
| `BACKEND_URL` | `https://localhost:8080` | URL the Next.js server uses to reach the Spring Boot backend |
| `BACKEND_TLS_VERIFY` | *(unset → true)* | Set to `false` to accept self-signed backend certs (Docker) |

### Build for production / Docker

`next.config.ts` sets `output: 'standalone'` so the Docker image only includes what Next.js
needs to run — no `node_modules` in the final layer.

```bash
# standalone build (used by Dockerfile)
npm run build
node .next/standalone/server.js
```

See `deploy.md` for Docker Compose instructions.

---

## File Layout

```
front/
├── Dockerfile
├── .dockerignore
├── src/
│   ├── app/
│   │   ├── page.tsx                        # Login page
│   │   ├── layout.tsx
│   │   ├── globals.css
│   │   ├── dashboard/page.tsx              # Post-login dashboard
│   │   ├── upload/page.tsx                 # Log upload + validation
│   │   ├── logs/
│   │   │   ├── page.tsx                    # Log file list
│   │   │   └── [filename]/page.tsx         # Log file table viewer
│   │   └── api/
│   │       ├── login/route.ts
│   │       ├── logout/route.ts
│   │       ├── ingest/route.ts
│   │       └── display/
│   │           ├── route.ts
│   │           └── [filename]/
│   │               ├── route.ts
│   │               └── analyze/route.ts
│   ├── lib/
│   │   ├── zscaler-validator.ts            # Client-side log validation
│   │   └── backend.ts                      # Undici fetch wrapper (self-signed cert support)
│   └── middleware.ts                       # Route protection
├── next.config.ts
├── package.json
└── tsconfig.json
```
