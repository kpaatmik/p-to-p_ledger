# P2P Payment Ledger — Architecture Document

## System Architecture Overview

The application follows a standard three-tier architecture:

```
┌─────────────────────────────────────────────────┐
│                React Frontend                   │
│  (Vite + React, REST API calls via Axios/Fetch) │
└───────────────────────┬─────────────────────────┘
                        │ HTTP / JSON (JWT Bearer)
┌───────────────────────▼─────────────────────────┐
│           Spring Boot Backend                   │
│  Controller → Service → Repository → Entity     │
│  Spring Security (JWT Filter)                   │
│  Spring Scheduler (Background Jobs)             │
└───────────────────────┬─────────────────────────┘
                        │ JDBC / JPA
┌───────────────────────▼─────────────────────────┐
│              PostgreSQL Database                │
│  14 tables, NUMERIC(19,4) for money,            │
│  UUID PKs, CHECK constraints, row locking       │
└─────────────────────────────────────────────────┘
```

### Backend Layer Responsibilities

| Layer       | Responsibility |
|-------------|---------------|
| Controller  | Accept HTTP requests, validate input DTOs, delegate to service, return HTTP responses. No business logic. |
| Service     | Business logic, transaction management (`@Transactional`), fraud checks, notifications, ownership validation. |
| Repository  | Spring Data JPA interfaces; custom JPQL or native SQL for complex queries and locking. |
| Entity      | JPA-mapped domain objects; no business logic beyond field-level validation. |
| DTO         | Separate request and response objects per endpoint; never expose entity internals directly. |
| Exception   | Custom exception hierarchy; global `@ControllerAdvice` returns consistent error JSON. |
| Scheduler   | `@Scheduled` methods; each job delegates to a service; logs every run in `scheduled_job_log`. |
| Security    | JWT filter, `UserDetailsService`, BCrypt for password and PIN hashing. |
| Util        | `BusinessDayCalculator`, `PinVerificationService`, and other stateless helpers. |

---

## Package Structure (Backend)

```
com.paymentledger
  ├── config/
  │     ├── SecurityConfig.java          # Spring Security filter chain, CORS, CSRF
  │     ├── JwtConfig.java               # JWT properties from application.properties
  │     └── SchedulerConfig.java         # Scheduler thread pool configuration
  │
  ├── controller/
  │     ├── AuthController.java          # POST /auth/register, POST /auth/login
  │     ├── WalletController.java        # GET /wallet, POST /wallet/load, PATCH /wallet/status
  │     ├── TransactionController.java   # POST /transfer, GET /transactions, GET /transactions/{id}
  │     ├── NotificationController.java  # GET /notifications, PATCH /notifications/{id}/read
  │     ├── StatisticsController.java    # GET /statistics
  │     ├── CheckPleaseController.java   # Reservation and merchant payment endpoints
  │     ├── AdminController.java         # All /admin/** endpoints
  │     └── MerchantController.java      # GET /merchants (user-facing search)
  │
  ├── service/
  │     ├── AuthService.java
  │     ├── WalletService.java
  │     ├── TransferService.java
  │     ├── LedgerService.java
  │     ├── FraudService.java
  │     ├── NotificationService.java
  │     ├── SettlementService.java
  │     ├── CheckPleaseService.java
  │     ├── MerchantPaymentService.java
  │     ├── AdminService.java
  │     ├── AuditLogService.java
  │     └── AccountLockService.java
  │
  ├── repository/
  │     ├── UserRepository.java
  │     ├── LoginAttemptRepository.java
  │     ├── WalletRepository.java
  │     ├── TransactionRepository.java
  │     ├── LedgerEntryRepository.java
  │     ├── FraudAlertRepository.java
  │     ├── NotificationRepository.java
  │     ├── AuditLogRepository.java
  │     ├── ScheduledJobLogRepository.java
  │     ├── DailySummaryRepository.java
  │     ├── SettlementBatchRepository.java
  │     ├── MerchantRepository.java
  │     ├── CheckPleaseReservationRepository.java
  │     └── MerchantPaymentRepository.java
  │
  ├── entity/
  │     ├── User.java
  │     ├── LoginAttempt.java
  │     ├── Wallet.java
  │     ├── Transaction.java
  │     ├── LedgerEntry.java
  │     ├── FraudAlert.java
  │     ├── Notification.java
  │     ├── AuditLog.java
  │     ├── ScheduledJobLog.java
  │     ├── DailySummary.java
  │     ├── SettlementBatch.java
  │     ├── Merchant.java
  │     ├── CheckPleaseReservation.java
  │     └── MerchantPayment.java
  │
  ├── dto/
  │     ├── request/
  │     │     ├── RegisterRequest.java
  │     │     ├── LoginRequest.java
  │     │     ├── LoadMoneyRequest.java
  │     │     ├── TransferRequest.java
  │     │     ├── ReservationRequest.java
  │     │     ├── MerchantPaymentRequest.java
  │     │     └── ...
  │     └── response/
  │           ├── AuthResponse.java
  │           ├── WalletResponse.java
  │           ├── TransactionResponse.java
  │           ├── ReservationResponse.java
  │           ├── ErrorResponse.java
  │           └── ...
  │
  ├── exception/
  │     ├── GlobalExceptionHandler.java  # @ControllerAdvice
  │     ├── InsufficientBalanceException.java
  │     ├── WalletStatusException.java
  │     ├── FraudBlockException.java
  │     ├── DuplicateRequestException.java
  │     ├── InvalidPinException.java
  │     ├── ReservationException.java
  │     └── ...
  │
  ├── scheduler/
  │     ├── SettlementJob.java           # @Scheduled — hourly
  │     ├── DailyLimitResetJob.java      # @Scheduled — midnight
  │     ├── AccountUnlockJob.java        # @Scheduled — every 5 minutes
  │     ├── DailySummaryJob.java         # @Scheduled — 11 PM
  │     └── ReservationExpiryJob.java    # @Scheduled — configurable interval
  │
  ├── security/
  │     ├── JwtUtil.java                 # Generate, validate, extract claims
  │     ├── JwtAuthFilter.java           # OncePerRequestFilter
  │     └── UserDetailsServiceImpl.java  # Load user from DB by email
  │
  └── util/
        ├── BusinessDayCalculator.java   # T+N business day calculation
        └── PinHashingUtil.java          # BCrypt PIN operations
```

