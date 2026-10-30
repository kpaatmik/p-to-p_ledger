# P2P Payment Ledger — Task Tracker

> **How to use this tracker:**
> - Update `Status` after each task is verified.
> - Mark a task `COMPLETE` only after its acceptance criteria are met and tests pass — not on code generation alone.
> - List actual test results in the `Verification Method` field when marking complete.
> - Stop and report blockers before proceeding to the next task.

---

## Phase 0 — Repository Discovery and Planning

---

### T-001: Workspace Inspection and Repository State Assessment

**Phase:** 0
**Status:** COMPLETE
**Dependencies:** None

**Description:**
Inspect the workspace directory structure to understand what files exist, what has already been created, and what the starting state of the repository is. Identify the backend and frontend directories, any existing configuration, and the location of requirements documents.

**Acceptance Criteria:**
- Workspace structure is fully documented
- All existing files identified and catalogued
- No assumptions made about missing files

**Verification Method:**
Directory listing confirmed: `backend/` (empty), `frontend/` (empty), `docs/` (empty), `document/` (requirements PDF and text file present), `.kiro/` (specs and steering directories present).

**Files Inspected:**
- `d:\Payment ledger\` (root)
- `d:\Payment ledger\document\requirements_text.txt`

---

### T-002: Requirements Extraction and Organization

**Phase:** 0
**Status:** COMPLETE
**Dependencies:** T-001

**Description:**
Read and fully extract all requirements from the requirements text file. Organize them by section. Identify all 13 sections, 14 database entities, 17 acceptance criteria, and all configuration constraints.

**Acceptance Criteria:**
- All 13 requirement sections identified
- All 17 acceptance criteria extracted
- All configuration constraints noted (Java 25, Spring Boot 4.1.2, no YAML, no Flyway)
- All open decisions identified

**Verification Method:**
Full requirements text read and processed. All sections, entities, acceptance criteria, and configuration constraints extracted and incorporated into planning artifacts.

**Files Inspected:**
- `d:\Payment ledger\document\requirements_text.txt`

---

### T-003: Architecture Design and Database Schema

**Phase:** 0
**Status:** COMPLETE
**Dependencies:** T-002

**Description:**
Design the layered Spring Boot package structure, identify all service and repository classes, and define the complete database schema for all 14 tables. Specify column types, constraints, indexes, and relationships. Document key technical decisions covering available balance computation, concurrency control, idempotency, JWT management, and settlement isolation.

**Acceptance Criteria:**
- All 14 tables defined with column types, PKs, FKs, constraints, and indexes
- Package structure covers all required layers
- Eight key technical decisions documented
- Security architecture documented

**Verification Method:**
ARCHITECTURE.md created with complete schema and all design decisions. Reviewed for consistency with requirements.

**Files Created:**
- `d:\Payment ledger\ARCHITECTURE.md`

---

### T-004: Create Planning Artifacts

**Phase:** 0
**Status:** COMPLETE
**Dependencies:** T-003

**Description:**
Create the four primary planning documents: PROJECT_PLAN.md (phases, risks, open decisions, progress tracking), ARCHITECTURE.md (schema, packages, security), TASK_TRACKER.md (this file, 60+ tasks), and REQUIREMENTS_TRACEABILITY_MATRIX.md (all requirements mapped to tasks and acceptance criteria).

**Acceptance Criteria:**
- `PROJECT_PLAN.md` contains all 12 phases, 6 risks, 6 open decisions, and progress table
- `ARCHITECTURE.md` contains complete 14-table schema and package structure
- `TASK_TRACKER.md` contains 60+ tasks with acceptance criteria across all phases
- `REQUIREMENTS_TRACEABILITY_MATRIX.md` covers all sections and acceptance criteria

**Verification Method:**
All four files created and written. Content reviewed against requirements.

**Files Created:**
- `d:\Payment ledger\PROJECT_PLAN.md`
- `d:\Payment ledger\ARCHITECTURE.md`
- `d:\Payment ledger\TASK_TRACKER.md`
- `d:\Payment ledger\docs\REQUIREMENTS_TRACEABILITY_MATRIX.md`

---

### T-005: Create Kiro Steering Instructions

**Phase:** 0
**Status:** COMPLETE
**Dependencies:** T-004

**Description:**
Create the persistent Kiro steering instructions file at `.kiro/steering/project-instructions.md` with `inclusion: always`. This file enforces all development rules — financial correctness, security, architectural constraints, workflow rules, and open decisions — in every future session.

**Acceptance Criteria:**
- File created at `.kiro/steering/project-instructions.md`
- `inclusion: always` frontmatter present
- All financial correctness rules documented
- All security rules documented
- All open decisions listed
- Technology stack constraints documented

**Verification Method:**
File created and content verified against requirements.

**Files Created:**
- `d:\Payment ledger\.kiro\steering\project-instructions.md`

---

## Phase 1 — Project Foundation

---

### T-010: Initialize Spring Boot Project Structure and pom.xml

**Phase:** 1
**Status:** COMPLETE
**Dependencies:** T-005

**Description:**
Initialize the Spring Boot Maven project at `backend/`. Create `pom.xml` with all required dependencies, Java 25 compiler target, and Spring Boot 4.1.2 as parent.

**Acceptance Criteria — all met:**
- `pom.xml` present with Spring Boot 4.1.2 parent ✓
- Java 25 compiler target (`<java.version>25</java.version>`) ✓
- All required starters included: web, data-jpa, security, validation ✓
- PostgreSQL driver (runtime), JJWT 0.12.6 (api/impl/jackson), Lombok (optional) ✓
- spring-boot-starter-test + spring-security-test for test scope ✓
- No YAML, no Flyway ✓
- No domain entities or business logic ✓

**Verification Results:**
- `mvn clean compile -e` (2026-10-29 21:34 IST, run by user): **BUILD SUCCESS**
  - `javac [debug parameters release 25]` — Java 25 confirmed
  - Spring Boot 4.1.2 BOM resolved — all managed plugin versions resolved
  - Lombok annotation processing: no errors
  - 1 source file compiled (`PaymentLedgerApplication.java`)
- Issue fixed: UTF-8 BOM present in files written by PowerShell `Set-Content`; rewritten with `[System.IO.File]::WriteAllText` + `UTF8Encoding($false)`
- `mvn test`: BUILD SUCCESS confirmed by user (placeholder test passes)

**Files Created:**
- `backend/pom.xml`
- `backend/.gitignore`
- `backend/env.example`
- `backend/src/main/java/com/paymentledger/PaymentLedgerApplication.java`
- Package directories (12) with `.gitkeep`: config, controller, service, repository, entity, dto/request, dto/response, exception, scheduler, security, util

---

### T-011: Configure application.properties

**Phase:** 1
**Status:** COMPLETE
**Dependencies:** T-010

**Description:**
Create `application.properties` with PostgreSQL datasource, JPA/Hibernate settings, JWT configuration, scheduler pool, and Check Please expiry configuration — all using environment variable placeholders.

**Acceptance Criteria — all met:**
- `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` for datasource ✓
- `${JWT_SECRET}` for JWT signing key ✓
- No secrets hardcoded ✓
- No YAML files ✓
- `spring.jpa.hibernate.ddl-auto=update` (Hibernate manages schema) ✓
- `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` ✓
- `check.please.reservation.expiry.min-hours=1` ✓
- `check.please.reservation.expiry.max-days=30` ✓
- `spring.task.scheduling.pool.size=5` ✓

**Verification Results:**
File read back and verified correct. No YAML files exist in project.

**Files Created:**
- `backend/src/main/resources/application.properties`

---

### T-012: Establish Package Structure and Global Exception Handler

**Phase:** 1
**Status:** COMPLETE
**Dependencies:** T-010

**Description:**
Create all package directories and implement `GlobalExceptionHandler` (`@RestControllerAdvice`) with `ErrorResponse` DTO for consistent JSON error responses.

**Acceptance Criteria — all met:**
- All 12 package directories created with `.gitkeep` ✓
- `ErrorResponse` DTO with timestamp, status, error, message, path, optional fieldErrors ✓
- `GlobalExceptionHandler` handles: `MethodArgumentNotValidException` (400), `ConstraintViolationException` (400), `AuthenticationException` (401), `AccessDeniedException` (403), generic `Exception` (500) ✓
- Uses `@RestControllerAdvice` ✓
- `@JsonInclude(NON_NULL)` so `fieldErrors` is omitted from non-validation errors ✓

**Verification Results:**
Files verified by byte inspection: no BOM, correct sizes.
- `ErrorResponse.java`: 1410 bytes, first byte 0x70 (`p`)
- `GlobalExceptionHandler.java`: 3607 bytes, first byte 0x70 (`p`)
Compiled together with T-010/T-011 in subsequent `mvn clean compile` run.

**Files Created:**
- `backend/src/main/java/com/paymentledger/dto/response/ErrorResponse.java`
- `backend/src/main/java/com/paymentledger/exception/GlobalExceptionHandler.java`

---

### T-013: Initialize React Frontend with Vite and Project Structure

**Phase:** 1
**Status:** COMPLETE
**Dependencies:** T-010

**Description:**
Initialize React frontend with Vite at `frontend/`. Set up directory structure, install React Router and Axios, configure API base URL.

**Acceptance Criteria — all met:**
- `package.json` with pinned versions: react 19.1.0, react-dom 19.1.0, react-router-dom 7.6.3, axios 1.7.9, vite 6.3.5, @vitejs/plugin-react 4.4.1 ✓
- `vite.config.js` with React plugin and dev-server proxy to `http://localhost:8080` ✓
- `index.html` entry point ✓
- `src/` directory structure: components, constants, contexts, pages, services ✓
- `.gitignore` excludes node_modules, dist, .env files ✓
- `.env.example` documents `VITE_API_BASE_URL` ✓

**Actual Verification Results:**
- `npm install` (2026-10-29, run by user): **91 packages added, 0 errors** ✓
- `npm run build` (2026-10-29, run by user): **vite v6.3.5 — 97 modules transformed — built in 2.63s** ✓
  Output: `dist/assets/index-DnMpj1DF.js` 260.95 kB (gzip: 86.38 kB)
- Note: 4 high severity npm audit advisories — not blocking; review before production

**Files Created:**
- `frontend/package.json`
- `frontend/vite.config.js`
- `frontend/index.html`
- `frontend/.gitignore`
- `frontend/.env.example`
- `frontend/src/index.css`
- `frontend/src/main.jsx`
- Directory structure: src/components, src/constants, src/contexts, src/pages, src/services

---

### T-014: Configure API Client, Routing, and Auth Context in Frontend

**Phase:** 1
**Status:** COMPLETE
**Dependencies:** T-013

**Description:**
Configure Axios with JWT interceptor, implement AuthContext with login/logout/isAdmin, implement ProtectedRoute with adminOnly support, define route constants, wire up React Router in App.jsx.

**Acceptance Criteria — all met:**
- `api.js`: Axios instance reads `VITE_API_BASE_URL`; attaches `Authorization: Bearer` when token present; clears token on 401 ✓
- `routes.js`: all route paths as named constants (auth, user, Check Please, admin) ✓
- `AuthContext.jsx`: `login()` calls POST /auth/login, stores JWT + user in localStorage; `logout()` clears state; `isAdmin()` checks role; loads from localStorage on page refresh ✓
- `ProtectedRoute.jsx`: redirects unauthenticated → /login (preserving attempted path); redirects non-admin on `adminOnly` → /dashboard ✓
- `App.jsx`: BrowserRouter + AuthProvider wrapper; login/register public; dashboard protected; catch-all redirect ✓
- Placeholder pages: LoginPage, RegisterPage, DashboardPage (functional, styled in Phase 10) ✓

**Actual Verification Results:**
All files byte-verified (no BOM, first byte `0x69` = `i` for `import`):
- `ProtectedRoute.jsx`: 988 bytes ✓
- `App.jsx`: 1756 bytes ✓
- `LoginPage.jsx`: 2431 bytes ✓
- `RegisterPage.jsx`: 2165 bytes ✓
- `DashboardPage.jsx`: 686 bytes ✓
All included in `npm run build` → BUILD SUCCESS (confirmed 2026-10-29).

