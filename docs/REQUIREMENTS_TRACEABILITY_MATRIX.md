# Requirements Traceability Matrix
## P2P Payment Ledger

> **Purpose:** Maps every requirement from all 13 sections of the requirements document to the implementing tasks and their acceptance tests.
> **Status values:** `Not Started` / `In Progress` / `Complete`

---

## Section 0 — Configuration

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-001 | Backend language: Java 25 | T-010 | `mvn clean compile` succeeds with Java 25 source/target | Not Started |
| REQ-002 | Backend framework: Spring Boot 4.1.2 — confirmed version; Java 25 toolchain compatibility verified in T-010 | T-010 | POM references Spring Boot 4.1.2; app starts; Java 25 compiler target confirmed; no annotation processor incompatibilities | Not Started |
| REQ-003 | Spring Data JPA for persistence | T-010, T-020 | JPA entities created; repositories query DB | Not Started |
| REQ-004 | Spring Security with JWT for authentication | T-010, T-030, T-031 | JWT issued on login; protected endpoints enforce auth | Not Started |
| REQ-005 | Spring Scheduler for background jobs | T-010, T-091 | Scheduled jobs initialize and run at configured intervals | Not Started |
| REQ-006 | PostgreSQL as the database | T-011, T-015 | App connects to PostgreSQL; schema created | Not Started |
| REQ-007 | React for the frontend | T-013, T-015 | Frontend builds and serves | Not Started |
| REQ-008 | Maven pom.xml as build tool | T-010 | `mvn clean package` succeeds | Not Started |
| REQ-009 | application.properties only; no YAML configuration | T-011 | No `.yml` or `.yaml` files exist in project | Not Started |
| REQ-010 | No Flyway | T-010, T-011 | No Flyway dependency in POM; no migration scripts | Not Started |

---

## Section 1 — Project Overview and Scope

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-011 | P2P digital wallet: register, load money, transfer, track | T-032, T-040, T-051, T-053 | Full user registration and transfer lifecycle works | Not Started |
| REQ-012 | ACH-style delayed settlement model | T-051, T-091, T-092 | Transfer stays PENDING until settlement date; then settles | Not Started |
| REQ-013 | Basic fraud detection | T-060, T-061, T-062 | Daily limit, duplicate, and high-value rules enforced | Not Started |
| REQ-014 | Notifications for users | T-100, T-101 | Users receive in-app notifications for key events | Not Started |
| REQ-015 | Background settlement and housekeeping jobs | T-091, T-094, T-095, T-096, T-097 | All five scheduled jobs run as configured | Not Started |
| REQ-016 | Admin monitoring view | T-103, T-104, T-105, T-106, T-107, T-108, T-109 | All admin panel endpoints accessible and return correct data | Not Started |
| REQ-017 | Check Please feature (merchant reservation + payment) | T-070 through T-082 | Full Check Please lifecycle works | Not Started |
| REQ-018 | Load money is simulated; no real bank integration | T-040 | Load endpoint increases balance without bank call | Not Started |
| REQ-019 | Merchant payments simulated in MVP; no payment gateway | T-075, T-076 | Merchant payment captured without external gateway call | Not Started |
| REQ-020 | Implementation agent may choose API routes and DTOs | T-010 through T-149 | All functional requirements met regardless of route names | Not Started |

---

