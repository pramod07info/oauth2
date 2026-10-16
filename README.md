# OAuth2 identity server

Spring Boot 3.5.16, Spring Authorization Server 1.5.8, Spring Security, Spring Data JPA / Hibernate, Flyway and PostgreSQL. Java 17+; Docker and CI use Java 21. Code is separated into controller, DTO, service, repository, configuration and exception packages.

## Features

- Database-backed registration, login, profile updates, password changes and paginated user administration.
- USER and ADMIN roles; public registration always creates USER. Only administrators can change roles, disable accounts, view other users or delete them.
- Standards-based OAuth2 Authorization Code with mandatory PKCE (S256), OpenID Connect discovery, consent, JWKS, refresh-token rotation, token revocation and introspection.
- RSA-signed access JWTs with five-minute lifetime, issuer, audience, UUID subject, roles and account version. Private signing material is supplied externally and survives restarts.
- BCrypt password hashes, database-backed 15-minute account lockout after five failed logins, CSRF-protected form login/consent/logout, secure session cookies by default, parameterized JPA queries, bean validation, optimistic concurrency and transactional audit records.
- API bearer authentication validates signature, expiry, issuer, audience, scopes, current account status and version, and persisted token activity. It never authenticates via a browser cookie. Account mutations invalidate existing API JWTs and browser sessions; password/permission changes delete persisted authorizations and refresh tokens.
- Flyway owns all schemas; Hibernate validates mappings with `ddl-auto=validate`; it never creates or alters production tables. OAuth2 clients, authorizations and consent persist in PostgreSQL.

## Start locally

Install Java 17+ (21 recommended), Maven 3.9+ and Docker Compose.

```bash
git clone https://github.com/pramod07info/oauth2.git
cd oauth2
cp .env.example .env
# Edit .env: set unique database, OAuth client and administrator passwords.
java scripts/GenerateKey.java secrets/jwk.json
docker compose --env-file .env up -d
# Bash: export the environment to the Java process, not only Docker Compose.
set -a
source .env
set +a
mvn spring-boot:run
```

For Windows PowerShell, after editing `.env`:

```powershell
Get-Content .env | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
java scripts/GenerateKey.java secrets/jwk.json
docker compose --env-file .env up -d
mvn spring-boot:run
```

Generate the key once; the script refuses to overwrite it. Do not commit `.env` or private keys. Local `.env.example` deliberately uses HTTP and `COOKIE_SECURE=false`. Production must use HTTPS and `COOKIE_SECURE=true`.

Login: <http://localhost:9000/login>. Discovery: <http://localhost:9000/.well-known/openid-configuration>. Health: <http://localhost:9000/actuator/health>.

The administrator is created only when `BOOTSTRAP_ADMIN_EMAIL` is configured and that email does not exist. No default password or dummy account is shipped. Remove both bootstrap variables after provisioning. An existing USER email is never silently promoted. The OAuth client is also inserted only once; changing its environment variables later does not rotate an existing database client.

## Obtain a token (Postman)

This server uses a browser login followed by an OAuth code exchange. `/login` is a CSRF-protected form endpoint; it is not a JSON password-to-JWT API. The password grant is intentionally unavailable.

1. Before the **first startup on a fresh database**, set `OAUTH_REDIRECT_URI=https://oauth.pstmn.io/v1/callback` in `.env`. This must exactly match Postman's callback URL. For an existing client, provision a new client ID or update its registration deliberately.
2. In Postman, choose **Authorization → OAuth 2.0 → Get New Access Token**, grant type **Authorization Code (With PKCE)** and challenge method **SHA-256**.
3. Authorization URL: `http://localhost:9000/oauth2/authorize`. Token URL: `http://localhost:9000/oauth2/token`.
4. Client ID: `user-management`. Client secret: your configured `OAUTH_CLIENT_SECRET`. Client authentication: **Send as Basic Auth header**. This is a confidential client; a SPA must use a separately provisioned public client or a backend-for-frontend and must never embed this secret.
5. Scope: `openid profile users.read users.write`. Enable browser authorization; sign in with a registered user or the bootstrap administrator and approve consent.
6. Use the resulting **access_token** as a Bearer token. An ID token is not accepted by the user API.

Refresh tokens are single-use after rotation:

```bash
curl -u "$OAUTH_CLIENT_ID:$OAUTH_CLIENT_SECRET" \
  -d grant_type=refresh_token -d "refresh_token=$REFRESH_TOKEN" \
  http://localhost:9000/oauth2/token
```

Always replace the stored refresh token with the new response value. Clients must serialize refresh requests. Revocation: authenticated POST `/oauth2/revoke` with `token=...`. Introspection: authenticated POST `/oauth2/introspect` with `token=...`. Revocation removes OAuth authorization validity; already-issued JWTs accepted by independent resource servers remain usable until expiry unless those servers also introspect or implement a revocation/account-version check. This API also checks persisted token activity, so individual access-token revocations take effect immediately here.

