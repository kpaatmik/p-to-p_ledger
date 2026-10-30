import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext.jsx";
import ROUTES from "../constants/routes.js";

/**
 * ProtectedRoute — wraps any route that requires authentication.
 *
 * Usage:
 *   <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
 *
 * adminOnly prop:
 *   <Route path="/admin" element={<ProtectedRoute adminOnly><AdminDashboard /></ProtectedRoute>} />
 *
 * Redirects:
 *   - Unauthenticated users  → /login  (with the attempted path saved in location.state)
 *   - Non-admin on adminOnly → /dashboard
 */
export default function ProtectedRoute({ children, adminOnly = false }) {
  const { currentUser, isAdmin } = useAuth();
  const location = useLocation();

  if (!currentUser) {
    return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />;
  }

  if (adminOnly && !isAdmin()) {
    return <Navigate to={ROUTES.DASHBOARD} replace />;
  }

  return children;
}