---

## Database Schema Design (14 Tables)

> All monetary columns use `NUMERIC(19,4)`. All primary keys are `UUID`. All timestamps are stored in UTC.
> `available_balance` is **not** a stored column — it is computed as `total_balance − reserved_balance` in the service layer to prevent synchronization bugs.

---

### Table: `users`

| Column               | Type                    | Constraints                              |
|----------------------|-------------------------|------------------------------------------|
| id                   | UUID                    | PRIMARY KEY                              |
| name                 | VARCHAR(255)            | NOT NULL                                 |
| email                | VARCHAR(255)            | UNIQUE NOT NULL                          |
| phone                | VARCHAR(20)             | UNIQUE NOT NULL                          |
| password_hash        | VARCHAR(255)            | NOT NULL                                 |
| pin_hash             | VARCHAR(255)            | NOT NULL                                 |
| role                 | VARCHAR(20)             | NOT NULL DEFAULT 'USER'                  |
| account_locked       | BOOLEAN                 | NOT NULL DEFAULT false                   |
| lock_expires_at      | TIMESTAMP               |                                          |
| failed_login_count   | INT                     | NOT NULL DEFAULT 0                       |
| created_at           | TIMESTAMP               | NOT NULL                                 |
| updated_at           | TIMESTAMP               | NOT NULL                                 |

**Indexes:** `idx_users_email`, `idx_users_phone`

---

### Table: `login_attempts`

| Column        | Type         | Constraints                     |
|---------------|--------------|---------------------------------|
| id            | UUID         | PRIMARY KEY                     |
| user_id       | UUID         | FK → users(id) NOT NULL         |
| attempted_at  | TIMESTAMP    | NOT NULL                        |
| success       | BOOLEAN      | NOT NULL                        |
| ip_address    | VARCHAR(45)  |                                 |

**Indexes:** `idx_login_attempts_user_id`, `idx_login_attempts_attempted_at`

---

### Table: `wallets`

| Column            | Type             | Constraints                                                        |
|-------------------|------------------|--------------------------------------------------------------------|
| id                | UUID             | PRIMARY KEY                                                        |
| user_id           | UUID             | FK → users(id) UNIQUE NOT NULL                                     |
| total_balance     | NUMERIC(19,4)    | NOT NULL DEFAULT 0, CHECK (total_balance >= 0)                     |
| reserved_balance  | NUMERIC(19,4)    | NOT NULL DEFAULT 0, CHECK (reserved_balance >= 0)                  |
| status            | VARCHAR(20)      | NOT NULL DEFAULT 'ACTIVE'                                          |
| created_at        | TIMESTAMP        | NOT NULL                                                           |
| updated_at        | TIMESTAMP        | NOT NULL                                                           |