## User API

All protected endpoints require a valid Bearer access token. GET requires `users.read`; mutations require `users.write`. ADMIN is additionally required where indicated.

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register USER |
| GET | `/api/users/me` | USER or ADMIN | Current user |
| PUT | `/api/users/me` | USER or ADMIN | Update own display name |
| PUT | `/api/users/me/password` | USER or ADMIN | Verify current password and change password |
| GET | `/api/users?page=0&size=20` | ADMIN | List users; maximum size 100 |
| GET | `/api/users/{uuid}` | ADMIN | View user |
| PUT | `/api/users/{uuid}` | ADMIN | Update display name |
| PUT | `/api/users/{uuid}/permissions` | ADMIN | Set role and enabled status |
| DELETE | `/api/users/{uuid}` | ADMIN | Delete user and OAuth authorizations |

Registration:

```bash
curl -X POST http://localhost:9000/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"developer@example.com","password":"A-unique-long-password!","displayName":"Developer"}'

curl http://localhost:9000/api/users/me -H "Authorization: Bearer $ACCESS_TOKEN"
```

Profile update body: `{"displayName":"Pramod Verma","version":0}`. Permissions body: `{"role":"ADMIN","enabled":true,"version":0}`. Supply the latest version returned by GET; stale updates return 409. Password change body: `{"currentPassword":"...","newPassword":"..."}`. Passwords require 12+ characters and at most 72 UTF-8 bytes. Email is canonicalized to lowercase and remains immutable. Administrators cannot delete themselves or change their own role/status; provision a second administrator before removing the first. Public registration can return 409 for duplicate emails; apply rate limits to reduce account enumeration and abuse.

Errors use HTTP status codes and Spring Problem Details where supported: 400 validation, 401 invalid/missing authentication, 403 denied access, 404 missing account, 409 conflict. Password hashes never appear in response DTOs or audit records.

## Tests

```bash
mvn verify
# Repeat against an isolated, disposable PostgreSQL database:
TEST_DB_URL=jdbc:postgresql://localhost:5432/oauth2_test \
TEST_DB_USERNAME=oauth2 TEST_DB_PASSWORD=your-test-password mvn verify
```

Without `TEST_DB_URL`, integration tests use H2 in PostgreSQL compatibility mode for fast local checks. CI uses real PostgreSQL 17 and Java 21, running the same Flyway migrations and OAuth2 round trips, plus bidirectional compatibility checks against records written by the previous JDBC implementation. Tests exercise real form authentication, PKCE code exchange, signed JWT verification, rotated refresh tokens, role boundaries, disabled accounts, administrator CRUD, validation and optimistic concurrency. Never point tests at a production database.

## Deployment requirements

This is a production-oriented implementation, not a deployment security certification. Before exposing it:

- Terminate TLS at a trusted ingress, use the public HTTPS `OAUTH_ISSUER`, exact HTTPS redirect URIs, secure cookies and an explicit proxy trust policy. Forwarded headers are ignored by default; do not trust arbitrary client-supplied headers.
- Apply distributed rate limits at the ingress to login, registration and token endpoints, alongside the built-in account lockout. Public registration has no email verification; add it or restrict registration according to your identity policy.
- Mount the private JWK read-only from a secrets manager, readable by container UID 10001. Back it up securely. All instances must share active signing keys. This version loads one key at startup; implement a key ring and overlapping public keys for zero-downtime rotation.
- Use a dedicated PostgreSQL service account, TLS database connections, encryption at rest, backups and restricted access: OAuth tables contain bearer/refresh credentials and serialized authorization state. Run migrations with a separate privileged role if your deployment policy requires it.
- Browser sessions are in memory; use a single instance or sticky sessions initially. Add Spring Session with a shared store before enabling arbitrary load balancing. Account-version checks reject stale sessions across instances, but do not replicate the sessions themselves.
- Set strict CORS allowlists if adding browser API access. The default configuration enables no cross-origin API access. Prefer a backend-for-frontend for browser clients.
- Add password recovery, email verification, MFA, organization-specific audit export/retention and abuse monitoring as required. Audit records store actor identifiers and target UUIDs, not passwords or request bodies.
- Review patch versions and dependency vulnerability scans, run the PostgreSQL CI suite, conduct a security review and load-test before rollout. Rotating refresh tokens here does not implement refresh-token family reuse detection.

Build the container with `docker build -t oauth2-server .`; supply database/secrets environment variables and mount the signing key at the configured `JWT_JWK_FILE`. It listens on port 9000 and runs as a non-root user.

## JPA / Hibernate persistence