## Section 2 — User Registration and Authentication

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-021 | Register with name, email, phone, password, 4-digit PIN | T-032 | Registration endpoint accepts all five fields and saves user | Not Started |
| REQ-022 | Successful registration creates exactly one ACTIVE wallet with ₹0 | T-032 | After registration, exactly one wallet exists with balance=0, status=ACTIVE | Not Started |
| REQ-023 | Login with email + password; JWT issued on success | T-033 | Login returns JWT; JWT is valid and contains correct claims | Not Started |
| REQ-024 | Support USER and ADMIN roles | T-031 | USER cannot access admin endpoints; ADMIN can | Not Started |
| REQ-025 | Users access only their own data | T-031, T-053, T-101 | User A cannot read user B's wallet, transactions, or notifications | Not Started |
| REQ-026 | Transaction PIN separate from login password | T-032, T-051 | PIN required for transfer; login uses password only | Not Started |
| REQ-027 | PIN required for money transfers | T-051 | Transfer without PIN or with wrong PIN is rejected | Not Started |
| REQ-028 | PIN required for applicable merchant payments | T-075 | Merchant payment without PIN or with wrong PIN is rejected | Not Started |
| REQ-029 | Track consecutive failed login attempts | T-034 | `failed_login_count` increments on each failure | Not Started |
| REQ-030 | Lock account after 5 consecutive failed login attempts for 30 minutes | T-034 | After 5 failures, account locked with `lock_expires_at = now + 30 min` | Not Started |
| REQ-031 | Automatically unlock eligible accounts | T-035, T-095 | Account unlocked by auto-unlock job after 30 minutes | Not Started |
| REQ-032 | Notify user on auto-unlock | T-035, T-100 | Unlock notification sent when account is unlocked | Not Started |
| REQ-033 | Hash passwords using secure hashing; never store plaintext | T-032 | Password field in DB is BCrypt hash; no plaintext found | Not Started |
| REQ-034 | Hash PINs using secure hashing; never store plaintext | T-032 | PIN field in DB is BCrypt hash; no plaintext found | Not Started |
| REQ-035 | Never log secrets (passwords, PINs, JWT tokens) | T-031, T-032, T-033 | Log output contains no passwords, PINs, or JWT token values | Not Started |

---

## Section 3 — Wallet Management

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-036 | Every user has exactly one wallet created at registration | T-032 | Registration creates exactly one wallet per user; duplicate creation rejected by UNIQUE constraint | Not Started |
| REQ-037 | Load simulated money: validate amount and record operation | T-040 | Load endpoint accepts positive amount; rejects zero/negative; creates ledger entry | Not Started |
| REQ-038 | Display total balance, available balance, reserved balance, today's spending | T-041 | GET /wallet returns all four values with correct amounts | Not Started |
| REQ-039 | Wallet statuses: ACTIVE, INACTIVE, FROZEN | T-042 | Wallet status field accepts only three values; transitions enforced | Not Started |
| REQ-040 | ACTIVE wallets can send and receive | T-051, T-092 | Transfer initiation and settlement succeed for ACTIVE wallets | Not Started |
| REQ-041 | INACTIVE wallet: blocks outgoing; incoming P2P declined → sender refunded + notified; owner can reactivate | T-042, T-051, T-093 | Sending from INACTIVE fails; settlement to INACTIVE = DECLINED + refund; owner reactivates | Not Started |
| REQ-042 | FROZEN wallet: blocks outgoing; incoming P2P declined → sender refunded + notified; only admin can freeze/unfreeze | T-042, T-051, T-093 | Sending from FROZEN fails; settlement to FROZEN = DECLINED + refund; admin controls | Not Started |
| REQ-043 | Available balance = total balance − reserved balance; reserved funds not spendable on unrelated transactions | T-043, T-071 | Available balance computed correctly; reserved funds unavailable for P2P and other merchant transactions | Not Started |
| REQ-044 | Use BigDecimal in Java and NUMERIC in PostgreSQL; no floats | T-020, T-021, T-022, T-026, T-027 | No float/double types in any monetary field; all NUMERIC(19,4) in DB | Not Started |

---

