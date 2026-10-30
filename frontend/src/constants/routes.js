/**
 * Centralised route path constants.
 * Import these in every component instead of using raw strings.
 */
const ROUTES = {
  // Auth
  LOGIN: "/login",
  REGISTER: "/register",

  // User
  DASHBOARD: "/dashboard",
  SEND_MONEY: "/send",
  TRANSACTIONS: "/transactions",
  TRANSACTION_DETAIL: "/transactions/:id",
  NOTIFICATIONS: "/notifications",
  STATISTICS: "/statistics",

  // Check Please
  MERCHANTS: "/merchants",
  RESERVATIONS: "/reservations",
  CREATE_RESERVATION: "/reservations/new",
  MERCHANT_PAYMENT: "/merchant-payment",

  // Admin
  ADMIN_DASHBOARD: "/admin",
  ADMIN_USERS: "/admin/users",
  ADMIN_TRANSACTIONS: "/admin/transactions",
  ADMIN_FRAUD: "/admin/fraud",
  ADMIN_SETTLEMENT: "/admin/settlement",
  ADMIN_JOBS: "/admin/jobs",
  ADMIN_RECONCILIATION: "/admin/reconciliation",
  ADMIN_MERCHANTS: "/admin/merchants",
};

export default ROUTES;