**Constraints:** `CHECK (total_balance >= reserved_balance)`
**Note:** `available_balance = total_balance − reserved_balance` is computed in the service layer, never stored.
**Indexes:** `idx_wallets_user_id`

---

### Table: `transactions`

| Column               | Type             | Constraints                                                  |
|----------------------|------------------|--------------------------------------------------------------|
| id                   | UUID             | PRIMARY KEY                                                  |
| request_id           | VARCHAR(255)     | UNIQUE NOT NULL — idempotency key                            |
| sender_id            | UUID             | FK → users(id)                                               |
| receiver_id          | UUID             | FK → users(id)                                               |
| amount               | NUMERIC(19,4)    | NOT NULL, CHECK (amount > 0)                                 |
| remark               | VARCHAR(500)     |                                                              |
| status               | VARCHAR(20)      | NOT NULL — PENDING / SUCCESS / FAILED / DECLINED             |
| type                 | VARCHAR(30)      | NOT NULL — P2P_TRANSFER                                      |
| settlement_due_date  | DATE             |                                                              |
| created_at           | TIMESTAMP        | NOT NULL                                                     |
| updated_at           | TIMESTAMP        | NOT NULL                                                     |
| settled_at           | TIMESTAMP        |                                                              |

**Indexes:** `idx_transactions_sender_id`, `idx_transactions_receiver_id`, `idx_transactions_status`, `idx_transactions_settlement_due_date`, `idx_transactions_request_id`

---

### Table: `ledger_entries`

| Column          | Type             | Constraints                      |
|-----------------|------------------|----------------------------------|
| id              | UUID             | PRIMARY KEY                      |
| transaction_id  | UUID             | FK → transactions(id) NOT NULL   |
| wallet_id       | UUID             | FK → wallets(id) NOT NULL        |
| entry_type      | VARCHAR(10)      | NOT NULL — DEBIT / CREDIT        |
| amount          | NUMERIC(19,4)    | NOT NULL, CHECK (amount > 0)     |
| description     | VARCHAR(500)     |                                  |
| created_at      | TIMESTAMP        | NOT NULL                         |

**Indexes:** `idx_ledger_entries_transaction_id`, `idx_ledger_entries_wallet_id`

---

### Table: `fraud_alerts`

| Column          | Type         | Constraints                                       |
|-----------------|--------------|---------------------------------------------------|
| id              | UUID         | PRIMARY KEY                                       |
| transaction_id  | UUID         | FK → transactions(id) NOT NULL                    |
| alert_type      | VARCHAR(30)  | NOT NULL — DAILY_LIMIT / DUPLICATE / HIGH_VALUE   |
| status          | VARCHAR(20)  | NOT NULL — PENDING / BLOCKED / DISMISSED          |
| created_at      | TIMESTAMP    | NOT NULL                                          |
| reviewed_at     | TIMESTAMP    |                                                   |
| reviewed_by     | UUID         | FK → users(id)                                    |

**Indexes:** `idx_fraud_alerts_transaction_id`, `idx_fraud_alerts_status`

---

### Table: `notifications`

| Column      | Type         | Constraints                       |
|-------------|--------------|-----------------------------------|
| id          | UUID         | PRIMARY KEY                       |
| user_id     | UUID         | FK → users(id) NOT NULL           |
| type        | VARCHAR(50)  | NOT NULL                          |
| message     | TEXT         | NOT NULL                          |
| read        | BOOLEAN      | NOT NULL DEFAULT false            |
| created_at  | TIMESTAMP    | NOT NULL                          |

**Indexes:** `idx_notifications_user_id`, `idx_notifications_read`

---

### Table: `audit_log`

| Column         | Type         | Constraints                   |
|----------------|--------------|-------------------------------|
| id             | UUID         | PRIMARY KEY                   |
| admin_id       | UUID         | FK → users(id) NOT NULL       |
| action         | VARCHAR(100) | NOT NULL                      |
| target_type    | VARCHAR(50)  |                               |
| target_id      | UUID         |                               |
| details        | TEXT         |                               |
| performed_at   | TIMESTAMP    | NOT NULL                      |

**Indexes:** `idx_audit_log_admin_id`, `idx_audit_log_performed_at`