## Section 4 — P2P Transfers and Delayed Settlement

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-045 | Search for registered recipients by phone or email | T-050 | Search returns matching user; excludes caller; returns safe fields only | Not Started |
| REQ-046 | Transfer requires: recipient selection, positive amount, optional remark, 4-digit PIN confirmation | T-051 | Transfer rejected without PIN; rejected for zero/negative amount; remark optional | Not Started |
| REQ-047 | Client-generated request_id for idempotency | T-052 | Duplicate request_id returns original result; no second debit | Not Started |
| REQ-048 | On initiation: sender available balance reduced immediately | T-051 | After initiation, sender available balance decreases by transfer amount | Not Started |
| REQ-049 | On initiation: sender DEBIT ledger entry created | T-051 | Ledger entry of type DEBIT for sender created at initiation | Not Started |
| REQ-050 | On initiation: transfer status = PENDING | T-051 | Transaction status is PENDING immediately after initiation | Not Started |
| REQ-051 | Receiver not credited or notified until settlement | T-051, T-092 | Receiver balance unchanged at initiation; receiver notified only after settlement | Not Started |
| REQ-052 | Record settlement_due_date = 3 business days after initiation (weekends excluded) | T-051, T-090 | settlement_due_date is exactly 3 business days later; weekends excluded (Saturdays and Sundays); Asia/Kolkata timezone used for "today" determination | Not Started |
| REQ-053 | Holiday calendar policy must be documented | T-090 | BusinessDayCalculator Javadoc documents weekends-only exclusion and Asia/Kolkata timezone policy for "today"; ARCHITECTURE.md documents the same | Not Started |
| REQ-054 | Settlement job: credit receiver, create CREDIT entry, status → SUCCESS, notify both | T-091, T-092 | After settlement job runs, receiver credited, CREDIT entry created, status SUCCESS, both notified | Not Started |
| REQ-055 | Settlement failure: status → FAILED, refund sender exactly once, notify sender, continue other transfers | T-091, T-093 | Failed settlement marks FAILED, refunds sender once, job continues | Not Started |
| REQ-056 | Receiver wallet INACTIVE/FROZEN at settlement: status → DECLINED, refund sender exactly once, notify sender | T-093 | Settlement to INACTIVE/FROZEN wallet = DECLINED + refund + sender notification | Not Started |
| REQ-057 | Valid and auditable transaction status transitions | T-051, T-092, T-093 | Status only transitions through valid paths: PENDING→SUCCESS, PENDING→FAILED, PENDING→DECLINED | Not Started |

---

## Section 5 — Fraud Controls

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-058 | Daily P2P limit: ₹50,000 per user; exceeding → blocked + fraud alert | T-060 | Transfer blocked when cumulative daily amount exceeds ₹50,000; FraudAlert DAILY_LIMIT created | Not Started |
| REQ-059 | Duplicate detection: same amount to same recipient within 2 minutes → blocked + fraud alert | T-061 | Identical transfer within 2-minute window blocked; FraudAlert DUPLICATE created | Not Started |
| REQ-060 | High-value threshold: P2P > ₹8,000 → allowed into PENDING but fraud alert created for admin | T-062 | Transfer > ₹8,000 proceeds to PENDING; FraudAlert HIGH_VALUE created for admin | Not Started |
| REQ-061 | Admin may block flagged pending transfer before settlement → FAILED, sender refunded, both notified | T-064 | Admin blocks HIGH_VALUE alert; transaction FAILED; sender refunded; both notified | Not Started |
| REQ-062 | If not blocked by settlement time → settles normally | T-091 | Unblocked HIGH_VALUE transfer settles normally at due date | Not Started |
| REQ-063 | All fraud alerts visible in admin queue | T-063 | GET /admin/fraud-alerts returns all alert types | Not Started |
| REQ-064 | Check Please must not disable critical security controls | T-075, T-078 | Critical checks (PIN, ownership, account lock) always enforced for merchant payments | Not Started |
| REQ-065 | Risk policies must distinguish P2P transfers from merchant payments | T-078 | Separate fraud check method for merchant payments; documented policy | Not Started |

---

## Section 6 — Transaction History and Statistics

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-066 | Mini-statement: last 10 transactions | T-053 | GET /transactions/mini returns exactly 10 most recent transactions | Not Started |
| REQ-067 | Paginated full history with date range, type, and status filters | T-053 | Paginated history with all filter combinations returns correct subsets | Not Started |
| REQ-068 | Pending P2P transfers show status, expected settlement date, days remaining | T-053, T-054 | PENDING transaction response includes settlement_due_date and days_remaining | Not Started |
| REQ-069 | Transaction details include related ledger entries | T-054 | GET /transactions/{id} returns all ledger entries for that transaction | Not Started |
| REQ-070 | Pending P2P has only sender DEBIT entry until settled | T-054 | PENDING transaction detail shows exactly one DEBIT entry; no CREDIT yet | Not Started |
| REQ-071 | Personal statistics: total sent, total received, this month's count | T-055 | Statistics endpoint returns correct totals for settled transactions | Not Started |
| REQ-072 | Users access only their own transaction and ledger data | T-053, T-054, T-055 | User A cannot view user B's transaction history or statistics | Not Started |

---

