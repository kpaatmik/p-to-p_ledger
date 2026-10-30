/**
 * Authentication API service.
 */

import apiClient from "../services/api.js";

export async function registerUser(data) {
  const response = await apiClient.post("/auth/register", data);
  return response.data;
}

export async function loginUser(email, password) {
  const response = await apiClient.post("/auth/login", {
    email,
    password,
  });
  return response.data;
}