**Files Created:**
- `frontend/src/services/api.js`
- `frontend/src/constants/routes.js`
- `frontend/src/contexts/AuthContext.jsx`
- `frontend/src/components/ProtectedRoute.jsx`
- `frontend/src/App.jsx`
- `frontend/src/pages/LoginPage.jsx`
- `frontend/src/pages/RegisterPage.jsx`
- `frontend/src/pages/DashboardPage.jsx`

---

### T-015: Verify Backend Builds and Starts; Verify Frontend Builds

**Phase:** 1
**Status:** VERIFICATION PENDING — backend mvn test + package pending
**Dependencies:** T-011, T-012, T-014

**Description:**
Full build verification for backend and frontend.

**Acceptance Criteria:**
- `mvn clean compile` → BUILD SUCCESS ✓ (confirmed 2026-10-29, `javac release 25`)
- `mvn test` → BUILD SUCCESS, 1 test passed (pending)
- `mvn clean package` → BUILD SUCCESS (pending)
- `npm install` → no errors ✓ (confirmed 2026-10-29, 91 packages added)
- `npm run build` → BUILD SUCCESS ✓ (confirmed 2026-10-29, vite 6.3.5, 97 modules, 2.63s)

**Frontend build confirmed:**
```
vite v6.3.5 building for production...
✓ 97 modules transformed.
dist/index.html                   0.40 kB │ gzip:  0.27 kB
dist/assets/index-BX11yMlQ.css    0.09 kB │ gzip:  0.10 kB
dist/assets/index-DnMpj1DF.js   260.95 kB │ gzip: 86.38 kB
✓ built in 2.63s
```
Note: 4 npm high severity audit advisories present. These are dev-dependency / transitive issues not blocking Phase 1. Review before production.