## Section 7 — Notifications

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-073 | In-app notification: transfer initiation | T-051, T-100 | Notification created for sender on transfer initiation | Not Started |
| REQ-074 | In-app notification: successful settlement | T-092, T-100 | Notifications created for both sender and receiver on settlement success | Not Started |
| REQ-075 | In-app notification: transfer failure or decline | T-093, T-100 | Notification created for sender on settlement failure or wallet decline | Not Started |
| REQ-076 | In-app notification: blocked transactions | T-060, T-061, T-064, T-100 | Notification created when transfer blocked by fraud rule or admin | Not Started |
| REQ-077 | In-app notification: fraud alerts | T-062, T-100 | Notification created when HIGH_VALUE fraud alert generated | Not Started |
| REQ-078 | In-app notification: account lock | T-034, T-100 | Notification created when account locked after 5 failures | Not Started |
| REQ-079 | In-app notification: account unlock | T-035, T-095, T-100 | Notification created when account auto-unlocked | Not Started |
| REQ-080 | Notify both sender and receiver after successful P2P settlement | T-092 | Both sender and receiver notification records created on SUCCESS | Not Started |
| REQ-081 | Notify sender when transfer fails, is declined, or is refunded | T-093 | Sender notification created for FAILED and DECLINED outcomes | Not Started |
| REQ-082 | Unread count on notification bell | T-102, T-125 | GET /notifications/unread-count returns correct integer | Not Started |
| REQ-083 | Users can view notifications and mark as read | T-101, T-125 | GET /notifications returns list; PATCH marks individual as read | Not Started |
| REQ-084 | Check Please notifications: reservation creation | T-071, T-100 | Notification created on successful reservation creation | Not Started |
| REQ-085 | Check Please notifications: merchant payment success/failure | T-076, T-077, T-100 | Notifications created for both payment capture and failure | Not Started |
| REQ-086 | Check Please notifications: reservation cancellation | T-074, T-100 | Notification created on reservation cancellation | Not Started |
| REQ-087 | Check Please notifications: reservation expiry | T-097, T-100 | Notification created when reservation expires | Not Started |

---

## Section 8 — Scheduled Background Jobs

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-088 | ACH settlement processor runs hourly | T-091 | Job has `@Scheduled(cron = "0 0 * * * *")` or equivalent; runs every hour | Not Started |
| REQ-089 | Processes PENDING transfers where settlement_due_date has arrived | T-091 | Only PENDING transactions with past/today due date are processed | Not Started |
| REQ-090 | For each successful settlement: credit receiver, write CREDIT entry, update status, notify both | T-092 | All four actions occur atomically per settlement | Not Started |
| REQ-091 | For individual settlement failure: mark FAILED, refund once, log, continue | T-093 | Single failure does not stop batch; refund is exactly one | Not Started |
| REQ-092 | Daily limit reset runs at midnight; timezone policy documented | T-094 | Daily limit resets at midnight IST (Asia/Kolkata); cron expression specifies `zone = "Asia/Kolkata"`; timezone documented | Not Started |
| REQ-093 | Account auto-unlock runs every 5 minutes | T-095 | Job runs on 5-minute schedule; unlocks eligible accounts | Not Started |
| REQ-094 | Daily summary and reconciliation runs at 11 PM | T-096 | Job runs at 11 PM IST (Asia/Kolkata); cron expression specifies `zone = "Asia/Kolkata"`; DailySummary record created | Not Started |
| REQ-095 | Summary covers: initiated, settled, fraud alerts, registrations, pending settlement | T-096 | DailySummary record has all five metric fields populated | Not Started |
| REQ-096 | Reconciliation compares debit vs credit ledger totals for settled transfers; pending excluded | T-096 | Reconciliation query excludes PENDING transactions | Not Started |
| REQ-097 | Reconciliation mismatch → critical admin notification | T-096, T-100 | Admin notification created when debit/credit totals do not match | Not Started |
| REQ-098 | Check Please expiry processing: releases reserved funds, marks EXPIRED, no double-release | T-097 | Expired reservations marked EXPIRED; reserved_balance released once | Not Started |
| REQ-099 | Every job run logs: start time, end time, success/failure, records processed, errors | T-098 | ScheduledJobLog record created for each job run with all fields | Not Started |
| REQ-100 | Each settlement batch records: settled count, failed count, total amount settled | T-091, T-098 | SettlementBatch record created per batch run with all three counts | Not Started |

