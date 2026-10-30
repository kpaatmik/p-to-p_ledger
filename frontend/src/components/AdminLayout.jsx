/**
 * Admin Layout — wraps admin pages.
 * Includes sidebar navigation and main content area.
 */

import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext.jsx";
import ROUTES from "../constants/routes.js";
import "../styles/admin-layout.css";

export default function AdminLayout({ children }) {
  const { currentUser, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  useEffect(() => {
    if (!isAdmin()) {
      navigate(ROUTES.DASHBOARD);
    }
  }, [isAdmin, navigate]);

  const handleLogout = () => {
    logout();
    navigate(ROUTES.LOGIN);
  };

  return (
    <div className="admin-layout">
      <nav className={`admin-sidebar ${sidebarOpen ? "open" : "closed"}`}>
        <div className="sidebar-header">
          <h2>Admin</h2>
          <button
            className="toggle-btn"
            onClick={() => setSidebarOpen(!sidebarOpen)}
          >
            ☰
          </button>
        </div>
        <ul className="sidebar-menu">
          <li>
            <Link to={ROUTES.ADMIN_DASHBOARD}>📊 Dashboard</Link>
          </li>
          <li>
            <Link to={ROUTES.ADMIN_USERS}>👥 Users</Link>
          </li>
          <li>
            <Link to={ROUTES.ADMIN_TRANSACTIONS}>💳 Transactions</Link>
          </li>
          <li>
            <Link to={ROUTES.ADMIN_FRAUD}>🚨 Fraud Review</Link>
          </li>
          <li>
            <Link to={ROUTES.ADMIN_SETTLEMENT}>⏳ Settlement</Link>
          </li>
          <li>
            <Link to={ROUTES.ADMIN_JOBS}>⚙️ Jobs</Link>
          </li>
          <li>
            <Link to={ROUTES.ADMIN_RECONCILIATION}>📋 Reconciliation</Link>
          </li>
          <li>
            <Link to={ROUTES.ADMIN_MERCHANTS}>🏪 Merchants</Link>
          </li>
        </ul>
        <div className="sidebar-footer">
          <p>{currentUser?.email}</p>
          <button onClick={handleLogout} className="btn btn-small">
            Logout
          </button>
        </div>
      </nav>
      <main className="admin-main-content">{children}</main>
    </div>
  );
}
