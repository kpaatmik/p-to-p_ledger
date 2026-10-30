/**
 * Admin API service.
 *
 * All paths under /admin (class-level @RequestMapping on AdminController).
 * Fraud alerts live under /admin/fraud-alerts (AdminFraudAlertController).
 */

import apiClient from "../services/api.js";

// ── Dashboard ──────────────────────────────────────────────────────────────

/** GET /admin/dashboard
 *  Response: AdminDashboardResponse {
 *    totalUsers, initiatedCount, initiatedAmount, settledCount, settledAmount,
 *    pendingSettlementAmount, pendingFraudAlerts,
 *    dailyVolumesLast30Days: [{ date, amount }, ...]
 *  }
 */
export async function getAdminDashboard() {
  const response = await apiClient.get("/admin/dashboard");
  return response.data;
}

// ── Users ──────────────────────────────────────────────────────────────────

/** GET /admin/users — Spring Page<AdminUserResponse> */
export async function getAdminUsers(page = 0, size = 10) {
  const response = await apiClient.get(
    `/admin/users?page=${page}&size=${size}`
  );
  return response.data;
}

/** GET /admin/users/{userId} — single AdminUserResponse */
export async function getAdminUserDetail(userId) {
  const response = await apiClient.get(`/admin/users/${userId}`);
  return response.data;
}

// ── Transactions ───────────────────────────────────────────────────────────

/**
 * GET /admin/transactions — Spring Page<AdminTransactionMonitorResponse>
 * Query params: status, minAmount, maxAmount, fromDate, toDate, page, size
 * fromDate/toDate must be ISO-8601 Instant strings (e.g. "2025-10-01T00:00:00Z")
 */
export async function getAdminTransactions(filters = {}) {
  const params = new URLSearchParams();
  if (filters.status) params.append("status", filters.status);
  if (filters.minAmount) params.append("minAmount", filters.minAmount);
  if (filters.maxAmount) params.append("maxAmount", filters.maxAmount);
  if (filters.fromDate) params.append("fromDate", filters.fromDate);
  if (filters.toDate) params.append("toDate", filters.toDate);
  params.append("page", filters.page ?? 0);
  params.append("size", filters.size ?? 10);

  const response = await apiClient.get(
    `/admin/transactions?${params.toString()}`
  );
  return response.data;
}

// ── Settlement Monitor ─────────────────────────────────────────────────────

/**
 * GET /admin/settlement-monitor — SettlementMonitorResponse {
 *   pendingCount, dueTodayCount, dueTomorrowCount, overdueCount,
 *   pendingTransactions: [{ transactionId, amount, senderEmail, receiverEmail,
 *                           settlementDueDate, daysUntilDue }]
 * }
 */
export async function getSettlementMonitor(page = 0, size = 50) {
  const response = await apiClient.get(
    `/admin/settlement-monitor?page=${page}&size=${size}`
  );
  return response.data;
}

/** POST /admin/settlement-monitor/trigger/{transactionId} — 204 No Content */
export async function triggerSettlement(transactionId) {
  await apiClient.post(
    `/admin/settlement-monitor/trigger/${transactionId}`
  );
}

// ── Jobs ───────────────────────────────────────────────────────────────────

/**
 * GET /admin/jobs — AdminJobLogsResponse { jobs: [AdminJobLogResponse, ...] }
 * jobs array is nested under the "jobs" key.
 */
export async function getJobLogs() {
  const response = await apiClient.get("/admin/jobs");
  return response.data; // { jobs: [...] }
}

// ── Reconciliation ─────────────────────────────────────────────────────────

/**
 * GET /admin/reconciliation — AdminReconciliationResponse {
 *   totalSettledDebits, totalSettledCredits, difference, matched
 * }
 */
export async function getReconciliation() {
  const response = await apiClient.get("/admin/reconciliation");
  return response.data;
}

// ── Merchants ──────────────────────────────────────────────────────────────

/** POST /admin/merchants — returns MerchantResponse (201) */
export async function registerMerchant(data) {
  const response = await apiClient.post("/admin/merchants", data);
  return response.data;
}

/**
 * PATCH /admin/merchants/{merchantId}/status
 * Body: { status: "ACTIVE" | "INACTIVE" | "SUSPENDED" }
 */
export async function updateMerchantStatus(merchantId, status) {
  const response = await apiClient.patch(
    `/admin/merchants/${merchantId}/status`,
    { status }
  );
  return response.data;
}

// ── Audit Logs ─────────────────────────────────────────────────────────────

/** GET /admin/audit-logs — Spring Page<AdminAuditLogResponse> */
export async function getAuditLogs(page = 0, size = 20) {
  const response = await apiClient.get(
    `/admin/audit-logs?page=${page}&size=${size}`
  );
  return response.data;
}

// ── Fraud Alerts ───────────────────────────────────────────────────────────

/**
 * GET /admin/fraud-alerts — Spring Page<FraudAlertResponse>
 * Query params: status (PENDING | BLOCKED | DISMISSED), page, size
 */
export async function getFraudAlerts(status = null, page = 0, size = 20) {
  const params = new URLSearchParams();
  if (status) params.append("status", status);
  params.append("page", page);
  params.append("size", size);

  const response = await apiClient.get(
    `/admin/fraud-alerts?${params.toString()}`
  );
  return response.data;
}

/**
 * PATCH /admin/fraud-alerts/{alertId}/block
 * Body: { alertId (UUID), action: "BLOCK", remarks: "" }
 */
export async function blockFraudAlert(alertId, remarks = "") {
  await apiClient.patch(`/admin/fraud-alerts/${alertId}/block`, {
    alertId,
    action: "BLOCK",
    remarks,
  });
}

/**
 * PATCH /admin/fraud-alerts/{alertId}/dismiss
 * Body: { alertId (UUID), action: "DISMISS", remarks: "" }
 */
export async function dismissFraudAlert(alertId, remarks = "") {
  await apiClient.patch(`/admin/fraud-alerts/${alertId}/dismiss`, {
    alertId,
    action: "DISMISS",
    remarks,
  });
}
