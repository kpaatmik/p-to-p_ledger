---
inclusion: always
---

# P2P Payment Ledger — Project Instructions

These instructions are the persistent development rules for this project. Follow them in every session without exception.

---

## Technology Stack

- Java 25, Spring Boot 4.1.2 — confirmed. T-010 verifies Maven resolution, Java 25 compiler target, and annotation processor compatibility (Lombok, Jakarta validation) before domain code is written.
- Spring Data JPA, Spring Security with JWT, Spring Scheduler
- PostgreSQL; React; Maven pom.xml; application.properties
- No YAML configuration files. No Flyway.

---

## Financial Correctness Rules

- Use `BigDecimal` in Java and `NUMERIC(19,4)` in PostgreSQL for ALL monetary values. Never use `float` or `double`.
- Use `@Transactional` for ALL financial workflows.
- Use `SELECT FOR UPDATE` (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on wallet rows during transfer initiation, merchant payment capture, reservation creation, cancellation, and expiry.
- Never allow partial financial state to persist. Roll back the entire transaction on any failure.
- `available_balance = total_balance − reserved_balance`. Compute in the service layer. Never store `available_balance` as a column.
- Prevent duplicate debits, credits, refunds, and reservation releases using idempotency keys and database constraints.
- All monetary arithmetic uses `BigDecimal` with `RoundingMode.HALF_UP`. Scale is 4 decimal places.

---

## Security Rules

- Hash passwords and transaction PINs using BCrypt. Never store plaintext.
- Never log passwords, PINs, JWT tokens, or any secret value.
- Never trust client-supplied user IDs for ownership. Always derive ownership from the authenticated JWT principal in `SecurityContextHolder`.
- All financial operations require transaction PIN verification before any balance change.
- JWT secret must come from the `JWT_SECRET` environment variable referenced as `${JWT_SECRET}` in `application.properties`. Never commit the secret to source code.
- Apply `@PreAuthorize` for role-based access. Users access only their own data. Admins access admin panel functions.
- PIN verification failure and login failure are separate counters. Only login failures trigger account locking.

---

## Workflow Rules

- Complete and verify ONE task before starting the next.
- Every task must pass its acceptance criteria before being marked COMPLETE.
- Update `TASK_TRACKER.md` after each task is verified, recording actual test results.
- Do not mark a task COMPLETE based on generated code alone. Tests must pass.
- Stop and report if a blocker is encountered (missing credentials, ambiguous requirement, unresolved open decision, external dependency failure).
- All tasks are sequential within a phase unless explicitly noted as parallelizable.

---

## Architectural Rules

- Keep P2P delayed settlement completely separate from merchant payment capture. No shared service methods.
- Check Please must never bypass critical security controls (PIN, account lock, ownership, valid JWT, valid merchant identity).
- Define merchant-payment risk rules as a separate method from P2P fraud rules. Do not call P2P fraud rules directly for merchant payments.
- Use UUID primary keys throughout all entities.
- All timestamps stored in UTC in the database. Display timezone conversion handled in the frontend.
- Use `@Transactional(propagation = REQUIRES_NEW)` for each individual settlement within the settlement job loop. This isolates failures so one failed settlement does not roll back others.

---

## Code Quality Rules

- Validate all inputs using Jakarta Bean Validation (`@NotNull`, `@Positive`, `@Size`, `@Email`, etc.) on all request DTOs.
- Return consistent error responses using the `ErrorResponse` DTO via the global `GlobalExceptionHandler` (`@ControllerAdvice`).
- Write unit tests for every service class.
- Write integration tests for every controller.
- No unused dependencies. Pin all dependency versions in `pom.xml`.
- Do not introduce any new frameworks or libraries not already in `pom.xml` without explicit user approval.

---

## Database Rules

- All 14 entities must be implemented: `users`, `login_attempts`, `wallets`, `transactions`, `ledger_entries`, `fraud_alerts`, `notifications`, `audit_log`, `scheduled_job_log`, `daily_summary`, `settlement_batches`, `merchants`, `check_please_reservations`, `merchant_payments`.
- The `transactions.request_id` and `merchant_payments.request_id` columns must have `UNIQUE` database constraints for idempotency enforcement.
- The `wallets.user_id` column must have a `UNIQUE` database constraint (one wallet per user).
- CHECK constraints: `wallet.total_balance >= 0`, `wallet.reserved_balance >= 0`, `wallet.total_balance >= wallet.reserved_balance`, `check_please_reservations.remaining_amount >= 0`, `check_please_reservations.remaining_amount <= max_amount`.

---

## Resolved Decisions (all approved — no longer require user action)

- **OD-001 RESOLVED** — Weekends-only business-day exclusion (Saturdays and Sundays). No public holiday calendar without explicit user approval.
- **OD-002 RESOLVED** — Asia/Kolkata (IST, UTC+5:30) for all scheduled jobs. DB timestamps in UTC. No DST. Cron expressions use `zone = "Asia/Kolkata"`.
- **OD-003 RESOLVED** — Reservation expiry: min 1 hour, max 30 days. Configurable via `check.please.reservation.expiry.min-hours` and `check.please.reservation.expiry.max-days` in application.properties.
- **OD-004 RESOLVED** — Merchant-payment risk: mandatory controls always active (JWT, PIN, ownership, wallet status, merchant identity, account lock, idempotency). P2P daily limit, duplicate-transfer rule, and high-value alert do NOT apply to merchant payments. Implemented as separate `checkMerchantPayment()` method.
- **OD-005 RESOLVED** — Currency: INR (₹). BigDecimal in Java, NUMERIC(19,4) in PostgreSQL.
- **OD-006 RESOLVED** — Spring Boot 4.1.2 + Java 25 confirmed. T-010 verifies Maven resolution and toolchain.

## Scheduler Configuration Rules

All cron-based scheduled jobs MUST specify `zone = "Asia/Kolkata"` in the @Scheduled annotation:
- Settlement processor: `@Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")` — hourly
- Daily limit reset: `@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")` — midnight IST
- Daily summary/reconciliation: `@Scheduled(cron = "0 0 23 * * *", zone = "Asia/Kolkata")` — 11 PM IST
- Account auto-unlock: `@Scheduled(fixedDelay = 300000)` — every 5 minutes (no timezone needed)
- Reservation expiry: schedule configurable in application.properties

India Standard Time (IST) is UTC+5:30 and does NOT observe daylight saving time. The UTC offset is fixed year-round.

---

## Reference Documents

The following planning documents are located in the project root and should be consulted before each phase:

| Document | Location | Purpose |
|----------|----------|---------|
| Project Plan | `D:\Payment ledger\PROJECT_PLAN.md` | Phases, risks, open decisions, progress |
| Architecture | `D:\Payment ledger\ARCHITECTURE.md` | Package structure, schema, security design |
| Task Tracker | `D:\Payment ledger\TASK_TRACKER.md` | All tasks with acceptance criteria |
| Requirements RTM | `D:\Payment ledger\docs\REQUIREMENTS_TRACEABILITY_MATRIX.md` | Requirements to tasks and tests |
| Requirements Text | `D:\Payment ledger\document\requirements_text.txt` | Original requirements baseline |
