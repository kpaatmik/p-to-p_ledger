/**
 * User Layout — wraps authenticated user pages.
 * Includes navigation bar, notification bell, and main content area.
 */

import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext.jsx";
import ROUTES from "../constants/routes.js";
import NotificationBell from "./NotificationBell.jsx";
import "../styles/layout.css";

export default function Layout({ children }) {
  const { currentUser, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate(ROUTES.LOGIN);
  };

  return (
    <div className="layout">
      <nav className="navbar">
        <div className="navbar-brand">
          <h1>💰 Payment Ledger</h1>
        </div>
        <div className="navbar-menu">
          <Link to={ROUTES.DASHBOARD}>Dashboard</Link>
          <Link to={ROUTES.SEND_MONEY}>Send Money</Link>
          <Link to={ROUTES.TRANSACTIONS}>History</Link>
          <Link to={ROUTES.MERCHANTS}>Check Please</Link>
          <Link to={ROUTES.STATISTICS}>Statistics</Link>
          {isAdmin() && <Link to={ROUTES.ADMIN_DASHBOARD}>Admin</Link>}
        </div>
        <div className="navbar-right">
          <NotificationBell />
          <div className="user-menu">
            <span>{currentUser?.email}</span>
            <button onClick={handleLogout} className="btn btn-small">
              Logout
            </button>
          </div>
        </div>
      </nav>
      <main className="main-content">{children}</main>
    </div>
  );
}