---

## Section 9 — Admin Panel and Audit

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-101 | Dashboard: registered users, today's initiated/settled counts and volume | T-103, T-130 | Dashboard endpoint returns all six metrics | Not Started |
| REQ-102 | Dashboard: total pending settlement amount | T-103 | Pending settlement total sum correct | Not Started |
| REQ-103 | Dashboard: pending fraud alerts count | T-103 | Pending fraud alert count correct | Not Started |
| REQ-104 | Dashboard: 30-day daily transaction volume chart data | T-103, T-130 | 30-day array of date/volume pairs returned | Not Started |
| REQ-105 | User list: wallet balance, status, registration date, recent transactions | T-104, T-131 | Admin user list includes all four fields per user | Not Started |
| REQ-106 | Admin can freeze and unfreeze wallets | T-042, T-111, T-131 | Admin freeze/unfreeze changes wallet status and writes audit log | Not Started |
| REQ-107 | Transaction monitoring: all transactions with date/status/amount filters | T-105, T-132 | Admin transaction list shows all users' transactions with working filters | Not Started |
| REQ-108 | Transaction monitoring: ledger details and clear pending-state presentation | T-105 | Admin transaction detail shows ledger entries; PENDING state clearly labeled | Not Started |
| REQ-109 | Settlement monitor: pending transfers, due-today/tomorrow/overdue counts | T-106, T-133 | Settlement monitor returns correct counts in each category | Not Started |
| REQ-110 | Admin can manually trigger settlement for eligible transaction | T-106, T-111 | Manual trigger settles specific PENDING transaction; audit log written | Not Started |
| REQ-111 | Fraud review: pending alerts; admin can block or dismiss eligible HIGH_VALUE alerts | T-107, T-064, T-132 | Block and dismiss actions work for PENDING alerts | Not Started |
| REQ-112 | Settled transaction alerts are read-only (no block/dismiss) | T-107 | Block/dismiss on settled transaction alert returns error | Not Started |
| REQ-113 | Job monitor: last 20 scheduled job runs and results | T-108, T-134 | Job monitor returns last 20 runs ordered by most recent | Not Started |
| REQ-114 | Reconciliation: settled debit vs credit totals, pending amount, mismatches | T-109, T-134 | Reconciliation endpoint returns all three values; mismatch flagged | Not Started |
| REQ-115 | Admin can register merchants; activate, deactivate, or suspend them | T-110, T-131 | Merchant CRUD endpoints work; all status transitions enforced | Not Started |
| REQ-116 | All admin actions written to audit log | T-111 | Audit log record created for every admin action across all admin endpoints | Not Started |

---

