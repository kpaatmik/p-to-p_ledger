/**
 * Wallet API service.
 *
 * Admin freeze endpoint lives under /wallet/admin/wallets/{id}/status
 * per WalletController @RequestMapping("/wallet") class prefix.
 */

import apiClient from "../services/api.js";

export async function getWallet() {
  const response = await apiClient.get("/wallet");
  return response.data;
}

export async function loadMoney(amount) {
  const response = await apiClient.post("/wallet/load", { amount });
  return response.data;
}

export async function reactivateWallet() {
  const response = await apiClient.patch("/wallet/reactivate");
  return response.data;
}

// Admin wallet actions — path: /wallet/admin/wallets/{id}/status
export async function freezeWallet(walletId) {
  const response = await apiClient.patch(
    `/wallet/admin/wallets/${walletId}/status`,
    { action: "FREEZE" }
  );
  return response.data;
}

export async function unfreezeWallet(walletId) {
  const response = await apiClient.patch(
    `/wallet/admin/wallets/${walletId}/status`,
    { action: "UNFREEZE" }
  );
  return response.data;
}
