/**
 * Transfer and Transaction API service.
 *
 * Key paths:
 *   POST /transfer        — initiate P2P transfer; body needs recipientId (UUID)
 *   GET  /transactions    — paginated history; Spring Page response
 *   GET  /transactions/mini  — latest 10; Spring Page response
 *   GET  /transactions/:id   — detail; TransferResponse (senderEmail/recipientEmail)
 *   GET  /transactions/statistics — personal stats (totalSent, totalReceived, monthlyTransactionCount)
 */

import apiClient from "../services/api.js";

/**
 * Search users by email or phone to get their UUID before sending money.
 * GET /users/search?q=<email or phone>
 * Returns { userId, name, maskedEmail, maskedPhone }
 */
export async function searchUser(query) {
  const response = await apiClient.get(
    `/users/search?q=${encodeURIComponent(query)}`
  );
  return response.data;
}

/**
 * Initiate a P2P transfer.
 * Body: { recipientId (UUID), amount, transactionPin, requestId }
 * Response: TransferResponse { transactionId, requestId, status, senderEmail,
 *   recipientEmail, amount, settlementDueDate, daysUntilSettlement }
 */
export async function initiateTransfer(data) {
  const response = await apiClient.post("/transfer", data);
  return response.data;
}

/**
 * Paginated transaction history.
 * Spring Page response: { content: [...], totalElements, totalPages, number, size }
 */
export async function getTransactions(filters = {}) {
  const params = new URLSearchParams();
  if (filters.type && filters.type !== "ALL") params.append("type", filters.type);
  if (filters.status && filters.status !== "ALL")
    params.append("status", filters.status);
  if (filters.from) params.append("from", filters.from);
  if (filters.to) params.append("to", filters.to);
  params.append("page", filters.page ?? 0);
  params.append("size", filters.size ?? 10);

  const response = await apiClient.get(`/transactions?${params.toString()}`);
  return response.data; // Spring Page: { content, totalElements, totalPages, number, size }
}

/**
 * Mini-statement — latest 10 transactions.
 * Returns Spring Page (same shape as getTransactions).
 */
export async function getMiniStatement() {
  const response = await apiClient.get("/transactions/mini");
  return response.data;
}

/**
 * Transaction detail.
 * Response: TransferResponse { transactionId, requestId, status,
 *   senderEmail, recipientEmail, amount, settlementDueDate, daysUntilSettlement }
 */
export async function getTransactionDetail(id) {
  const response = await apiClient.get(`/transactions/${id}`);
  return response.data;
}

/**
 * Personal statistics.
 * Response: { totalSent, totalReceived, monthlyTransactionCount }
 */
export async function getStatistics() {
  const response = await apiClient.get("/transactions/statistics");
  return response.data;
}
