import { useAuth } from "../contexts/AuthContext.jsx";

/**
 * DashboardPage — Phase 1 placeholder.
 * Full wallet dashboard with balance breakdown and mini-statement
 * is implemented in T-121 (Phase 10).
 */
export default function DashboardPage() {
  const { currentUser, logout } = useAuth();

  return (
    <div style={{ maxWidth: 600, margin: "40px auto", padding: "0 16px" }}>
      <h1>Dashboard</h1>
      <p>Welcome, {currentUser?.email}</p>
      <p>
        <em>Wallet and transaction features are implemented in Phase 2–9.</em>
      </p>
      <button onClick={logout} style={{ marginTop: 16, padding: "8px 16px" }}>
        Sign out
      </button>
    </div>
  );
}