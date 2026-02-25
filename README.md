# AuthN/AuthZ Microservice

Spring Boot authentication and authorization service for the Wavy backend.

## Tech Stack

- Java 17
- Spring Boot 3.5.8
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JobRunr
- JWT (JJWT 0.12.6)
- Maven Wrapper

## Current Features

- User registration and login
- JWT-based authentication (RSA `RS256`, `kid`, JWKS)
- Access + refresh token pair issuance (JSON body)
- Refresh token rotation and reuse detection
- Refresh token revocation on logout
- Scheduled cleanup for expired/revoked refresh tokens
- Login brute-force protection (failed-attempt lockout)
  - Pluggable attempt store abstraction (`LoginAttemptStore`)
  - Redis-backed store for distributed deployments
- `/api/auth/me` authenticated profile endpoint
- Email verification flow
- Forgot password / reset password flow
- OAuth2 login + internal OAuth2 code exchange flow
- Role + permission entities and seed migrations
- JWKS endpoint for token signature verification (`/.well-known/jwks.json`)

## What Was Implemented (Done)

### 1) Spring configuration hardening pattern

Implemented a typed, validated configuration approach:

- Added configuration scanning:
  - `src/main/java/com/serdarahmanov/music_app_backend/MusicAppBackendApplication.java`
- Updated and validated existing `application.*` config:
  - `src/main/java/com/serdarahmanov/music_app_backend/utility/ApplicationProperties.java`
- Added new typed config classes:
  - `src/main/java/com/serdarahmanov/music_app_backend/utility/config/JwtProperties.java`
  - `src/main/java/com/serdarahmanov/music_app_backend/utility/config/AppOauth2Properties.java`
- Removed direct `@Value` usage from:
  - `src/main/java/com/serdarahmanov/music_app_backend/auth/jwt/JWTService.java`
  - `src/main/java/com/serdarahmanov/music_app_backend/auth/forcodex/service/Oauth2AuthorizationCodeService.java`

### 2) JWT upgrade to RSA + `kid` + JWKS

Replaced shared-secret JWT signing with RSA signing and key identification:

- RSA key management provider:
  - `src/main/java/com/serdarahmanov/music_app_backend/auth/jwt/JwtKeyProvider.java`
- JWT service updated to:
  - sign with RSA private key (`RS256`)
  - include `kid` in JWT header
  - include and validate `iss` claim
  - resolve verification key by `kid`
  - file: `src/main/java/com/serdarahmanov/music_app_backend/auth/jwt/JWTService.java`
- Added JWKS endpoint:
  - `GET /.well-known/jwks.json`
  - file: `src/main/java/com/serdarahmanov/music_app_backend/auth/jwt/JwksController.java`
- Opened JWKS endpoint in security config:
  - `src/main/java/com/serdarahmanov/music_app_backend/auth/config/SecurityConfig.java`

### 3) Config cleanup and developer documentation

- Reworked `application.yml` to env-driven placeholders and explanatory comments:
  - `src/main/resources/application.yml`
- Added environment template file:
  - `.env.example`

### 4) Refresh token architecture

Implemented server-side refresh-token flow:

- Added refresh token persistence and migration:
  - `src/main/resources/db/migration/V11__create_refresh_token_table.sql`
  - `src/main/java/com/serdarahmanov/music_app_backend/auth/refresh/RefreshToken.java`
  - `src/main/java/com/serdarahmanov/music_app_backend/auth/refresh/RefreshTokenRepository.java`
- Added refresh token service:
  - `src/main/java/com/serdarahmanov/music_app_backend/auth/refresh/RefreshTokenService.java`
  - Generates opaque random refresh tokens
  - Stores only `SHA-256` hash at rest
  - Rotates refresh token on every `/refresh`
  - Detects reuse and revokes whole token family
  - Revokes active refresh tokens on logout
- Added scheduled cleanup job:
  - `src/main/java/com/serdarahmanov/music_app_backend/auth/refresh/jobs/RefreshTokenCleanupJob.java`
  - Deletes expired and revoked refresh tokens older than retention window
- Updated auth endpoints to return token pair in JSON:
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
  - `POST /api/auth/oauth2/exchange`

### 5) Verification

- Build check completed successfully:
  - `./mvnw test` (Windows: `.\mvnw.cmd test`)
- Current status:
  - tests run: 37
  - failures: 0
  - errors: 0

## Important Development Behavior

- If `jwt.rsa-private-key-pem` and `jwt.rsa-public-key-pem` are not provided, the app generates an ephemeral RSA key pair at startup.
- In this mode, JWTs become invalid after server restart.
- This is acceptable for development, but not for production.
- For production profile (`SPRING_PROFILES_ACTIVE=prod`), ephemeral JWT keys are disabled and startup fails unless RSA PEM keys are provided.

## Run Locally

1. Start PostgreSQL and ensure DB is available.
2. Provide required runtime variables (manually or via your IDE run config).
3. Run:

```powershell
.\mvnw.cmd spring-boot:run
```

Or:

```powershell
.\mvnw.cmd test
```

## Main Auth Endpoints

Base path: `/api/auth`

- `POST /register`
- `POST /login`
- `POST /logout`
- `POST /refresh`
- `GET /me`
- `GET /verify-email`
- `POST /forgot-password`
- `GET /forgot-password`
- `POST /reset-password`
- `POST /password/set-request`
- `POST /update-password`
- `POST /oauth2/exchange`
- `GET /.well-known/jwks.json`

## Token Response Contract

`POST /login`, `POST /refresh`, and `POST /oauth2/exchange` return:

