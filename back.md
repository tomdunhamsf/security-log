# Security Log Backend

Spring Boot 3.4 / Java 21 REST API.  All source files live in `./back/`.

---

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/auth` | None | Authenticate; returns `{ "token": "..." }` |
| `POST` | `/ingest` | Bearer token | Upload a Zscaler NSS log file (multipart `file` field) |
| `GET` | `/display` | Bearer token | Returns JSON array of distinct log names |
| `GET` | `/display/{logName}` | Bearer token | Returns JSON array of all rows for that log |
| `POST` | `/display/{logName}/analyze` | Bearer token | Fetches all rows and returns an OpenAI threat analysis |

### POST /auth

Request body:
```json
{ "username": "admin", "password": "ChangeMe123!" }
```
Response (200):
```json
{ "token": "eyJ..." }
```
Response (401):
```json
{ "error": "Invalid credentials" }
```

### POST /ingest

`Content-Type: multipart/form-data` with field `file`.

Response (200):
```json
{ "message": "Ingested 1234 log entries from access.log", "count": 1234 }
```
Appends to any existing rows with the same log name (never overwrites).

**Format auto-detection**: the first non-blank line is inspected. If it contains `=` the file is parsed as Zscaler NSS key=value format (`ZscalerParser`). Otherwise it is parsed as tab-delimited with columns in fixed order: `time cip sip login ua method url respcode reqhdrsize reqsize resphrdsize respsize referrer` (`TsvParser`). Header rows (first column == `time`) are skipped automatically.

### GET /display

```json
["access.log", "proxy-2024-10.log"]
```

### POST /display/{logName}/analyze

Fetches all rows for the log from the database, sends them as a TSV to GPT-4o via LangChain4j, and returns a threat verdict.

Response (200):
```json
{ "description": "Possible data exfiltration via repeated large POST to external host", "certainty": 87, "rows": [2, 5, 6] }
```
No-threat response:
```json
{ "description": null, "certainty": 0, "rows": null }
```
Response (404): log name not found.
Response (502): OpenAI call failed.

Requires `OPEN_AI_KEY` environment variable to be set.

---

### GET /display/{logName}

```json
[
  {
    "recordId": 1,
    "logName": "access.log",
    "time": "Oct  7 16:00:05 2024",
    "cip": "10.0.0.1",
    "sip": "1.2.3.4",
    "login": "user@example.com",
    "ua": "Mozilla/5.0 ...",
    "method": "GET",
    "url": "https://example.com/",
    "respcode": "200",
    "reqhdrsize": "512",
    "reqsize": "1024",
    "resphrdsize": "256",
    "respsize": "4096",
    "referrer": null
  }
]
```

Returns 404 if no entries exist for the given log name.

---

## Database Schema

Managed by Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

### `users` table

| Column | Type | Notes |
|--------|------|-------|
| `username` | VARCHAR (PK) | Maps to the "user" column described in the spec |
| `password_hash` | VARCHAR | BCrypt hash (cost 10) |

`user` and `password` are SQL reserved words; the actual column names are `username` and
`password_hash` to avoid quoting requirements.

### `logs` table

| Column | Type | Notes |
|--------|------|-------|
| `record_id` | BIGSERIAL (PK) | Auto-generated |
| `log_name` | VARCHAR | Filename of the uploaded file; indexed |
| `time` | VARCHAR | Parsed syslog timestamp from line prefix |
| `cip` | VARCHAR | Client IP (`clientip` NSS field) |
| `sip` | VARCHAR | Server IP (`serverip` NSS field) |
| `login` | VARCHAR | Username (`username` NSS field) |
| `ua` | VARCHAR(1024) | User-agent (`useragent` NSS field) |
| `method` | VARCHAR(16) | HTTP method (`reqmethod` / `httpmethod` NSS field) |
| `url` | VARCHAR(2048) | Request URL |
| `respcode` | VARCHAR(8) | HTTP status code (`status` NSS field) |
| `reqhdrsize` | VARCHAR | Request header size |
| `reqsize` | VARCHAR | Request body size |
| `resphrdsize` | VARCHAR | Response header size (note: column name matches spec exactly) |
| `respsize` | VARCHAR | Response body size |
| `referrer` | VARCHAR(2048) | Referer URL (`refererURL` / `referrer` NSS field) |

---

## Security

- **Passwords**: BCrypt (cost 10) via Spring Security `BCryptPasswordEncoder`.
- **Tokens**: HS256 JWT signed with a configurable Base64 secret; 8-hour expiry.
- **Transport**: HTTPS only. A self-signed PKCS12 keystore is generated at build time
  by the `keytool-maven-plugin` (10-year validity). Replace with a real cert for production.
- **Session**: Stateless — no server-side sessions.

---

## Configuration

All sensitive values are overridable via environment variables:

| Property | Env var | Default |
|----------|---------|---------|
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/securitylog` |
| `spring.datasource.username` | `DB_USER` | `securitylog` |
| `spring.datasource.password` | `DB_PASSWORD` | `securitylog` |
| `jwt.secret` | `JWT_SECRET` | Dev-only placeholder (Base64, ≥256 bits) |
| *(none)* | `OPEN_AI_KEY` | OpenAI API key required for `/analyze` endpoint |

**Always override `JWT_SECRET` in production** with a strong random value, e.g.:
```bash
openssl rand -base64 32
```

---

## Default Admin Account

On first startup, if the `users` table is empty, a default admin account is created:
- **Username**: `admin`
- **Password**: `ChangeMe123!`

Change this immediately. The startup log will warn loudly if it's still the default.

---

## Running with Docker Compose

```bash
cd back
docker compose up --build
```

This starts PostgreSQL 16 on port 5432 and the app on `https://localhost:8080`.

Postgres data is persisted in the `pgdata` named volume.

## Running locally (requires Java 21 + Maven 3.9+)

```bash
cd back
# Start Postgres first, then:
mvn clean package -DskipTests
java -jar target/security-log-backend-*.jar
```

The keytool-maven-plugin generates `target/classes/keystore.p12` during `package`.

---

## File Layout

```
back/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── src/main/java/com/securitylog/
    ├── SecurityLogApplication.java
    ├── config/
    │   └── SecurityConfig.java          # Spring Security + CORS + BCrypt bean
    ├── controller/
    │   ├── AuthController.java          # POST /auth
    │   ├── IngestController.java        # POST /ingest
    │   └── DisplayController.java       # GET /display, GET /display/{logName}
    ├── dto/
    │   ├── LoginRequest.java
    │   ├── LoginResponse.java
    │   └── AnalysisResult.java          # Record: description, certainty, rows
    ├── entity/
    │   ├── User.java
    │   └── LogEntry.java
    ├── init/
    │   └── DataInitializer.java         # Creates default admin if no users exist
    ├── repository/
    │   ├── UserRepository.java
    │   └── LogEntryRepository.java
    ├── security/
    │   └── JwtAuthFilter.java           # Extracts Bearer token, sets SecurityContext
    ├── service/
    │   ├── AuthService.java             # BCrypt verify + JWT issue
    │   ├── JwtService.java              # JJWT generate/validate/extract
    │   ├── LogService.java              # Parse + batch-insert; list/query
    │   └── OpenAiAnalyzer.java          # Formats log rows as TSV, calls GPT-4o, parses JSON result
    └── util/
        ├── ZscalerParser.java           # NSS key=value line → LogEntry
        └── TsvParser.java               # Tab-delimited line → LogEntry (fixed column order)
```
