import { createContext, useContext, useState, useCallback } from "react";
import apiClient from "../services/api.js";

/**
 * AuthContext — application-wide authentication state.
 *
 * Provides:
 *   currentUser  - decoded user object or null
 *   token        - raw JWT string or null
 *   login(email, password) - calls POST /auth/login, stores JWT, returns user
 *   logout()     - clears state and localStorage
 *   isAdmin()    - true when currentUser.role === "ADMIN"
 */
const AuthContext = createContext(null);

function parseJwtPayload(token) {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

function loadFromStorage() {
  const token = localStorage.getItem("jwt_token");
  const user = localStorage.getItem("current_user");
  return {
    token: token ?? null,
    currentUser: user ? JSON.parse(user) : null,
  };
}

export function AuthProvider({ children }) {
  const [{ token, currentUser }, setAuth] = useState(loadFromStorage);

  const login = useCallback(async (email, password) => {
    const response = await apiClient.post("/auth/login", { email, password });
    const { token: jwt } = response.data;
    const payload = parseJwtPayload(jwt);
    const user = {
      id: payload?.userId ?? payload?.sub,
      email: payload?.sub,
      role: payload?.role ?? "USER",
    };
    localStorage.setItem("jwt_token", jwt);
    localStorage.setItem("current_user", JSON.stringify(user));
    setAuth({ token: jwt, currentUser: user });
    return user;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("jwt_token");
    localStorage.removeItem("current_user");
    setAuth({ token: null, currentUser: null });
  }, []);

  const isAdmin = useCallback(
    () => currentUser?.role === "ADMIN",
    [currentUser]
  );

  return (
    <AuthContext.Provider value={{ currentUser, token, login, logout, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
}

/** Hook — throws if used outside AuthProvider. */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

export default AuthContext;