## Section 10 — Check Please

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-117 | Only ACTIVE merchants eligible for new reservations and eligible payments | T-070, T-071, T-075 | Reservation creation fails for INACTIVE/SUSPENDED merchant | Not Started |
| REQ-118 | User reserves up to maximum amount; qualifying payment ≤ remaining reserved amount | T-071, T-075 | Payment exceeding remaining_amount is rejected | Not Started |
| REQ-119 | User selects expiry timestamp; system validates it is in the future and within configurable limits | T-071 | Past expiry rejected; expiry < 1 hour from now rejected (min=1h); expiry > 30 days from now rejected (max=30d); both limits configurable in application.properties | Not Started |
| REQ-120 | Reserved amount set aside from available balance; cannot be used for unrelated P2P or merchant transactions | T-071, T-043 | After reservation, available_balance decreases; P2P transfer limited by reduced available balance | Not Started |
| REQ-121 | Valid reservation = positive signal under merchant-risk policy; mandatory security checks remain active | T-078 | Merchant payments with valid reservation still enforce PIN, ownership, account lock checks | Not Started |
| REQ-122 | Merchant payments captured immediately (not delayed like P2P) | T-076 | Merchant payment capture completes in same request; no settlement_due_date | Not Started |
| REQ-123 | Failure before capture: reserved funds NOT debited; reservation available for retry if still active | T-077 | After payment failure, balance unchanged, reservation still ACTIVE | Not Started |
| REQ-124 | Successful payment: consumes reservation, debits payment amount, releases unused reserved amount, marks CONSUMED | T-076 | After capture: total_balance -payment_amount, reserved_balance -max_remaining, reservation CONSUMED | Not Started |
| REQ-125 | Reservation authorizes only ONE successful payment; unused remainder released after capture | T-076 | Reservation CONSUMED after one payment; second payment attempt fails | Not Started |
| REQ-126 | Reservation states: ACTIVE → CONSUMED / CANCELLED / EXPIRED | T-074, T-076, T-097 | All three terminal state transitions work; terminal reservations cannot authorize further payments | Not Started |
| REQ-127 | User cannot reserve more than available balance | T-071 | Reservation exceeding available balance rejected | Not Started |
| REQ-128 | Reservation bound to one user, one wallet, one merchant | T-071, T-072 | Reservation entity has user_id, wallet_id, merchant_id; payment validated against all three | Not Started |
| REQ-129 | Payment merchant must match reservation merchant | T-072 | Merchant mismatch in payment request rejected | Not Started |
| REQ-130 | Payment amount: positive and ≤ remaining reserved amount | T-075 | Zero/negative amount rejected; amount > remaining_amount rejected | Not Started |
| REQ-131 | Inactive/suspended merchant: no new reservations or eligible payments | T-071, T-075 | Reservation and payment for INACTIVE/SUSPENDED merchant rejected | Not Started |
| REQ-132 | Inactive/frozen wallet: cannot make merchant payment | T-075 | Merchant payment from INACTIVE/FROZEN wallet rejected | Not Started |
| REQ-133 | Critical security blocks override Check Please | T-078 | Invalid PIN, invalid ownership, compromised account block even valid reservations | Not Started |
| REQ-134 | Idempotency to prevent double debit | T-082 | Duplicate merchant payment request_id returns original result; no second debit | Not Started |
| REQ-135 | DB transactions + row locking to prevent concurrent payments consuming same reservation/funds | T-081 | Concurrent payment test: exactly one capture; no double debit | Not Started |
| REQ-136 | Failed attempts don't consume reservation if no capture occurred | T-077 | Failed payment leaves reservation ACTIVE and balance unchanged | Not Started |
| REQ-137 | P2P fraud rules must NOT be blindly reused for merchant payments; define explicit merchant-payment risk rules | T-078 | `checkMerchantPayment()` is a separate method from `checkP2PTransfer()`; P2P daily limit, duplicate-transfer rule, and high-value threshold do not apply to merchant payments; policy documented in code comments and ARCHITECTURE.md | Not Started |
| REQ-138 | Accounting: available = total − reserved | T-043 | Available balance formula verified by integration test across all scenarios | Not Started |
| REQ-139 | Creating reservation increases reserved balance; does not change total balance | T-071 | After reservation: total_balance unchanged, reserved_balance increased by max_amount | Not Started |
| REQ-140 | Successful payment: decreases total balance by captured amount, releases full remaining reservation | T-076 | After payment: total_balance -payment_amount, reserved_balance -original_remaining | Not Started |
| REQ-141 | Cancellation/expiry: no change to total balance; reduces reserved balance | T-074, T-097 | After cancel/expiry: total_balance unchanged, reserved_balance -remaining_amount | Not Started |
| REQ-142 | All wallet/reservation/payment/ledger changes commit atomically | T-076, T-144 | Simulated failure mid-capture results in complete rollback; no partial state | Not Started |

---

## Section 11 — Database Requirements

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-143 | 14 logical entities: users, login_attempts, wallets, transactions, ledger_entries, fraud_alerts, notifications, audit_log, scheduled_job_log, daily_summary, settlement_batches, merchants, check_please_reservations, merchant_payments | T-020–T-027 | All 14 tables present in DB schema | Not Started |
| REQ-144 | Use foreign keys, uniqueness constraints, CHECK constraints, and indexes | T-020–T-027 | DB schema inspection confirms FK, UNIQUE, CHECK, and index presence | Not Started |
| REQ-145 | UUID or consistent ID convention | T-020 | All entity IDs are UUID type | Not Started |
| REQ-146 | NUMERIC for monetary values; BigDecimal in Java | T-020–T-027 | No float/double in any monetary column; all NUMERIC(19,4) | Not Started |
| REQ-147 | Transaction boundaries and locking for concurrent financial operations | T-044, T-076, T-081 | Concurrency tests confirm row-level locks prevent data corruption | Not Started |
| REQ-148 | Schema preserves original P2P ledger and settlement behaviour while adding Check Please | T-027 | Settlement and Check Please tests both pass in same schema | Not Started |