---

### Table: `scheduled_job_log`

| Column             | Type         | Constraints                             |
|--------------------|--------------|------------------------------------------|
| id                 | UUID         | PRIMARY KEY                              |
| job_name           | VARCHAR(100) | NOT NULL                                 |
| started_at         | TIMESTAMP    | NOT NULL                                 |
| ended_at           | TIMESTAMP    |                                          |
| status             | VARCHAR(20)  | NOT NULL — RUNNING / SUCCESS / FAILED    |
| records_processed  | INT          |                                          |
| error_details      | TEXT         |                                          |

**Indexes:** `idx_job_log_job_name`, `idx_job_log_started_at`

---

### Table: `daily_summary`

| Column                    | Type             | Constraints              |
|---------------------------|------------------|--------------------------|
| id                        | UUID             | PRIMARY KEY              |
| summary_date              | DATE             | UNIQUE NOT NULL          |
| initiated_count           | INT              |                          |
| initiated_amount          | NUMERIC(19,4)    |                          |
| settled_count             | INT              |                          |
| settled_amount            | NUMERIC(19,4)    |                          |
| fraud_alert_count         | INT              |                          |
| new_registrations         | INT              |                          |
| pending_settlement_amount | NUMERIC(19,4)    |                          |
| created_at                | TIMESTAMP        | NOT NULL                 |

**Indexes:** `idx_daily_summary_date`

---

### Table: `settlement_batches`

| Column                | Type             | Constraints                         |
|-----------------------|------------------|-------------------------------------|
| id                    | UUID             | PRIMARY KEY                         |
| job_log_id            | UUID             | FK → scheduled_job_log(id)          |
| settled_count         | INT              |                                     |
| failed_count          | INT              |                                     |
| total_amount_settled  | NUMERIC(19,4)    |                                     |
| batch_run_at          | TIMESTAMP        | NOT NULL                            |

---

### Table: `merchants`

| Column          | Type         | Constraints                                              |
|-----------------|--------------|----------------------------------------------------------|
| id              | UUID         | PRIMARY KEY                                              |
| name            | VARCHAR(255) | NOT NULL                                                 |
| email           | VARCHAR(255) | UNIQUE NOT NULL                                          |
| status          | VARCHAR(20)  | NOT NULL DEFAULT 'ACTIVE' — ACTIVE / INACTIVE / SUSPENDED|
| registered_by   | UUID         | FK → users(id) — admin who registered the merchant      |
| created_at      | TIMESTAMP    | NOT NULL                                                 |
| updated_at      | TIMESTAMP    | NOT NULL                                                 |

**Indexes:** `idx_merchants_email`, `idx_merchants_status`

---

### Table: `check_please_reservations`

| Column            | Type             | Constraints                                                              |
|-------------------|------------------|--------------------------------------------------------------------------|
| id                | UUID             | PRIMARY KEY                                                              |
| user_id           | UUID             | FK → users(id) NOT NULL                                                  |
| wallet_id         | UUID             | FK → wallets(id) NOT NULL                                                |
| merchant_id       | UUID             | FK → merchants(id) NOT NULL                                              |
| max_amount        | NUMERIC(19,4)    | NOT NULL, CHECK (max_amount > 0)                                         |
| remaining_amount  | NUMERIC(19,4)    | NOT NULL, CHECK (remaining_amount >= 0)                                  |
| status            | VARCHAR(20)      | NOT NULL DEFAULT 'ACTIVE' — ACTIVE / CONSUMED / CANCELLED / EXPIRED     |
| expires_at        | TIMESTAMP        | NOT NULL                                                                 |
| created_at        | TIMESTAMP        | NOT NULL                                                                 |
| updated_at        | TIMESTAMP        | NOT NULL                                                                 |

**Constraints:** `CHECK (remaining_amount <= max_amount)`
**Indexes:** `idx_reservations_user_id`, `idx_reservations_merchant_id`, `idx_reservations_status`, `idx_reservations_expires_at`

---

### Table: `merchant_payments`

