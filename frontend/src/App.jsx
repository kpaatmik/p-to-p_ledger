import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import ROUTES from "./constants/routes.js";

// Public pages
import LoginPage from "./pages/LoginPage.jsx";
import RegisterPage from "./pages/RegisterPage.jsx";

// User pages
import WalletDashboard from "./pages/WalletDashboard.jsx";
import SendMoneyPage from "./pages/SendMoneyPage.jsx";
import TransactionHistoryPage from "./pages/TransactionHistoryPage.jsx";
import TransactionDetailPage from "./pages/TransactionDetailPage.jsx";
import NotificationsPage from "./pages/NotificationsPage.jsx";
import StatisticsPage from "./pages/StatisticsPage.jsx";
import MerchantListPage from "./pages/MerchantListPage.jsx";
import CreateReservationPage from "./pages/CreateReservationPage.jsx";
import ReservationsPage from "./pages/ReservationsPage.jsx";
import MerchantPaymentPage from "./pages/MerchantPaymentPage.jsx";

// Admin pages
import AdminDashboard from "./pages/admin/AdminDashboard.jsx";
import AdminTransactionsPage from "./pages/admin/AdminTransactionsPage.jsx";
import AdminUsersPage from "./pages/admin/AdminUsersPage.jsx";
import AdminJobsPage from "./pages/admin/AdminJobsPage.jsx";
import AdminFraudReviewPage from "./pages/admin/AdminFraudReviewPage.jsx";
import AdminSettlementPage from "./pages/admin/AdminSettlementPage.jsx";
import AdminReconciliationPage from "./pages/admin/AdminReconciliationPage.jsx";

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
                <WalletDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.SEND_MONEY}
            element={
              <ProtectedRoute>
                <SendMoneyPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.TRANSACTIONS}
            element={
              <ProtectedRoute>
                <TransactionHistoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.TRANSACTION_DETAIL}
            element={
              <ProtectedRoute>
                <TransactionDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.NOTIFICATIONS}
            element={
              <ProtectedRoute>
                <NotificationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.STATISTICS}
            element={
              <ProtectedRoute>
                <StatisticsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.MERCHANTS}
            element={
              <ProtectedRoute>
                <MerchantListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.CREATE_RESERVATION}
            element={
              <ProtectedRoute>
                <CreateReservationPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.RESERVATIONS}
            element={
              <ProtectedRoute>
                <ReservationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.MERCHANT_PAYMENT}
            element={
              <ProtectedRoute>
                <MerchantPaymentPage />
              </ProtectedRoute>
            }
          />

          {/* Admin routes */}
          <Route
            path={ROUTES.ADMIN_DASHBOARD}
            element={
              <ProtectedRoute adminOnly>
                <AdminDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.ADMIN_USERS}
            element={
              <ProtectedRoute adminOnly>
                <AdminUsersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.ADMIN_TRANSACTIONS}
            element={
              <ProtectedRoute adminOnly>
                <AdminTransactionsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.ADMIN_FRAUD}
            element={
              <ProtectedRoute adminOnly>
                <AdminFraudReviewPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.ADMIN_SETTLEMENT}
            element={
              <ProtectedRoute adminOnly>
                <AdminSettlementPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.ADMIN_JOBS}
            element={
              <ProtectedRoute adminOnly>
                <AdminJobsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.ADMIN_RECONCILIATION}
            element={
              <ProtectedRoute adminOnly>
                <AdminReconciliationPage />
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