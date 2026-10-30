/**
 * Merchant and Check Please API service.
 *
 * Key differences from initial stub:
 *   GET /reservations returns List<ReservationResponse> (NOT Page) — no .content wrapper
 *   POST /reservations — expiresAt must be ISO-8601 Instant string (e.g. "2025-12-31T18:30:00Z")
 *   DELETE /reservations/{id} — returns 204 No Content
 */

import apiClient from "../services/api.js";

/** GET /merchants?search=... — returns List<MerchantResponse> */
export async function getMerchants(search = "") {
  const params = search ? `?search=${encodeURIComponent(search)}` : "";
  const response = await apiClient.get(`/merchants${params}`);
  return response.data; // Array directly
}

/**
 * POST /reservations
 * Body: { merchantId (UUID), maxAmount, expiresAt (ISO-8601 Instant string) }
 * Response 201: ReservationResponse
 */
export async function createReservation(data) {
  const response = await apiClient.post("/reservations", data);
  return response.data;
}

/**
 * GET /reservations?status=...&page=...&size=...
 * Returns List<ReservationResponse> — NOT a Spring Page. No .content wrapper.
 */
export async function getReservations(status = null, page = 0, size = 20) {
  const params = new URLSearchParams();
  if (status) params.append("status", status);
  params.append("page", page);
  params.append("size", size);

  const response = await apiClient.get(`/reservations?${params.toString()}`);
  return response.data; // Plain array
}

/** GET /reservations/{id} — single ReservationResponse */
export async function getReservationDetail(id) {
  const response = await apiClient.get(`/reservations/${id}`);
  return response.data;
}

/** DELETE /reservations/{id} — 204 No Content */
export async function cancelReservation(id) {
  await apiClient.delete(`/reservations/${id}`);
}

/**
 * POST /merchant-payments
 * Body: { requestId (String), reservationId (UUID), amount, transactionPin (4 digits) }
 * Response 201: MerchantPaymentResponse
 */
export async function captureMerchantPayment(data) {
  const response = await apiClient.post("/merchant-payments", data);
  return response.data;
}