| Column           | Type             | Constraints                                                        |
|------------------|------------------|--------------------------------------------------------------------|
| id               | UUID             | PRIMARY KEY                                                        |
| request_id       | VARCHAR(255)     | UNIQUE NOT NULL — idempotency key                                  |
| reservation_id   | UUID             | FK → check_please_reservations(id) NOT NULL                        |
| user_id          | UUID             | FK → users(id) NOT NULL                                            |
| merchant_id      | UUID             | FK → merchants(id) NOT NULL                                        |
| wallet_id        | UUID             | FK → wallets(id) NOT NULL                                          |
| amount           | NUMERIC(19,4)    | NOT NULL, CHECK (amount > 0)                                       |
| status           | VARCHAR(20)      | NOT NULL — PENDING / CAPTURED / FAILED / REJECTED                  |
| ledger_entry_id  | UUID             | FK → ledger_entries(id)                                            |
| created_at       | TIMESTAMP        | NOT NULL                                                           |
| updated_at       | TIMESTAMP        | NOT NULL                                                           |

**Indexes:** `idx_merchant_payments_request_id`, `idx_merchant_payments_reservation_id`, `idx_merchant_payments_user_id`

---

## Key Technical Decisions

### KTD-001 — Available Balance Computation
`available_balance = total_balance − reserved_balance` is computed in the service layer on every read. It is **not** stored as a database column. This prevents the three values from going out of sync when only two are updated.

### KTD-002 — Business Day Calculation
Settlement due date = initiation date + 3 business days. Business days exclude Saturdays and Sundays. No public holiday adjustments are applied in this version. Timezone for "today" determination: Asia/Kolkata (IST). The `BusinessDayCalculator` utility accepts a pluggable holiday list so the calendar can be updated without changing settlement logic. Policy documented in `BusinessDayCalculator.java` Javadoc and in the Resolved Policy Decisions section of this document.

### KTD-003 — JWT Signing and Secret Management
- Algorithm: HS256
- Secret: read from environment variable `JWT_SECRET` via `${JWT_SECRET}` in `application.properties`
- The secret is **never** hard-coded, logged, or committed to source control
- Token expiry is configurable in `application.properties`

### KTD-004 — Idempotency Keys
- `transactions.request_id` has a `UNIQUE` database constraint
- `merchant_payments.request_id` has a `UNIQUE` database constraint
- On duplicate `request_id`: service returns the original result; no second processing occurs
- Constraint violation is caught and translated to `DuplicateRequestException` → HTTP 409

### KTD-005 — Concurrency Control
- Transfer initiation: `SELECT FOR UPDATE` on sender's wallet row before debit
- Merchant payment capture: `SELECT FOR UPDATE` on wallet row and reservation row within a single `@Transactional` block
- Reservation creation: `SELECT FOR UPDATE` on wallet row before reserving funds
- Reservation cancellation and expiry: `SELECT FOR UPDATE` on reservation row to prevent double-release

### KTD-006 — Settlement Job Isolation
Each transfer is settled in its own `@Transactional(propagation = REQUIRES_NEW)` method. This ensures:
- A single settlement failure does not roll back the entire batch
- Successfully settled transfers are committed immediately
- The outer job transaction records batch counts regardless of individual failures

### KTD-007 — Monetary Arithmetic
- All monetary operations use `BigDecimal` with `RoundingMode.HALF_UP`
- No `float` or `double` types are used anywhere in money calculations
- Division is avoided where possible; where necessary, scale is set to 4 decimal places

### KTD-008 — Timestamp Storage
- All timestamps stored in UTC in the database
- Frontend displays timestamps in user's local timezone (browser handles conversion)
- All scheduled jobs use `zone = "Asia/Kolkata"` (IST, UTC+5:30). India Standard Time does not observe daylight saving time; UTC offset is fixed at +5:30 year-round.
- Scheduled job trigger times (midnight reset, 11 PM reconciliation) are defined using Asia/Kolkata zone and documented in `application.properties`

---

## Resolved Policy Decisions

The following policy decisions were open during planning and have been formally resolved. They are recorded here as authoritative references for all implementation phases.

### PD-001 — Currency
Indian Rupee (INR, symbol ₹). All monetary values use `BigDecimal` in Java and `NUMERIC(19,4)` in PostgreSQL. Thresholds: ₹50,000 daily P2P limit; ₹8,000 high-value P2P alert.

### PD-002 — Business Day Calculation
Settlement due date = initiation date + 3 business days. Business days exclude Saturdays and Sundays. No public holiday adjustments in this version. Timezone for "today" determination: Asia/Kolkata (IST). Implemented in `BusinessDayCalculator.java`. Adding a holiday list requires explicit user approval before implementation.