All production persistence uses Jakarta Persistence entities and Spring Data JPA repositories: users, audits, OAuth2 clients, authorizations and consent. `JpaRegisteredClientRepository`, `JpaAuthorizationService` and `JpaAuthorizationConsentService` implement Spring Authorization Server contracts. Spring Boot 3 uses `jakarta.persistence` (modern JPA), not the older `javax.persistence` API.

Existing V1/V2 Flyway migrations, table/column names, IDs and OAuth JSON serialization remain compatible. **Keep your existing database and signing key**; no table reset or data export is required. JDBC adapters appear only in a migration regression test to verify old/new record compatibility. Client configuration, environment variables, API URLs and Postman requests remain the same.

`AppUser.version` uses Hibernate `@Version` for concurrency checks. Audit and authorization cleanup participate in the user service transaction. Open Session in View is disabled; DTOs are assembled within transactions. Account-lockout counters use atomic JPQL updates without changing the account token version.

## Angular Identity Console

The `frontend/` project uses Angular 21 LTS with standalone components, lazy routes, reactive forms, signals, strict TypeScript and a responsive layout. Use Node.js **24 LTS** and npm. Keep your existing database and signing key.

### Run on Windows

1. Pull this branch (or `main` after merging) and restart Spring Boot with your existing environment variables. For local HTTP use `OAUTH_ISSUER=http://localhost:9000`, `COOKIE_SECURE=false`, and `UI_BASE_URL=http://localhost:4200`.
2. In a separate PowerShell terminal:

```powershell
cd "C:\your-project-path\oauth2\frontend"
npm ci
npm start
```

3. Open **http://localhost:4200**. Register a member or choose **Continue to sign in**. Enter the account email in the identity server's Username field, enter its password and approve consent.

The server automatically provisions a separate confidential `identity-ui` OAuth client, using the existing `OAUTH_CLIENT_SECRET`, callback `${OAUTH_ISSUER}/login/oauth2/code/identity-ui`, mandatory S256 PKCE, consent and refresh rotation. The existing Postman client/redirect are unchanged. Like other clients, this registration is inserted once; changing issuer or secret later requires deliberately updating the stored registration. Never place a client secret in Angular environment files.

### Pages and API coverage

| Page | Features / existing APIs |
|---|---|
| Sign in / registration | Browser OAuth2 authorization, code exchange, CSRF-protected registration |
| Overview | Current identity, role, session expiry, administrator user count |
| My profile | View and update own profile; optimistic version handling |
| Security | Password change, refresh, token introspection |
| Users & roles (ADMIN) | Paginated listing; member details; display-name updates; role assignment; enable/disable; confirmed deletion |
| System status | Health, OpenID discovery and public JWKS |
| Sign out | Revoke the current authorization's access/refresh tokens and invalidate the browser session |

### Browser security and deployment

Angular calls same-origin `/bff/*` endpoints. The Spring OAuth2 Client implements the backend-for-frontend: client credentials and access/refresh tokens remain in the server session, never browser storage or frontend responses. Spring validates OIDC identity tokens and uses PKCE. Every browser mutation requires a CSRF token obtained from `/bff/csrf`. Existing `/api/*` endpoints remain bearer-only and enforce authorization roles/scopes server-side. The bridge forwards only explicit user-management routes; it is not an arbitrary URL proxy.

Refresh is serialized within each server session and happens automatically near expiry. Profile updates refresh the token/version; password changes sign out. Browser sessions are currently in-memory. Use sticky sessions for multiple instances or configure a shared session store with a distributed refresh lock before removing affinity. Session logout revokes the current login's authorization, not other devices.

The development proxy in `frontend/proxy.conf.json` connects to localhost:9000. Production: `npm run build`, serve `frontend/dist/identity-console/browser` with SPA fallback, and route `/bff/**`, `/api/**`, `/oauth2/**`, `/.well-known/**`, `/login`, `/error` and `/actuator/health` to Spring. The Angular sign-in route is `/sign-in` to avoid colliding with Spring's `/login`. Serve both from the same HTTPS origin; configure the public `OAUTH_ISSUER`, `UI_BASE_URL` and secure cookies. Do not deploy `ng serve` or enable permissive CORS in production.

### Frontend validation

```powershell
cd frontend
npm ci
npm run build
npm test
npx playwright install chromium
npm run e2e
```

The default browser suite uses mocked API responses for deterministic UI tests. To exercise the actual backend, start it with an isolated database and the development bootstrap admin, then set `$env:REAL_BACKEND="1"` before `npm run e2e`. Override `E2E_ADMIN_EMAIL` and `E2E_ADMIN_PASSWORD` if needed. The live test creates a temporary member, signs in through consent, edits its profile, refreshes/inspects the session, changes its password, then uses ADMIN to change permissions and delete it. Never run this against production. GitHub CI runs this live flow against PostgreSQL 17 in addition to backend and frontend unit tests.
