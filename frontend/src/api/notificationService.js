/**
 * Notification API service.
 */

import apiClient from "../services/api.js";

export async function getNotifications(page = 0, size = 20) {
  const response = await apiClient.get(
    `/notifications?page=${page}&size=${size}`
  );
  return response.data;
}

export async function markNotificationAsRead(id) {
  const response = await apiClient.patch(`/notifications/${id}/read`);
  return response.data;
}

export async function getUnreadCount() {
  const response = await apiClient.get("/notifications/unread-count");
  return response.data;
}