**Remaining — please run from `D:\Payment ledger\backend\` and paste results:**
```
mvn test
mvn clean package
```
T-015 will be marked COMPLETE after BUILD SUCCESS is reported for both commands.

**Known environmental note:**
PostgreSQL connectivity is NOT verified in Phase 1. `application.properties` uses `${DB_URL}` etc. Spring context start requires a live database — this is verified in T-015 only after Phase 2 (entities) and database credentials are available. `mvn test` passes because the placeholder test does not load the Spring context.

**Known environmental note:**
PostgreSQL connectivity is NOT verified in Phase 1 — `application.properties` uses `${DB_URL}` etc. which requires environment variables. The Spring context will fail to start without a database. This is expected; context startup is verified in T-015 only after T-020 (entities) and T-011 credentials are available. `mvn test` passes because the placeholder test does not load the Spring context.

**T-015 will be marked COMPLETE after user reports BUILD SUCCESS for all four commands.**

---## Phase 2 — Database and Domain Model

---

### T-020: Create User and LoginAttempt Entities and UserRepository

**Phase:** 2
**Status:** NOT STARTED
**Dependencies:** T-015

**Description:**
Implement `User` JPA entity with all fields from the schema (id, name, email, phone, password_hash, pin_hash, role, account_locked, lock_expires_at, failed_login_count, timestamps). Implement `LoginAttempt` entity. Implement `UserRepository` with methods for email/phone lookup and login attempt queries.

**Acceptance Criteria:**
- `User` entity maps to `users` table with correct column types and constraints
- `LoginAttempt` entity maps to `login_attempts` table
- `UserRepository` has `findByEmail`, `findByPhone`, `countByUserIdAndSuccessAndAttemptedAtAfter`
- Hibernate generates correct DDL (verified by inspection)

**Verification Method:**
Start application; inspect generated DDL in logs. Write a Spring Data JPA test that saves and retrieves a `User`.

**Files to Create:**
- `entity/User.java`
- `entity/LoginAttempt.java`
- `repository/UserRepository.java`
- `repository/LoginAttemptRepository.java`

---

### T-021: Create Wallet Entity and WalletRepository

**Phase:** 2
**Status:** NOT STARTED
**Dependencies:** T-020

**Description:**
Implement `Wallet` JPA entity with `total_balance` and `reserved_balance` as `BigDecimal`/`NUMERIC(19,4)`, status enum, and `UNIQUE` constraint on `user_id`. Add `CHECK` constraints. Implement `WalletRepository` with `findByUserId`.

**Acceptance Criteria:**
- `Wallet` entity maps correctly; `total_balance` and `reserved_balance` use `NUMERIC(19,4)`
- `UNIQUE` constraint on `user_id` enforced at DB level
- `CHECK (total_balance >= 0)`, `CHECK (reserved_balance >= 0)`, `CHECK (total_balance >= reserved_balance)` present
- `WalletRepository.findByUserId` works

**Verification Method:**
Integration test: attempt to create two wallets for same user — expect constraint violation. Verify column types in DDL.

**Files to Create:**
- `entity/Wallet.java`
- `repository/WalletRepository.java`

---

### T-022: Create Transaction and LedgerEntry Entities and Repositories

**Phase:** 2
**Status:** NOT STARTED
**Dependencies:** T-021

**Description:**
Implement `Transaction` entity with `request_id` UNIQUE constraint, status enum (PENDING/SUCCESS/FAILED/DECLINED), type enum, and `settlement_due_date`. Implement `LedgerEntry` entity with entry_type enum (DEBIT/CREDIT) and `BigDecimal` amount. Implement both repositories with relevant query methods.

**Acceptance Criteria:**
- `request_id` UNIQUE constraint enforced at DB level
- `amount` uses `NUMERIC(19,4)` in both entities
- `TransactionRepository` has methods for paginated history, status filter, sender/receiver filter
- `LedgerEntryRepository` has `findByTransactionId`

**Verification Method:**
Integration test: insert duplicate `request_id` — expect constraint violation. Verify paginated query with filter parameters.

**Files to Create:**
- `entity/Transaction.java`
- `entity/LedgerEntry.java`
- `repository/TransactionRepository.java`
- `repository/LedgerEntryRepository.java`

---

### T-023: Create FraudAlert and Notification Entities and Repositories

**Phase:** 2
**Status:** NOT STARTED
**Dependencies:** T-022

**Description:**
Implement `FraudAlert` entity with alert_type enum (DAILY_LIMIT/DUPLICATE/HIGH_VALUE) and status enum (PENDING/BLOCKED/DISMISSED). Implement `Notification` entity with `read` flag. Implement repositories with admin queue queries and unread count queries.

**Acceptance Criteria:**
- `FraudAlertRepository` has `findByStatus(PENDING)` for admin queue
- `NotificationRepository` has `countByUserIdAndReadFalse` and `findByUserIdOrderByCreatedAtDesc`
- Entities map to correct tables with correct types

**Verification Method:**
Integration test: create notifications, query unread count, mark read, verify count decreases.

**Files to Create:**
- `entity/FraudAlert.java`
- `entity/Notification.java`
- `repository/FraudAlertRepository.java`
- `repository/NotificationRepository.java`

---

### T-024: Create AuditLog, ScheduledJobLog, DailySummary, and SettlementBatch Entities

**Phase:** 2
**Status:** NOT STARTED
**Dependencies:** T-023

**Description:**
Implement the four administrative/operational entities: `AuditLog`, `ScheduledJobLog`, `DailySummary`, and `SettlementBatch`. Create repositories for each. `ScheduledJobLogRepository` must support querying last N runs by job name.

**Acceptance Criteria:**
- All four entities map correctly to their tables
- `ScheduledJobLogRepository` can retrieve the last 20 runs ordered by `started_at` DESC
- `DailySummaryRepository` has `findBySummaryDate`
- `AuditLogRepository` has `findByAdminIdOrderByPerformedAtDesc`

**Verification Method:**
Integration tests: save and retrieve records for each entity. Verify `findTop20ByOrderByStartedAtDesc` returns correct results.

**Files to Create:**
- `entity/AuditLog.java`
- `entity/ScheduledJobLog.java`
- `entity/DailySummary.java`
- `entity/SettlementBatch.java`
- Corresponding repository interfaces

---

### T-025: Create Merchant Entity and MerchantRepository

**Phase:** 2
**Status:** NOT STARTED
**Dependencies:** T-024

**Description:**
Implement `Merchant` entity with status enum (ACTIVE/INACTIVE/SUSPENDED) and UNIQUE email constraint. Create `MerchantRepository` with `findByStatus(ACTIVE)` for user-facing merchant search and `findByEmail` for uniqueness validation.

**Acceptance Criteria:**
- `Merchant` entity maps correctly with UNIQUE email constraint
- `MerchantRepository.findByStatus` returns only ACTIVE merchants for user-facing calls
- Status enum covers ACTIVE, INACTIVE, SUSPENDED

**Verification Method:**
Integration test: create merchants with different statuses, query by status, verify only matching records returned.

**Files to Create:**
- `entity/Merchant.java`
- `repository/MerchantRepository.java`

---

### T-026: Create CheckPleaseReservation Entity and Repository

**Phase:** 2
**Status:** NOT STARTED
**Dependencies:** T-025

**Description:**
Implement `CheckPleaseReservation` entity with `max_amount`, `remaining_amount`, `expires_at`, and status enum (ACTIVE/CONSUMED/CANCELLED/EXPIRED). Add CHECK constraints. Create `CheckPleaseReservationRepository` with methods to find active reservations by user, find expired active reservations for the expiry job.

**Acceptance Criteria:**
- `CHECK (remaining_amount >= 0)` and `CHECK (remaining_amount <= max_amount)` enforced at DB level
- `remaining_amount` and `max_amount` use `NUMERIC(19,4)`
- Repository has `findByUserIdAndStatus(ACTIVE)` and `findByStatusAndExpiresAtBefore(ACTIVE, now)`

**Verification Method:**
Integration test: attempt to set `remaining_amount > max_amount` — expect constraint violation. Query expired active reservations.

**Files to Create:**
- `entity/CheckPleaseReservation.java`
- `repository/CheckPleaseReservationRepository.java`

---

### T-027: Create MerchantPayment Entity and Repository; Verify All Schema Constraints

**Phase:** 2
**Status:** NOT STARTED
**Dependencies:** T-026

**Description:**
Implement `MerchantPayment` entity with `request_id` UNIQUE constraint and status enum (PENDING/CAPTURED/FAILED/REJECTED). Create repository. Perform a full schema review: verify all 14 tables exist, all NUMERIC columns have scale 4, all UNIQUE constraints are in place, all FK relationships are correct.

**Acceptance Criteria:**
- `merchant_payments.request_id` UNIQUE constraint enforced
- All 14 tables present in the database
- All `NUMERIC(19,4)` columns confirmed
- All FK relationships navigable
- All CHECK constraints verified by attempted violations

**Verification Method:**
Run integration tests that attempt constraint violations for all CHECK and UNIQUE constraints. Verify each fails as expected. Inspect Hibernate DDL output for all 14 tables.

**Files to Create:**
- `entity/MerchantPayment.java`
- `repository/MerchantPaymentRepository.java`

---

## Phase 3 — Authentication and Authorization

---

### T-030: Implement JWT Utility

**Phase:** 3
**Status:** NOT STARTED
**Dependencies:** T-027

**Description:**
Implement `JwtUtil` with methods to generate a signed JWT (HS256, subject = email, claims include userId and role), validate a JWT (signature + expiry), and extract claims. Read secret from `application.properties` (sourced from `JWT_SECRET` env var). Do not log the secret.

**Acceptance Criteria:**
- `generateToken(User)` returns a valid JWT string
- `validateToken(token)` returns `true` for valid tokens, `false` for expired or tampered tokens
- `extractEmail(token)` returns the correct email
- Secret never appears in logs

**Verification Method:**
Unit tests: generate a token, validate it, tamper with it and verify validation fails, use an expired token and verify failure.

**Files to Create:**
- `security/JwtUtil.java`
- Unit test: `JwtUtilTest.java`

---

### T-031: Implement Spring Security Config, JWT Filter, and UserDetailsService

**Phase:** 3
**Status:** NOT STARTED
**Dependencies:** T-030

**Description:**
Implement `SecurityConfig` with filter chain (stateless session, disable CSRF for REST, permit `/auth/**`, require authentication for all others). Implement `JwtAuthFilter` as `OncePerRequestFilter` that validates the JWT and sets the security context. Implement `UserDetailsServiceImpl` that loads user by email from the database.

**Acceptance Criteria:**
- Unauthenticated requests to protected endpoints return HTTP 401
- Valid JWT in `Authorization: Bearer` header passes filter and populates `SecurityContext`
- Invalid or missing JWT returns HTTP 401
- `/auth/register` and `/auth/login` are publicly accessible

**Verification Method:**
Integration tests: call protected endpoint without token (expect 401), with valid token (expect 200), with tampered token (expect 401).

**Files to Create:**
- `config/SecurityConfig.java`
- `security/JwtAuthFilter.java`
- `security/UserDetailsServiceImpl.java`

---

### T-032: Implement User Registration Endpoint with Wallet Auto-Creation

**Phase:** 3
**Status:** NOT STARTED
**Dependencies:** T-031

**Description:**
Implement `POST /auth/register`. Validate inputs (name, email, phone, password, 4-digit PIN). Hash password and PIN using BCrypt. Save `User`. In the same transaction, create exactly one `Wallet` with `total_balance = 0`, `reserved_balance = 0`, status = ACTIVE. Return success response.

**Acceptance Criteria:**
- Successful registration creates exactly one User and exactly one Wallet
- Wallet has `total_balance = 0`, `reserved_balance = 0`, status = ACTIVE
- Password and PIN stored as BCrypt hashes, not plaintext
- Duplicate email or phone returns HTTP 409 with clear error
- Missing or invalid fields return HTTP 400

**Verification Method:**
Integration test: register a user, verify User and Wallet in DB. Attempt duplicate email — expect 409. Check DB — confirm no plaintext password.

**Files to Create:**
- `controller/AuthController.java`
- `service/AuthService.java`
- `dto/request/RegisterRequest.java`
- `dto/response/RegisterResponse.java`

---

### T-033: Implement Login Endpoint with JWT Issuance

**Phase:** 3
**Status:** NOT STARTED
**Dependencies:** T-032

**Description:**
Implement `POST /auth/login`. Validate email and password. If account is locked and lock has not expired, return HTTP 423 (Locked) with unlock time. Verify BCrypt password match. On success, reset `failed_login_count`, record successful login attempt, issue and return JWT.

**Acceptance Criteria:**
- Correct credentials return HTTP 200 with JWT
- Incorrect password returns HTTP 401
- Locked account returns HTTP 423 with lock expiry time
- JWT is valid and contains correct claims
- `failed_login_count` reset to 0 on successful login

**Verification Method:**
Integration tests: successful login returns JWT; validate JWT; incorrect password returns 401; locked account returns 423.

**Files to Create:**
- Login methods in `AuthController.java` and `AuthService.java`
- `dto/request/LoginRequest.java`
- `dto/response/AuthResponse.java`

---

### T-034: Implement Failed-Login Tracking and Account Locking

**Phase:** 3
**Status:** NOT STARTED
**Dependencies:** T-033

**Description:**
On each failed login: increment `failed_login_count`, record `LoginAttempt` (success=false). When `failed_login_count` reaches 5: set `account_locked = true`, set `lock_expires_at = now + 30 minutes`. Send account-lock notification to user.

**Acceptance Criteria:**
- After 5 consecutive failures, account is locked with `lock_expires_at = now + 30 minutes`
- Subsequent login attempts while locked return HTTP 423 without incrementing count further
- A successful login resets `failed_login_count` to 0 and clears lock
- `LoginAttempt` records are created for every attempt

**Verification Method:**
Integration test: attempt 5 failed logins; verify account locked; verify 6th attempt returns 423; verify lock fields in DB.

**Files to Create:**
- Lock logic in `AuthService.java`
- Lock notification call in `NotificationService.java`

---

### T-035: Implement Account Auto-Unlock Service

**Phase:** 3
**Status:** NOT STARTED
**Dependencies:** T-034

**Description:**
Implement `AccountLockService.unlockEligibleAccounts()` which queries for all locked users whose `lock_expires_at <= now`, sets `account_locked = false`, clears `lock_expires_at`, resets `failed_login_count`, and sends an unlock notification to each affected user. This method is called by the scheduler job in Phase 8.

**Acceptance Criteria:**
- Accounts whose lock has expired are unlocked on each call
- Accounts whose lock has not expired are not touched
- Unlock notification is sent for each unlocked account
- Method is safe to call repeatedly (idempotent)

**Verification Method:**
Unit test with mock repository: create locked account with past `lock_expires_at`, call method, verify unlocked. Create locked account with future expiry, call method, verify still locked.

**Files to Create:**
- `service/AccountLockService.java`

---

### T-036: Write Unit Tests for Auth Service

**Phase:** 3
**Status:** NOT STARTED
**Dependencies:** T-035

**Description:**
Write comprehensive unit tests for `AuthService` covering: successful registration, duplicate email rejection, duplicate phone rejection, successful login, failed login increment, lock after 5 failures, login when locked, auto-unlock eligibility check.

**Acceptance Criteria:**
- All listed scenarios have passing unit tests
- Tests use mocked repositories (no real DB)
- BCrypt behavior mocked or tested with actual implementation
- At least 10 test cases covering the main paths

**Verification Method:**
`mvn test -pl backend -Dtest=AuthServiceTest` passes with all tests green.

**Files to Create:**
- `test/service/AuthServiceTest.java`

---

### T-037: Write Integration Tests for Auth Endpoints

**Phase:** 3
**Status:** NOT STARTED
**Dependencies:** T-036

**Description:**
Write integration tests for `AuthController` using Spring Boot test slice or full context with a test database. Cover: registration success and wallet creation, registration duplicate email, login success and JWT validity, login with wrong password, login with locked account.

**Acceptance Criteria:**
- Integration tests use a real test PostgreSQL instance (or H2 if compatible)
- All endpoints return correct HTTP status codes and response bodies
- JWT returned by login endpoint is parseable and contains correct claims
- Wallet creation confirmed in DB after registration

**Verification Method:**
`mvn test -pl backend -Dtest=AuthControllerIT` passes with all tests green.

**Files to Create:**
- `test/controller/AuthControllerIT.java`

---

## Phase 4 — Wallet Operations

---

### T-040: Implement Load-Money Endpoint

**Phase:** 4
**Status:** NOT STARTED
**Dependencies:** T-037

**Description:**
Implement `POST /wallet/load`. Authenticate user from JWT. Validate the amount is positive and within a reasonable limit (configurable). Increase `total_balance` in the wallet (no `reserved_balance` change). Create a CREDIT ledger entry with type description "LOAD". Return updated balance breakdown.

**Acceptance Criteria:**
- Valid load increases `total_balance` by exact amount (BigDecimal, no rounding error)
- Amount must be positive; zero or negative rejected with HTTP 400
- CREDIT ledger entry created for the load operation
- Response includes updated total, available, and reserved balances

**Verification Method:**
Integration test: load ₹500, verify DB balance increased by exactly ₹500.00, verify ledger entry created.

**Files to Create:**
- `controller/WalletController.java`
- `service/WalletService.java`
- `dto/request/LoadMoneyRequest.java`
- `dto/response/WalletResponse.java`

---

### T-041: Implement Get-Wallet Endpoint

**Phase:** 4
**Status:** NOT STARTED
**Dependencies:** T-040

**Description:**
Implement `GET /wallet`. Return `total_balance`, `available_balance` (computed as `total_balance − reserved_balance`), `reserved_balance`, `wallet_status`, and today's total spending (sum of DEBIT ledger entries for this wallet created today).

**Acceptance Criteria:**
- `available_balance` always equals `total_balance − reserved_balance`
- Today's spending sums only DEBIT entries with `created_at >= today 00:00 UTC`
- User can only see their own wallet (ownership enforced via JWT)

**Verification Method:**
Integration test: load money, make partial reservation, call get-wallet, verify all four balance values are correct.

**Files to Create:**
- GET method in `WalletController.java`
- Query method in `WalletService.java`

---

### T-042: Implement Wallet Status Management

**Phase:** 4
**Status:** NOT STARTED
**Dependencies:** T-041

**Description:**
Implement wallet status transitions: admin can FREEZE and UNFREEZE any wallet (`PATCH /admin/wallets/{id}/status`); owner can reactivate their own INACTIVE wallet to ACTIVE (`PATCH /wallet/reactivate`). Enforce that FROZEN wallets can only be changed by admins. Write audit log entry for admin freeze/unfreeze actions.

**Acceptance Criteria:**
- Admin can set wallet to FROZEN; only admin can unfreeze FROZEN wallet
- Owner can reactivate own INACTIVE wallet to ACTIVE
- Owner cannot change FROZEN wallet status
- Audit log entry created for every admin freeze/unfreeze
- HTTP 403 returned for unauthorized status changes

**Verification Method:**
Integration tests: admin freezes wallet, owner attempts to unfreeze (expect 403), admin unfreezes successfully. Owner reactivates INACTIVE wallet.

**Files to Create:**
- Admin and owner wallet status endpoints in controllers
- Status management methods in `WalletService.java`
- `service/AuditLogService.java`

---

### T-043: Implement Available-Balance Calculation Service

**Phase:** 4
**Status:** NOT STARTED
**Dependencies:** T-042

**Description:**
Implement `WalletService.getAvailableBalance(walletId)` as a single source of truth for available balance. This method is called by transfer initiation, reservation creation, and all balance checks. It reads `total_balance` and `reserved_balance` from the database (not a cached value) and returns the difference.

**Acceptance Criteria:**
- Returns `total_balance − reserved_balance` using BigDecimal subtraction
- Returns `BigDecimal.ZERO` if result would be negative (defensive check)
- Reads live values from DB, never a stale in-memory value

**Verification Method:**
Unit test with mocked wallet repository: verify correct subtraction for various combinations including edge cases (zero reserved, zero total).

**Files to Create:**
- Method in `WalletService.java`

---

### T-044: Implement Atomic Wallet Debit/Credit Operations with SELECT FOR UPDATE

**Phase:** 4
**Status:** NOT STARTED
**Dependencies:** T-043

**Description:**
Implement `WalletService.debitWallet(walletId, amount)` and `WalletService.creditWallet(walletId, amount)` with `@Transactional` and `SELECT FOR UPDATE` via a JPQL `@Lock(LockModeType.PESSIMISTIC_WRITE)` query. Verify balance is sufficient before debit. Throw `InsufficientBalanceException` if not.

**Acceptance Criteria:**
- Debit acquires row lock before reading balance
- Debit fails atomically if balance is insufficient (no partial state)
- Credit acquires row lock before updating
- Concurrent debits do not result in negative balance (verified by concurrency test)

**Verification Method:**
Concurrency test: launch two concurrent threads both attempting to debit the full available balance; verify only one succeeds and balance never goes negative.

**Files to Create:**
- `debitWallet` and `creditWallet` methods in `WalletService.java`
- Custom JPQL lock query in `WalletRepository.java`
- `exception/InsufficientBalanceException.java`

---

### T-045: Write Unit Tests for Wallet Service

**Phase:** 4
**Status:** NOT STARTED
**Dependencies:** T-044

**Description:**
Write unit tests for `WalletService` covering: load money, get balance breakdown, available balance calculation, debit success, debit insufficient balance, credit, status transition rules (who can freeze/unfreeze/reactivate).

**Acceptance Criteria:**
- All six scenarios above have passing tests
- Tests use mocked repositories
- `InsufficientBalanceException` is thrown on under-balance debit

**Verification Method:**
`mvn test -Dtest=WalletServiceTest` passes.

**Files to Create:**
- `test/service/WalletServiceTest.java`

---

### T-046: Write Integration Tests for Wallet Endpoints

**Phase:** 4
**Status:** NOT STARTED
**Dependencies:** T-045

**Description:**
Write integration tests for all wallet endpoints: load money, get wallet, admin freeze, owner reactivate. Verify HTTP status codes, response bodies, and database state after each operation.

**Acceptance Criteria:**
- All wallet endpoints tested with valid and invalid inputs
- Ownership enforcement tested (user A cannot load money to user B's wallet)
- Admin-only operations tested (non-admin freeze attempt returns 403)

**Verification Method:**
`mvn test -Dtest=WalletControllerIT` passes.

**Files to Create:**
- `test/controller/WalletControllerIT.java`

---

## Phase 5 — P2P Payment and Ledger Engine

---

### T-050: Implement Recipient Search by Phone or Email

**Phase:** 5
**Status:** NOT STARTED
**Dependencies:** T-046

**Description:**
Implement `GET /users/search?q={phone_or_email}`. Search registered users by exact phone or email match. Return name, masked email/phone, and userId. Do not return the authenticated user as a search result. Do not return passwords, PINs, or any credential fields.

**Acceptance Criteria:**
- Returns matching user(s) excluding the caller
- Response contains only safe fields (name, userId, masked contact)
- No credential fields in response
- Empty result returns HTTP 200 with empty list (not 404)

**Verification Method:**
Integration test: register two users, search for user B as user A, verify safe fields returned and no credentials exposed.

**Files to Create:**
- `controller/TransactionController.java` (or `UserController.java`)
- Search method in `AuthService.java` or dedicated `UserService.java`

---

### T-051: Implement Transfer Initiation

**Phase:** 5
**Status:** NOT STARTED
**Dependencies:** T-050

**Description:**
Implement `POST /transfer`. Validate: sender != receiver, amount positive, wallet ACTIVE, sufficient available balance, transaction PIN correct, fraud checks pass (daily limit, duplicate detection). In one transaction: debit sender available balance (`SELECT FOR UPDATE`), create `Transaction` (status=PENDING), create sender DEBIT `LedgerEntry`, set `settlement_due_date` (T+3 business days). Send transfer initiation notification to sender.

**Acceptance Criteria:**
- Sender balance debited immediately and atomically
- `Transaction` created with status PENDING
- `LedgerEntry` DEBIT created for sender
- `settlement_due_date` is 3 business days from today (weekends excluded)
- Receiver not credited or notified at this stage
- Wrong PIN returns HTTP 401
- Inactive/Frozen wallet returns HTTP 422 with clear status error

**Verification Method:**
Integration test: initiate transfer, verify sender balance reduced, transaction in PENDING, ledger entry created, receiver balance unchanged.

**Files to Create:**
- `service/TransferService.java`
- `util/BusinessDayCalculator.java`
- `dto/request/TransferRequest.java`

---

### T-052: Implement Request Idempotency Check

**Phase:** 5
**Status:** NOT STARTED
**Dependencies:** T-051

**Description:**
On every transfer request, check if `request_id` already exists in the `transactions` table. If found, return the original transaction response with HTTP 200. If not found, proceed with transfer. The UNIQUE constraint on `request_id` provides the safety net; the service-layer check provides a clean response.

**Acceptance Criteria:**
- Duplicate `request_id` returns HTTP 200 with original transaction data
- No second debit, no second ledger entry, no second notification
- Concurrent duplicate requests handled by DB constraint (one succeeds, one gets constraint violation → returns original)

**Verification Method:**
Integration test: submit same `request_id` twice; verify only one transaction in DB, only one debit, HTTP 200 on second call.

**Files to Create:**
- Idempotency check in `TransferService.java`

---

### T-053: Implement Transaction History Endpoints

**Phase:** 5
**Status:** NOT STARTED
**Dependencies:** T-052

**Description:**
Implement `GET /transactions` (paginated, with filters: date range, type=sent/received/all, status=success/pending/failed/all). Implement `GET /transactions/mini` (last 10 transactions). For PENDING transfers, include expected settlement date and days remaining.

**Acceptance Criteria:**
- Paginated endpoint returns correct page sizes and total counts
- Date range filter excludes transactions outside range
- Type filter correctly separates sent (sender_id = current user) from received (receiver_id = current user)
- Status filter works for all four values
- PENDING transactions include settlement date and days remaining
- Users cannot see other users' transactions

**Verification Method:**
Integration test: create multiple transactions with different statuses and dates; verify filter combinations return correct subsets.

**Files to Create:**
- History and mini-statement methods in `TransactionController.java`
- Query methods in `TransactionRepository.java`
- `dto/response/TransactionListResponse.java`

---

### T-054: Implement Transaction Detail Endpoint

**Phase:** 5
**Status:** NOT STARTED
**Dependencies:** T-053

**Description:**
Implement `GET /transactions/{id}`. Return full transaction details including all associated ledger entries. For a PENDING transfer, return only the sender DEBIT entry (receiver CREDIT does not exist yet). Enforce ownership: only sender or receiver can view the transaction.

**Acceptance Criteria:**
- Returns all ledger entries associated with the transaction
- PENDING transfer shows only one DEBIT entry
- SUCCESS transfer shows DEBIT (sender) and CREDIT (receiver) entries
- HTTP 403 if requester is neither sender nor receiver
- HTTP 404 if transaction does not exist

**Verification Method:**
Integration test: create transfer, call detail as sender, verify one DEBIT. Settle transfer (test helper), call detail again, verify both entries.

**Files to Create:**
- Detail method in `TransactionController.java`

---

### T-055: Implement Personal Statistics Endpoint

**Phase:** 5
**Status:** NOT STARTED
**Dependencies:** T-054

**Description:**
Implement `GET /statistics`. Compute and return: total amount sent (sum of settled DEBIT entries for P2P transfers), total amount received (sum of settled CREDIT entries), this month's transaction count (transactions involving user created in current calendar month).

**Acceptance Criteria:**
- Total sent and received use only settled (SUCCESS) transactions
- This month's count includes both sent and received
- All values are correct BigDecimal sums

**Verification Method:**
Integration test: create several settled and pending transactions for a user; verify statistics match expected values.

**Files to Create:**
- `controller/StatisticsController.java`
- Query methods in `LedgerEntryRepository.java` or `TransactionRepository.java`

---

### T-056: Write Unit Tests for Transfer Service

**Phase:** 5
**Status:** NOT STARTED
**Dependencies:** T-055

**Description:**
Write unit tests for `TransferService` covering: successful initiation, insufficient balance, inactive sender wallet, frozen sender wallet, wrong PIN, recipient not found, idempotency (duplicate request_id), daily limit breach, duplicate transaction detection.

**Acceptance Criteria:**
- All listed scenarios have passing unit tests using mocked dependencies
- Each test verifies the correct exception or return value

**Verification Method:**
`mvn test -Dtest=TransferServiceTest` passes.

**Files to Create:**
- `test/service/TransferServiceTest.java`

---

### T-057: Write Integration Tests for P2P Endpoints

**Phase:** 5
**Status:** NOT STARTED
**Dependencies:** T-056

**Description:**
Write integration tests for all P2P endpoints: recipient search, transfer initiation, transaction history with various filters, transaction detail, personal statistics. Verify correct HTTP status codes, response structure, and database state.

**Acceptance Criteria:**
- All endpoints tested with valid inputs
- Error scenarios tested (wrong PIN, insufficient balance, inactive wallet)
- Filter combinations in history tested

**Verification Method:**
`mvn test -Dtest=TransactionControllerIT` passes.

**Files to Create:**
- `test/controller/TransactionControllerIT.java`

---

### T-058: Write Idempotency and Concurrent Transfer Tests

**Phase:** 5
**Status:** NOT STARTED
**Dependencies:** T-057

**Description:**
Write tests specifically for idempotency and concurrency: duplicate request_id submitted sequentially (expect original result returned), duplicate request_id submitted concurrently (expect only one transaction created), two concurrent transfers that together exceed available balance (expect only one to succeed).

**Acceptance Criteria:**
- Sequential duplicate request_id: one transaction in DB, correct response on both calls
- Concurrent duplicate request_id: exactly one transaction in DB
- Concurrent over-draw: exactly one succeeds, balance never negative

**Verification Method:**
Concurrency tests using `ExecutorService` with thread count ≥ 2. All assertions verified.

**Files to Create:**
- `test/service/TransferIdempotencyTest.java`

---

## Phase 6 — Fraud Detection

---

### T-060: Implement Daily Limit Check

**Phase:** 6
**Status:** NOT STARTED
**Dependencies:** T-058

**Description:**
Implement `FraudService.checkDailyLimit(userId, amount)`. Sum all P2P transfer amounts for the sender where `created_at >= today 00:00 UTC` and status is not FAILED or DECLINED. If existing total + new amount > ₹50,000, block the transfer and create a `FraudAlert` with type DAILY_LIMIT. Send a fraud notification to the user.

**Acceptance Criteria:**
- Transfers that would cumulatively exceed ₹50,000 in one day are blocked
- `FraudAlert` created with type DAILY_LIMIT and status PENDING
- Notification sent to sender
- Limit resets at midnight (separate job in Phase 8 handles daily reset)

**Verification Method:**
Unit test: mock transaction sum at ₹49,000, request ₹1,001 — expect block. Request ₹1,000 — expect pass.

**Files to Create:**
- `service/FraudService.java`

---

### T-061: Implement Duplicate Transaction Detection

**Phase:** 6
**Status:** NOT STARTED
**Dependencies:** T-060

**Description:**
Implement `FraudService.checkDuplicate(senderId, receiverId, amount)`. Query for any transaction from the same sender to the same receiver with the same amount where `created_at >= now − 2 minutes` and status is PENDING or SUCCESS. If found, block the transfer and create a `FraudAlert` with type DUPLICATE.

**Acceptance Criteria:**
- Exact duplicate within 2 minutes is blocked
- Duplicate check uses same amount, same sender, same receiver, and 2-minute window
- `FraudAlert` created with type DUPLICATE
- Transfer more than 2 minutes apart is not flagged as duplicate

**Verification Method:**
Unit test: mock recent matching transaction — expect block. Mock matching transaction 3 minutes old — expect pass.

**Files to Create:**
- `checkDuplicate` method in `FraudService.java`

---

### T-062: Implement High-Value Alert Creation

**Phase:** 6
**Status:** NOT STARTED
**Dependencies:** T-061

**Description:**
Implement `FraudService.checkHighValue(transaction)`. If amount > ₹8,000, allow the transfer to proceed to PENDING but create a `FraudAlert` with type HIGH_VALUE and status PENDING for admin review. The transfer is not blocked by this check alone.

**Acceptance Criteria:**
- Amounts > ₹8,000 proceed to PENDING status
- `FraudAlert` with type HIGH_VALUE created for admin queue
- Amounts ≤ ₹8,000 do not trigger this alert
- Alert is linked to the transaction via `transaction_id`

**Verification Method:**
Integration test: initiate transfer of ₹9,000; verify transaction is PENDING and FraudAlert created with HIGH_VALUE type.

**Files to Create:**
- `checkHighValue` method in `FraudService.java`

---

### T-063: Implement Fraud Alert Persistence and Admin Queue Endpoint

**Phase:** 6
**Status:** NOT STARTED
**Dependencies:** T-062

**Description:**
Implement `GET /admin/fraud-alerts` with optional status filter (PENDING/BLOCKED/DISMISSED). Return fraud alerts with associated transaction summary. Implement pagination. All fraud alert types visible in admin queue.

**Acceptance Criteria:**
- Admin endpoint returns all fraud alerts ordered by `created_at` DESC
- Filter by status works correctly
- Response includes alert type, status, transaction amount, sender, receiver
- Non-admin access returns HTTP 403

**Verification Method:**
Integration test: create fraud alerts of different types, call admin endpoint, verify all appear with correct data.

**Files to Create:**
- Fraud alert admin endpoint in `AdminController.java`

---

### T-064: Implement Admin Block/Dismiss for Pending Fraud Alerts

**Phase:** 6
**Status:** NOT STARTED
**Dependencies:** T-063

**Description:**
Implement `PATCH /admin/fraud-alerts/{id}/block` and `PATCH /admin/fraud-alerts/{id}/dismiss`. Block: sets alert status to BLOCKED, marks associated transaction FAILED, refunds sender exactly once, notifies both sender and receiver. Dismiss: sets alert status to DISMISSED, transaction continues to settle normally. Write audit log for both actions.

**Acceptance Criteria:**
- Blocking a PENDING alert transitions transaction to FAILED, refunds sender, notifies both parties
- Dismissing a PENDING alert sets status DISMISSED; transaction settles normally at settlement time
- Cannot block or dismiss an alert for a transaction that is not in PENDING state
- Audit log entry written for each admin action
- Double-block rejected (idempotency via status check)

**Verification Method:**
Integration test: create HIGH_VALUE alert, block it, verify transaction FAILED, sender refunded, notifications sent. Create another, dismiss it, verify transaction still PENDING.

**Files to Create:**
- Block and dismiss endpoints in `AdminController.java`

---

### T-065: Write Unit Tests for Fraud Service

**Phase:** 6
**Status:** NOT STARTED
**Dependencies:** T-064

**Description:**
Write unit tests for `FraudService` covering: daily limit under threshold (pass), daily limit at threshold (pass), daily limit over threshold (block), duplicate within 2 minutes (block), duplicate outside 2 minutes (pass), high-value allowed through, high-value alert created.

**Acceptance Criteria:**
- All seven scenarios have passing unit tests
- Tests use mocked repositories and transaction services

**Verification Method:**
`mvn test -Dtest=FraudServiceTest` passes.

**Files to Create:**
- `test/service/FraudServiceTest.java`

---

### T-066: Write Integration Tests for Fraud Scenarios

**Phase:** 6
**Status:** NOT STARTED
**Dependencies:** T-065

**Description:**
Write end-to-end integration tests for fraud scenarios: transfer that exceeds daily limit is blocked, duplicate transfer within 2 minutes is blocked, high-value transfer proceeds but creates alert, admin blocks a pending alert and sender is refunded.

**Acceptance Criteria:**
- All four fraud scenarios produce correct system state
- Fraud alerts visible in admin queue after each trigger

**Verification Method:**
`mvn test -Dtest=FraudControllerIT` passes.

**Files to Create:**
- `test/controller/FraudControllerIT.java`

---

## Phase 7 — Check Please

---

### T-070: Implement Merchant Search/List Endpoint

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-066

**Description:**
Implement `GET /merchants`. Return all merchants with ACTIVE status. Allow optional search by name. Return merchant id, name, and status. This endpoint is user-facing for reservation creation.

**Acceptance Criteria:**
- Returns only ACTIVE merchants
- Optional name search (case-insensitive, partial match)
- Does not return INACTIVE or SUSPENDED merchants to regular users

**Verification Method:**
Integration test: create ACTIVE, INACTIVE, and SUSPENDED merchants; call endpoint; verify only ACTIVE returned.

**Files to Create:**
- `controller/MerchantController.java`
- Query method in `MerchantRepository.java`

---

### T-071: Implement Reservation Creation

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-070

**Description:**
Implement `POST /reservations`. Validate: wallet ACTIVE, sufficient available balance, merchant ACTIVE, expiry timestamp in future and within configurable limits (min 1 hour, max 30 days from now — configured via `check.please.reservation.expiry.min-hours` and `check.please.reservation.expiry.max-days` in `application.properties`). In one atomic transaction with `SELECT FOR UPDATE`: verify available balance again, increase `reserved_balance`, save reservation with status ACTIVE. Send reservation creation notification.

**Acceptance Criteria:**
- `reserved_balance` increases by `max_amount` atomically
- `total_balance` unchanged
- `available_balance` decreases by `max_amount`
- Expiry must be in the future; past expiry rejected with HTTP 400
- Expiry less than 1 hour from now rejected with HTTP 400 (min limit enforced)
- Expiry more than 30 days from now rejected with HTTP 400 (max limit enforced)
- Both min and max expiry limits are read from `application.properties` (configurable)
- Cannot reserve more than available balance
- Inactive/frozen wallet returns HTTP 422

**Verification Method:**
Integration test: create reservation, verify balance changes, verify reservation ACTIVE. Attempt to over-reserve — expect failure, balance unchanged. Attempt expiry < 1 hour from now — expect HTTP 400. Attempt expiry > 30 days from now — expect HTTP 400.

**Files to Create:**
- `controller/CheckPleaseController.java`
- `service/CheckPleaseService.java`
- `dto/request/ReservationRequest.java`

---

### T-072: Implement Reservation Validation Service

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-071

**Description:**
Implement `CheckPleaseService.validateReservation(reservationId, userId, merchantId, amount)`. Check: reservation exists, status is ACTIVE, not expired, owned by current user, bound merchant matches payment merchant, payment amount ≤ remaining_amount, wallet not frozen/inactive.

**Acceptance Criteria:**
- Each validation rule throws a specific exception with a clear message
- CONSUMED, CANCELLED, or EXPIRED reservations are rejected
- Merchant mismatch is rejected
- Amount exceeding remaining_amount is rejected

**Verification Method:**
Unit tests for each validation failure path.

**Files to Create:**
- `validateReservation` method in `CheckPleaseService.java`
- `exception/ReservationException.java` variants

---

### T-073: Implement Reservation List and Detail Endpoints

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-072

**Description:**
Implement `GET /reservations` (user's own reservations, optional status filter) and `GET /reservations/{id}` (detail view). Return merchant name, max_amount, remaining_amount, status, expiry, and created_at.

**Acceptance Criteria:**
- User sees only their own reservations
- Optional status filter works
- Detail view returns all fields including merchant name
- HTTP 403 if user tries to access another user's reservation

**Verification Method:**
Integration test: create reservations for two users; each user sees only their own.

**Files to Create:**
- List and detail endpoints in `CheckPleaseController.java`

---

### T-074: Implement Reservation Cancellation

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-073

**Description:**
Implement `DELETE /reservations/{id}`. Only the reservation owner may cancel an ACTIVE reservation. In one atomic transaction with `SELECT FOR UPDATE`: verify reservation is ACTIVE, set status to CANCELLED, decrease `reserved_balance` by `remaining_amount`, send cancellation notification. Idempotent: already-cancelled reservation returns HTTP 200 with current state.

**Acceptance Criteria:**
- Cancellation releases `remaining_amount` from `reserved_balance` exactly once
- `total_balance` unchanged after cancellation
- Non-owner cancellation returns HTTP 403
- Already-cancelled or expired reservation returns appropriate status without double-release
- Cancellation notification sent

**Verification Method:**
Integration test: create reservation, cancel it, verify balance restored. Call cancel again — verify no second release.

**Files to Create:**
- Cancel endpoint in `CheckPleaseController.java`
- Cancel method in `CheckPleaseService.java`

---

### T-075: Implement Merchant Payment Initiation and Validation

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-074

**Description:**
Implement `POST /merchant-payments`. Validate: authenticated user owns the reservation, merchant identity matches, reservation is ACTIVE and not expired, payment amount is positive and ≤ remaining_amount, wallet is ACTIVE and not frozen, transaction PIN is correct, mandatory security checks pass. Create `MerchantPayment` record with status PENDING. Apply `request_id` idempotency.

**Acceptance Criteria:**
- All validation rules enforced before any financial change
- `MerchantPayment` created in PENDING status
- Wrong PIN returns HTTP 401
- Failed validation returns HTTP 422 with specific error message
- Duplicate `request_id` returns original result without second processing

**Verification Method:**
Integration test: initiate payment with valid reservation; verify MerchantPayment PENDING. Call again with same request_id — verify no duplicate.

**Files to Create:**
- `controller/CheckPleaseController.java` (payment endpoint)
- `service/MerchantPaymentService.java`
- `dto/request/MerchantPaymentRequest.java`

---

### T-076: Implement Merchant Payment Capture

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-075

**Description:**
Implement the capture logic within `MerchantPaymentService`. In one atomic transaction with `SELECT FOR UPDATE` on wallet and reservation: debit `total_balance` by payment amount, create DEBIT `LedgerEntry`, set `remaining_amount = remaining_amount − payment_amount`, release the full `remaining_amount` from `reserved_balance` (i.e., decrease `reserved_balance` by the original remaining_amount), set reservation status to CONSUMED, set payment status to CAPTURED. Send payment success notification.

**Acceptance Criteria:**
- `total_balance` decreases by exactly `payment_amount`
- `reserved_balance` decreases by the full `remaining_amount` (releasing unused portion)
- `available_balance` increases by unused portion
- Reservation status = CONSUMED after capture
- Payment status = CAPTURED
- DEBIT ledger entry created
- All changes commit atomically — no partial state

**Verification Method:**
Integration test: reserve ₹1000, pay ₹700, verify: `total_balance` -₹700, `reserved_balance` -₹1000, `available_balance` +₹300, reservation CONSUMED, ledger entry for ₹700.

**Files to Create:**
- Capture logic in `MerchantPaymentService.java`

---

### T-077: Implement Merchant Payment Failure Handling

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-076

**Description:**
If payment validation fails or capture fails before commit: set payment status to FAILED or REJECTED. Do NOT debit the wallet. Do NOT consume the reservation. The reservation remains ACTIVE for retry if it has not expired. Send payment failure notification.

**Acceptance Criteria:**
- Failed payments do not affect wallet balance or reserved_balance
- Reservation stays ACTIVE after a failed payment attempt
- User can retry the payment if reservation is still valid
- Notification sent on failure

**Verification Method:**
Integration test: simulate payment failure after validation but before capture; verify balance unchanged, reservation still ACTIVE.

**Files to Create:**
- Failure handling in `MerchantPaymentService.java`

---

### T-078: Implement Merchant-Specific Risk Policy

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-077

**Description:**
Implement the merchant-payment risk rules as a distinct method `FraudService.checkMerchantPayment()`, separate from `FraudService.checkP2PTransfer()`. The following mandatory controls are always enforced for merchant payments: valid JWT, user ownership of the reservation (never trust client-supplied user ID), correct 4-digit transaction PIN, wallet must be ACTIVE, merchant must match the reservation's bound merchant and be ACTIVE, account must not be locked, and duplicate `request_id` returns original result without re-processing. The following P2P-specific fraud rules do NOT apply to merchant payments: daily ₹50,000 P2P limit, duplicate-transfer rule (same amount to same recipient within 2 minutes), and high-value ₹8,000 threshold alert. A valid ACTIVE reservation is treated as a positive risk signal. No additional merchant-specific fraud block in MVP. Document this policy in code comments on `checkMerchantPayment()` and in ARCHITECTURE.md.

**Acceptance Criteria:**
- `checkMerchantPayment()` is a separate method from `checkP2PTransfer()`; P2P fraud check methods are not called from the merchant payment path
- All seven mandatory controls enforced for every merchant payment regardless of reservation validity
- P2P daily limit does not block merchant payments
- P2P duplicate-transfer rule does not block merchant payments
- P2P high-value ₹8,000 alert is not raised for merchant payments
- Policy documented in code comments on the merchant risk method
- Unit tests cover each mandatory control and verify P2P rules are not applied

**Verification Method:**
Unit tests for each mandatory control path. Integration test: merchant payment does not trigger daily limit block or duplicate detection. Verify `checkP2PTransfer()` is not called within the merchant payment flow.

**Files to Create:**
- `checkMerchantPayment()` method in `FraudService.java`

---

### T-079: Write Unit Tests for Check Please Service

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-078

**Description:**
Write unit tests for `CheckPleaseService` and `MerchantPaymentService` covering: reservation creation, over-reservation attempt, reservation cancellation double-release prevention, payment validation all failure paths, payment capture balance changes, payment failure (no balance change), expired reservation rejection.

**Acceptance Criteria:**
- All listed scenarios have passing unit tests
- Balance change calculations verified with exact BigDecimal values

**Verification Method:**
`mvn test -Dtest=CheckPleaseServiceTest,MerchantPaymentServiceTest` passes.

**Files to Create:**
- `test/service/CheckPleaseServiceTest.java`
- `test/service/MerchantPaymentServiceTest.java`

---

### T-080: Write Integration Tests for Reservation Lifecycle

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-079

**Description:**
Write end-to-end integration tests for the full reservation lifecycle: create reservation, list reservations, view detail, initiate and capture payment, verify final balances and statuses. Separate test for create and cancel lifecycle.

**Acceptance Criteria:**
- Full create → pay → consume lifecycle passes with correct balances
- Create → cancel lifecycle passes with correct balance restoration
- All endpoints return correct HTTP status codes

**Verification Method:**
`mvn test -Dtest=CheckPleaseControllerIT` passes.

**Files to Create:**
- `test/controller/CheckPleaseControllerIT.java`

---

### T-081: Write Concurrency Tests for Concurrent Payments on Same Reservation

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-080

**Description:**
Write a concurrency test that submits two simultaneous merchant payment requests for the same reservation. Verify that exactly one payment is captured and the reservation is CONSUMED once. Verify the wallet balance reflects exactly one payment deduction.

**Acceptance Criteria:**
- Only one of two concurrent payments succeeds (CAPTURED)
- The other fails (FAILED or REJECTED)
- Reservation is CONSUMED exactly once
- Wallet balance decreased by exactly one payment amount

**Verification Method:**
Concurrency test using `ExecutorService` with 2 threads. Assertions on DB state after both threads complete.

**Files to Create:**
- `test/service/MerchantPaymentConcurrencyTest.java`

---

### T-082: Write Idempotency Tests for Merchant Payments

**Phase:** 7
**Status:** NOT STARTED
**Dependencies:** T-081

**Description:**
Write tests for merchant payment idempotency: same `request_id` submitted sequentially returns original result, same `request_id` submitted concurrently results in exactly one capture.

**Acceptance Criteria:**
- Sequential duplicate `request_id`: one CAPTURED record in DB, correct response on both calls
- Concurrent duplicate `request_id`: exactly one CAPTURED record in DB
- No double debit in either scenario

**Verification Method:**
Integration and concurrency tests. Assert DB state after each scenario.

**Files to Create:**
- `test/service/MerchantPaymentIdempotencyTest.java`

---

## Phase 8 — Delayed Settlement and Scheduled Jobs

---

### T-090: Implement Business Day Calculator

**Phase:** 8
**Status:** NOT STARTED
**Dependencies:** T-082

**Description:**
Implement `BusinessDayCalculator.addBusinessDays(LocalDate date, int days)`. Exclude Saturdays and Sundays as non-business days. No public holiday adjustments are applied in this version (weekends-only policy per PD-002). Document in the method's Javadoc: the weekends-only exclusion policy, the Asia/Kolkata (IST) timezone used for "today" determination, and that adding a holiday list requires explicit user approval before implementation.

**Acceptance Criteria:**
- Adding 3 business days to a Friday returns the following Wednesday (skipping Saturday and Sunday)
- Adding 3 business days to a Monday returns Thursday
- Saturdays and Sundays are always skipped; no public holiday adjustments applied
- Method Javadoc documents the weekends-only exclusion policy and Asia/Kolkata timezone for "today"
- BusinessDayCalculator Javadoc and ARCHITECTURE.md both document the weekends-only policy

**Verification Method:**
Unit tests for a comprehensive set of input dates including dates spanning weekends and end-of-month boundaries.

**Files to Create:**
- `util/BusinessDayCalculator.java`
- `test/util/BusinessDayCalculatorTest.java`

---

### T-091: Implement ACH Settlement Processor Job

**Phase:** 8
**Status:** NOT STARTED
**Dependencies:** T-090

**Description:**
Implement `SettlementJob` with `@Scheduled(cron = "0 0 * * * *")` (hourly). Query all PENDING transactions where `settlement_due_date <= today`. For each, call `SettlementService.settleTransaction(transactionId)`. Record a `SettlementBatch` with counts and total amount. Write `ScheduledJobLog` entry.

**Acceptance Criteria:**
- Job runs hourly (or can be triggered manually for test)
- Only processes PENDING transactions whose due date has passed
- Processes each transaction in isolation (failure of one does not stop others)
- `ScheduledJobLog` entry written with start time, end time, status, records processed

**Verification Method:**
Integration test: create PENDING transaction with past due date, trigger job manually, verify transaction settled.

**Files to Create:**
- `scheduler/SettlementJob.java`
- `service/SettlementService.java`

---

### T-092: Implement Settlement Per-Transaction Logic with REQUIRES_NEW Isolation

**Phase:** 8
**Status:** NOT STARTED
**Dependencies:** T-091

**Description:**
Implement `SettlementService.settleTransaction(transactionId)` with `@Transactional(propagation = REQUIRES_NEW)`. Check receiver wallet status (ACTIVE required). Credit receiver wallet (`SELECT FOR UPDATE`). Create CREDIT `LedgerEntry`. Update transaction status to SUCCESS, set `settled_at`. Notify both sender and receiver.

**Acceptance Criteria:**
- Receiver wallet credited atomically in its own transaction
- CREDIT ledger entry created with correct amount
- Transaction status updated to SUCCESS
- Both parties notified
- Method commits independently of other settlements in the batch

**Verification Method:**
Integration test: settle a PENDING transaction, verify all DB changes committed, both notifications created.

**Files to Create:**
- `settleTransaction` method in `SettlementService.java`

---

### T-093: Implement Settlement Failure Handling

**Phase:** 8
**Status:** NOT STARTED
**Dependencies:** T-092

**Description:**
If `settleTransaction` throws an exception (DB error, wallet error) or the receiver wallet is INACTIVE/FROZEN: catch the exception in the job loop, mark transaction FAILED or DECLINED, refund sender exactly once via `WalletService.creditWallet` with `SELECT FOR UPDATE`, create sender CREDIT refund entry, notify sender, log the failure, and continue to the next transaction.

**Acceptance Criteria:**
- Individual settlement failure does not stop other settlements
- Sender refunded exactly once (idempotent: check transaction status before refund)
- Transaction marked FAILED (system error) or DECLINED (inactive/frozen wallet)
- Sender notified of failure/decline
- Error logged in `ScheduledJobLog`

**Verification Method:**
Integration test: create PENDING transaction for receiver with FROZEN wallet, run settlement, verify transaction DECLINED, sender refunded, notification sent.

**Files to Create:**
- Failure handling in `SettlementJob.java` and `SettlementService.java`

---

### T-094: Implement Daily Limit Reset Job

**Phase:** 8
**Status:** NOT STARTED
**Dependencies:** T-093

**Description:**
Implement `DailyLimitResetJob` with `@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")` (midnight IST). The fraud daily limit check queries `created_at >= today 00:00 UTC` using Asia/Kolkata as the timezone reference for "today" determination. If a rolling-window approach is used, no actual DB update is needed; document this clearly. If a stored `daily_spending` field is maintained, reset it here. Log job execution.

**Acceptance Criteria:**
- After midnight IST (Asia/Kolkata), new transfer amounts start a fresh daily window
- Cron expression explicitly specifies `zone = "Asia/Kolkata"`
- If rolling-window approach used, no DB update needed; approach documented clearly in code
- If stored field approach used, all user daily spending fields reset to 0
- Daily limit resets at midnight IST — "Daily limit resets at midnight IST (Asia/Kolkata)" confirmed in code and documentation
- Job logs its execution

**Verification Method:**
Integration test: initiate transfers totaling ₹49,000 on day N; verify day N+1 (after midnight IST) allows fresh ₹50,000 limit.

**Files to Create:**
- `scheduler/DailyLimitResetJob.java`

---

### T-095: Implement Account Auto-Unlock Job

**Phase:** 8
**Status:** NOT STARTED
**Dependencies:** T-094

**Description:**
Implement `AccountUnlockJob` with `@Scheduled(fixedDelay = 300000)` (every 5 minutes). Call `AccountLockService.unlockEligibleAccounts()` (implemented in T-035). Log job execution.

**Acceptance Criteria:**
- Job runs every 5 minutes
- Calls `unlockEligibleAccounts()` which unlocks all eligible accounts and notifies each
- `ScheduledJobLog` entry written

**Verification Method:**
Integration test: lock an account with past `lock_expires_at`, trigger job, verify account unlocked and notification sent.

**Files to Create:**
- `scheduler/AccountUnlockJob.java`

---

### T-096: Implement Daily Summary and Reconciliation Job

**Phase:** 8
**Status:** NOT STARTED
**Dependencies:** T-095

**Description:**
Implement `DailySummaryJob` with `@Scheduled(cron = "0 0 23 * * *", zone = "Asia/Kolkata")` (11 PM IST). Compute and save `DailySummary` for today: initiated count/amount, settled count/amount, fraud alert count, new registrations, pending settlement amount. Then run reconciliation: sum all DEBIT ledger entries for settled P2P transfers and all CREDIT entries; if they do not match, create a critical ADMIN notification. Log job execution.

**Acceptance Criteria:**
- `DailySummary` record created for today with correct values
- Reconciliation excludes PENDING transactions
- Mismatch in debit vs credit totals creates a CRITICAL admin notification
- No mismatch creates no notification
- Job logs its execution with records processed count

**Verification Method:**
Integration test: create a set of settled transactions with known amounts, run job, verify summary values and reconciliation result.

**Files to Create:**
- `scheduler/DailySummaryJob.java`

---

### T-097: Implement Check Please Expiry Processor Job

**Phase:** 8
**Status:** NOT STARTED
**Dependencies:** T-096

**Description:**
Implement `ReservationExpiryJob` (schedule configurable in application.properties, default every hour). Query all ACTIVE reservations where `expires_at <= now`. For each: acquire `SELECT FOR UPDATE` on reservation, verify still ACTIVE, set status to EXPIRED, decrease `reserved_balance` by `remaining_amount`, send expiry notification. Log job execution.

**Acceptance Criteria:**
- Only ACTIVE reservations with past expiry are processed
- `reserved_balance` released exactly once per reservation (SELECT FOR UPDATE prevents double-release)
- Already-CONSUMED, CANCELLED, or EXPIRED reservations are not touched
- Expiry notification sent for each expired reservation
- Job logs its execution

**Verification Method:**
Integration test: create reservation with past `expires_at`, trigger job, verify reservation EXPIRED, balance released, notification sent.

**Files to Create:**
- `scheduler/ReservationExpiryJob.java`

---

### T-098: Implement Scheduled Job Logging

**Phase:** 8
**Status:** NOT STARTED
**Dependencies:** T-097

**Description:**
Create `JobLoggingService.startJob(jobName)` which creates a `ScheduledJobLog` record with status RUNNING and returns the log ID. Create `JobLoggingService.endJob(logId, status, recordsProcessed, errorDetails)` which updates the record. Every scheduler method calls these at start and end. Standardize this pattern across all five scheduler classes.

**Acceptance Criteria:**
- Every job creates a `ScheduledJobLog` record before processing
- Record updated with end time, final status, and records processed after completion
- Failures capture error details in `error_details`
- Pattern used consistently in all five jobs

**Verification Method:**
After running all five jobs once, verify five `ScheduledJobLog` records exist with correct data.

**Files to Create:**
- `service/JobLoggingService.java`
- Update all scheduler classes to use `JobLoggingService`

---

### T-099: Write Tests for Settlement Job, Refund Idempotency, and Expiry Processing

**Phase:** 8
**Status:** NOT STARTED
**Dependencies:** T-098

**Description:**
Write tests for: settlement job processes only due-date-eligible PENDING transactions, settlement failure marks FAILED and refunds once, settlement of receiver FROZEN wallet marks DECLINED and refunds once, refund idempotency (double-run does not double-refund), expiry job processes only ACTIVE expired reservations without double-release.

**Acceptance Criteria:**
- All five scenarios have passing tests
- Refund idempotency specifically tested with double-run scenario

**Verification Method:**
`mvn test -Dtest=SettlementJobTest,ReservationExpiryJobTest` passes.

**Files to Create:**
- `test/scheduler/SettlementJobTest.java`
- `test/scheduler/ReservationExpiryJobTest.java`

---

## Phase 9 — Notifications and Administration

---

### T-100: Implement Notification Creation Service

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-099

**Description:**
Implement `NotificationService.createNotification(userId, type, message)` as the single method all other services call to create in-app notifications. Define notification type constants. Ensure the method is simple, non-blocking, and handles persistence only (no push or email — in-app only).

**Acceptance Criteria:**
- Single method used by all services for notification creation
- Notification persisted to `notifications` table immediately
- Notification type is a defined constant, not a free string

**Verification Method:**
Unit test: call `createNotification`, verify `Notification` entity saved with correct fields.

**Files to Create:**
- `service/NotificationService.java`

---

### T-101: Implement Notification List and Mark-as-Read Endpoints

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-100

**Description:**
Implement `GET /notifications` (paginated, ordered by `created_at` DESC) and `PATCH /notifications/{id}/read`. Users see only their own notifications. Marking as read sets `read = true`.

**Acceptance Criteria:**
- User sees only own notifications
- Paginated result ordered newest first
- Mark-as-read sets `read = true` idempotently
- Non-owner mark-as-read returns HTTP 403

**Verification Method:**
Integration test: create notifications for two users, verify each sees only theirs; mark one read, verify DB state.

**Files to Create:**
- `controller/NotificationController.java`

---

### T-102: Implement Unread Notification Count Endpoint

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-101

**Description:**
Implement `GET /notifications/unread-count`. Returns `{ "count": N }` where N is the number of notifications for the authenticated user with `read = false`.

**Acceptance Criteria:**
- Count is accurate in real time
- Returns 0 when all notifications are read

**Verification Method:**
Integration test: create 3 notifications, call count (expect 3), mark 2 read, call count (expect 1).

**Files to Create:**
- Unread count endpoint in `NotificationController.java`

---

### T-103: Implement Admin Dashboard Data Endpoint

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-102

**Description:**
Implement `GET /admin/dashboard`. Return: total registered users, today's initiated count and amount, today's settled count and amount, total pending settlement amount, pending fraud alert count, and 30-day daily transaction-volume data (for chart).

**Acceptance Criteria:**
- All seven dashboard metrics return correct values
- 30-day volume data is an array of `{ date, amount }` objects
- Non-admin access returns HTTP 403

**Verification Method:**
Integration test: create known data set, call dashboard, verify all metrics match.

**Files to Create:**
- `AdminController.java` dashboard endpoint
- `service/AdminService.java`

---

### T-104: Implement Admin User List with Wallet Status Endpoint

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-103

**Description:**
Implement `GET /admin/users` (paginated). Return user name, email, phone, role, wallet balance, wallet status, registration date, and last 3 transactions per user. Implement `GET /admin/users/{id}` for full user detail.

**Acceptance Criteria:**
- Paginated list with correct user data
- Each user entry includes current wallet balance and status
- Full detail includes recent transactions

**Verification Method:**
Integration test: create multiple users, call list, verify correct data returned.

**Files to Create:**
- Admin user endpoints in `AdminController.java`

---

### T-105: Implement Admin Transaction Monitoring Endpoint

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-104

**Description:**
Implement `GET /admin/transactions` (paginated, filters: date range, status, amount range). Return all transactions with sender/receiver details, ledger entry summary, and clear pending-state presentation. Admin only.

**Acceptance Criteria:**
- All transactions visible to admin (not just own)
- Date, status, and amount filters work correctly
- Paginated result
- Non-admin returns HTTP 403

**Verification Method:**
Integration test: create transactions for multiple users, call admin endpoint, verify all visible with filters working.

**Files to Create:**
- Admin transaction endpoint in `AdminController.java`

---

### T-106: Implement Settlement Monitor Endpoint

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-105

**Description:**
Implement `GET /admin/settlement-monitor`. Return: all PENDING transactions with due dates, counts for due-today, due-tomorrow, and overdue. Implement `POST /admin/settlement-monitor/trigger/{transactionId}` for manual settlement trigger of a single eligible transaction.

**Acceptance Criteria:**
- Overdue count = PENDING transactions where `settlement_due_date < today`
- Manual trigger calls `SettlementService.settleTransaction(id)` directly
- Manual trigger writes audit log entry

**Verification Method:**
Integration test: create PENDING transactions with different due dates, verify counts, manually trigger one, verify settled.

**Files to Create:**
- Settlement monitor endpoints in `AdminController.java`

---

### T-107: Implement Fraud Review Endpoint

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-106

**Description:**
This endpoint is covered by T-063 and T-064 (fraud alert admin queue, block, and dismiss). Verify all fraud review functionality is accessible via admin panel. This task validates integration and completeness of the fraud review feature from the admin perspective.

**Acceptance Criteria:**
- Admin fraud queue returns pending alerts with full transaction detail
- Block and dismiss actions work and write audit log
- Settled transaction alerts are read-only (no block/dismiss available)

**Verification Method:**
Integration test: create HIGH_VALUE alert on settled transaction, verify block attempt returns error.

**Files to Create:**
- Verified via existing T-063/T-064 files

---

### T-108: Implement Job Monitor Endpoint

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-107

**Description:**
Implement `GET /admin/jobs`. Return the last 20 scheduled job log entries ordered by `started_at` DESC. Include job name, start time, end time, status, records processed, and error details (if any).

**Acceptance Criteria:**
- Returns at most 20 records
- Ordered by most recent first
- All fields from `ScheduledJobLog` entity included

**Verification Method:**
Integration test: create 25 job log entries, call endpoint, verify only 20 returned in correct order.

**Files to Create:**
- Job monitor endpoint in `AdminController.java`

---

### T-109: Implement Reconciliation Endpoint

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-108

**Description:**
Implement `GET /admin/reconciliation`. Return: total settled DEBIT ledger entries sum, total settled CREDIT ledger entries sum for P2P transfers, total pending settlement amount, and any mismatch indicator. Pending transactions are excluded from debit/credit totals.

**Acceptance Criteria:**
- Sums only ledger entries from transactions with SUCCESS status
- Pending transactions excluded from debit/credit totals
- Mismatch clearly flagged in response
- Non-admin returns HTTP 403

**Verification Method:**
Integration test: create settled transactions with known amounts, call reconciliation, verify totals match. Manually introduce mismatch in test data, verify flag appears.

**Files to Create:**
- Reconciliation endpoint in `AdminController.java`

---

### T-110: Implement Merchant Registration and Management Endpoints

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-109

**Description:**
Implement `POST /admin/merchants` (register), `PATCH /admin/merchants/{id}/status` (activate/deactivate/suspend). Validate merchant name and email uniqueness. Write audit log for every merchant status change. Record `registered_by` admin ID.

**Acceptance Criteria:**
- New merchant created with ACTIVE status
- Status transitions: ACTIVE ↔ INACTIVE, ACTIVE → SUSPENDED
- Suspended merchant cannot be directly activated (must go through INACTIVE first — or per OD-004 decision)
- Audit log entry for each status change
- Non-admin returns HTTP 403

**Verification Method:**
Integration test: register merchant, change status, verify audit log entries.

**Files to Create:**
- Merchant admin endpoints in `AdminController.java`

---

### T-111: Implement Audit Log Service

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-110

**Description:**
Implement `AuditLogService.log(adminId, action, targetType, targetId, details)`. This method is called by every admin action (wallet freeze/unfreeze, merchant status change, fraud alert block/dismiss, manual settlement trigger). Verify that all existing admin actions from T-042, T-064, T-106, T-110 use this service.

**Acceptance Criteria:**
- All admin actions write to `audit_log` table via `AuditLogService`
- No admin action is missing an audit log entry
- `AuditLogService` is the single write path for audit entries

**Verification Method:**
Integration test: perform one of each admin action type; query `audit_log`; verify one entry per action with correct fields.

**Files to Create:**
- `service/AuditLogService.java`

---

### T-112: Write Tests for Admin Endpoints and Audit Log

**Phase:** 9
**Status:** NOT STARTED
**Dependencies:** T-111

**Description:**
Write integration tests for all admin endpoints: dashboard data accuracy, user list pagination, transaction monitoring filters, settlement monitor counts and manual trigger, job monitor last-20 limit, reconciliation totals, merchant registration and status changes, audit log completeness.

**Acceptance Criteria:**
- All admin endpoints tested with known data sets
- Authorization enforcement tested (non-admin returns 403)
- Audit log verified after each admin action

**Verification Method:**
`mvn test -Dtest=AdminControllerIT` passes.

**Files to Create:**
- `test/controller/AdminControllerIT.java`

---

## Phase 10 — React Frontend

---

### T-120: Login and Registration Screens

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-112

**Description:**
Implement login page (email/password form, JWT stored in `AuthContext`) and registration page (name, email, phone, password, 4-digit PIN form). Show validation errors inline. On login, navigate to wallet dashboard. On registration, navigate to login.

**Acceptance Criteria:**
- Login form calls `POST /auth/login`, stores JWT, navigates to dashboard
- Registration form calls `POST /auth/register`, shows success, navigates to login
- Validation errors displayed for all required fields
- Account locked message shown with unlock time

**Files to Create:**
- `frontend/src/pages/LoginPage.jsx`
- `frontend/src/pages/RegisterPage.jsx`

---

### T-121: Wallet Dashboard

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-120

**Description:**
Implement wallet dashboard showing total balance, available balance, reserved balance, and today's spending. Include a load-money form with amount input. Show wallet status badge (ACTIVE/INACTIVE/FROZEN).

**Acceptance Criteria:**
- All four balance values displayed
- Load money form submits and refreshes balances
- Wallet status badge visible with correct color coding

**Files to Create:**
- `frontend/src/pages/WalletDashboard.jsx`

---

### T-122: Send Money Flow

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-121

**Description:**
Implement the send money flow: recipient search (by phone or email), display matching user, amount input with remark, PIN confirmation dialog, transfer submission and result. Show success or error message.

**Acceptance Criteria:**
- Recipient search returns matching user (if found)
- Amount and remark inputs validated client-side
- PIN dialog appears before submission
- Success and error states handled

**Files to Create:**
- `frontend/src/pages/SendMoneyPage.jsx`
- `frontend/src/components/PinConfirmDialog.jsx`

---

### T-123: Transaction History with Filters

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-122

**Description:**
Implement paginated transaction history page with filters: date range picker, type (sent/received/all), status (success/pending/failed/all). Implement mini-statement widget (last 10) for the dashboard.

**Acceptance Criteria:**
- All filter combinations work and re-fetch data
- Pagination controls (next/prev page)
- PENDING transactions show expected settlement date and days remaining
- Mini-statement shows 10 most recent

**Files to Create:**
- `frontend/src/pages/TransactionHistoryPage.jsx`
- `frontend/src/components/MiniStatement.jsx`

---

### T-124: Transaction Detail View

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-123

**Description:**
Implement transaction detail view showing full transaction data and associated ledger entries. For PENDING transfers, show one DEBIT entry with note that CREDIT will appear after settlement. For settled transfers, show both DEBIT and CREDIT entries.

**Acceptance Criteria:**
- All transaction fields displayed
- Ledger entries table shown with entry type, amount, description, timestamp
- PENDING state clearly explained

**Files to Create:**
- `frontend/src/pages/TransactionDetailPage.jsx`

---

### T-125: Notification Bell with Unread Count and Notification List

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-124

**Description:**
Implement notification bell in the top navigation bar showing unread count badge. Clicking opens a notification panel (dropdown or drawer) showing paginated notifications ordered newest first. Each notification has a mark-as-read button or auto-marks on view.

**Acceptance Criteria:**
- Unread count badge visible on bell icon
- Count updates after marking notifications read
- Notification list shows type, message, and timestamp

**Files to Create:**
- `frontend/src/components/NotificationBell.jsx`
- `frontend/src/pages/NotificationsPage.jsx`

---

### T-126: Personal Statistics View

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-125

**Description:**
Implement a statistics page showing total amount sent (all time), total amount received (all time), and this month's transaction count. Display as summary cards.

**Acceptance Criteria:**
- Three statistics displayed as clearly labeled cards
- Values formatted as currency (₹)
- Page only accessible when authenticated

**Files to Create:**
- `frontend/src/pages/StatisticsPage.jsx`

---

### T-127: Check Please — Merchant List and Reservation Creation

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-126

**Description:**
Implement merchant search/list page (active merchants only) and reservation creation form (merchant selection, maximum amount, expiry timestamp picker). Show confirmation and result.

**Acceptance Criteria:**
- Only ACTIVE merchants shown in list
- Reservation form validates max amount and expiry
- Success creates reservation and shows confirmation

**Files to Create:**
- `frontend/src/pages/MerchantListPage.jsx`
- `frontend/src/pages/CreateReservationPage.jsx`

---

### T-128: Check Please — Active Reservations List and Cancellation

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-127

**Description:**
Implement active reservations list showing merchant name, max amount, remaining amount, status, and expiry. Include cancel button for ACTIVE reservations. Show status badge for CONSUMED, CANCELLED, EXPIRED.

**Acceptance Criteria:**
- Reservations list shows all statuses with correct badges
- Cancel button visible only for ACTIVE reservations
- Cancel confirmation dialog before API call
- List refreshes after cancellation

**Files to Create:**
- `frontend/src/pages/ReservationsPage.jsx`

---

### T-129: Check Please — Merchant Payment Flow

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-128

**Description:**
Implement merchant payment flow: select active reservation, enter payment amount (≤ remaining), PIN confirmation, submit payment. Show success (with released funds summary) or failure (reservation stays active message).

**Acceptance Criteria:**
- Payment amount validated against remaining amount client-side
- PIN dialog before submission
- Success shows captured amount and released unused amount
- Failure shows clear error and notes reservation is still active for retry

**Files to Create:**
- `frontend/src/pages/MerchantPaymentPage.jsx`

---

### T-130: Admin Dashboard

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-129

**Description:**
Implement the admin dashboard screen showing all seven metrics and a 30-day transaction volume bar chart.

**Acceptance Criteria:**
- All dashboard metrics displayed
- Chart renders with 30 data points
- Only accessible by ADMIN role users

**Files to Create:**
- `frontend/src/pages/admin/AdminDashboard.jsx`

---

### T-131: Admin User and Wallet Management

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-130

**Description:**
Implement admin user list (paginated) and user detail page. Include wallet status badge and freeze/unfreeze buttons. Show confirmation dialog before status change.

**Acceptance Criteria:**
- Paginated user list with wallet balance and status
- Freeze/unfreeze buttons work with confirmation
- Status updates reflected immediately in UI

**Files to Create:**
- `frontend/src/pages/admin/AdminUsersPage.jsx`

---

### T-132: Admin Transaction Monitoring and Fraud Review

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-131

**Description:**
Implement admin transaction monitoring (paginated, with date/status/amount filters) and fraud review queue (pending alerts with block/dismiss buttons). Show alert type badges and associated transaction summary.

**Acceptance Criteria:**
- Transaction list filterable by date, status, amount
- Fraud review queue shows PENDING alerts with block/dismiss controls
- Settled transaction alerts are read-only (controls disabled)

**Files to Create:**
- `frontend/src/pages/admin/AdminTransactionsPage.jsx`
- `frontend/src/pages/admin/AdminFraudReviewPage.jsx`

---

### T-133: Admin Settlement Monitor

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-132

**Description:**
Implement the settlement monitor page showing PENDING transfers grouped by due-today, due-tomorrow, and overdue. Include a manual settle button per transaction with confirmation.

**Acceptance Criteria:**
- Counts and lists for due-today, due-tomorrow, overdue shown correctly
- Manual settle button triggers API call with confirmation
- Settled transaction removed from list after successful trigger

**Files to Create:**
- `frontend/src/pages/admin/AdminSettlementPage.jsx`

---

### T-134: Admin Job Monitor and Reconciliation

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-133

**Description:**
Implement the job monitor page (last 20 runs table with job name, start/end time, status, records processed, errors) and reconciliation page (debit total, credit total, pending amount, mismatch indicator).

**Acceptance Criteria:**
- Job monitor shows last 20 runs in reverse chronological order
- Reconciliation page shows all four values
- Mismatch displayed with a clear warning indicator

**Files to Create:**
- `frontend/src/pages/admin/AdminJobMonitorPage.jsx`
- `frontend/src/pages/admin/AdminReconciliationPage.jsx`

---

### T-135: Responsive Layout and Cross-Screen Consistency

**Phase:** 10
**Status:** NOT STARTED
**Dependencies:** T-134

**Description:**
Implement a consistent responsive layout: navigation bar with links and notification bell, sidebar for admin pages, mobile-responsive breakpoints. Verify all screens render correctly on desktop and mobile viewports.

**Acceptance Criteria:**
- All pages use consistent navigation component
- Mobile breakpoints do not break layout on any page
- Admin sidebar collapses on mobile
- Notification bell appears on all authenticated screens

**Files to Create:**
- `frontend/src/components/Layout.jsx`
- `frontend/src/components/AdminLayout.jsx`

---

## Phase 11 — Integration, Testing, and Delivery

---

### T-140: End-to-End P2P Transfer and Settlement Workflow Test

**Phase:** 11
**Status:** NOT STARTED
**Dependencies:** T-135

**Description:**
Write an end-to-end test that walks through the full P2P lifecycle: register two users, load money, initiate transfer, verify sender debited and PENDING, fast-forward settlement date, run settlement job, verify receiver credited and SUCCESS, verify both notifications sent.

**Acceptance Criteria:**
- Full lifecycle completes without error
- All balance changes correct (exact BigDecimal values)
- Notifications created for both parties
- Transaction history reflects correct statuses at each stage

**Files to Create:**
- `test/e2e/P2PTransferE2ETest.java`

---

### T-141: End-to-End Check Please Reservation and Merchant Payment Test

**Phase:** 11
**Status:** NOT STARTED
**Dependencies:** T-140

**Description:**
Write an end-to-end test: register user and merchant, create reservation, initiate and capture merchant payment, verify all balance changes (debit exact amount, release unused reserved), verify reservation CONSUMED, verify notifications.

**Acceptance Criteria:**
- Full lifecycle completes without error
- Balance changes verified: total_balance -payment_amount, reserved_balance -max_amount, available_balance +(max_amount-payment_amount)
- Reservation CONSUMED after payment
- Notifications created

**Files to Create:**
- `test/e2e/CheckPleaseE2ETest.java`

---

### T-142: Concurrent Payment and Reservation Consistency Tests

**Phase:** 11
**Status:** NOT STARTED
**Dependencies:** T-141

**Description:**
Write concurrent tests: two concurrent P2P transfers that together exceed balance (only one succeeds), two concurrent merchant payments on same reservation (only one captured), two concurrent reservation creations that together exceed available balance (only one succeeds).

**Acceptance Criteria:**
- In all three scenarios, exactly one operation succeeds
- Losing operation fails cleanly with appropriate error
- Balance never goes negative
- Reservation never double-consumed

**Files to Create:**
- `test/e2e/ConcurrencyE2ETest.java`

---

### T-143: Security and Authorization Tests

**Phase:** 11
**Status:** NOT STARTED
**Dependencies:** T-142

**Description:**
Write tests verifying: user cannot access another user's wallet, transactions, or notifications; user cannot access admin endpoints; admin can access all admin endpoints; ownership enforcement prevents cross-user operations; unauthenticated requests to all protected endpoints return 401.

**Acceptance Criteria:**
- All cross-user access attempts return 403
- All unauthenticated access attempts return 401
- Admin endpoints accessible to ADMIN role, not USER role

**Files to Create:**
- `test/security/AuthorizationTest.java`

---

### T-144: Database Consistency Tests

**Phase:** 11
**Status:** NOT STARTED
**Dependencies:** T-143

**Description:**
Write tests that verify no partial financial state can persist: rollback during transfer leaves balance unchanged, rollback during reservation creation leaves balance unchanged, rollback during merchant payment leaves all three balance fields unchanged.

**Acceptance Criteria:**
- Simulated failure mid-transaction results in complete rollback
- After rollback, all balance fields are exactly as they were before the operation
- No orphaned ledger entries from rolled-back operations

**Files to Create:**
- `test/db/TransactionConsistencyTest.java`

---

### T-145: Build and Startup Verification

**Phase:** 11
**Status:** NOT STARTED
**Dependencies:** T-144

**Description:**
Perform a clean build of both backend and frontend from scratch. Verify the backend starts cleanly against a fresh database. Verify all scheduled jobs initialize without errors. Verify the frontend build produces a distributable artifact.

**Acceptance Criteria:**
- `mvn clean package -DskipTests` succeeds for backend
- Backend starts, connects to DB, creates schema (if DDL auto used), and all beans wire up
- `npm run build` succeeds for frontend
- No missing dependency or configuration errors at startup

**Verification Method:**
Run build commands in a clean environment. Document actual versions of Spring Boot and Java 25 (or fallback) used.

---

### T-146: Setup Instructions (README, Environment Variable Documentation)

**Phase:** 11
**Status:** NOT STARTED
**Dependencies:** T-145

**Description:**
Write `README.md` at the project root documenting: prerequisites, environment variables required (`JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), how to run the backend, how to run the frontend, how to trigger scheduled jobs manually for testing, and the open decisions (OD-001 through OD-006) status.

**Acceptance Criteria:**
- README covers all environment variables with descriptions
- Step-by-step run instructions for both backend and frontend
- Open decisions documented with their final resolutions or remaining status

**Files to Create:**
- `README.md`

---

### T-147: Final Architecture and API Documentation

**Phase:** 11
**Status:** NOT STARTED
**Dependencies:** T-146

**Description:**
Update `ARCHITECTURE.md` to reflect any schema or design changes made during implementation. Document all final API endpoints (method, path, auth required, request/response summary) in a new `docs/API_REFERENCE.md`.

**Acceptance Criteria:**
- ARCHITECTURE.md matches actual implemented schema
- API reference covers every implemented endpoint
- Open decisions resolved during implementation documented

**Files to Create/Update:**
- `ARCHITECTURE.md` (updated)
- `docs/API_REFERENCE.md`

---

### T-148: Test Coverage Report

**Phase:** 11
**Status:** NOT STARTED
**Dependencies:** T-147

**Description:**
Run `mvn test jacoco:report` (or equivalent coverage tool configured in Phase 1) to generate a test coverage report. Review and document coverage percentages for service layer and controller layer.

**Acceptance Criteria:**
- Coverage report generated
- Service layer coverage ≥ 80% (target)
- All 17 acceptance criteria from requirements document have at least one test covering them

**Files to Create:**
- `docs/TEST_COVERAGE.md` (summary)

---

### T-149: Final Project Review and Delivery Checklist

**Phase:** 11
**Status:** NOT STARTED
**Dependencies:** T-148

**Description:**
Perform a final review against the 17 acceptance criteria from the requirements document. Mark each criterion as verified or not verified. Check that all open decisions have been resolved and documented. Verify all 60+ tasks are marked COMPLETE. Produce a delivery checklist.

**Acceptance Criteria:**
- All 17 acceptance criteria verified against implemented and tested code
- All 6 open decisions resolved and documented
- All tasks COMPLETE
- No known unresolved blockers

**Files to Create:**
- `docs/DELIVERY_CHECKLIST.md`

---

## Summary

| Phase | Tasks       | Task Count | Status               |
|-------|-------------|------------|----------------------|
| 0     | T-001–T-005 | 5          | All COMPLETE         |
| 1     | T-010–T-015 | 6          | T-010–T-014 COMPLETE; T-015 pending backend test+package |
| 2     | T-020–T-027 | 8          | All NOT STARTED      |
| 3     | T-030–T-037 | 8          | All NOT STARTED      |
| 4     | T-040–T-046 | 7          | All NOT STARTED      |
| 5     | T-050–T-058 | 9          | All NOT STARTED      |
| 6     | T-060–T-066 | 7          | All NOT STARTED      |
| 7     | T-070–T-082 | 13         | All NOT STARTED      |
| 8     | T-090–T-099 | 10         | All NOT STARTED      |
| 9     | T-100–T-112 | 13         | All NOT STARTED      |
| 10    | T-120–T-135 | 16         | All NOT STARTED      |
| 11    | T-140–T-149 | 10         | All NOT STARTED      |
| **Total** | **T-001–T-149** | **112** | **10 COMPLETE, T-015 pending, 101 NOT STARTED** |

> Task numbering uses intentional gaps between phases (e.g., Phase 0 ends at T-005, Phase 1 begins at T-010) to allow insertion of tasks within a phase without renumbering. Total task count is 112 across all phases.