---

## Section 12 — Security, Consistency, and Operational Requirements

| REQ-ID  | Requirement Description | Implementing Task(s) | Acceptance Test | Status |
|---------|------------------------|----------------------|-----------------|--------|
| REQ-149 | Spring Security with JWT and role-based authorization | T-031 | All protected endpoints enforce JWT auth; admin endpoints enforce ADMIN role | Not Started |
| REQ-150 | Never trust client-supplied user IDs for ownership checks | T-031, T-051 | Service layer uses JWT principal ID; test confirms cross-user access blocked | Not Started |
| REQ-151 | Hash passwords and PINs; never log secrets | T-032, T-033, T-035 | DB contains only BCrypt hashes; log scan finds no secrets | Not Started |
| REQ-152 | Validate input and return consistent error responses | T-012 | Invalid input returns consistent ErrorResponse JSON | Not Started |
| REQ-153 | @Transactional for financial workflows; appropriate row locking | T-044, T-051, T-076 | Financial operations are transactional; SELECT FOR UPDATE on wallet rows | Not Started |
| REQ-154 | Enforce idempotency via DB constraints; return original results on retry | T-052, T-082 | Duplicate request_id handled correctly in both P2P and merchant payment | Not Started |
| REQ-155 | Settlement, refunds, expiry, cancellation, and capture safe to retry | T-093, T-097, T-074 | Double-run tests confirm no double debit/credit/release | Not Started |
| REQ-156 | Prevent duplicate debits, credits, refunds, and reservation releases | T-044, T-093, T-097 | Idempotency tests confirm no duplicates under retry and concurrency | Not Started |
| REQ-157 | Keep P2P delayed settlement separate from merchant payment capture | T-091, T-076 | No shared code path between settlement job and merchant capture | Not Started |
| REQ-158 | Log job execution and admin actions | T-098, T-111 | Every job run and admin action has log record | Not Started |
| REQ-159 | Document timezone, business-day, and reservation-expiry policies | T-090, T-094, T-119, T-146 | Policies documented in ARCHITECTURE.md and application.properties comments | Not Started |
| REQ-160 | Simulated educational wallet; not production-ready | T-146 | README clearly states this is a simulated educational project | Not Started |

---

## Section 13 — Acceptance Criteria

