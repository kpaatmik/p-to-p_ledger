import axios from "axios";

/**
 * Pre-configured Axios instance.
 *
 * Base URL: reads VITE_API_BASE_URL from the environment (set in .env.local).
 * Falls back to the Vite dev-server proxy path "/api" when not set.
 *
 * Request interceptor: attaches "Authorization: Bearer <token>" to every
 * request when a JWT is present in localStorage.
 *
 * Response interceptor: on 401, clears the stored token so the user is
 * redirected to login by the ProtectedRoute wrapper.
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 15000,
});

// Attach JWT Bearer token to every outgoing request.
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("jwt_token");
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// On 401 responses, clear the stale token.
// The ProtectedRoute component handles the redirect to /login.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("jwt_token");
      localStorage.removeItem("current_user");
    }
    return Promise.reject(error);
  }
);

export default apiClient;