### PD-003 — Timezone Policy
All scheduled jobs use Asia/Kolkata (IST, UTC+5:30, fixed — no DST). Database timestamps stored as UTC. Frontend displays timestamps in the user's browser timezone. Scheduled cron expressions use `zone = "Asia/Kolkata"`.
- Midnight daily-limit reset: `@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")`
- 11 PM daily summary/reconciliation: `@Scheduled(cron = "0 0 23 * * *", zone = "Asia/Kolkata")`
- Hourly settlement processor: `@Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")`
- Account auto-unlock: `@Scheduled(fixedDelay = 300000)` (timezone-independent)

India Standard Time (IST) does not observe daylight saving time; UTC offset is fixed at +5:30 year-round. No DST handling is required.

### PD-004 — Reservation Expiry Limits
Minimum expiry: 1 hour from creation time. Maximum expiry: 30 days from creation time. Configurable in `application.properties`:
```
check.please.reservation.expiry.min-hours=1
check.please.reservation.expiry.max-days=30
```
Validation rule: `expires_at` must satisfy `now + minHours <= expires_at <= now + maxDays`. Both limits must be enforced in `CheckPleaseService` before the reservation is persisted.

### PD-005 — Merchant-Payment Risk Policy
**Mandatory controls (always active for merchant payments):**
1. Authentication: valid JWT required
2. Ownership: user must own the reservation (never trust client-supplied user ID)
3. PIN validation: correct 4-digit transaction PIN required
4. Wallet status: wallet must be ACTIVE (not INACTIVE or FROZEN)
5. Merchant identity: merchant must match reservation's bound merchant and be ACTIVE
6. Account lock: locked accounts cannot make payments
7. Idempotency: duplicate `request_id` returns original result without re-processing

**Not applicable to merchant payments (P2P-specific rules):**
- Daily ₹50,000 P2P limit — does NOT apply to merchant payments
- Duplicate-transfer rule (same amount to same recipient within 2 min) — does NOT apply to merchant payments
- High-value ₹8,000 threshold alert — does NOT apply to merchant payments

**Implementation:** `FraudService.checkMerchantPayment()` is a distinct method from `FraudService.checkP2PTransfer()`. A valid ACTIVE Check Please reservation acts as a positive risk signal. No additional merchant-specific fraud block in MVP.

### PD-006 — Spring Boot and Java Version
Spring Boot 4.1.2 with Java 25. Confirmed available. T-010 verifies Maven artifact resolution, Java 25 compiler target, and annotation processor compatibility (Lombok, Jakarta validation). No downgrade path is planned; if incompatibility is found during T-010, stop and report before implementation continues.

---

## Security Architecture

### Authentication Flow
```
Client → POST /auth/login (email + password)
       → AuthController → AuthService
       → Load UserDetails → BCrypt verify password
       → Check account not locked
       → Issue JWT (HS256, exp = configured duration)
       ← Return JWT in response body
```

### Per-Request Authorization Flow
```
Client → Any protected endpoint (Authorization: Bearer <token>)
       → JwtAuthFilter (OncePerRequestFilter)
       → Validate JWT signature and expiry
       → Extract email → Load UserDetails
       → Set SecurityContextHolder
       → Controller → Service
       → @PreAuthorize checks role
       → Service validates ownership against JWT principal
```

### Ownership Enforcement
- All service methods that access user-owned resources accept the authenticated user's ID from `SecurityContextHolder`
- Client-supplied user IDs in request bodies or path parameters are **never** used for ownership checks
- Only used to identify the target resource (e.g., recipient in a transfer)

### PIN Verification
- Transaction PIN stored as BCrypt hash in `users.pin_hash`
- PIN verification occurs in service layer before any financial operation
- Failed PIN attempts do not count toward login failure tracking (separate concern)
- Account locking applies to login failures only

### Role-Based Access
- `USER` role: access own wallet, transactions, notifications, Check Please
- `ADMIN` role: access admin panel, freeze/unfreeze wallets, manage merchants, view all transactions, fraud review, job monitor, reconciliation, audit log
- `@PreAuthorize("hasRole('ADMIN')")` on all admin endpoints
- `@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")` on user endpoints where admins may also call

### Input Validation
- All request DTOs annotated with Jakarta Bean Validation (`@NotNull`, `@Positive`, `@Size`, `@Email`, etc.)
- Global `@ControllerAdvice` catches `MethodArgumentNotValidException` and returns a consistent error JSON structure
- All custom exceptions map to appropriate HTTP status codes via `@ExceptionHandler` methods