| REQ-ID  | Acceptance Criterion | Implementing Task(s) | Verifying Test | Status |
|---------|---------------------|----------------------|----------------|--------|
| AC-001  | Registration creates exactly one ACTIVE wallet with ₹0 initial balance | T-032, T-037 | `AuthControllerIT` — register user, verify single wallet with balance=0 and status=ACTIVE | Not Started |
| AC-002  | JWT authentication and USER/ADMIN access controls work | T-031, T-033, T-037 | `AuthorizationTest` — valid JWT accesses protected endpoint; invalid JWT returns 401; ADMIN endpoint returns 403 for USER | Not Started |
| AC-003  | Five consecutive failed login attempts cause 30-minute lock; auto-unlock and notification work | T-034, T-035, T-095, T-036, T-037 | `AuthControllerIT` — 5 failures lock account; lock_expires_at set; auto-unlock job unlocks; notification sent | Not Started |
| AC-004  | P2P transfers debit immediately and settle after three business days | T-051, T-090, T-091, T-092, T-140 | `P2PTransferE2ETest` — balance debited at initiation; settlement settles after 3 business days | Not Started |
| AC-005  | Repeated P2P request IDs do not cause duplicate transfers or debits | T-052, T-058 | `TransferIdempotencyTest` — duplicate request_id results in one transaction, one debit | Not Started |
| AC-006  | Daily limits, duplicate detection, and high-value fraud alerts work as specified | T-060, T-061, T-062, T-065, T-066 | `FraudControllerIT` — all three fraud rules trigger correctly with correct alert types | Not Started |
| AC-007  | Settlement failures and receiver wallet rejections refund the sender exactly once | T-093, T-099 | `SettlementJobTest` — failure and FROZEN/INACTIVE receiver scenarios each refund sender exactly once | Not Started |
| AC-008  | Users can view paginated transaction history, ledger details, notifications, and personal statistics | T-053, T-054, T-055, T-101, T-057 | `TransactionControllerIT` — all history endpoints return paginated correct data | Not Started |
| AC-009  | Admin dashboard, fraud review, job monitoring, settlement monitoring, audit, and reconciliation available | T-103–T-112, T-130–T-134 | `AdminControllerIT` — all admin endpoints return correct data | Not Started |
| AC-010  | Users can reserve funds for an active registered merchant with a maximum amount and user-selected expiry | T-071, T-080 | `CheckPleaseControllerIT` — reservation created; balance changes verified | Not Started |
| AC-011  | Reserved funds cannot be spent on unrelated transactions | T-071, T-043, T-080 | `CheckPleaseControllerIT` — P2P transfer attempt after reservation respects reduced available balance | Not Started |
| AC-012  | A valid eligible merchant payment consumes the reservation once and releases unused reserved funds | T-076, T-080 | `CheckPleaseE2ETest` — payment capture produces correct balance changes; reservation CONSUMED | Not Started |
| AC-013  | Expired or cancelled reservations release remaining reserved funds exactly once | T-074, T-097, T-080 | `ReservationExpiryJobTest` and `CheckPleaseControllerIT` — cancel and expiry each release funds once | Not Started |
| AC-014  | Check Please does not bypass critical security controls | T-075, T-078, T-079 | `CheckPleaseServiceTest` — wrong PIN, locked account, invalid ownership all block merchant payment despite valid reservation | Not Started |
| AC-015  | Merchant payment retries are idempotent and concurrent operations preserve wallet and ledger consistency | T-082, T-081 | `MerchantPaymentIdempotencyTest` and `MerchantPaymentConcurrencyTest` — both pass | Not Started |
| AC-016  | All scheduled jobs and admin actions are logged | T-098, T-111, T-099, T-112 | After running all jobs and performing all admin actions, corresponding log records exist | Not Started |
| AC-017  | Automated tests cover ownership, expiry, concurrency, refunds, payment failure, fraud policy, and reconciliation | T-143, T-142, T-099, T-079, T-065, T-109 | All listed test classes pass; coverage report shows ≥80% service layer coverage | Not Started |

---

## Traceability Summary

| Sections Covered | Total Requirements | Tasks Referenced |
|------------------|--------------------|------------------|
| Configuration (0) | REQ-001 to REQ-010 (10) | T-010, T-011, T-013, T-015 |
| Project Overview (1) | REQ-011 to REQ-020 (10) | T-032, T-040, T-051, T-053, T-091 |
| Registration & Auth (2) | REQ-021 to REQ-035 (15) | T-031–T-037 |
| Wallet Management (3) | REQ-036 to REQ-044 (9) | T-040–T-046 |
| P2P Transfers (4) | REQ-045 to REQ-057 (13) | T-050–T-058, T-090–T-093 |
| Fraud Controls (5) | REQ-058 to REQ-065 (8) | T-060–T-066 |
| Transaction History (6) | REQ-066 to REQ-072 (7) | T-053–T-057 |
| Notifications (7) | REQ-073 to REQ-087 (15) | T-100–T-102 |
| Scheduled Jobs (8) | REQ-088 to REQ-100 (13) | T-090–T-099 |
| Admin Panel (9) | REQ-101 to REQ-116 (16) | T-103–T-112 |
| Check Please (10) | REQ-117 to REQ-142 (26) | T-070–T-082 |
| Database (11) | REQ-143 to REQ-148 (6) | T-020–T-027 |
| Security & Ops (12) | REQ-149 to REQ-160 (12) | T-031, T-044, T-052, T-076, T-093, T-098, T-111 |
| Acceptance Criteria (13) | AC-001 to AC-017 (17) | T-036–T-037, T-057–T-058, T-065–T-066, T-079–T-082, T-099, T-112, T-140–T-149 |

**Total unique requirements tracked: 177 (REQ-001–REQ-160, AC-001–AC-017)**