```json
{
  "accessToken": "string",
  "accessTokenType": "Bearer",
  "accessTokenExpiresInMs": 3600000,
  "refreshToken": "string",
  "refreshTokenExpiresInMs": 1209600000
}
```

Refresh request body:

```json
{
  "refreshToken": "string"
}
```

Logout request body (device/session scoped revoke):

```json
{
  "refreshToken": "string"
}
```

## Auth Error Contract

The API returns a consistent error body:

```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message",
  "traceId": "request-correlation-id"
}
```

Trace correlation behavior:

- Request header `X-Trace-Id` is accepted and propagated to response header/body.
- If missing, the service generates a trace id and returns it in:
  - response header `X-Trace-Id`
  - error body field `traceId`
- Logs include the same trace id via MDC (`%X{traceId}`) for request/error correlation.

Auth error matrix:

- `400 BAD_REQUEST` + `BAD_REQUEST`
  - Illegal/invalid auth flow state (for example malformed refresh request in service logic)
- `400 BAD_REQUEST` + `VALIDATION_ERROR`
  - Bean validation failures (for example blank `refreshToken`)
- `401 UNAUTHORIZED` + `UNAUTHORIZED`
  - Missing authentication context (for example unauthenticated `/api/auth/me` or `/api/auth/logout` call path)
- `401 UNAUTHORIZED` + `INVALID_CREDENTIALS`
  - Login failure due to invalid username/password
- `403 FORBIDDEN` + `ACCOUNT_DISABLED`
  - Login blocked because the account is disabled
- `403 FORBIDDEN` + `ACCOUNT_LOCKED`
  - Login blocked because the account is locked
- `429 TOO_MANY_REQUESTS` + `RATE_LIMITED`
  - Login temporarily blocked after too many failed attempts
- `401 UNAUTHORIZED` + `REFRESH_TOKEN_EXPIRED`
  - Refresh token expired
- `401 UNAUTHORIZED` + `REFRESH_TOKEN_INVALID`
  - Refresh token not recognized / invalid
- `401 UNAUTHORIZED` + `REFRESH_TOKEN_REUSED`
  - Refresh token reuse detected (family revocation triggered)
- `403 FORBIDDEN` + `FORBIDDEN`
  - Authenticated but not authorized to access resource
- `403 FORBIDDEN` + `REFRESH_TOKEN_FORBIDDEN`
  - Refresh token ownership mismatch
- `400 BAD_REQUEST` + `VERIFICATION_CODE_NOT_FOUND`
  - Email verification code is missing/invalid
- `400 BAD_REQUEST` + `RESET_PASSWORD_TOKEN_NOT_FOUND`
  - Password reset token is missing/invalid
- `500 INTERNAL_SERVER_ERROR` + `INTERNAL_ERROR`
  - Unhandled server-side error

Contract verification:

- Web layer matrix tests: `src/test/java/com/serdarahmanov/music_app_backend/auth/controller/AuthControllerWebTest.java`
- Integration checks for auth flow and login credential failure contract:
  - `src/test/java/com/serdarahmanov/music_app_backend/auth/integration/AuthRefreshLifecycleIntegrationTest.java`
  - includes assertion that generated `X-Trace-Id` equals error-body `traceId`

Config:

- `JWT_EXPIRATION_MS` (access token TTL)
- `JWT_REFRESH_EXPIRATION_MS` (refresh token TTL)
- `JWT_ALLOW_EPHEMERAL_KEYS` (dev only; keep `false` in production)
- `APP_CORS_ALLOWED_ORIGINS` (comma-separated origins)
- `JOBRUNR_DASHBOARD_ENABLED` (should be `false` in production)
- `APP_REFRESH_CLEANUP_INTERVAL_MS`
- `APP_REFRESH_CLEANUP_INITIAL_DELAY_MS`
- `APP_REFRESH_CLEANUP_RETENTION_MS`
- `APP_LOGIN_MAX_FAILED_ATTEMPTS`
- `APP_LOGIN_LOCK_DURATION_MS`

Login attempt store behavior:

- Uses Redis-backed store for shared lockout state across instances.
- Startup is fail-fast if Redis is unreachable.
- Metric emitted on blocked attempts: `auth_login_rate_limited_total`.

## What Should Be Done Next (Recommended Order)

### Priority 1: Refresh token hardening

- Add stricter anomaly detection/alerts for token family reuse

### Priority 2: Authorization enforcement hardening

- Add method-level authorization (`@PreAuthorize`)
- Enforce resource ownership checks in service layer
- Standardize permission naming and checks across endpoints

### Priority 3: Security controls

- Rate limit login and forgot-password endpoints
- Add account lock/backoff for brute-force protection
- Require verified email for privileged operations
- Add optional MFA (TOTP or email OTP)

### Priority 4: Testing baseline (critical)

- Add unit tests for JWT generation/validation
- Add integration tests for full auth flows:
  - register/login/me/refresh/logout
  - forgot/reset password
  - oauth2 exchange
- Add negative-path tests (expired token, invalid kid, revoked refresh token)

### Priority 5: API quality and operational readiness

- Add OpenAPI/Swagger docs
- Standardize error response format (`code`, `message`, `traceId`)
- Add audit logging for security events (login fail/success, password reset, role updates)
- Add metrics/alerts for auth anomalies

### Priority 6: Environment and secret management (planned for later)

- Move all secrets to environment variables / secret manager
- Rotate previously exposed secrets
- Add `application-dev.yml` and `application-prod.yml` profiles

## Known Gaps Right Now

- Automated tests are still limited in coverage
- Dev fallback keypair invalidates tokens on restart
- Some security settings are development-friendly (for example local CORS)
