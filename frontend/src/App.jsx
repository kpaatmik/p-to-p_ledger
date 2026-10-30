import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import ROUTES from "./constants/routes.js";

// Placeholder page components — replaced with real implementations in Phase 10.
import LoginPage from "./pages/LoginPage.jsx";
import RegisterPage from "./pages/RegisterPage.jsx";
import DashboardPage from "./pages/DashboardPage.jsx";

/**
 * App — root component.
 *
 * Wraps the entire tree with AuthProvider so every component can access
 * authentication state via useAuth().
 *
 * Route structure:
 *   /              → redirect to /dashboard
 *   /login         → public
 *   /register      → public
 *   /dashboard     → protected (USER)
 *   /admin/*       → protected (ADMIN only)  [wired up in Phase 10]
 *
 * All other Phase 10 routes are added here as their pages are implemented.
 */
export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public routes */}
          <Route path={ROUTES.LOGIN} element={<LoginPage />} />
          <Route path={ROUTES.REGISTER} element={<RegisterPage />} />

          {/* Protected user routes */}
          <Route
            path={ROUTES.DASHBOARD}
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />

          {/* Default redirect */}
          <Route path="/" element={<Navigate to={ROUTES.DASHBOARD} replace />} />

          {/* Catch-all */}
          <Route path="*" element={<Navigate to={ROUTES.DASHBOARD} replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}